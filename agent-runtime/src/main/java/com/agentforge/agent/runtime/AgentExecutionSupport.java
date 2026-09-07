package com.agentforge.agent.runtime;

import com.agentforge.agent.core.api.AgentChatRequest;
import com.agentforge.agent.core.api.AgentChatResponse;
import com.agentforge.agent.core.api.AgentPendingActionView;
import com.agentforge.agent.core.api.AgentRagHitView;
import com.agentforge.agent.conversation.AgentConversationService;
import com.agentforge.agent.observability.AgentMetrics;
import com.agentforge.agent.observability.AgentSession;
import com.agentforge.agent.observability.AgentSessionMapper;
import com.agentforge.agent.core.spi.RagCategoryResolver;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.stereotype.Component;

/**
 * 各 Agent 模式共享的执行支撑组件。
 *
 * <p>三种模式（manual / langchain4j / agentscope）在「准备上下文 → 落库 → 组装响应」阶段
 * 都依赖本类，避免重复实现会话、指标、traceId 等横切逻辑。
 *
 * <p>关键方法：
 * <ul>
 *   <li>{@link #prepareChat} — 生成 traceId、sessionId、turnIndex、历史对话、RAG category</li>
 *   <li>{@link #saveSession} — 写入 agent_sessions 与 conversation_turns</li>
 *   <li>{@link #buildResponse} — 统一构造 {@link com.agentforge.agent.core.api.AgentChatResponse}</li>
 * </ul>
 */
@Component
public class AgentExecutionSupport {
    private static final Logger log = LoggerFactory.getLogger(AgentExecutionSupport.class);

    private final AgentSessionMapper sessionMapper;
    private final AgentMetrics metrics;
    private final AgentConversationService conversationService;
    private final AgentRequestContext requestContext;

    private final RagCategoryResolver ragCategoryResolver;

    /**
     * @param sessionMapper 会话落库 Mapper
     * @param metrics 问答/工具/RAG 指标采集
     * @param conversationService 多轮对话读写
     * @param requestContext 当前 HTTP 请求上下文
     * @param ragCategoryResolver RAG category 解析 SPI
     */
    public AgentExecutionSupport(AgentSessionMapper sessionMapper,
                                 AgentMetrics metrics,
                                 AgentConversationService conversationService,
                                 AgentRequestContext requestContext,
                                 RagCategoryResolver ragCategoryResolver) {
        this.sessionMapper = sessionMapper;
        this.metrics = metrics;
        this.conversationService = conversationService;
        this.requestContext = requestContext;
        this.ragCategoryResolver = ragCategoryResolver;
    }

    /**
     * 准备单次问答的上下文。同一 HTTP 请求内若已 prepare 过（如框架降级到 manual），直接复用，避免重复记轮次。
     *
     * @param request 客户端问答请求
     * @param tracePrefix traceId 前缀（MDC 无值时使用）
     * @param startedAt 请求开始时间戳，用于生成 fallback traceId
     * @return 预处理结果，含 traceId、sessionId、轮次、历史等
     */
    public PreparedChat prepareChat(AgentChatRequest request, String tracePrefix, long startedAt) {
        Optional<PreparedChat> reused = AgentRequestContextHolder.peek();
        if (reused.isPresent()) {
            return reused.get();
        }
        PreparedChat fromRequestScope = readPreparedFromRequestScope().orElse(null);
        if (fromRequestScope != null) {
            AgentRequestContextHolder.set(fromRequestScope);
            return fromRequestScope;
        }

        // 优先使用 TraceIdFilter 写入 MDC 的 traceId
        String traceId = Optional.ofNullable(MDC.get("traceId")).orElse(tracePrefix + "-" + startedAt);
        String sessionId = conversationService.resolveSessionId(request == null ? null : request.sessionId());
        int turnIndex = conversationService.nextTurnIndex(sessionId);
        String question = sanitizeQuestion(request);
        Optional<String> category = ragCategoryResolver.resolve(
            request == null ? null : request.category(),
            question
        );
        PreparedChat prepared = new PreparedChat(traceId, sessionId, turnIndex, question, category,
            conversationService.formatHistory(sessionId));

        // SSE 异步线程里 request scope 已失效，绑定失败时只依赖 ThreadLocal Holder
        bindPreparedToRequestScope(prepared);
        AgentRequestContextHolder.set(prepared);
        conversationService.appendTurn(sessionId, turnIndex, traceId, "user", question);
        return prepared;
    }

    private Optional<PreparedChat> readPreparedFromRequestScope() {
        try {
            return Optional.ofNullable(requestContext.preparedChat());
        } catch (BeanCreationException | IllegalStateException ex) {
            log.debug("request scope 不可用，跳过 AgentRequestContext 复用: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private void bindPreparedToRequestScope(PreparedChat prepared) {
        try {
            requestContext.bindPreparedChat(prepared);
        } catch (BeanCreationException | IllegalStateException ex) {
            log.debug("request scope 不可用，仅使用 ThreadLocal 上下文: {}", ex.getMessage());
        }
    }

    /**
     * 解析当前请求的 traceId。
     *
     * @param prefix MDC 无值时的前缀
     * @param startedAt 时间戳后缀
     * @return traceId 字符串
     */
    public String traceId(String prefix, long startedAt) {
        return Optional.ofNullable(MDC.get("traceId")).orElse(prefix + "-" + startedAt);
    }

    /**
     * 清洗用户问题文本（去空白、防空）。
     *
     * @param request 问答请求，可为 null
     * @return 非 null 的 trimmed 问题
     */
    public String sanitizeQuestion(AgentChatRequest request) {
        String raw = request == null ? "" : request.question();
        return raw == null ? "" : raw.trim();
    }

    /** 问答开始时递增 chat 计数器。 */
    public void beginChatMetrics() {
        metrics.incrementChat();
    }

    /**
     * 记录单次问答耗时。
     *
     * @param durationMs 耗时毫秒
     */
    public void recordChatDuration(long durationMs) {
        metrics.recordChatDuration(durationMs);
    }

    /**
     * 持久化问答会话并追加 assistant 轮次。
     *
     * @param traceId 链路追踪 ID
     * @param sessionId 会话 ID
     * @param turnIndex 轮次序号
     * @param question 用户问题
     * @param answer 模型回答（会先清洗引用脚注）
     * @param durationMs 耗时毫秒
     */
    public void saveSession(String traceId, String sessionId, int turnIndex, String question, String answer, long durationMs) {
        String sanitized = sanitizeAnswer(answer);
        AgentSession session = new AgentSession(traceId, sessionId, turnIndex, question, sanitized, durationMs);
        sessionMapper.insert(session);
        conversationService.appendTurn(sessionId, turnIndex, traceId, "assistant", sanitized);
    }

    /**
     * 组装完整问答响应（含 session、待确认写操作）。
     *
     * @param traceId        链路追踪 ID
     * @param sessionId      会话 ID
     * @param turnIndex      轮次序号
     * @param question       用户问题
     * @param answer         模型回答
     * @param durationMs     耗时毫秒
     * @param toolCalls      工具调用摘要列表
     * @param ragHits        RAG 命中摘要列表
     * @param runtime        运行时标识（manual / langchain4j / agentscope）
     * @param pendingActions 待人工确认的写操作
     * @return 统一响应 DTO
     */
    public AgentChatResponse buildResponse(String traceId, String sessionId, int turnIndex, String question, String answer,
                                           long durationMs, List<String> toolCalls,
                                           List<String> ragHits, String runtime,
                                           List<AgentPendingActionView> pendingActions) {
        return buildResponse(traceId, sessionId, turnIndex, question, answer, durationMs,
            toolCalls, ragHits, runtime, pendingActions, List.of());
    }

    /**
     * 组装完整问答响应（含结构化 RAG 命中详情）。
     */
    public AgentChatResponse buildResponse(String traceId, String sessionId, int turnIndex, String question, String answer,
                                           long durationMs, List<String> toolCalls,
                                           List<String> ragHits, String runtime,
                                           List<AgentPendingActionView> pendingActions,
                                           List<AgentRagHitView> ragHitDetails) {
        return new AgentChatResponse(
            traceId, question, sanitizeAnswer(answer), durationMs, toolCalls, ragHits, runtime,
            sessionId, turnIndex,
            pendingActions == null ? List.of() : pendingActions,
            ragHitDetails == null ? List.of() : ragHitDetails
        );
    }

    /**
     * 清洗模型回答（去除末尾引用脚注等）。
     *
     * @param answer 原始回答
     * @return 清洗后的回答
     */
    public String sanitizeAnswer(String answer) {
        return CustomerServicePlainFormatter.stripCitationFooter(answer);
    }

    /**
     * 单次 chat 的预处理结果，贯穿 Handler 执行全过程。
     *
     * @param traceId 链路追踪 ID
     * @param sessionId 会话 ID
     * @param turnIndex 当前轮次序号
     * @param question 清洗后的用户问题
     * @param category RAG 检索 category 过滤，由 RagCategoryResolver SPI 解析
     * @param history 格式化后的最近 N 轮对话，注入 LLM prompt
     */
    public record PreparedChat(
        String traceId,
        String sessionId,
        int turnIndex,
        String question,
        Optional<String> category,
        String history
    ) {
    }
}
