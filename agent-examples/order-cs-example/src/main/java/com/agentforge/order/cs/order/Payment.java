package com.agentforge.order.cs.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 支付记录实体，映射 MySQL {@code payments} 表。
 *
 * <p>记录订单支付渠道、支付状态及回调状态，聚合进 {@link OrderDetail} 供 Agent 回答支付类问题。
 */
@TableName("payments")
public class Payment {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联订单号 */
    private String orderNo;
    /** 支付渠道，如 ALIPAY、WECHAT */
    private String channel;
    /** 支付状态，如 PENDING、SUCCESS */
    private String status;
    /** 支付回调状态，如 PENDING、SUCCESS、FAILED */
    private String callbackStatus;
    /** 实际支付时间 */
    private LocalDateTime paidAt;
    /** 记录创建时间 */
    private LocalDateTime createdAt;

    /** 无参构造，供 MyBatis-Plus 使用。 */
    public Payment() {
    }

    /**
     * 构造支付记录（不含 id 与 createdAt）。
     *
     * @param orderNo        订单号
     * @param channel        支付渠道
     * @param status         支付状态
     * @param callbackStatus 回调状态
     * @param paidAt         支付时间
     */
    public Payment(String orderNo, String channel, String status, String callbackStatus, LocalDateTime paidAt) {
        this.orderNo = orderNo;
        this.channel = channel;
        this.status = status;
        this.callbackStatus = callbackStatus;
        this.paidAt = paidAt;
    }

    /** @return 主键 */
    public Long getId() { return id; }
    /** @return 关联订单号 */
    public String getOrderNo() { return orderNo; }
    /** @return 支付渠道 */
    public String getChannel() { return channel; }
    /** @return 支付状态 */
    public String getStatus() { return status; }
    /** @return 支付回调状态 */
    public String getCallbackStatus() { return callbackStatus; }
    /** @return 实际支付时间 */
    public LocalDateTime getPaidAt() { return paidAt; }
    /** @return 创建时间 */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** @param id 主键 */
    public void setId(Long id) { this.id = id; }
    /** @param orderNo 关联订单号 */
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    /** @param channel 支付渠道 */
    public void setChannel(String channel) { this.channel = channel; }
    /** @param status 支付状态 */
    public void setStatus(String status) { this.status = status; }
    /** @param callbackStatus 支付回调状态 */
    public void setCallbackStatus(String callbackStatus) { this.callbackStatus = callbackStatus; }
    /** @param paidAt 实际支付时间 */
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    /** @param createdAt 创建时间 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
