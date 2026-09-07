package com.agentforge.order.cs.mode.agentscope;

import com.agentforge.agent.runtime.AgentExecutionSupport;
import com.agentforge.agent.runtime.AgentRequestContextHolder;
import com.agentforge.agent.rag.CustomerServicePrompts;
import com.agentforge.agent.rag.KnowledgeDocument;
import com.agentforge.order.cs.tool.OrderTools;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AgentScope 框架订单工具适配器。
 *
 * <p>每次 AgentScope 请求单独创建，通过 {@link AgentRequestContextHolder#callWith} 注入
 * sessionId / traceId，再委托 {@link OrderTools} 执行业务逻辑。
 * 由 {@link com.agentforge.order.cs.mode.OrderFrameworkToolRegistrar} 注册到平台 SPI。
 */
public class AgentScopeOrderToolAdapter {
    private static final Logger log = LoggerFactory.getLogger(AgentScopeOrderToolAdapter.class);

    private final OrderTools orderTools;
    private final List<String> toolCalls;
    private final AgentExecutionSupport.PreparedChat prepared;

    /**
     * 构造 AgentScope 工具适配器。
     *
     * @param orderTools 订单业务工具
     * @param toolCalls  工具调用记录列表（可变，供平台收集）
     * @param prepared   当前请求的 PreparedChat 上下文
     */
    public AgentScopeOrderToolAdapter(OrderTools orderTools,
                                      List<String> toolCalls,
                                      AgentExecutionSupport.PreparedChat prepared) {
        this.orderTools = orderTools;
        this.toolCalls = toolCalls;
        this.prepared = prepared;
    }

    /**
     * 查询订单完整业务事实。
     *
     * @param orderNo 订单号
     * @return 订单详情 JSON
     */
    @Tool(name = "get_order_detail", description = "根据订单号查询订单、支付、退款、权益等完整业务事实。遇到 ORD- 开头的订单号时优先使用。", readOnly = true)
    public String getOrderDetail(
        @ToolParam(name = "order_no", description = "订单号，例如 ORD-1002") String orderNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            log.info("AgentScope 工具开始查询订单详情 orderNo={}", orderNo);
            recordCall("get_order_detail", "order_no=" + orderNo);
            return orderTools.getOrderDetail(orderNo);
        });
    }

    /**
     * 查询支付与回调状态。
     *
     * @param orderNo 订单号
     * @return 支付状态 JSON
     */
    @Tool(name = "get_payment_status", description = "根据订单号查询支付状态和支付回调状态。用户询问支付成功、支付异常、回调失败时使用。", readOnly = true)
    public String getPaymentStatus(
        @ToolParam(name = "order_no", description = "订单号，例如 ORD-1002") String orderNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            log.info("AgentScope 工具开始查询支付状态 orderNo={}", orderNo);
            recordCall("get_payment_status", "order_no=" + orderNo);
            return orderTools.getPaymentStatus(orderNo);
        });
    }

    /**
     * 查询退款状态与原因。
     *
     * @param orderNo 订单号
     * @return 退款状态 JSON
     */
    @Tool(name = "get_refund_status", description = "根据订单号查询退款状态和退款原因。用户询问退款、退款拒绝、退款进度时使用。", readOnly = true)
    public String getRefundStatus(
        @ToolParam(name = "order_no", description = "订单号，例如 ORD-1001") String orderNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            log.info("AgentScope 工具开始查询退款状态 orderNo={}", orderNo);
            recordCall("get_refund_status", "order_no=" + orderNo);
            return orderTools.getRefundStatus(orderNo);
        });
    }

    /**
     * 查询权益发放状态。
     *
     * @param orderNo 订单号
     * @return 权益状态 JSON
     */
    @Tool(name = "get_benefit_status", description = "根据订单号查询会员权益发放状态。用户询问会员、权益、到账失败时使用。", readOnly = true)
    public String getBenefitStatus(
        @ToolParam(name = "order_no", description = "订单号，例如 ORD-1003") String orderNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            log.info("AgentScope 工具开始查询权益状态 orderNo={}", orderNo);
            recordCall("get_benefit_status", "order_no=" + orderNo);
            return orderTools.getBenefitStatus(orderNo);
        });
    }

    /**
     * 检索客服知识库。
     *
     * @param question 客服原始问题
     * @return 格式化的知识库规则文本
     */
    @Tool(name = "search_knowledge", description = "检索退款、支付、会员权益等客服知识库。回答前必须用原始问题检索一次规则依据。", readOnly = true)
    public String searchKnowledge(
        @ToolParam(name = "question", description = "客服原始问题") String question) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            log.info("AgentScope 工具开始检索知识库 question={}", question);
            recordCall("search_knowledge", "question=" + question);
            List<KnowledgeDocument> hits = orderTools.searchKnowledge(question, prepared.category());
            if (hits.isEmpty()) {
                return "知识库未命中相关规则。";
            }
            return CustomerServicePrompts.formatPolicyContext(hits);
        });
    }

    /**
     * 创建待人工确认的退款申请。
     *
     * @param orderNo 订单号
     * @param reason  退款原因
     * @return 待确认动作 JSON
     */
    @Tool(name = "submit_refund_request", description = "创建待人工确认的退款申请。用户明确要求发起退款时使用，不会直接执行退款。", readOnly = false)
    public String submitRefundRequest(
        @ToolParam(name = "order_no", description = "订单号") String orderNo,
        @ToolParam(name = "reason", description = "退款原因") String reason) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            log.info("AgentScope 工具创建退款申请 orderNo={}", orderNo);
            recordCall("submit_refund_request", "order_no=" + orderNo);
            return orderTools.submitRefundRequest(orderNo, reason);
        });
    }

    /**
     * 创建待人工确认的权益重试申请。
     *
     * @param orderNo 订单号
     * @return 操作结果 JSON
     */
    @Tool(name = "retry_benefit_issue", description = "创建待人工确认的权益重试申请。仅当 get_benefit_status 显示发放失败(FAILED)且用户明确要求补发时使用；权益为发放中(PENDING)或仅咨询原因时禁止调用。", readOnly = false)
    public String retryBenefitIssue(
        @ToolParam(name = "order_no", description = "订单号") String orderNo) {
        return AgentRequestContextHolder.callWith(prepared, () -> {
            log.info("AgentScope 工具创建权益重试申请 orderNo={}", orderNo);
            recordCall("retry_benefit_issue", "order_no=" + orderNo);
            return orderTools.retryBenefitIssue(orderNo);
        });
    }

    /** 记录工具调用到平台收集列表。 */
    private void recordCall(String toolName, String input) {
        toolCalls.add(toolName + "(" + input + ")");
    }
}
