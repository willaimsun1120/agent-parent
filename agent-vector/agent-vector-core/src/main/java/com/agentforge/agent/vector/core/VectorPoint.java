package com.agentforge.agent.vector.core;

import java.util.List;
import java.util.Map;

/**
 * 向量库写入点（与具体引擎无关）。
 *
 * @param id      点 ID（Qdrant point id / Milvus 主键）
 * @param vector   embedding 向量
 * @param payload 元数据：doc_code、title、category、version、content、source
 */
public record VectorPoint(
    String id,
    List<Float> vector,
    Map<String, Object> payload
) {
}
