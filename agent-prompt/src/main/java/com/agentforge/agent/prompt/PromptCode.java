package com.agentforge.agent.prompt;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 提示词编码（promptCode）编解码工具。
 *
 * <p>格式：{@code domain.module.scene.mode.role}，共五段小写下划线分隔。
 */
public final class PromptCode {

    private static final Pattern CODE_PATTERN =
        Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*){4}$");

    private PromptCode() {
    }

    /**
     * 由五段业务维度拼接生成 promptCode。
     *
     * @param bizDomain  业务领域
     * @param bizModule  业务模块
     * @param scene      业务场景
     * @param agentMode  运行模式
     * @param promptRole 提示词角色
     * @return 五段式 promptCode
     */
    public static String of(String bizDomain, String bizModule, String scene, String agentMode, String promptRole) {
        return String.join(".",
            normalize(bizDomain),
            normalize(bizModule),
            normalize(scene),
            normalize(agentMode),
            normalize(promptRole));
    }

    /**
     * 解析 promptCode 为结构化五元组。
     *
     * @param promptCode 五段式编码
     * @return 解析结果
     * @throws IllegalArgumentException 格式不合法时
     */
    public static Parsed parse(String promptCode) {
        if (promptCode == null || !CODE_PATTERN.matcher(promptCode).matches()) {
            throw new IllegalArgumentException("Invalid promptCode: " + promptCode);
        }
        String[] parts = promptCode.split("\\.");
        return new Parsed(parts[0], parts[1], parts[2], parts[3], parts[4], promptCode);
    }

    /**
     * 判断 promptCode 是否符合五段式格式。
     *
     * @param promptCode 待校验编码
     * @return 合法返回 {@code true}
     */
    public static boolean isValid(String promptCode) {
        return promptCode != null && CODE_PATTERN.matcher(promptCode).matches();
    }

    private static String normalize(String segment) {
        return segment.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * promptCode 解析后的结构化表示。
     *
     * @param bizDomain  业务领域
     * @param bizModule  业务模块
     * @param scene      业务场景
     * @param agentMode  运行模式
     * @param promptRole 提示词角色
     * @param promptCode 原始完整编码
     */
    public record Parsed(
        String bizDomain,
        String bizModule,
        String scene,
        String agentMode,
        String promptRole,
        String promptCode
    ) {
        /** @return 业务领域中文标签 */
        public String domainLabel() {
            return switch (bizDomain) {
                case "order" -> "订单";
                default -> bizDomain;
            };
        }

        /** @return 业务模块中文标签 */
        public String moduleLabel() {
            return switch (bizModule) {
                case "cs" -> "客服";
                default -> bizModule;
            };
        }

        /** @return 业务场景中文标签 */
        public String sceneLabel() {
            return switch (scene) {
                case "chat" -> "对话";
                default -> scene;
            };
        }

        /** @return 运行模式中文标签 */
        public String agentModeLabel() {
            return switch (agentMode) {
                case "framework" -> "框架模式";
                case "manual" -> "手写模式";
                default -> agentMode;
            };
        }

        /** @return 提示词角色中文标签 */
        public String promptRoleLabel() {
            return switch (promptRole) {
                case "system" -> "System";
                case "user" -> "User";
                case "tool_retry" -> "工具重试";
                default -> promptRole;
            };
        }

        /**
         * 生成面包屑式中文展示文本。
         *
         * @return 各维度标签以「 · 」连接
         */
        public String breadcrumb() {
            return domainLabel() + " · " + moduleLabel() + " · " + sceneLabel()
                + " · " + agentModeLabel() + " · " + promptRoleLabel();
        }
    }
}
