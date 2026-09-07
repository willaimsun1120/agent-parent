package com.agentforge.agent.vector.core;

/**
 * 向量检索命中结果（与具体向量引擎无关）。
 */
public record VectorSearchHit(
    String id,
    String source,
    String content,
    double score,
    String docCode,
    String title,
    String category,
    int version
) {
    /** 生成可展示引用，如 leave-policy v1 · 请假与年假政策 */
    public String citation() {
        if (docCode != null && !docCode.isBlank()) {
            return docCode + " v" + version + " · " + title;
        }
        return source;
    }
}
