package com.agentforge.order.cs.cache;

/**
 * 订单业务 JetCache 缓存名与 TTL 常量。
 *
 * <p>订单特定缓存常量从平台 {@link com.agentforge.agent.core.cache.AgentCacheConstants} 迁出，
 * 保持平台核心层不包含业务特定常量。
 */
public final class OrderCacheConstants {

    private OrderCacheConstants() {
    }

    public static final String ORDER_DETAIL_KEY = "agent:order-detail";

    public static final int ORDER_EXPIRE_SECONDS = 60;
    public static final int ORDER_LOCAL_EXPIRE_SECONDS = 30;
    public static final int ORDER_REFRESH_SECONDS = 30;
}
