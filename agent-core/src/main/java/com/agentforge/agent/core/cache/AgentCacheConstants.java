package com.agentforge.agent.core.cache;

/**
 * 跨模块共享的 JetCache 缓存名与 TTL 常量（平台通用部分）。
 *
 * <p>业务特定的缓存常量（如订单详情）应定义在业务 Demo 模块中，
 * 不应出现在平台核心层。
 */
public final class AgentCacheConstants {

    private AgentCacheConstants() {
    }

    public static final String EMBEDDING_KEY = "agent:embedding";
    public static final String RAG_SEARCH_KEY = "agent:rag-search";
    public static final String PROMPT_TEMPLATE_KEY = "agent:prompt-template";

    public static final int EMBEDDING_EXPIRE_MINUTES = 7 * 24 * 60;
    public static final int EMBEDDING_LOCAL_EXPIRE_MINUTES = 5;

    public static final int RAG_EXPIRE_MINUTES = 15;
    public static final int RAG_LOCAL_EXPIRE_MINUTES = 2;
    public static final int RAG_REFRESH_MINUTES = 10;
    public static final int RAG_STOP_REFRESH_MINUTES = 120;

    public static final int PROMPT_EXPIRE_MINUTES = 7 * 24 * 60;
    public static final int PROMPT_LOCAL_EXPIRE_MINUTES = 60;

    public static final int LOCAL_LIMIT = 20_000;
    public static final int PENETRATION_PROTECT_SECONDS = 5;
    public static final String SERIAL_POLICY = "java";
}
