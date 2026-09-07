package com.agentforge.agent.observability;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent 工具调用日志实体，映射表 {@code agent_tool_logs}。
 *
 * <p>每次工具执行（订单查询、知识库检索、HITL 创建等）写入一条记录，
 * 按 traceId 串联同一次问答中的全部工具调用，供 {@link ObservabilityController#tools} 查询。
 */
@TableName("agent_tool_logs")
public class AgentToolLog {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联链路追踪 ID */
    private String traceId;
    /** 工具名称 */
    private String toolName;
    /** 入参 JSON */
    private String inputJson;
    /** 出参摘要 */
    private String outputSummary;
    /** 执行耗时毫秒 */
    private Long durationMs;
    /** 是否执行成功 */
    private Boolean success;
    /** 创建时间 */
    private LocalDateTime createdAt;

    public AgentToolLog() {
    }

    /**
     * 业务构造，写入工具调用核心字段。
     */
    public AgentToolLog(String traceId, String toolName, String inputJson, String outputSummary, Long durationMs,
                        Boolean success) {
        this.traceId = traceId;
        this.toolName = toolName;
        this.inputJson = inputJson;
        this.outputSummary = outputSummary;
        this.durationMs = durationMs;
        this.success = success;
    }

    public Long getId() { return id; }
    public String getTraceId() { return traceId; }
    public String getToolName() { return toolName; }
    public String getInputJson() { return inputJson; }
    public String getOutputSummary() { return outputSummary; }
    public Long getDurationMs() { return durationMs; }
    public Boolean getSuccess() { return success; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public void setInputJson(String inputJson) { this.inputJson = inputJson; }
    public void setOutputSummary(String outputSummary) { this.outputSummary = outputSummary; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public void setSuccess(Boolean success) { this.success = success; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
