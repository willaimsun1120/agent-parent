package com.agentforge.agent.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** {@link KnowledgeDocument} 记录类型字段存取测试。 */
class KnowledgeDocumentTest {

    /** 验证简化构造器正确保存来源、内容与分数。 */
    @Test
    void storesKnowledgeHitFields() {
        KnowledgeDocument document = new KnowledgeDocument("refund#0", "refund-policy.md", "7 days", 0.91);

        assertThat(document.source()).isEqualTo("refund-policy.md");
        assertThat(document.content()).contains("7");
        assertThat(document.score()).isGreaterThan(0.9);
    }
}
