package com.agentforge.order.cs.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 知识库切片实体，映射 MySQL {@code knowledge_chunks} 表。
 *
 * <p>由 {@link KnowledgeChunkService} 将文章正文按固定窗口切分，
 * 每个切片对应 Qdrant 中的一个向量点（{@link #vectorPointId}）。
 */
@TableName("knowledge_chunks")
public class KnowledgeChunk {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属文档编码 */
    private String docCode;
    /** 切片序号，从 0 开始 */
    private Integer chunkIndex;
    /** Qdrant 向量点 ID（UUID，由 docCode+index+version+content 确定性生成） */
    private String vectorPointId;
    /** 切片文本内容 */
    private String content;
    /** 切片所属文章版本号 */
    private Integer articleVersion;
    /** 创建时间 */
    private LocalDateTime createdAt;

    /** @return 主键 */
    public Long getId() { return id; }
    /** @return 文档编码 */
    public String getDocCode() { return docCode; }
    /** @return 切片序号 */
    public Integer getChunkIndex() { return chunkIndex; }
    /** @return Qdrant 向量点 ID */
    public String getVectorPointId() { return vectorPointId; }
    /** @return 切片文本 */
    public String getContent() { return content; }
    /** @return 文章版本号 */
    public Integer getArticleVersion() { return articleVersion; }
    /** @return 创建时间 */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param id 主键 */
    public void setId(Long id) { this.id = id; }
    /** @param docCode 文档编码 */
    public void setDocCode(String docCode) { this.docCode = docCode; }
    /** @param chunkIndex 切片序号 */
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    /** @param vectorPointId Qdrant 向量点 ID */
    public void setVectorPointId(String vectorPointId) { this.vectorPointId = vectorPointId; }
    /** @param content 切片文本 */
    public void setContent(String content) { this.content = content; }
    /** @param articleVersion 文章版本号 */
    public void setArticleVersion(Integer articleVersion) { this.articleVersion = articleVersion; }
    /** @param createdAt 创建时间 */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
