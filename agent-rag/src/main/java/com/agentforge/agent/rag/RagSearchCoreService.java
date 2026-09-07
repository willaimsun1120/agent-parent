package com.agentforge.agent.rag;

import com.agentforge.agent.cache.CachedEmbeddingService;
import com.agentforge.agent.core.spi.LocalKnowledgeSource;
import com.agentforge.agent.vector.core.VectorStore;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * RAG 核心检索服务（无观测落库）。
 *
 * <p>优先查询向量库（Qdrant / Milvus）；连接或查询失败时降级为本地关键词匹配检索。
 * 由 {@link com.agentforge.agent.cache.CachedRagSearchService} 包装缓存。
 */
@Service
public class RagSearchCoreService {

    private final VectorStore vectorStore;
    private final CachedEmbeddingService embeddingService;
    private final LocalKnowledgeSource localKnowledgeSource;

    public RagSearchCoreService(VectorStore vectorStore,
                                CachedEmbeddingService embeddingService,
                                LocalKnowledgeSource localKnowledgeSource) {
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
        this.localKnowledgeSource = localKnowledgeSource;
    }

    /**
     * 执行知识检索，向量库不可用时自动降级本地检索。
     */
    public List<KnowledgeDocument> search(String question, int topK, Optional<String> category) {
        try {
            List<Float> queryVector = embeddingService.embed(question);
            return vectorStore.search(queryVector, topK, category).stream()
                .map(KnowledgeDocument::from)
                .toList();
        } catch (RuntimeException ex) {
            return searchLocal(question, topK, category);
        }
    }
    private List<KnowledgeDocument> searchLocal(String question, int topK, Optional<String> category) {
        String lower = question.toLowerCase();
        return localKnowledgeSource.localDocuments().stream()
            .filter(doc -> category.isEmpty() || category.get().equalsIgnoreCase(doc.category()))
            .map(document -> new KnowledgeDocument(
                document.id(),
                document.source(),
                document.content(),
                score(lower, document.content(), document.category()),
                document.docCode(),
                document.title(),
                document.category(),
                document.version()
            ))
            .sorted(Comparator.comparingDouble(KnowledgeDocument::score).reversed())
            .limit(topK)
            .toList();
    }

    /** 基于问题关键词与文档分类/内容的简单打分规则。 */
    private double score(String question, String content, String category) {
        String lower = content.toLowerCase();
        double score = 0.1;
        if (question.contains("退款") || question.contains("refund")) {
            score += "refund".equalsIgnoreCase(category) || lower.contains("refund") ? 0.8 : 0;
        }
        if (question.contains("支付") || question.contains("payment")) {
            score += "payment".equalsIgnoreCase(category) || lower.contains("payment") ? 0.8 : 0;
        }
        if (question.contains("权益") || question.contains("benefit")) {
            score += "benefit".equalsIgnoreCase(category) || lower.contains("benefit") ? 0.8 : 0;
        }
        return score;
    }
}
