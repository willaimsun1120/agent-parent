package com.agentforge.agent.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Agent 平台缓存失效服务。
 *
 * <p>在知识库变更后递增 epoch，间接使 RAG 检索缓存失效。
 */
@Service
public class AgentCacheEvictionService {
    private static final Logger log = LoggerFactory.getLogger(AgentCacheEvictionService.class);

    private final KnowledgeCacheEpochService knowledgeCacheEpochService;

    /**
     * 构造缓存失效服务。
     *
     * @param knowledgeCacheEpochService 知识库缓存 epoch 管理器
     */
    public AgentCacheEvictionService(KnowledgeCacheEpochService knowledgeCacheEpochService) {
        this.knowledgeCacheEpochService = knowledgeCacheEpochService;
    }

    /** 知识库变更后递增 epoch，使 RAG 缓存 key 失效。 */
    public void onKnowledgeChanged() {
        long epoch = knowledgeCacheEpochService.bump();
        log.info("知识库缓存 epoch 已递增 epoch={}", epoch);
    }
}
