package com.agentforge.agent.mode.core;

import com.agentforge.agent.core.api.AgentChatRequest;
import com.agentforge.agent.core.api.AgentChatResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 框架模式统一降级组件。
 *
 * <p>LangChain4j / AgentScope 在 API Key 缺失、未调工具、异常等场景下转 manual 执行，
 * 并在响应 {@code runtime} 与 {@code toolCalls} 中标注降级原因，便于排查。
 *
 * <p>通过 {@link FallbackChatSupport} 接口解耦对 agent-mode-manual 的编译期依赖。
 */
@Component
public class AgentFallbackSupport {

    /** 调用 fallback 的 chatAsFallback（不重复计 metrics），并合并降级说明到 toolCalls。 */
    public AgentChatResponse fallbackToManual(FallbackChatSupport fallbackChat,
                                              AgentChatRequest request,
                                              String reason,
                                              String sourceMode) {
        AgentChatResponse response = fallbackChat.chatAsFallback(request);
        List<String> toolCalls = new ArrayList<>();
        toolCalls.add(reason);
        toolCalls.addAll(response.toolCalls());
        return new AgentChatResponse(
            response.traceId(),
            response.question(),
            response.answer(),
            response.durationMs(),
            toolCalls,
            response.ragHits(),
            sourceMode + " fallback -> " + response.runtime(),
            response.sessionId(),
            response.turnIndex(),
            response.pendingActions(),
            response.ragHitDetails()
        );
    }
}
