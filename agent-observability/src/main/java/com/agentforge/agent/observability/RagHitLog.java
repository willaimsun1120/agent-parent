package com.agentforge.agent.observability;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RAG 检索命中日志实体，映射表 {@code rag_hit_logs}。
 *
 * <p>每次知识库检索命中时写入，按 traceId 与问答会话关联，
 * 供 {@link ObservabilityController#ragHits} 调试 RAG 召回质量。
 */
@TableName("rag_hit_logs")
public class RagHitLog {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联链路追踪 ID */
    private String traceId;
    /** 文档来源标识（如 doc id / 文件名） */
    private String source;
    /** 相似度得分 */
    private BigDecimal score;
    /** 命中内容预览（截断） */
    private String contentPreview;
    /** 创建时间 */
    private LocalDateTime createdAt;

    public RagHitLog() {
    }

    /**
     * 业务构造，写入命中核心字段。
     */
    public RagHitLog(String traceId, String source, BigDecimal score, String contentPreview) {
        this.traceId = traceId;
        this.source = source;
        this.score = score;
        this.contentPreview = contentPreview;
    }

    public Long getId() { return id; }
    public String getTraceId() { return traceId; }
    public String getSource() { return source; }
    public BigDecimal getScore() { return score; }
    public String getContentPreview() { return contentPreview; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public void setSource(String source) { this.source = source; }
    public void setScore(BigDecimal score) { this.score = score; }
    public void setContentPreview(String contentPreview) { this.contentPreview = contentPreview; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
