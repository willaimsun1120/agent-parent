package com.agentforge.order.cs.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 订单详情加载器（无缓存）。
 *
 * <p>从 MySQL 四表聚合 {@link OrderDetail}，供 {@link com.agentforge.order.cs.cache.CachedOrderDetailService}
 * 包装缓存；与平台 SPI 无直接关系，是业务事实的底层数据源。
 */
@Service
public class OrderDetailLoader {
    private static final Logger log = LoggerFactory.getLogger(OrderDetailLoader.class);

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final BenefitMapper benefitMapper;

    /**
     * 构造详情加载器。
     *
     * @param orderMapper   订单 Mapper
     * @param paymentMapper 支付 Mapper
     * @param refundMapper  退款 Mapper
     * @param benefitMapper 权益 Mapper
     */
    public OrderDetailLoader(OrderMapper orderMapper, PaymentMapper paymentMapper,
                             RefundMapper refundMapper, BenefitMapper benefitMapper) {
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.benefitMapper = benefitMapper;
    }

    /**
     * 从数据库加载指定订单的聚合详情。
     *
     * @param orderNo 订单号
     * @return 聚合后的订单详情
     * @throws ResponseStatusException 订单不存在时返回 404
     */
    public OrderDetail load(String orderNo) {
        log.info("加载订单详情（无缓存） orderNo={}", orderNo);
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
            .eq(Order::getOrderNo, orderNo)
            .last("LIMIT 1"));
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderNo);
        }
        // 取最新一条支付记录
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
            .eq(Payment::getOrderNo, orderNo)
            .orderByDesc(Payment::getCreatedAt)
            .last("LIMIT 1"));
        // 取最新一条退款记录
        Refund refund = refundMapper.selectOne(new LambdaQueryWrapper<Refund>()
            .eq(Refund::getOrderNo, orderNo)
            .orderByDesc(Refund::getCreatedAt)
            .last("LIMIT 1"));
        // 汇总该订单下全部权益，格式化为 Agent 可读摘要
        List<String> benefits = benefitMapper.selectList(new LambdaQueryWrapper<Benefit>()
                .eq(Benefit::getOrderNo, orderNo)
                .orderByDesc(Benefit::getCreatedAt))
            .stream()
            .map(benefit -> benefit.getBenefitName() + ":" + benefit.getStatus()
                + (benefit.getFailReason() == null ? "" : "(" + benefit.getFailReason() + ")"))
            .toList();
        return new OrderDetail(
            order.getOrderNo(),
            order.getUserNo(),
            order.getStatus(),
            order.getAmount(),
            order.getPaidAt(),
            order.getCompletedAt(),
            payment == null ? "UNKNOWN" : payment.getStatus(),
            payment == null ? "UNKNOWN" : payment.getCallbackStatus(),
            refund == null ? "NONE" : refund.getStatus(),
            refund == null ? null : refund.getReason(),
            benefits
        );
    }
}
