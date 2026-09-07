package com.agentforge.order.cs.action;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.agentforge.agent.core.exception.AgentErrorCode;
import com.agentforge.agent.core.exception.AgentException;
import com.agentforge.agent.core.spi.ActionExecutionContext;
import com.agentforge.agent.core.spi.ActionExecutor;
import com.agentforge.order.cs.cache.CachedOrderDetailService;
import com.agentforge.order.cs.order.Refund;
import com.agentforge.order.cs.order.RefundMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 退款申请执行器。
 *
 * <p>实现平台 SPI {@link ActionExecutor}，动作类型 {@code SUBMIT_REFUND}。
 * 人工在管理页确认后，将退款记录写入 MySQL 并失效订单缓存；
 * 对应 Agent 工具 {@link com.agentforge.order.cs.tool.OrderTools#submitRefundRequest} 创建的待确认动作。
 */
@Component
public class SubmitRefundActionExecutor implements ActionExecutor {
    private static final Logger log = LoggerFactory.getLogger(SubmitRefundActionExecutor.class);

    private final RefundMapper refundMapper;
    private final CachedOrderDetailService cachedOrderDetailService;
    private final ObjectMapper objectMapper;

    /**
     * 构造退款执行器。
     *
     * @param refundMapper             退款 Mapper
     * @param cachedOrderDetailService 订单缓存服务
     * @param objectMapper             JSON 解析
     */
    public SubmitRefundActionExecutor(RefundMapper refundMapper,
                                      CachedOrderDetailService cachedOrderDetailService,
                                      ObjectMapper objectMapper) {
        this.refundMapper = refundMapper;
        this.cachedOrderDetailService = cachedOrderDetailService;
        this.objectMapper = objectMapper;
    }

    /**
     * 返回本执行器支持的动作类型。
     *
     * @return {@code SUBMIT_REFUND}
     */
    @Override
    public String actionType() {
        return "SUBMIT_REFUND";
    }

    /**
     * 执行退款申请：校验无进行中退款后写入 REQUESTED 记录。
     *
     * @param context 平台传入的动作执行上下文
     */
    @Override
    public void execute(ActionExecutionContext context) {
        Refund existing = refundMapper.selectOne(new LambdaQueryWrapper<Refund>()
            .eq(Refund::getOrderNo, context.orderNo())
            .orderByDesc(Refund::getCreatedAt)
            .last("LIMIT 1"));
        if (existing != null && !"NONE".equals(existing.getStatus()) && !"REJECTED".equals(existing.getStatus())) {
            throw new AgentException(AgentErrorCode.ACTION_CONFLICT, "订单已有进行中的退款记录");
        }
        String reason = readReason(context.payloadJson());
        Refund refund = new Refund(context.orderNo(), "REQUESTED", reason, LocalDateTime.now());
        refundMapper.insert(refund);
        cachedOrderDetailService.evict(context.orderNo());
        log.info("退款申请执行完成 actionId={} orderNo={}", context.actionId(), context.orderNo());
    }

    /** 从 payload JSON 中解析退款原因字段。 */
    private String readReason(String payloadJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(payloadJson == null ? "{}" : payloadJson, Map.class);
            Object reason = payload.get("reason");
            return reason == null ? "" : String.valueOf(reason);
        } catch (JsonProcessingException ex) {
            return "";
        }
    }
}
