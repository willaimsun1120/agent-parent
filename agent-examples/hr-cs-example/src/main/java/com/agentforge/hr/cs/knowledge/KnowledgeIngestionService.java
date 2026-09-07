package com.agentforge.hr.cs.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.agentforge.agent.cache.AgentCacheEvictionService;
import com.agentforge.agent.core.spi.KnowledgeIngestionFacade;
import com.agentforge.agent.core.spi.LocalKnowledgeSource;
import com.agentforge.agent.vector.core.VectorPoint;
import com.agentforge.agent.vector.core.VectorStore;
import com.agentforge.agent.rag.EmbeddingService;
import com.agentforge.agent.rag.KnowledgeDocument;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 知识库向量入库服务（Qdrant / Milvus 可切换）。
 */
@Service
public class KnowledgeIngestionService implements KnowledgeIngestionFacade, LocalKnowledgeSource {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeChunkService chunkService;
    private final AgentCacheEvictionService cacheEvictionService;

    public KnowledgeIngestionService(VectorStore vectorStore, EmbeddingService embeddingService,
                                     KnowledgeArticleMapper articleMapper, KnowledgeChunkService chunkService,
                                     AgentCacheEvictionService cacheEvictionService) {
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
        this.articleMapper = articleMapper;
        this.chunkService = chunkService;
        this.cacheEvictionService = cacheEvictionService;
    }

    @Override
    public Map<String, Object> ingestKnowledgeBase() {
        return syncPublishedArticles();
    }

    @Override
    public Map<String, Object> syncPublishedArticles() {
        log.info("开始全量同步已发布知识到 {} collection={}", vectorStore.type(), vectorStore.collectionName());
        vectorStore.ensureCollection();

        List<KnowledgeArticle> published = listPublishedArticles();
        Set<String> publishedDocCodes = new HashSet<>();
        List<KnowledgeChunk> allChunks = new ArrayList<>();

        for (KnowledgeArticle article : published) {
            publishedDocCodes.add(article.getDocCode());
            vectorStore.deleteByDocCode(article.getDocCode());
            allChunks.addAll(rebuildAndCollect(article));
        }

        cleanupUnpublishedVectors(publishedDocCodes);
        upsertPoints(allChunks, published);
        markArticlesSynced(published);
        cacheEvictionService.onKnowledgeChanged();
        log.info("知识向量同步完成 articles={} points={}", published.size(), allChunks.size());
        return Map.of(
            "vectorStore", vectorStore.type(),
            "collection", vectorStore.collectionName(),
            "articles", published.size(),
            "points", allChunks.size()
        );
    }

    @Override
    public Map<String, Object> syncArticle(String docCode) {
        vectorStore.ensureCollection();
        KnowledgeArticle article = articleMapper.selectOne(new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getDocCode, docCode)
            .last("LIMIT 1"));
        if (article == null) {
            throw new IllegalArgumentException("Knowledge article not found: " + docCode);
        }
        if (!"PUBLISHED".equals(article.getStatus())) {
            vectorStore.deleteByDocCode(docCode);
            chunkService.deleteByDocCode(docCode);
            article.setVectorSyncedAt(null);
            articleMapper.updateById(article);
            cacheEvictionService.onKnowledgeChanged();
            return Map.of("docCode", docCode, "articles", 0, "points", 0, "removed", true);
        }
        vectorStore.deleteByDocCode(docCode);
        List<KnowledgeChunk> chunks = rebuildAndCollect(article);
        upsertPoints(chunks, List.of(article));
        article.setVectorSyncedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        cacheEvictionService.onKnowledgeChanged();
        return Map.of("docCode", docCode, "articles", 1, "points", chunks.size());
    }

    @Override
    public List<LocalKnowledgeSource.KnowledgeDocumentRef> localDocuments() {
        List<LocalKnowledgeSource.KnowledgeDocumentRef> documents = new ArrayList<>();
        for (KnowledgeArticle article : listPublishedArticles()) {
            for (KnowledgeChunk chunk : chunkService.listByDocCode(article.getDocCode())) {
                documents.add(new LocalKnowledgeSource.KnowledgeDocumentRef(
                    chunk.getVectorPointId(),
                    chunk.getDocCode() + " · " + article.getTitle(),
                    chunk.getContent(),
                    article.getDocCode(),
                    article.getTitle(),
                    article.getCategory(),
                    article.getVersion()
                ));
            }
        }
        return documents;
    }

    List<KnowledgeDocument> readKnowledgeDocuments() {
        List<KnowledgeDocument> documents = new ArrayList<>();
        for (KnowledgeArticle article : listPublishedArticles()) {
            for (KnowledgeChunk chunk : chunkService.listByDocCode(article.getDocCode())) {
                documents.add(toDocument(article, chunk, 1.0));
            }
        }
        return documents;
    }

    private List<KnowledgeChunk> rebuildAndCollect(KnowledgeArticle article) {
        return chunkService.rebuildChunks(article);
    }

    private void cleanupUnpublishedVectors(Set<String> publishedDocCodes) {
        List<KnowledgeArticle> all = articleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
            .select(KnowledgeArticle::getDocCode, KnowledgeArticle::getStatus));
        for (KnowledgeArticle article : all) {
            if (!publishedDocCodes.contains(article.getDocCode())) {
                vectorStore.deleteByDocCode(article.getDocCode());
                chunkService.deleteByDocCode(article.getDocCode());
            }
        }
    }

    private void upsertPoints(List<KnowledgeChunk> chunks, List<KnowledgeArticle> articles) {
        Map<String, KnowledgeArticle> articleMap = articles.stream()
            .collect(java.util.stream.Collectors.toMap(KnowledgeArticle::getDocCode, article -> article));
        if (chunks.isEmpty()) {
            return;
        }
        List<VectorPoint> points = chunks.stream()
            .map(chunk -> toPoint(chunk, articleMap.get(chunk.getDocCode())))
            .toList();
        vectorStore.upsert(points);
    }

    private VectorPoint toPoint(KnowledgeChunk chunk, KnowledgeArticle article) {
        return new VectorPoint(
            chunk.getVectorPointId(),
            embeddingService.embed(chunk.getContent()),
            Map.of(
                "source", chunk.getDocCode() + " · " + article.getTitle(),
                "doc_code", chunk.getDocCode(),
                "title", article.getTitle(),
                "category", article.getCategory(),
                "version", article.getVersion(),
                "content", chunk.getContent()
            )
        );
    }

    private KnowledgeDocument toDocument(KnowledgeArticle article, KnowledgeChunk chunk, double score) {
        return new KnowledgeDocument(
            chunk.getVectorPointId(),
            chunk.getDocCode() + " · " + article.getTitle(),
            chunk.getContent(),
            score,
            article.getDocCode(),
            article.getTitle(),
            article.getCategory(),
            article.getVersion()
        );
    }

    private void markArticlesSynced(List<KnowledgeArticle> articles) {
        LocalDateTime now = LocalDateTime.now();
        for (KnowledgeArticle article : articles) {
            article.setVectorSyncedAt(now);
            articleMapper.updateById(article);
        }
    }

    private List<KnowledgeArticle> listPublishedArticles() {
        return articleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getStatus, "PUBLISHED")
            .orderByAsc(KnowledgeArticle::getDocCode));
    }
}
