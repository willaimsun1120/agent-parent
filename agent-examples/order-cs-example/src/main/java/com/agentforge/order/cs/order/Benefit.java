package com.agentforge.order.cs.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 会员权益发放记录实体，映射 MySQL {@code benefits} 表。
 *
 * <p>记录订单关联的权益名称、发放状态及失败原因，聚合进 {@link OrderDetail}；
 * 权益重试由 {@link com.agentforge.order.cs.action.RetryBenefitActionExecutor} 执行。
 */
@TableName("benefits")
public class Benefit {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户编号 */
    private String userNo;
    /** 关联订单号 */
    private String orderNo;
    /** 权益名称，如 SVIP monthly bonus */
    private String benefitName;
    /** 发放状态，如 PENDING、ISSUED、FAILED */
    private String status;
    /** 发放失败时的原因说明 */
    private String failReason;
    /** 记录创建时间 */
    private LocalDateTime createdAt;

    /** 无参构造，供 MyBatis-Plus 使用。 */
    public Benefit() {
    }

    /**
     * 构造权益记录（不含 id 与 createdAt）。
     *
     * @param userNo      用户编号
     * @param orderNo     订单号
     * @param benefitName 权益名称
     * @param status      发放状态
     * @param failReason  失败原因
     */
    public Benefit(String userNo, String orderNo, String benefitName, String status, String failReason) {
        this.userNo = userNo;
        this.orderNo = orderNo;
        this.benefitName = benefitName;
        this.status = status;
        this.failReason = failReason;
    }

    /** @return 主键 */
    public Long getId() { return id; }
    /** @return 用户编号 */
    public String getUserNo() { return userNo; }
    /** @return 关联订单号 */
    public String getOrderNo() { return orderNo; }
    /** @return 权益名称 */
    public String getBenefitName() { return benefitName; }
    /** @return 发放状态 */
    public String getStatus() { return status; }
    /** @return 失败原因 */
    public String getFailReason() { return failReason; }
    /** @return 创建时间 */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** @param id 主键 */
    public void setId(Long id) { this.id = id; }
    /** @param userNo 用户编号 */
    public void setUserNo(String userNo) { this.userNo = userNo; }
    /** @param orderNo 关联订单号 */
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    /** @param benefitName 权益名称 */
    public void setBenefitName(String benefitName) { this.benefitName = benefitName; }
    /** @param status 发放状态 */
    public void setStatus(String status) { this.status = status; }
    /** @param failReason 失败原因 */
    public void setFailReason(String failReason) { this.failReason = failReason; }
    /** @param createdAt 创建时间 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
