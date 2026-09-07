package com.agentforge.agent.core.spi;

import java.util.Collections;
import java.util.Map;

/**
 * 会话级扩展状态，经 {@link SessionStateStore} JSON 持久化。
 *
 * <p>内部 {@link #attributes()} 返回不可变 Map，防止调用方意外修改。
 * 如需更新状态，应构造新的 {@code SessionState} 实例后调用 {@link SessionStateStore#save}。
 *
 * @param attributes 键值对形式的业务自定义属性（不可变）
 */
public record SessionState(Map<String, Object> attributes) {

    /**
     * 创建状态实例，将传入的 Map 包装为不可变视图。
     *
     * @param attributes 业务自定义属性
     */
    public SessionState {
        attributes = Collections.unmodifiableMap(attributes);
    }

    /** 创建空状态实例。 */
    public SessionState() {
        this(Map.of());
    }
}
