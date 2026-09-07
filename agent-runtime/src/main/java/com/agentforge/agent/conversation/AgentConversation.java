package com.agentforge.agent.conversation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 会话元数据实体，映射表 {@code agent_conversations}。
 *
 * <p>每个 sessionId 对应一行，存储会话级 JSON 状态（{@link com.agentforge.agent.session.DbSessionStateStore}），
 * 具体消息内容在 {@link AgentConversationTurn} 中按轮次存储。
 */
@TableName("agent_conversations")
public class AgentConversation {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 会话唯一标识（客户端传入或平台生成 sess-uuid） */
    private String sessionId;
    /** 会话结构化状态 JSON，列名 session_state */
    @TableField("session_state")
    private String sessionStateJson;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getSessionStateJson() { return sessionStateJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setSessionStateJson(String sessionStateJson) { this.sessionStateJson = sessionStateJson; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
