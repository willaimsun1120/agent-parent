package com.agentforge.order.cs.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link KnowledgeChunkService} 单元测试。
 *
 * <p>验证文本切片重叠逻辑与 Qdrant 向量点 ID 的确定性生成，不依赖数据库。
 */
class KnowledgeChunkServiceTest {

    private final KnowledgeChunkService chunkService = new KnowledgeChunkService(null);

    @Test
    void splitsWithOverlap() {
        String text = "a".repeat(1200);
        var chunks = chunkService.split(text, 500, 50);
        assertThat(chunks).hasSizeGreaterThan(2);
        assertThat(chunks.get(0)).hasSize(500);
    }

    @Test
    void buildPointIdIsStable() {
        String id1 = chunkService.buildPointId("refund-policy", 0, 2, "hello");
        String id2 = chunkService.buildPointId("refund-policy", 0, 2, "hello");
        assertThat(id1).isEqualTo(id2);
    }
}
