package com.agentforge.hr.cs.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 知识库文章实体，映射 MySQL {@code knowledge_articles} 表。
 *
 * <p>存储客服规则原文，发布后经 {@link KnowledgeIngestionService} 切片并同步至 Qdrant 向量库，
 * 供 Agent RAG 检索使用。
 */
@TableName("knowledge_articles")
public class KnowledgeArticle {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 文档唯一编码，如 refund-policy */
    private String docCode;
    /** 文章标题 */
    private String title;
    /** RAG 分类，如 refund、payment、benefit */
    private String category;
    /** 文章正文（Markdown 或纯文本） */
    private String content;
    /** 发布状态：DRAFT 或 PUBLISHED */
    private String status;
    /** 版本号，每次更新递增，用于向量点 ID 隔离 */
    private Integer version;
    /** 最近一次向量同步完成时间 */
    private LocalDateTime vectorSyncedAt;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** @return 主键 */
    public Long getId() { return id; }
    /** @return 文档编码 */
    public String getDocCode() { return docCode; }
    /** @return 标题 */
    public String getTitle() { return title; }
    /** @return RAG 分类 */
    public String getCategory() { return category; }
    /** @return 正文内容 */
    public String getContent() { return content; }
    /** @return 发布状态 */
    public String getStatus() { return status; }
    /** @return 版本号 */
    public Integer getVersion() { return version; }
    /** @return 向量同步时间 */
    public LocalDateTime getVectorSyncedAt() { return vectorSyncedAt; }
    /** @return 创建时间 */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** @return 更新时间 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @param id 主键 */
    public void setId(Long id) { this.id = id; }
    /** @param docCode 文档编码 */
    public void setDocCode(String docCode) { this.docCode = docCode; }
    /** @param title 标题 */
    public void setTitle(String title) { this.title = title; }
    /** @param category RAG 分类 */
    public void setCategory(String category) { this.category = category; }
    /** @param content 正文内容 */
    public void setContent(String content) { this.content = content; }
    /** @param status 发布状态 */
    public void setStatus(String status) { this.status = status; }
    /** @param version 版本号 */
    public void setVersion(Integer version) { this.version = version; }
    /** @param vectorSyncedAt 向量同步时间 */
    public void setVectorSyncedAt(LocalDateTime vectorSyncedAt) { this.vectorSyncedAt = vectorSyncedAt; }
    /** @param createdAt 创建时间 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    /** @param updatedAt 更新时间 */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
