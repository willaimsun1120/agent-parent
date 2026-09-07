package com.agentforge.agent.cache.config;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import org.springframework.context.annotation.Configuration;

/**
 * JetCache 注解驱动缓存配置。
 *
 * <p>启用方法级缓存与 {@code @CreateCache} 注解，扫描 agent 与业务 demo 缓存包。
 */
@Configuration
@EnableMethodCache(basePackages = {"com.agentforge.agent.cache", "com.agentforge.order.cs.cache"})
@EnableCreateCacheAnnotation
public class JetCacheConfiguration {
}
