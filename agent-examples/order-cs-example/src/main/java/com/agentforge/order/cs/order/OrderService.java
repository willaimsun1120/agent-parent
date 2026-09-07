package com.agentforge.order.cs.order;

import com.agentforge.order.cs.cache.CachedOrderDetailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 订单业务服务。
 *
 * <p>封装订单列表、详情查询与级联删除，详情经 {@link CachedOrderDetailService} 缓存后
 * 供 REST API 与 {@link com.agentforge.order.cs.tool.OrderTools} 共用同一份 MySQL 事实。
 */
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final BenefitMapper benefitMapper;
    private final CachedOrderDetailService cachedOrderDetailService;

    /**
     * 构造订单服务。
     *
     * @param orderMapper              订单 Mapper
     * @param paymentMapper            支付 Mapper
     * @param refundMapper             退款 Mapper
     * @param benefitMapper            权益 Mapper
     * @param cachedOrderDetailService 带缓存的详情服务
     */
    public OrderService(OrderMapper orderMapper, PaymentMapper paymentMapper,
                        RefundMapper refundMapper, BenefitMapper benefitMapper,
                        CachedOrderDetailService cachedOrderDetailService) {
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.benefitMapper = benefitMapper;
        this.cachedOrderDetailService = cachedOrderDetailService;
    }

    /**
     * 查询全部订单详情列表，按创建时间倒序。
     *
     * @return 订单详情列表
     */
    public List<OrderDetail> listOrders() {
        log.info("开始查询订单列表");
        return orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getCreatedAt))
            .stream()
            .map(order -> getOrderDetail(order.getOrderNo()))
            .toList();
    }

    /**
     * 按订单号查询聚合详情（走 JetCache 缓存）。
     *
     * @param orderNo 订单号
     * @return 订单聚合详情
     * @throws ResponseStatusException 订单不存在时返回 404
     */
    public OrderDetail getOrderDetail(String orderNo) {
        log.info("开始查询订单详情 orderNo={}", orderNo);
        try {
            OrderDetail detail = cachedOrderDetailService.getOrderDetail(orderNo);
            log.info("订单详情查询完成 orderNo={} status={} paymentStatus={} refundStatus={}",
                detail.orderNo(), detail.status(), detail.paymentStatus(), detail.refundStatus());
            return detail;
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("订单不存在 orderNo={}", orderNo);
            }
            throw ex;
        }
    }

    /**
     * 删除订单及其关联的支付、退款、权益记录（Demo 级联清理，不删用户）。
     *
     * @param orderNo 订单号
     * @throws ResponseStatusException 订单不存在时返回 404
     */
    public void deleteOrder(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
            .eq(Order::getOrderNo, orderNo)
            .last("LIMIT 1"));
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderNo);
        }
        int payments = paymentMapper.delete(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderNo, orderNo));
        int refunds = refundMapper.delete(new LambdaQueryWrapper<Refund>().eq(Refund::getOrderNo, orderNo));
        int benefits = benefitMapper.delete(new LambdaQueryWrapper<Benefit>().eq(Benefit::getOrderNo, orderNo));
        orderMapper.deleteById(order.getId());
        cachedOrderDetailService.evict(orderNo);
        log.info("删除订单及关联数据 orderNo={} payments={} refunds={} benefits={}",
            orderNo, payments, refunds, benefits);
    }
}
