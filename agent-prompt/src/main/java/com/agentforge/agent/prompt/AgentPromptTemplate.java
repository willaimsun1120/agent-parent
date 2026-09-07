package com.agentforge.agent.prompt;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent 提示词模板实体，映射表 {@code agent_prompt_templates}。
 */
@TableName("agent_prompt_templates")
public class AgentPromptTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String promptCode;
    private String bizDomain;
    private String bizModule;
    private String scene;
    private String agentMode;
    private String promptRole;
    private String title;
    private String description;
    private String content;
    private String status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** @return 主键 ID */
    public Long getId() { return id; }
    /** @return 五段式 promptCode */
    public String getPromptCode() { return promptCode; }
    /** @return 业务领域 */
    public String getBizDomain() { return bizDomain; }
    /** @return 业务模块 */
    public String getBizModule() { return bizModule; }
    /** @return 业务场景 */
    public String getScene() { return scene; }
    /** @return 运行模式 */
    public String getAgentMode() { return agentMode; }
    /** @return 提示词角色 */
    public String getPromptRole() { return promptRole; }
    /** @return 模板标题 */
    public String getTitle() { return title; }
    /** @return 模板描述 */
    public String getDescription() { return description; }
    /** @return 模板正文 */
    public String getContent() { return content; }
    /** @return 状态（ACTIVE / DRAFT） */
    public String getStatus() { return status; }
    /** @return 版本号 */
    public Integer getVersion() { return version; }
    /** @return 创建时间 */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** @return 更新时间 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @param id 主键 ID */
    public void setId(Long id) { this.id = id; }
    /** @param promptCode 五段式 promptCode */
    public void setPromptCode(String promptCode) { this.promptCode = promptCode; }
    /** @param bizDomain 业务领域 */
    public void setBizDomain(String bizDomain) { this.bizDomain = bizDomain; }
    /** @param bizModule 业务模块 */
    public void setBizModule(String bizModule) { this.bizModule = bizModule; }
    /** @param scene 业务场景 */
    public void setScene(String scene) { this.scene = scene; }
    /** @param agentMode 运行模式 */
    public void setAgentMode(String agentMode) { this.agentMode = agentMode; }
    /** @param promptRole 提示词角色 */
    public void setPromptRole(String promptRole) { this.promptRole = promptRole; }
    /** @param title 模板标题 */
    public void setTitle(String title) { this.title = title; }
    /** @param description 模板描述 */
    public void setDescription(String description) { this.description = description; }
    /** @param content 模板正文 */
    public void setContent(String content) { this.content = content; }
    /** @param status 状态 */
    public void setStatus(String status) { this.status = status; }
    /** @param version 版本号 */
    public void setVersion(Integer version) { this.version = version; }
    /** @param createdAt 创建时间 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    /** @param updatedAt 更新时间 */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
