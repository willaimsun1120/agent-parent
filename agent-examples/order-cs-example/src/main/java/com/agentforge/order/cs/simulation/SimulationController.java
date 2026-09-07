package com.agentforge.order.cs.simulation;

import com.agentforge.order.cs.order.OrderDetail;
import com.agentforge.order.cs.order.User;
import com.agentforge.order.cs.order.UserMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单模拟 REST 控制器。
 *
 * <p>暴露 Demo 专用的下单、支付回调、权益发放 API，供管理页驱动订单状态变化；
 * 与平台 SPI 无直接耦合。
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {
    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);

    private final OrderSimulationService simulationService;
    private final UserMapper userMapper;

    /**
     * 构造模拟控制器。
     *
     * @param simulationService 订单模拟服务
     * @param userMapper        用户 Mapper
     */
    public SimulationController(OrderSimulationService simulationService, UserMapper userMapper) {
        this.simulationService = simulationService;
        this.userMapper = userMapper;
    }

    /**
     * 查询全部模拟用户列表。
     *
     * @return 用户列表
     */
    @GetMapping("/users")
    public List<User> listUsers() {
        log.info("查询模拟用户列表");
        return userMapper.selectList(null);
    }

    /**
     * 模拟用户下单。
     *
     * @param request 下单请求
     * @return 新建订单详情
     */
    @PostMapping("/orders")
    public OrderDetail createOrder(@RequestBody CreateOrderRequest request) {
        log.info("收到模拟下单请求 userNo={}", request == null ? null : request.userNo());
        return simulationService.createOrder(request);
    }

    /**
     * 模拟支付渠道回调。
     *
     * @param orderNo 订单号
     * @param result  回调结果，默认 SUCCESS
     * @return 更新后的订单详情
     */
    @PostMapping("/orders/{orderNo}/payment-callback")
    public OrderDetail paymentCallback(@PathVariable String orderNo,
                                       @RequestParam(defaultValue = "SUCCESS") String result) {
        log.info("收到模拟支付回调 orderNo={} result={}", orderNo, result);
        return simulationService.simulatePaymentCallback(orderNo, result);
    }

    /**
     * 模拟权益发放。
     *
     * @param orderNo 订单号
     * @param result  发放结果，默认 SUCCESS
     * @return 更新后的订单详情
     */
    @PostMapping("/orders/{orderNo}/benefit-issue")
    public OrderDetail benefitIssue(@PathVariable String orderNo,
                                    @RequestParam(defaultValue = "SUCCESS") String result) {
        log.info("收到模拟权益发放 orderNo={} result={}", orderNo, result);
        return simulationService.simulateBenefitIssue(orderNo, result);
    }
}
