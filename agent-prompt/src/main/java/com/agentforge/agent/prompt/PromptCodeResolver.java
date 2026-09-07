package com.agentforge.agent.prompt;

import java.util.Map;

/**
 * 提示词编码解析器。
 *
 * <p>兼容旧版短别名（如 {@code framework_system}），统一解析为五段式新版编码。
 */
public final class PromptCodeResolver {

    private static final Map<String, String> LEGACY_ALIASES = Map.of(
        "framework_system", PromptTemplateCodes.ORDER_CS_CHAT_FRAMEWORK_SYSTEM,
        "manual_system", PromptTemplateCodes.ORDER_CS_CHAT_MANUAL_SYSTEM,
        "framework_user", PromptTemplateCodes.ORDER_CS_CHAT_FRAMEWORK_USER,
        "framework_tool_retry_suffix", PromptTemplateCodes.ORDER_CS_CHAT_FRAMEWORK_TOOL_RETRY,
        "manual_user", PromptTemplateCodes.ORDER_CS_CHAT_MANUAL_USER
    );

    private PromptCodeResolver() {
    }

    /**
     * 将 promptCode 解析为规范的五段式编码。
     *
     * @param promptCode 原始编码或旧版别名
     * @return 规范化后的 promptCode
     * @throws IllegalArgumentException 编码为空或格式非法时
     */
    public static String resolve(String promptCode) {
        if (promptCode == null || promptCode.isBlank()) {
            throw new IllegalArgumentException("promptCode 不能为空");
        }
        String trimmed = promptCode.trim();
        String legacy = LEGACY_ALIASES.get(trimmed);
        if (legacy != null) {
            return legacy;
        }
        if (PromptCode.isValid(trimmed)) {
            return trimmed;
        }
        throw new IllegalArgumentException("Invalid promptCode: " + promptCode);
    }
}
