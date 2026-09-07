package com.agentforge.agent.rag;

import com.agentforge.agent.cache.CachedRagSearchService;
import com.agentforge.agent.observability.AgentMetrics;
import com.agentforge.agent.observability.RagHitLog;
import com.agentforge.agent.observability.RagHitLogMapper;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * RAG 检索门面：向量检索 + 命中落库 + 指标。
 *
 * <p>底层经 {@link CachedRagSearchService}（JetCache）访问 Qdrant；
 * 每次检索将 top 结果写入 {@code rag_hit_logs}，与 traceId 对齐可观测链路。
 * category 过滤由调用方传入（通常来自 {@link com.agentforge.agent.core.spi.RagCategoryResolver}）。
 */
@Service
public class RagSearchService {
    private static final Logger log = LoggerFactory.getLogger(RagSearchService.class);

    private final CachedRagSearchService cachedRagSearchService;
    private final RagHitLogMapper ragHitLogMapper;
    private final AgentMetrics metrics;

    /**
     * 构造 RAG 检索门面。
     *
     * @param cachedRagSearchService 带 JetCache 的检索服务
     * @param ragHitLogMapper        命中日志持久化 Mapper
     * @param metrics                平台指标收集器
     */
    public RagSearchService(CachedRagSearchService cachedRagSearchService,
                            RagHitLogMapper ragHitLogMapper,
                            AgentMetrics metrics) {
        this.cachedRagSearchService = cachedRagSearchService;
        this.ragHitLogMapper = ragHitLogMapper;
        this.metrics = metrics;
    }

    /**
     * 按问题检索知识库，不限制业务分类。
     *
     * @param question 用户问题或检索文本
     * @param topK     返回的最大命中条数
     * @return 按相似度排序的知识片段列表
     */
    public List<KnowledgeDocument> search(String question, int topK) {
        return search(question, topK, Optional.empty());
    }

    /**
     * 按问题检索知识库，可选按 category 过滤。
     *
     * @param question 用户问题或检索文本
     * @param topK     返回的最大命中条数
     * @param category 业务分类（如 refund、payment），为空则不过滤
     * @return 按相似度排序的知识片段列表
     */
    public List<KnowledgeDocument> search(String question, int topK, Optional<String> category) {
        long started = System.currentTimeMillis();
        metrics.incrementRagSearch();
        log.info("开始 RAG 检索 question={} topK={} category={}", question, topK, category.orElse("-"));
        List<KnowledgeDocument> hits = cachedRagSearchService.search(question, topK, category);
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            for (KnowledgeDocument hit : hits) {
                ragHitLogMapper.insert(new RagHitLog(
                    traceId,
                    hit.citation(),
                    BigDecimal.valueOf(hit.score()),
                    preview(hit.content())
                ));
            }
        }
        metrics.recordRagDuration(System.currentTimeMillis() - started);
        log.info("RAG 检索完成 hits={} durationMs={}", hits.size(), System.currentTimeMillis() - started);
        return hits;
    }

    /** 截断内容用于命中日志预览，避免单条日志过长。 */
    private String preview(String content) {
        return content.length() <= 500 ? content : content.substring(0, 500);
    }
}
