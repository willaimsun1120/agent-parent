package com.agentforge.agent.mode.langchain4j;

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
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * LangChain4j 模式：通过 AiServices + QwenChatModel 让模型自动选择工具。
 *
 * <p>继承 {@link FrameworkModeSupport} 公共骨架，仅实现框架特定的推理逻辑。
 * 降级策略由基类统一处理。
 */
@Service
public class LangChain4jAgentModeHandler extends FrameworkModeSupport implements AgentModeHandler {
    private static final Logger log = LoggerFactory.getLogger(LangChain4jAgentModeHandler.class);
    private static final String FRAMEWORK_NAME = "LangChain4j";

    private final DashScopeProperties dashScopeProperties;
    private final FrameworkToolRegistrar toolRegistrar;
    private final PromptTemplateProvider promptTemplateProvider;

    public LangChain4jAgentModeHandler(DashScopeProperties dashScopeProperties,
                                       FrameworkToolRegistrar toolRegistrar,
                                       AgentActionService actionService,
                                       FallbackChatSupport fallbackChat,
                                       AgentExecutionSupport executionSupport,
                                       AgentFallbackSupport fallbackSupport,
                                       ClarificationService clarificationService,
                                       PromptTemplateProvider promptTemplateProvider) {
        super(executionSupport, actionService, fallbackChat, fallbackSupport, clarificationService);
        this.dashScopeProperties = dashScopeProperties;
        this.toolRegistrar = toolRegistrar;
        this.promptTemplateProvider = promptTemplateProvider;
    }

    @Override
    public AgentMode mode() {
        return AgentMode.LANGCHAIN4J;
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
        FrameworkToolRegistrar.ToolContext toolContext = new FrameworkToolRegistrar.ToolContext(
            prepared.sessionId(), traceId, prepared.turnIndex(), question, prepared.category());
        Object tools = toolRegistrar.createLangChain4jTools(collector, toolContext);
        OrderCustomerServiceAssistant assistant = buildAssistant(tools);
        String userPrompt = promptTemplateProvider.frameworkUserPrompt(question, prepared.history());
        String answer = assistant.chat(userPrompt);
        List<String> toolCalls = new ArrayList<>(collector.toolCalls());

        if (toolCalls.isEmpty() && toolRegistrar.expectsToolCalls(question)) {
            log.warn("LangChain4j 未调用任何工具 traceId={}，降级为手写模式", traceId);
            return fallbackSupport.fallbackToManual(fallbackChat,
                new AgentChatRequest(prepared.question(), null, null, null),
                FRAMEWORK_NAME + " 未调用工具，已降级到手写模式", FRAMEWORK_NAME);
        }

        if (answer == null || answer.isBlank()) {
            answer = "LangChain4j 已完成推理，但没有返回可展示文本。请查看后台日志确认模型响应。";
        }

        log.info("LangChain4j 模式处理完成 traceId={} toolCalls={}", traceId, toolCalls.size());
        return buildFrameworkResponse(prepared, started, answer,
            toolCalls.isEmpty() ? toolRegistrar.toolNamesHint() : toolCalls,
            FrameworkAgentSupport.ragHitHints(toolCalls, FRAMEWORK_NAME),
            "LangChain4j AiServices + QwenChatModel");
    }

    private OrderCustomerServiceAssistant buildAssistant(Object tools) {
        ChatLanguageModel model = new QwenToolResultCompatChatModel(QwenChatModel.builder()
            .apiKey(dashScopeProperties.apiKey())
            .modelName(dashScopeProperties.chatModel())
            .temperature(0.2f)
            .build());

        return AiServices.builder(OrderCustomerServiceAssistant.class)
            .chatLanguageModel(model)
            .tools(tools)
            .systemMessageProvider(memoryId -> promptTemplateProvider.getFrameworkSystem())
            .build();
    }

    interface OrderCustomerServiceAssistant {
        String chat(@UserMessage String question);
    }
}
