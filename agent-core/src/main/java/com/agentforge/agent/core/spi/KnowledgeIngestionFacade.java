package com.agentforge.agent.core.spi;

import java.util.Map;

/**
 * 知识库向量同步 SPI，由业务 Demo 实现。
 *
 * <p>供管理端或定时任务触发，将本地文档写入向量库（如 Qdrant）。
 */
public interface KnowledgeIngestionFacade {

    /**
     * 全量导入知识库。
     *
     * @return 同步结果摘要（如文档数、耗时等）
     */
    Map<String, Object> ingestKnowledgeBase();

    /**
     * 同步所有已发布文章。
     *
     * @return 同步结果摘要
     */
    Map<String, Object> syncPublishedArticles();

    /**
     * 按文档编码同步单篇文章。
     *
     * @param docCode 业务文档编码
     * @return 同步结果摘要
     */
    Map<String, Object> syncArticle(String docCode);
}
