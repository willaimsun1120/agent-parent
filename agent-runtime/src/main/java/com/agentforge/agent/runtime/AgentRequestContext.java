package com.agentforge.agent.runtime;

import com.agentforge.agent.runtime.AgentExecutionSupport.PreparedChat;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * 单次 HTTP 请求的 Agent 上下文（RequestScope）。
 *
 * <p>在 {@link AgentExecutionSupport#prepareChat} 时绑定 traceId、sessionId、category，
 * 业务工具（如 OrderTools）通过 {@link AgentRequestContextHolder} 读取，
 * 用于 HITL 写操作关联 session、RAG 按 category 过滤等。
 */
@Component
@RequestScope
public class AgentRequestContext {
    /** 当前会话 ID */
    private String sessionId;
    /** 当前链路追踪 ID */
    private String traceId;
    /** RAG 检索 category，可为 null */
    private String category;
    /** 已绑定的预处理结果，用于同请求内复用 */
    private PreparedChat preparedChat;

    /**
     * @return 会话 ID
     */
    public String sessionId() {
        return sessionId;
    }

    /**
     * @return 链路追踪 ID
     */
    public String traceId() {
        return traceId;
    }

    /**
     * @return RAG category，未设置时为 null
     */
    public String category() {
        return category;
    }

    /**
     * @return 已绑定的 PreparedChat，未 prepare 时为 null
     */
    public PreparedChat preparedChat() {
        return preparedChat;
    }

    /**
     * 绑定预处理结果并同步 sessionId、traceId、category。
     *
     * @param preparedChat {@link AgentExecutionSupport#prepareChat} 的返回值
     */
    public void bindPreparedChat(PreparedChat preparedChat) {
        this.preparedChat = preparedChat;
        this.sessionId = preparedChat.sessionId();
        this.traceId = preparedChat.traceId();
        this.category = preparedChat.category().orElse(null);
    }
}
