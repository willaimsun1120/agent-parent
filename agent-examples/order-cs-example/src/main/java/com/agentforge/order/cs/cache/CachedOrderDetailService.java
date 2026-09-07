package com.agentforge.order.cs.cache;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CachePenetrationProtect;
import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.Cached;
import com.alicp.jetcache.anno.CacheType;
import com.agentforge.agent.core.cache.AgentCacheConstants;
import com.agentforge.order.cs.order.OrderDetail;
import com.agentforge.order.cs.order.OrderDetailLoader;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 带 JetCache 本地缓存的订单详情服务。
 *
 * <p>包装 {@link OrderDetailLoader}，使用业务定义的 {@link OrderCacheConstants} 缓存键与 TTL，
 * 在订单变更时由业务层调用 {@link #evict(String)} 失效，与平台缓存体系对齐。
 */
@Service
public class CachedOrderDetailService {

    private final OrderDetailLoader orderDetailLoader;
    private final CachedOrderDetailService self;

    public CachedOrderDetailService(OrderDetailLoader orderDetailLoader, @Lazy CachedOrderDetailService self) {
        this.orderDetailLoader = orderDetailLoader;
        this.self = self;
    }

    public OrderDetail getOrderDetail(String orderNo) {
        return self.cachedGetOrderDetail(orderNo);
    }

    @CacheInvalidate(name = OrderCacheConstants.ORDER_DETAIL_KEY, key = "#orderNo")
    public void evict(String orderNo) {
    }

    @Cached(
        name = OrderCacheConstants.ORDER_DETAIL_KEY,
        key = "#orderNo",
        expire = OrderCacheConstants.ORDER_EXPIRE_SECONDS,
        localExpire = OrderCacheConstants.ORDER_LOCAL_EXPIRE_SECONDS,
        localLimit = AgentCacheConstants.LOCAL_LIMIT,
        timeUnit = TimeUnit.SECONDS,
        cacheType = CacheType.LOCAL,
        syncLocal = true,
        cacheNullValue = false,
        serialPolicy = AgentCacheConstants.SERIAL_POLICY
    )
    @CachePenetrationProtect(timeout = AgentCacheConstants.PENETRATION_PROTECT_SECONDS)
    @CacheRefresh(
        refresh = OrderCacheConstants.ORDER_REFRESH_SECONDS,
        refreshLockTimeout = 1,
        stopRefreshAfterLastAccess = 5,
        timeUnit = TimeUnit.MINUTES
    )
    public OrderDetail cachedGetOrderDetail(String orderNo) {
        return orderDetailLoader.load(orderNo);
    }
}
