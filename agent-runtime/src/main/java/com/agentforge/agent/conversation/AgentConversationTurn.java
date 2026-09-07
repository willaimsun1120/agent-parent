package com.agentforge.agent.conversation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 对话轮次实体，映射表 {@code agent_conversation_turns}。
 *
 * <p>每条记录表示某 session 某轮次中 user 或 assistant 的一条消息，
 * 由 {@link AgentConversationService} 读写，用于多轮 prompt 注入与审计。
 */
@TableName("agent_conversation_turns")
public class AgentConversationTurn {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属会话 ID */
    private String sessionId;
    /** 轮次序号（从 1 递增） */
    private Integer turnIndex;
    /** 关联的链路追踪 ID */
    private String traceId;
    /** 消息角色：user / assistant */
    private String role;
    /** 消息正文 */
    private String content;
    /** 创建时间 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public Integer getTurnIndex() { return turnIndex; }
    public String getTraceId() { return traceId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setTurnIndex(Integer turnIndex) { this.turnIndex = turnIndex; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
