package com.agentforge.agent.core.mode;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent 运行模式（请求体 {@code mode} 字段或平台默认配置）。
 *
 * <ul>
 *   <li>{@code manual} — Java 手写编排，逻辑透明，默认模式，亦作框架降级兜底</li>
 *   <li>{@code agentscope} — AgentScope ReActAgent，模型自动选工具</li>
 *   <li>{@code langchain4j} — LangChain4j AiServices，模型自动选工具</li>
 * </ul>
 */
public enum AgentMode {
    MANUAL("manual"),
    AGENTSCOPE("agentscope"),
    LANGCHAIN4J("langchain4j");

    private static final Logger log = LoggerFactory.getLogger(AgentMode.class);

    private final String code;

    AgentMode(String code) {
        this.code = code;
    }

    /** 返回请求体 / 配置中使用的模式字符串（如 {@code manual}）。 */
    public String code() {
        return code;
    }

    /**
     * 将请求或配置中的模式字符串解析为枚举。
     *
     * @param value 模式字符串，支持 code 或枚举名（大小写不敏感）
     * @return 匹配的模式；空或未知值时返回 {@link #MANUAL}
     */
    public static AgentMode from(String value) {
        if (value == null || value.isBlank()) {
            return MANUAL;
        }
        return Arrays.stream(values())
            .filter(mode -> mode.code.equalsIgnoreCase(value.trim()) || mode.name().equalsIgnoreCase(value.trim()))
            .findFirst()
            .orElseGet(() -> {
                log.warn("未识别的 Agent 模式 '{}'，降级为 MANUAL", value);
                return MANUAL;
            });
    }
}
