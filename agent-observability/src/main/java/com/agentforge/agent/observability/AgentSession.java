package com.agentforge.agent.observability;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent 问答会话记录实体，映射表 {@code agent_sessions}。
 *
 * <p>每次 chat 完成后由 {@link com.agentforge.agent.runtime.AgentExecutionSupport#saveSession} 写入，
 * 供可观测性 API 与审计追溯；与 {@code agent_conversation_turns} 互补（本表侧重单次问答快照）。
 */
@TableName("agent_sessions")
public class AgentSession {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 链路追踪 ID，关联工具/RAG 日志 */
    private String traceId;
    /** 多轮会话 ID */
    private String sessionId;
    /** 会话内轮次序号 */
    private Integer turnIndex;
    /** 用户问题 */
    private String question;
    /** 模型回答（已清洗） */
    private String answer;
    /** 问答耗时毫秒 */
    private Long durationMs;
    /** 创建时间 */
    private LocalDateTime createdAt;

    public AgentSession() {
    }

    /**
     * 简化构造（不含 sessionId/turnIndex，兼容旧调用）。
     */
    public AgentSession(String traceId, String question, String answer, Long durationMs) {
        this(traceId, null, 0, question, answer, durationMs);
    }

    /**
     * 完整构造，写入全部业务字段。
     */
    public AgentSession(String traceId, String sessionId, Integer turnIndex, String question, String answer, Long durationMs) {
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.turnIndex = turnIndex;
        this.question = question;
        this.answer = answer;
        this.durationMs = durationMs;
    }

    public Long getId() { return id; }
    public String getTraceId() { return traceId; }
    public String getSessionId() { return sessionId; }
    public Integer getTurnIndex() { return turnIndex; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public Long getDurationMs() { return durationMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setTurnIndex(Integer turnIndex) { this.turnIndex = turnIndex; }
    public void setQuestion(String question) { this.question = question; }
    public void setAnswer(String answer) { this.answer = answer; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
