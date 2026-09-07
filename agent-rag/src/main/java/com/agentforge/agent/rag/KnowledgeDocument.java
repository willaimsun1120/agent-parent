package com.agentforge.agent.rag;

import com.agentforge.agent.vector.core.VectorSearchHit;
import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 检索命中的知识片段。
 *
 * @param id       向量库或本地文档 ID
 * @param source   来源文件名或路径
 * @param content  片段正文
 * @param score    相似度分数
 * @param docCode  业务文档编码
 * @param title    文档标题
 * @param category 业务分类
 * @param version  文档版本号
 */
public record KnowledgeDocument(
    String id,
    String source,
    String content,
    double score,
    String docCode,
    String title,
    String category,
    int version
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 简化构造器，用于仅含基础字段的场景。
     *
     * @param id      文档 ID
     * @param source  来源
     * @param content 正文
     * @param score   分数
     */
    public KnowledgeDocument(String id, String source, String content, double score) {
        this(id, source, content, score, "", source, "", 0);
    }

    /**
     * 从向量检索命中结果转换为 RAG 文档 DTO。
     */
    public static KnowledgeDocument from(VectorSearchHit hit) {
        return new KnowledgeDocument(
            hit.id(),
            hit.source(),
            hit.content(),
            hit.score(),
            hit.docCode(),
            hit.title(),
            hit.category(),
            hit.version()
        );
    }

    /**
     * 生成可展示的引用字符串，优先使用 docCode + 版本 + 标题。
     *
     * @return 引用文本
     */
    public String citation() {
        if (docCode != null && !docCode.isBlank()) {
            return docCode + " v" + version + " · " + title;
        }
        return source;
    }
}
