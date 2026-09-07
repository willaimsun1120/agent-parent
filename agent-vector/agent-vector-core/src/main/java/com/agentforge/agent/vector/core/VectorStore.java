package com.agentforge.agent.vector.core;

import java.util.List;
import java.util.Optional;

/**
 * 向量数据库 SPI（Qdrant / Milvus 等可插拔实现）。
 *
 * <p>由 {@code agent.vector-store.type} 选择具体 Bean；
 * 知识入库与 RAG 检索均通过本接口访问向量库。
 */
public interface VectorStore {

    /** 引擎类型标识：qdrant / milvus */
    String type();

    /** 当前使用的 collection 名称。 */
    String collectionName();

    /** 确保 collection 存在（幂等）。 */
    void ensureCollection();

    /** 批量 upsert 向量点。 */
    void upsert(List<VectorPoint> points);

    /** 按 doc_code 删除该文档全部向量点。 */
    void deleteByDocCode(String docCode);

    /**
     * 向量相似度检索。
     *
     * @param queryVector 问题 embedding
     * @param topK        返回条数上限
     * @param category    可选分类过滤
     */
    List<VectorSearchHit> search(List<Float> queryVector, int topK, Optional<String> category);
}
