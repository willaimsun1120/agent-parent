package com.agentforge.agent.cache;

import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.Cached;
import com.alicp.jetcache.anno.CacheType;
import com.agentforge.agent.core.cache.AgentCacheConstants;
import com.agentforge.agent.rag.KnowledgeDocument;
import com.agentforge.agent.rag.RagSearchCoreService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 带 JetCache 本地缓存的 RAG 检索服务。
 *
 * <p>缓存 key 包含知识库 epoch，知识变更后自动失效；支持穿透保护与定时刷新。
 */
@Service
public class CachedRagSearchService {

    private final RagSearchCoreService ragSearchCoreService;
    private final KnowledgeCacheEpochService knowledgeCacheEpochService;
    private final CachedRagSearchService self;

    /**
     * 构造缓存检索服务。
     *
     * @param ragSearchCoreService       核心检索实现
     * @param knowledgeCacheEpochService 知识库 epoch 服务
     * @param self                       自引用，用于触发 AOP 缓存代理
     */
    public CachedRagSearchService(RagSearchCoreService ragSearchCoreService,
                                    KnowledgeCacheEpochService knowledgeCacheEpochService,
                                    @Lazy CachedRagSearchService self) {
        this.ragSearchCoreService = ragSearchCoreService;
        this.knowledgeCacheEpochService = knowledgeCacheEpochService;
        this.self = self;
    }

    /**
     * 检索知识库，命中 JetCache 时直接返回缓存结果。
     *
     * @param question 检索问题
     * @param topK     返回条数
     * @param category 可选分类过滤
     * @return 知识片段列表
     */
    public List<KnowledgeDocument> search(String question, int topK, Optional<String> category) {
        String categoryValue = category.orElse(null);
        String cacheKey = AgentCacheKeys.ragKey(
            question, topK, categoryValue, knowledgeCacheEpochService.current());
        return self.cachedSearch(cacheKey, question, topK, categoryValue);
    }

    @Cached(
        name = AgentCacheConstants.RAG_SEARCH_KEY,
        key = "#cacheKey",
        expire = AgentCacheConstants.RAG_EXPIRE_MINUTES,
        localExpire = AgentCacheConstants.RAG_LOCAL_EXPIRE_MINUTES,
        localLimit = AgentCacheConstants.LOCAL_LIMIT,
        timeUnit = TimeUnit.MINUTES,
        cacheType = CacheType.LOCAL,
        syncLocal = true,
        cacheNullValue = true,
        serialPolicy = AgentCacheConstants.SERIAL_POLICY
    )
    @CachePenetrationProtect(timeout = AgentCacheConstants.PENETRATION_PROTECT_SECONDS)
    @CacheRefresh(
        refresh = AgentCacheConstants.RAG_REFRESH_MINUTES,
        refreshLockTimeout = 1,
        stopRefreshAfterLastAccess = AgentCacheConstants.RAG_STOP_REFRESH_MINUTES,
        timeUnit = TimeUnit.MINUTES
    )
    /**
     * 实际执行检索的缓存方法，由 {@link #search} 通过自引用调用以触发 AOP。
     *
     * @param cacheKey  含 epoch 的缓存键
     * @param question  检索问题
     * @param topK      返回条数
     * @param category  分类字符串，可为 null
     * @return 知识片段列表
     */
    public List<KnowledgeDocument> cachedSearch(String cacheKey, String question, int topK, String category) {
        Optional<String> normalized = category == null || category.isBlank()
            ? Optional.empty()
            : Optional.of(category.trim());
        return ragSearchCoreService.search(question, topK, normalized);
    }
}
