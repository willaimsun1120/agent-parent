package com.agentforge.order.cs.tool;

import com.agentforge.agent.action.AgentActionService;
import com.agentforge.agent.core.api.AgentPendingActionView;
import com.agentforge.agent.runtime.AgentRequestContext;
import com.agentforge.agent.runtime.AgentRequestContextHolder;
import com.agentforge.agent.observability.AgentMetrics;
import com.agentforge.agent.observability.AgentToolLog;
import com.agentforge.agent.observability.AgentToolLogMapper;
import com.agentforge.order.cs.order.OrderDetail;
import com.agentforge.order.cs.order.OrderService;
import com.agentforge.agent.rag.KnowledgeDocument;
import com.agentforge.agent.core.spi.RagCategoryResolver;
import com.agentforge.agent.rag.RagSearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 订单客服业务工具集（核心委托层）。
 *
 * <p>封装订单查询、知识库检索与待确认写操作，被三种 Agent 模式共用：
 * <ul>
 *   <li>手写模式 {@link com.agentforge.order.cs.mode.OrderManualAgentExecutor} 直接调用</li>
 *   <li>框架模式经 {@link com.agentforge.order.cs.mode.OrderFrameworkToolRegistrar} 适配 LangChain4j / AgentScope</li>
 * </ul>
 * 工具调用统一记录平台可观测性（{@link AgentMetrics}、{@link AgentToolLogMapper}），
 * 写操作通过平台 {@link AgentActionService} 创建待人工确认动作，不直接改库。
 */
@Component
public class OrderTools {
    private static final Logger log = LoggerFactory.getLogger(OrderTools.class);

    private final OrderService orderService;
    private final RagSearchService ragSearchService;
    private final AgentActionService actionService;
    private final AgentRequestContext requestContext;
    private final AgentToolLogMapper toolLogMapper;
    private final AgentMetrics metrics;
    private final ObjectMapper objectMapper;

    private final RagCategoryResolver ragCategoryResolver;

    /**
     * 构造订单工具集。
     *
     * @param orderService         订单业务服务
     * @param ragSearchService     平台 RAG 检索服务
     * @param actionService        平台待确认动作服务
     * @param requestContext       当前请求上下文
     * @param toolLogMapper        工具调用日志 Mapper
     * @param metrics              Agent 指标
     * @param objectMapper         JSON 序列化
     * @param ragCategoryResolver  RAG 分类解析 SPI 实现
     */
    public OrderTools(OrderService orderService, RagSearchService ragSearchService,
                      AgentActionService actionService, AgentRequestContext requestContext,
                      AgentToolLogMapper toolLogMapper, AgentMetrics metrics,
                      ObjectMapper objectMapper, RagCategoryResolver ragCategoryResolver) {
        this.orderService = orderService;
        this.ragSearchService = ragSearchService;
        this.actionService = actionService;
        this.requestContext = requestContext;
        this.toolLogMapper = toolLogMapper;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.ragCategoryResolver = ragCategoryResolver;
    }

    /**
     * 查询订单完整业务事实（JSON 字符串）。
     *
     * @param orderNo 订单号
     * @return 订单详情 JSON
     */
    public String getOrderDetail(String orderNo) {
        return recordToolCall("getOrderDetail", Map.of("orderNo", orderNo), () -> toJson(orderService.getOrderDetail(orderNo)));
    }

    /**
     * 查询支付与回调状态（JSON 字符串）。
     *
     * @param orderNo 订单号
     * @return 支付状态 JSON
     */
    public String getPaymentStatus(String orderNo) {
        return recordToolCall("getPaymentStatus", Map.of("orderNo", orderNo), () -> {
            OrderDetail detail = orderService.getOrderDetail(orderNo);
            return toJson(Map.of(
                "orderNo", orderNo,
                "paymentStatus", detail.paymentStatus(),
                "paymentCallbackStatus", detail.paymentCallbackStatus()
            ));
        });
    }

    /**
     * 查询退款状态与原因（JSON 字符串）。
     *
     * @param orderNo 订单号
     * @return 退款状态 JSON
     */
    public String getRefundStatus(String orderNo) {
        return recordToolCall("getRefundStatus", Map.of("orderNo", orderNo), () -> {
            OrderDetail detail = orderService.getOrderDetail(orderNo);
            return toJson(Map.of(
                "orderNo", orderNo,
                "refundStatus", detail.refundStatus(),
                "refundReason", detail.refundReason() == null ? "" : detail.refundReason()
            ));
        });
    }

    /**
     * 查询权益发放状态（JSON 字符串）。
     *
     * @param orderNo 订单号
     * @return 权益状态 JSON
     */
    public String getBenefitStatus(String orderNo) {
        return recordToolCall("getBenefitStatus", Map.of("orderNo", orderNo), () -> {
            OrderDetail detail = orderService.getOrderDetail(orderNo);
            return toJson(Map.of("orderNo", orderNo, "benefits", detail.benefits()));
        });
    }

    /**
     * 检索客服知识库，自动解析 RAG 分类。
     *
     * @param question 用户问题
     * @return 命中的知识文档列表
     */
    public List<KnowledgeDocument> searchKnowledge(String question) {
        Optional<String> category = resolveCategory(question);
        return searchKnowledge(question, category);
    }

    /**
     * 检索客服知识库，可指定 RAG 分类过滤。
     *
     * @param question 用户问题
     * @param category 知识分类（可选）
     * @return 命中的知识文档列表
     */
    public List<KnowledgeDocument> searchKnowledge(String question, Optional<String> category) {
        long started = System.currentTimeMillis();
        log.info("开始调用知识库检索工具 question={} category={}", question, category.orElse("-"));
        List<KnowledgeDocument> hits = ragSearchService.search(question, 3, category);
        saveToolLog("searchKnowledge", toJson(Map.of("question", question, "category", category.orElse(""))),
            "命中文档数: " + hits.size(), System.currentTimeMillis() - started, true);
        return hits;
    }

    /**
     * 创建待人工确认的退款申请（不直接写入退款表）。
     *
     * @param orderNo 订单号
     * @param reason  退款原因
     * @return 待确认动作 JSON
     */
    public String submitRefundRequest(String orderNo, String reason) {
        return recordToolCall("submitRefundRequest", Map.of("orderNo", orderNo, "reason", reason), () -> {
            // 先校验订单存在
            orderService.getOrderDetail(orderNo);
            AgentPendingActionView action = actionService.createPending(
                "SUBMIT_REFUND", orderNo, Map.of("reason", reason == null ? "" : reason),
                resolveSessionId(), resolveTraceId());
            return toJson(Map.of(
                "actionId", action.actionId(),
                "status", action.status(),
                "message", "退款申请已创建，等待人工确认后才会写入退款表。"
            ));
        });
    }

    /**
     * 创建待人工确认的权益重试申请（不直接执行补发）。
     *
     * @param orderNo 订单号
     * @return 操作结果 JSON（含 success 标志）
     */
    public String retryBenefitIssue(String orderNo) {
        return recordToolCall("retryBenefitIssue", Map.of("orderNo", orderNo), () -> {
            try {
                OrderDetail detail = orderService.getOrderDetail(orderNo);
                // 仅当订单或权益处于失败态时才允许申请重试
                if (!"BENEFIT_FAILED".equals(detail.status())
                    && detail.benefits().stream().noneMatch(item -> item.contains(":FAILED"))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前订单状态不支持权益重试");
                }
                AgentPendingActionView action = actionService.createPending(
                    "RETRY_BENEFIT", orderNo, Map.of(),
                    resolveSessionId(), resolveTraceId());
                return toJson(Map.of(
                    "success", true,
                    "actionId", action.actionId(),
                    "status", action.status(),
                    "message", "权益重试申请已创建，等待人工确认后才会执行。"
                ));
            } catch (ResponseStatusException ex) {
                // 业务校验失败时返回友好提示，不向上抛异常
                return toJson(Map.of(
                    "success", false,
                    "message", ex.getReason(),
                    "hint", "请先调用 get_benefit_status 确认权益状态；发放中无需重试，仅发放失败且用户要求补发时才可申请。"
                ));
            }
        });
    }

    /**
     * 解析 RAG 检索分类：优先 ThreadLocal 上下文，再请求上下文，最后 SPI 推断。
     */
    private Optional<String> resolveCategory(String question) {
        Optional<String> fromHolder = AgentRequestContextHolder.category();
        if (fromHolder.isPresent()) {
            return fromHolder;
        }
        try {
            if (requestContext.category() != null && !requestContext.category().isBlank()) {
                return Optional.of(requestContext.category());
            }
        } catch (RuntimeException ignored) {
            // SSE 异步线程 request scope 已失效
        }
        return ragCategoryResolver.resolve(null, question);
    }

    /**
     * 统一工具调用包装：计数、计时、日志与异常处理。
     */
    private String recordToolCall(String toolName, Map<String, Object> input, ToolInvoker invoker) {
        long started = System.currentTimeMillis();
        metrics.incrementToolCall();
        String inputJson = toJson(input);
        log.info("开始调用订单业务工具 toolName={} input={}", toolName, inputJson);
        try {
            String output = invoker.invoke();
            long durationMs = System.currentTimeMillis() - started;
            metrics.recordToolDuration(durationMs);
            saveToolLog(toolName, inputJson, preview(output), durationMs, true);
            log.info("订单业务工具调用成功 toolName={} durationMs={}", toolName, durationMs);
            return output;
        } catch (RuntimeException ex) {
            long durationMs = System.currentTimeMillis() - started;
            metrics.recordToolDuration(durationMs);
            saveToolLog(toolName, inputJson, ex.getMessage(), durationMs, false);
            log.warn("订单业务工具调用失败 toolName={} durationMs={} message={}", toolName, durationMs, ex.getMessage());
            throw ex;
        }
    }

    /** 将工具调用写入平台 tool_log 表（需 traceId 存在）。 */
    private void saveToolLog(String toolName, String inputJson, String outputSummary, long durationMs, boolean success) {
        String traceId = resolveTraceId();
        if (traceId == null) {
            return;
        }
        toolLogMapper.insert(new AgentToolLog(traceId, toolName, inputJson, outputSummary, durationMs, success));
    }

    /** 从 MDC、ThreadLocal 或请求上下文解析 traceId。 */
    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            return traceId;
        }
        traceId = AgentRequestContextHolder.traceId();
        if (traceId != null) {
            return traceId;
        }
        return safeRequestTraceId();
    }

    /** 从 ThreadLocal 或请求上下文解析 sessionId。 */
    private String resolveSessionId() {
        String sessionId = AgentRequestContextHolder.sessionId();
        if (sessionId != null) {
            return sessionId;
        }
        return safeRequestSessionId();
    }

    private String safeRequestTraceId() {
        try {
            return requestContext.traceId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String safeRequestSessionId() {
        try {
            return requestContext.sessionId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** 对象序列化为 JSON 字符串。 */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON serialization failed", ex);
        }
    }

    /** 截断过长输出用于日志摘要。 */
    private String preview(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 500 ? text : text.substring(0, 500);
    }

    /** 工具调用函数式接口，供 {@link #recordToolCall} 使用。 */
    @FunctionalInterface
    private interface ToolInvoker {
        String invoke();
    }
}
