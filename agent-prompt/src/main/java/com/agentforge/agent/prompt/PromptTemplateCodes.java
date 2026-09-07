package com.agentforge.agent.prompt;

/**
 * 提示词编码规范（promptCode）。
 * <p>
 * 格式：{@code {bizDomain}.{bizModule}.{scene}.{agentMode}.{promptRole}}
 * <ul>
 *   <li><b>bizDomain</b> 业务领域 — 如 {@code order} 订单</li>
 *   <li><b>bizModule</b> 业务模块 — 如 {@code cs} 客服助手</li>
 *   <li><b>scene</b> 业务场景 — 如 {@code chat} 多轮对话</li>
 *   <li><b>agentMode</b> 运行模式 — {@code framework} / {@code manual}</li>
 *   <li><b>promptRole</b> 提示词角色 — {@code system} / {@code user} / {@code tool_retry}</li>
 * </ul>
 * 示例：{@code order.cs.chat.framework.system}
 */
public final class PromptTemplateCodes {

    public static final String BIZ_DOMAIN_ORDER = "order";
    public static final String BIZ_DOMAIN_HR = "hr";
    public static final String BIZ_MODULE_CS = "cs";
    public static final String SCENE_CHAT = "chat";

    public static final String AGENT_MODE_FRAMEWORK = "framework";
    public static final String AGENT_MODE_MANUAL = "manual";

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_TOOL_RETRY = "tool_retry";

    /** 订单 · 客服 · 对话 · 框架模式 · System */
    public static final String ORDER_CS_CHAT_FRAMEWORK_SYSTEM =
        PromptCode.of(BIZ_DOMAIN_ORDER, BIZ_MODULE_CS, SCENE_CHAT, AGENT_MODE_FRAMEWORK, ROLE_SYSTEM);

    /** 订单 · 客服 · 对话 · 手写模式 · System */
    public static final String ORDER_CS_CHAT_MANUAL_SYSTEM =
        PromptCode.of(BIZ_DOMAIN_ORDER, BIZ_MODULE_CS, SCENE_CHAT, AGENT_MODE_MANUAL, ROLE_SYSTEM);

    /** 订单 · 客服 · 对话 · 框架模式 · User */
    public static final String ORDER_CS_CHAT_FRAMEWORK_USER =
        PromptCode.of(BIZ_DOMAIN_ORDER, BIZ_MODULE_CS, SCENE_CHAT, AGENT_MODE_FRAMEWORK, ROLE_USER);

    /** 订单 · 客服 · 对话 · 框架模式 · 未调工具重试后缀 */
    public static final String ORDER_CS_CHAT_FRAMEWORK_TOOL_RETRY =
        PromptCode.of(BIZ_DOMAIN_ORDER, BIZ_MODULE_CS, SCENE_CHAT, AGENT_MODE_FRAMEWORK, ROLE_TOOL_RETRY);

    /** 订单 · 客服 · 对话 · 手写模式 · User */
    public static final String ORDER_CS_CHAT_MANUAL_USER =
        PromptCode.of(BIZ_DOMAIN_ORDER, BIZ_MODULE_CS, SCENE_CHAT, AGENT_MODE_MANUAL, ROLE_USER);

    /** HR · 客服 · 对话 · 框架模式 · System */
    public static final String HR_CS_CHAT_FRAMEWORK_SYSTEM =
        PromptCode.of(BIZ_DOMAIN_HR, BIZ_MODULE_CS, SCENE_CHAT, AGENT_MODE_FRAMEWORK, ROLE_SYSTEM);

    /** HR · 客服 · 对话 · 手写模式 · System */
    public static final String HR_CS_CHAT_MANUAL_SYSTEM =
        PromptCode.of(BIZ_DOMAIN_HR, BIZ_MODULE_CS, SCENE_CHAT, AGENT_MODE_MANUAL, ROLE_SYSTEM);

    /** HR · 客服 · 对话 · 框架模式 · User */
    public static final String HR_CS_CHAT_FRAMEWORK_USER =
        PromptCode.of(BIZ_DOMAIN_HR, BIZ_MODULE_CS, SCENE_CHAT, AGENT_MODE_FRAMEWORK, ROLE_USER);

    /** HR · 客服 · 对话 · 框架模式 · 未调工具重试后缀 */
    public static final String HR_CS_CHAT_FRAMEWORK_TOOL_RETRY =
        PromptCode.of(BIZ_DOMAIN_HR, BIZ_MODULE_CS, SCENE_CHAT, AGENT_MODE_FRAMEWORK, ROLE_TOOL_RETRY);

    /** HR · 客服 · 对话 · 手写模式 · User */
    public static final String HR_CS_CHAT_MANUAL_USER =
        PromptCode.of(BIZ_DOMAIN_HR, BIZ_MODULE_CS, SCENE_CHAT, AGENT_MODE_MANUAL, ROLE_USER);

    private PromptTemplateCodes() {
    }
}
