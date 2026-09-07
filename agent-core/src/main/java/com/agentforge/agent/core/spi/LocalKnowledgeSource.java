package com.agentforge.agent.core.spi;

import java.util.List;

/**
 * 本地知识库文档来源 SPI（Qdrant 不可用时的降级检索）。
 *
 * <p>业务 Demo 提供内存或文件级文档列表，供 RAG 模块在向量库故障时兜底。
 */
public interface LocalKnowledgeSource {

    /**
     * @return 可用于本地检索的知识文档列表
     */
    List<KnowledgeDocumentRef> localDocuments();

    /**
     * 本地知识文档引用。
     *
     * @param id       文档唯一 ID
     * @param source   来源标识（如文件名）
     * @param content  正文内容
     * @param docCode  业务文档编码
     * @param title    标题
     * @param category 业务分类，用于过滤
     * @param version  版本号
     */
    record KnowledgeDocumentRef(
        String id,
        String source,
        String content,
        String docCode,
        String title,
        String category,
        int version
    ) {
    }
}
