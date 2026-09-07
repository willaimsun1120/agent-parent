package com.agentforge.agent.cache;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.Cached;
import com.alicp.jetcache.anno.CacheType;
import com.agentforge.agent.core.cache.AgentCacheConstants;
import com.agentforge.agent.prompt.PromptCodeResolver;
import com.agentforge.agent.prompt.PromptTemplateLoader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 带 JetCache 本地缓存的提示词模板读取服务。
 *
 * <p>支持预热、按 promptCode 失效与主动刷新。
 */
@Service
public class CachedPromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(CachedPromptTemplateService.class);

    private final PromptTemplateLoader promptTemplateLoader;
    private final CachedPromptTemplateService self;

    /**
     * 构造缓存模板服务。
     *
     * @param promptTemplateLoader 数据库模板加载器
     * @param self                 自引用，用于触发 AOP 缓存代理
     */
    public CachedPromptTemplateService(PromptTemplateLoader promptTemplateLoader,
                                       @Lazy CachedPromptTemplateService self) {
        this.promptTemplateLoader = promptTemplateLoader;
        this.self = self;
    }

    /**
     * 获取指定 promptCode 的模板正文，优先命中缓存。
     *
     * @param promptCode 模板编码（支持旧版别名）
     * @return 模板内容
     */
    public String getContent(String promptCode) {
        return self.cachedGetContent(PromptCodeResolver.resolve(promptCode));
    }

    /** 预热全部 ACTIVE 模板到 JetCache。 */
    public void warmup() {
        List<String> activeCodes = promptTemplateLoader.listActivePromptCodes();
        for (String promptCode : activeCodes) {
            getContent(promptCode);
        }
        log.info("提示词 JetCache 预热完成 activeCount={}", activeCodes.size());
    }

    /**
     * 失效并重新加载指定模板的缓存。
     *
     * @param promptCode 模板编码
     */
    public void refresh(String promptCode) {
        String normalized = PromptCodeResolver.resolve(promptCode);
        self.evict(normalized);
        getContent(normalized);
    }

    /**
     * 使指定 promptCode 的 JetCache 条目失效。
     *
     * @param promptCode 已规范化的模板编码
     */
    @CacheInvalidate(name = AgentCacheConstants.PROMPT_TEMPLATE_KEY, key = "#promptCode")
    public void evict(String promptCode) {
        // 触发 JetCache 失效
    }

    @Cached(
        name = AgentCacheConstants.PROMPT_TEMPLATE_KEY,
        key = "#promptCode",
        expire = AgentCacheConstants.PROMPT_EXPIRE_MINUTES,
        localExpire = AgentCacheConstants.PROMPT_LOCAL_EXPIRE_MINUTES,
        localLimit = AgentCacheConstants.LOCAL_LIMIT,
        timeUnit = TimeUnit.MINUTES,
        cacheType = CacheType.LOCAL,
        syncLocal = true,
        cacheNullValue = false,
        serialPolicy = AgentCacheConstants.SERIAL_POLICY
    )
    @CachePenetrationProtect(timeout = AgentCacheConstants.PENETRATION_PROTECT_SECONDS)
    /**
     * 实际加载模板正文的缓存方法，由 {@link #getContent} 通过自引用调用以触发 AOP。
     *
     * @param promptCode 已规范化的模板编码
     * @return 模板内容
     */
    public String cachedGetContent(String promptCode) {
        return promptTemplateLoader.loadContent(promptCode);
    }
}
