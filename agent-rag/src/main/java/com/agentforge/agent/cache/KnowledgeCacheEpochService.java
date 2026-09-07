package com.agentforge.agent.cache;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** 知识库版本 epoch：每次 sync/publish 后递增，用于 RAG 缓存 key 失效。 */
@Component
public class KnowledgeCacheEpochService {

    private final AtomicLong epoch = new AtomicLong(1);

    /**
     * 获取当前知识库缓存版本号。
     *
     * @return 当前 epoch 值
     */
    public long current() {
        return epoch.get();
    }

    /**
     * 递增知识库缓存版本号，使旧 RAG 缓存 key 失效。
     *
     * @return 递增后的 epoch 值
     */
    public long bump() {
        return epoch.incrementAndGet();
    }
}
