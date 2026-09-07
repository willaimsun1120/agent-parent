package com.agentforge.agent.mode.agentscope;

import com.agentforge.agent.action.AgentActionService;
import com.agentforge.agent.clarification.ClarificationService;
import com.agentforge.agent.core.api.AgentChatRequest;
import com.agentforge.agent.core.api.AgentChatResponse;
import com.agentforge.agent.core.config.DashScopeProperties;
import com.agentforge.agent.core.mode.AgentMode;
import com.agentforge.agent.core.mode.AgentModeHandler;
import com.agentforge.agent.core.spi.FrameworkToolRegistrar;
import com.agentforge.agent.mode.core.AgentFallbackSupport;
import com.agentforge.agent.mode.core.FallbackChatSupport;
import com.agentforge.agent.mode.core.FrameworkModeSupport;
import com.agentforge.agent.prompt.PromptTemplateProvider;
import com.agentforge.agent.runtime.AgentExecutionSupport;
import com.agentforge.agent.runtime.FrameworkAgentSupport;
import com.agentforge.agent.runtime.ListToolCallCollector;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.tool.Toolkit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AgentScope 模式：ReActAgent + Toolkit，模型自主多轮推理与选工具。
 *
 * <p>继承 {@link FrameworkModeSupport} 公共骨架，仅实现框架特定的推理逻辑。
 * AgentScope 对「不调用工具」更敏感，首轮无工具时带强制提示重试一次，仍失败则降级 manual。
 */
@Service
public class AgentScopeAgentModeHandler extends FrameworkModeSupport implements AgentModeHandler {
    private static final Logger log = LoggerFactory.getLogger(AgentScopeAgentModeHandler.class);
    private static final String FRAMEWORK_NAME = "AgentScope";

    private final DashScopeProperties dashScopeProperties;
    private final FrameworkToolRegistrar toolRegistrar;
    private final AgentScopeRuntimeInfo runtimeInfo;
    private final PromptTemplateProvider promptTemplateProvider;

    public AgentScopeAgentModeHandler(DashScopeProperties dashScopeProperties,
                                      FrameworkToolRegistrar toolRegistrar,
                                      AgentActionService actionService,
                                      FallbackChatSupport fallbackChat,
                                      AgentExecutionSupport executionSupport,
                                      AgentFallbackSupport fallbackSupport,
                                      ClarificationService clarificationService,
                                      AgentScopeRuntimeInfo runtimeInfo,
                                      PromptTemplateProvider promptTemplateProvider) {
        super(executionSupport, actionService, fallbackChat, fallbackSupport, clarificationService);
        this.dashScopeProperties = dashScopeProperties;
        this.toolRegistrar = toolRegistrar;
        this.runtimeInfo = runtimeInfo;
        this.promptTemplateProvider = promptTemplateProvider;
    }

    @Override
    public AgentMode mode() {
        return AgentMode.AGENTSCOPE;
    }

    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        return doChat(request, FRAMEWORK_NAME, dashScopeProperties.enabled());
    }

    @Override
    protected AgentChatResponse invokeFramework(AgentExecutionSupport.PreparedChat prepared,
                                                 long started, String frameworkName) {
        String traceId = prepared.traceId();
        String question = prepared.question();

        ListToolCallCollector collector = new ListToolCallCollector();
        Msg result = invokeAgent(collector, prepared,
            promptTemplateProvider.frameworkUserPrompt(question, prepared.history()));

        if (collector.toolCalls().isEmpty() && toolRegistrar.expectsToolCalls(question)) {
            log.warn("AgentScope 首轮未调用工具 traceId={}，带强制提示重试", traceId);
            result = invokeAgent(collector, prepared,
                promptTemplateProvider.frameworkToolRetryPrompt(question, prepared.history()));
        }

        if (collector.toolCalls().isEmpty() && toolRegistrar.expectsToolCalls(question)) {
            log.warn("AgentScope 重试后仍未调用工具 traceId={}，降级为手写模式", traceId);
            return fallbackSupport.fallbackToManual(fallbackChat,
                new AgentChatRequest(prepared.question(), null, null, null),
                FRAMEWORK_NAME + " 未调用工具，已降级到手写模式", FRAMEWORK_NAME);
        }

        String answer = result == null ? "" : result.getTextContent();
        if (answer == null || answer.isBlank()) {
            answer = "AgentScope 已完成推理，但没有返回可展示文本。请查看后台日志确认模型响应。";
        }

        List<String> toolCalls = new ArrayList<>(collector.toolCalls());
        log.info("AgentScope 模式处理完成 traceId={} toolCalls={}", traceId, toolCalls.size());
        return buildFrameworkResponse(prepared, started, answer,
            toolCalls.isEmpty() ? toolRegistrar.toolNamesHint() : toolCalls,
            FrameworkAgentSupport.ragHitHints(toolCalls, FRAMEWORK_NAME),
            "AgentScope ReActAgent + " + runtimeInfo.harnessClassName());
    }

    private Msg invokeAgent(ListToolCallCollector collector, AgentExecutionSupport.PreparedChat prepared, String userPrompt) {
        try (ReActAgent agent = buildAgent(collector, prepared)) {
            return agent.call(List.of(new UserMessage(userPrompt))).block();
        }
    }

    private ReActAgent buildAgent(ListToolCallCollector collector, AgentExecutionSupport.PreparedChat prepared) {
        FrameworkToolRegistrar.ToolContext toolContext = new FrameworkToolRegistrar.ToolContext(
            prepared.sessionId(), prepared.traceId(), prepared.turnIndex(), prepared.question(), prepared.category());
        Object toolAdapter = toolRegistrar.createAgentScopeTools(collector, toolContext);
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(toolAdapter);
        toolkit.setChunkCallback((toolUse, toolResult) -> {
            String input = toolUse.getInput() == null ? "{}" : toolUse.getInput().toString();
            collector.recordCall(toolUse.getName(), input);
            log.info("AgentScope ReAct 调用工具 toolName={} input={}", toolUse.getName(), input);
        });

        DashScopeChatModel model = DashScopeChatModel.builder()
            .apiKey(dashScopeProperties.apiKey())
            .modelName(dashScopeProperties.chatModel())
            .stream(false)
            .enableThinking(false)
            .build();

        return ReActAgent.builder()
            .name("framework-agent")
            .sysPrompt(promptTemplateProvider.getFrameworkSystem())
            .model(model)
            .toolkit(toolkit)
            .hook(new AgentScopeToolCallingHook())
            .maxIters(8)
            .generateOptions(GenerateOptions.builder()
                .temperature(0.2)
                .parallelToolCalls(false)
                .build())
            .build();
    }
}
