package com.agentforge.order.cs.simulation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.agentforge.order.cs.cache.CachedOrderDetailService;
import com.agentforge.order.cs.order.Benefit;
import com.agentforge.order.cs.order.BenefitMapper;
import com.agentforge.order.cs.order.Order;
import com.agentforge.order.cs.order.OrderDetail;
import com.agentforge.order.cs.order.OrderMapper;
import com.agentforge.order.cs.order.OrderService;
import com.agentforge.order.cs.order.Payment;
import com.agentforge.order.cs.order.PaymentMapper;
import com.agentforge.order.cs.order.User;
import com.agentforge.order.cs.order.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 订单生命周期模拟服务。
 *
 * <p>Demo 专用：模拟下单、支付回调、权益发放等外部系统行为，驱动 MySQL 订单状态变化，
 * 供管理页演示与 {@link com.agentforge.order.cs.action.RetryBenefitActionExecutor} 调用；
 * 与平台 SPI 无直接耦合。
 */
@Service
public class OrderSimulationService {
    private static final Logger log = LoggerFactory.getLogger(OrderSimulationService.class);
    private static final AtomicLong ORDER_SEQ = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final BenefitMapper benefitMapper;
    private final OrderService orderService;
    private final CachedOrderDetailService cachedOrderDetailService;

    /**
     * 构造模拟服务。
     *
     * @param userMapper               用户 Mapper
     * @param orderMapper              订单 Mapper
     * @param paymentMapper            支付 Mapper
     * @param benefitMapper            权益 Mapper
     * @param orderService             订单业务服务
     * @param cachedOrderDetailService 订单缓存服务
     */
    public OrderSimulationService(UserMapper userMapper, OrderMapper orderMapper, PaymentMapper paymentMapper,
                                    BenefitMapper benefitMapper, OrderService orderService,
                                    CachedOrderDetailService cachedOrderDetailService) {
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.benefitMapper = benefitMapper;
        this.orderService = orderService;
        this.cachedOrderDetailService = cachedOrderDetailService;
    }

    /**
     * 模拟用户下单：创建 CREATED 订单 + PENDING 支付记录。
     *
     * @param request 下单请求
     * @return 新建订单的聚合详情
     */
    public OrderDetail createOrder(CreateOrderRequest request) {
        validateCreateRequest(request);
        User user = requireUser(request.userNo());
        String orderNo = nextOrderNo();
        LocalDateTime now = LocalDateTime.now();

        Order order = new Order(orderNo, user.getUserNo(), "CREATED", request.amount(), null, null);
        orderMapper.insert(order);

        Payment payment = new Payment(orderNo, request.channel().trim().toUpperCase(), "PENDING", "PENDING", null);
        paymentMapper.insert(payment);

        log.info("模拟下单成功 orderNo={} userNo={} amount={}", orderNo, user.getUserNo(), request.amount());
        cachedOrderDetailService.evict(orderNo);
        return orderService.getOrderDetail(orderNo);
    }

    /**
     * 模拟支付渠道回调。
     *
     * <p>SUCCESS：支付成功且回调成功，订单 COMPLETED，权益 PENDING。
     * FAILED：支付成功但回调失败，订单 PENDING_PAYMENT_CALLBACK。
     *
     * @param orderNo 订单号
     * @param result  回调结果（SUCCESS 或 FAILED）
     * @return 更新后的订单详情
     */
    public OrderDetail simulatePaymentCallback(String orderNo, String result) {
        Order order = requireOrder(orderNo);
        Payment payment = requirePayment(orderNo);
        if (!"PENDING".equals(payment.getStatus()) && !"SUCCESS".equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前支付状态不支持回调模拟: " + payment.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setStatus("SUCCESS");
        payment.setPaidAt(now);
        paymentMapper.updateById(payment);

        if ("SUCCESS".equalsIgnoreCase(result)) {
            payment.setCallbackStatus("SUCCESS");
            paymentMapper.updateById(payment);
            order.setStatus("COMPLETED");
            order.setPaidAt(now);
            order.setCompletedAt(now);
            orderMapper.updateById(order);
            ensureBenefitPending(order);
            log.info("模拟支付回调成功 orderNo={}", orderNo);
        } else if ("FAILED".equalsIgnoreCase(result)) {
            payment.setCallbackStatus("FAILED");
            paymentMapper.updateById(payment);
            order.setStatus("PENDING_PAYMENT_CALLBACK");
            order.setPaidAt(now);
            orderMapper.updateById(order);
            log.info("模拟支付回调失败 orderNo={}", orderNo);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "result must be SUCCESS or FAILED");
        }
        cachedOrderDetailService.evict(orderNo);
        return orderService.getOrderDetail(orderNo);
    }

    /**
     * 模拟权益下发：SUCCESS → ISSUED；FAILED → BENEFIT_FAILED。
     *
     * @param orderNo 订单号
     * @param result  发放结果（SUCCESS 或 FAILED）
     * @return 更新后的订单详情
     */
    public OrderDetail simulateBenefitIssue(String orderNo, String result) {
        Order order = requireOrder(orderNo);
        Benefit benefit = benefitMapper.selectOne(new LambdaQueryWrapper<Benefit>()
            .eq(Benefit::getOrderNo, orderNo)
            .orderByDesc(Benefit::getCreatedAt)
            .last("LIMIT 1"));
        if (benefit == null) {
            benefit = ensureBenefitPending(order);
        }

        if ("SUCCESS".equalsIgnoreCase(result)) {
            benefit.setStatus("ISSUED");
            benefit.setFailReason(null);
            benefitMapper.updateById(benefit);
            if (!"COMPLETED".equals(order.getStatus())) {
                order.setStatus("COMPLETED");
                orderMapper.updateById(order);
            }
            log.info("模拟权益发放成功 orderNo={}", orderNo);
        } else if ("FAILED".equalsIgnoreCase(result)) {
            benefit.setStatus("FAILED");
            benefit.setFailReason("Benefit service timeout.");
            benefitMapper.updateById(benefit);
            order.setStatus("BENEFIT_FAILED");
            orderMapper.updateById(order);
            log.info("模拟权益发放失败 orderNo={}", orderNo);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "result must be SUCCESS or FAILED");
        }
        cachedOrderDetailService.evict(orderNo);
        return orderService.getOrderDetail(orderNo);
    }

    /** 校验下单请求必填字段与金额合法性。 */
    private void validateCreateRequest(CreateOrderRequest request) {
        if (request == null || request.userNo() == null || request.userNo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userNo is required");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be positive");
        }
        if (request.channel() == null || request.channel().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "channel is required");
        }
    }

    /** 按 userNo 查询用户，不存在则 404。 */
    private User requireUser(String userNo) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUserNo, userNo)
            .last("LIMIT 1"));
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userNo);
        }
        return user;
    }

    /** 按 orderNo 查询订单，不存在则 404。 */
    private Order requireOrder(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
            .eq(Order::getOrderNo, orderNo)
            .last("LIMIT 1"));
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderNo);
        }
        return order;
    }

    /** 按 orderNo 查询最新支付记录，不存在则 404。 */
    private Payment requirePayment(String orderNo) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
            .eq(Payment::getOrderNo, orderNo)
            .orderByDesc(Payment::getCreatedAt)
            .last("LIMIT 1"));
        if (payment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found for order: " + orderNo);
        }
        return payment;
    }

    /** 确保订单存在 PENDING 权益记录，不存在则按用户等级创建。 */
    private Benefit ensureBenefitPending(Order order) {
        Benefit existing = benefitMapper.selectOne(new LambdaQueryWrapper<Benefit>()
            .eq(Benefit::getOrderNo, order.getOrderNo())
            .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        User user = requireUser(order.getUserNo());
        Benefit benefit = new Benefit(order.getUserNo(), order.getOrderNo(), benefitName(user.getLevelName()), "PENDING", null);
        benefitMapper.insert(benefit);
        return benefit;
    }

    /** 根据会员等级推断权益名称。 */
    private String benefitName(String levelName) {
        if ("SVIP".equalsIgnoreCase(levelName)) {
            return "SVIP monthly bonus";
        }
        if ("VIP".equalsIgnoreCase(levelName)) {
            return "VIP coupon package";
        }
        return "New user benefit";
    }

    /** 生成唯一订单号 ORD-{seq}。 */
    private String nextOrderNo() {
        return "ORD-" + ORDER_SEQ.incrementAndGet();
    }
}
