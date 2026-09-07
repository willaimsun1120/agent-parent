package com.agentforge.agent.action;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * HITL 写操作请求实体，映射表 {@code agent_action_requests}。
 *
 * <p>Agent 工具发起写操作时创建 PENDING 记录，人工 confirm/reject 后更新状态，
 * confirm 时由 {@link AgentActionService} 路由到对应 {@link com.agentforge.agent.core.spi.ActionExecutor} 执行。
 */
@TableName("agent_action_requests")
public class AgentActionRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String actionId;
    private String sessionId;
    private String traceId;
    private String actionType;
    private String orderNo;
    private String payloadJson;
    private String status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getActionId() { return actionId; }
    public String getSessionId() { return sessionId; }
    public String getTraceId() { return traceId; }
    public String getActionType() { return actionType; }
    public String getOrderNo() { return orderNo; }
    public String getPayloadJson() { return payloadJson; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public void setStatus(String status) { this.status = status; }
    public void setReason(String reason) { this.reason = reason; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
