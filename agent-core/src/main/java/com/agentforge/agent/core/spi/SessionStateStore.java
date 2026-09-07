package com.agentforge.agent.core.spi;

import java.util.Optional;

/**
 * 会话扩展状态持久化 SPI。
 *
 * <p>用于跨轮次保存业务自定义属性（如已解析的订单号），由平台在问答前后读写。
 */
public interface SessionStateStore {

    /**
     * 加载会话状态。
     *
     * @param sessionId 会话 ID
     * @return 已持久化的状态；不存在时为空
     */
    Optional<SessionState> load(String sessionId);

    /**
     * 持久化会话状态。
     *
     * @param sessionId 会话 ID
     * @param state     待保存的状态
     */
    void save(String sessionId, SessionState state);
}
