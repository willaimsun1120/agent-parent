package com.agentforge.order.cs.order;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单 REST 控制器。
 *
 * <p>暴露订单列表、详情与删除 API，供管理页面与调试使用；
 * Agent 工具通过 {@link OrderService} 读取同一份数据，与平台 SPI 无直接耦合。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    /**
     * 构造订单控制器。
     *
     * @param orderService 订单业务服务
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 查询全部订单详情列表。
     *
     * @return 订单详情列表
     */
    @GetMapping
    public List<OrderDetail> listOrders() {
        long started = System.currentTimeMillis();
        log.info("收到订单列表查询请求");
        List<OrderDetail> orders = orderService.listOrders();
        log.info("订单列表查询请求完成 count={} durationMs={}", orders.size(), System.currentTimeMillis() - started);
        return orders;
    }

    /**
     * 按订单号查询详情。
     *
     * @param orderNo 订单号
     * @return 订单聚合详情
     */
    @GetMapping("/{orderNo}")
    public OrderDetail getOrder(@PathVariable String orderNo) {
        long started = System.currentTimeMillis();
        log.info("收到订单详情查询请求 orderNo={}", orderNo);
        // 关键入口：页面点选订单和 Agent 工具查询，最终都会读取同一份 MySQL 订单事实。
        OrderDetail detail = orderService.getOrderDetail(orderNo);
        log.info("订单详情查询请求完成 orderNo={} durationMs={}", orderNo, System.currentTimeMillis() - started);
        return detail;
    }

    /**
     * 删除指定订单及其关联数据。
     *
     * @param orderNo 订单号
     */
    @DeleteMapping("/{orderNo}")
    public void deleteOrder(@PathVariable String orderNo) {
        long started = System.currentTimeMillis();
        log.info("收到订单删除请求 orderNo={}", orderNo);
        orderService.deleteOrder(orderNo);
        log.info("订单删除请求完成 orderNo={} durationMs={}", orderNo, System.currentTimeMillis() - started);
    }
}
