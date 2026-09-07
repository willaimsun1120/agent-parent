package com.agentforge.order.cs.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 退款记录实体，映射 MySQL {@code refunds} 表。
 *
 * <p>记录退款申请状态与原因，由 {@link com.agentforge.order.cs.action.SubmitRefundActionExecutor}
 * 在人工确认后写入，聚合进 {@link OrderDetail} 供 Agent 回答退款类问题。
 */
@TableName("refunds")
public class Refund {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联订单号 */
    private String orderNo;
    /** 退款状态，如 NONE、REQUESTED、REJECTED */
    private String status;
    /** 退款原因说明 */
    private String reason;
    /** 用户发起退款申请的时间 */
    private LocalDateTime requestedAt;
    /** 记录创建时间 */
    private LocalDateTime createdAt;

    /** 无参构造，供 MyBatis-Plus 使用。 */
    public Refund() {
    }

    /**
     * 构造退款记录（不含 id 与 createdAt）。
     *
     * @param orderNo     订单号
     * @param status      退款状态
     * @param reason      退款原因
     * @param requestedAt 申请时间
     */
    public Refund(String orderNo, String status, String reason, LocalDateTime requestedAt) {
        this.orderNo = orderNo;
        this.status = status;
        this.reason = reason;
        this.requestedAt = requestedAt;
    }

    /** @return 主键 */
    public Long getId() { return id; }
    /** @return 关联订单号 */
    public String getOrderNo() { return orderNo; }
    /** @return 退款状态 */
    public String getStatus() { return status; }
    /** @return 退款原因 */
    public String getReason() { return reason; }
    /** @return 申请时间 */
    public LocalDateTime getRequestedAt() { return requestedAt; }
    /** @return 创建时间 */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** @param id 主键 */
    public void setId(Long id) { this.id = id; }
    /** @param orderNo 关联订单号 */
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    /** @param status 退款状态 */
    public void setStatus(String status) { this.status = status; }
    /** @param reason 退款原因 */
    public void setReason(String reason) { this.reason = reason; }
    /** @param requestedAt 申请时间 */
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    /** @param createdAt 创建时间 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
