package com.agentforge.agent.cache;

import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.Cached;
import com.alicp.jetcache.anno.CacheType;
import com.agentforge.agent.core.cache.AgentCacheConstants;
import com.agentforge.agent.rag.EmbeddingService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 带 JetCache 本地缓存的文本向量化服务。
 *
 * <p>以文本 SHA-256 为缓存 key，避免对相同文本重复调用百炼 embedding 接口。
 */
@Service
public class CachedEmbeddingService {

    private final EmbeddingService embeddingService;
    private final CachedEmbeddingService self;

    /**
     * 构造缓存向量化服务。
     *
     * @param embeddingService 底层向量化实现
     * @param self             自引用，用于触发 AOP 缓存代理
     */
    public CachedEmbeddingService(EmbeddingService embeddingService, @Lazy CachedEmbeddingService self) {
        this.embeddingService = embeddingService;
        this.self = self;
    }

    /**
     * 将文本转为向量，优先命中本地缓存。
     *
     * @param text 待向量化文本
     * @return 浮点向量
     */
    public List<Float> embed(String text) {
        return self.cachedEmbed(text);
    }

    @Cached(
        name = AgentCacheConstants.EMBEDDING_KEY,
        key = "T(com.agentforge.agent.cache.AgentCacheKeys).sha256(#text)",
        expire = AgentCacheConstants.EMBEDDING_EXPIRE_MINUTES,
        localExpire = AgentCacheConstants.EMBEDDING_LOCAL_EXPIRE_MINUTES,
        localLimit = AgentCacheConstants.LOCAL_LIMIT,
        timeUnit = TimeUnit.MINUTES,
        cacheType = CacheType.LOCAL,
        syncLocal = true,
        cacheNullValue = true,
        serialPolicy = AgentCacheConstants.SERIAL_POLICY
    )
    @CachePenetrationProtect(timeout = AgentCacheConstants.PENETRATION_PROTECT_SECONDS)
    /**
     * 实际执行向量化的缓存方法，由 {@link #embed} 通过自引用调用以触发 AOP。
     *
     * @param text 待向量化文本
     * @return 浮点向量
     */
    public List<Float> cachedEmbed(String text) {
        return embeddingService.embed(text);
    }
}
