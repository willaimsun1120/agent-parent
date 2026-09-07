package com.agentforge.order.cs.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，映射 MySQL {@code orders} 表。
 *
 * <p>承载订单主状态与金额等核心事实，供 {@link OrderDetailLoader} 聚合为 {@link OrderDetail}，
 * 再被 Agent 工具与 REST API 查询。与平台 SPI 无直接依赖，仅作为业务数据源。
 */
@TableName("orders")
public class Order {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务订单号，如 ORD-1001，Agent 识别与查询的主键 */
    private String orderNo;
    /** 下单用户编号，关联 {@link User#userNo} */
    private String userNo;
    /** 订单状态，如 CREATED、COMPLETED、PENDING_PAYMENT_CALLBACK、BENEFIT_FAILED */
    private String status;
    /** 订单金额 */
    private BigDecimal amount;
    /** 支付完成时间 */
    private LocalDateTime paidAt;
    /** 订单完成时间，退款规则判断依据之一 */
    private LocalDateTime completedAt;
    /** 记录创建时间 */
    private LocalDateTime createdAt;

    /** 无参构造，供 MyBatis-Plus 使用。 */
    public Order() {
    }

    /**
     * 构造订单实体（不含 id 与 createdAt）。
     *
     * @param orderNo      订单号
     * @param userNo       用户编号
     * @param status       订单状态
     * @param amount       金额
     * @param paidAt       支付时间
     * @param completedAt  完成时间
     */
    public Order(String orderNo, String userNo, String status, BigDecimal amount, LocalDateTime paidAt,
                 LocalDateTime completedAt) {
        this.orderNo = orderNo;
        this.userNo = userNo;
        this.status = status;
        this.amount = amount;
        this.paidAt = paidAt;
        this.completedAt = completedAt;
    }

    /** @return 主键 */
    public Long getId() { return id; }
    /** @return 业务订单号 */
    public String getOrderNo() { return orderNo; }
    /** @return 用户编号 */
    public String getUserNo() { return userNo; }
    /** @return 订单状态 */
    public String getStatus() { return status; }
    /** @return 订单金额 */
    public BigDecimal getAmount() { return amount; }
    /** @return 支付完成时间 */
    public LocalDateTime getPaidAt() { return paidAt; }
    /** @return 订单完成时间 */
    public LocalDateTime getCompletedAt() { return completedAt; }
    /** @return 创建时间 */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** @param id 主键 */
    public void setId(Long id) { this.id = id; }
    /** @param orderNo 业务订单号 */
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    /** @param userNo 用户编号 */
    public void setUserNo(String userNo) { this.userNo = userNo; }
    /** @param status 订单状态 */
    public void setStatus(String status) { this.status = status; }
    /** @param amount 订单金额 */
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    /** @param paidAt 支付完成时间 */
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    /** @param completedAt 订单完成时间 */
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    /** @param createdAt 创建时间 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
