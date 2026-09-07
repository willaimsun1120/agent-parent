package com.agentforge.agent.prompt;

import com.agentforge.agent.cache.CachedPromptTemplateService;
import com.agentforge.agent.core.config.AgentPlatformProperties;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 提示词模板读取门面（DB + JetCache）。
 *
 * <p>模板按五段式 promptCode 存储（见 {@link PromptTemplateCodes}），
 * 业务通过 {@link com.agentforge.agent.core.spi.PromptTemplateSeeder} 在启动时写入默认模板。
 * manual / framework 模式从此获取 system / user prompt。
 */
@Component
public class PromptTemplateProvider {

    private final CachedPromptTemplateService cachedPromptTemplateService;
    private final AgentPlatformProperties platformProperties;

    /**
     * 构造提示词模板门面。
     *
     * @param cachedPromptTemplateService 带 JetCache 的模板读取服务
     * @param platformProperties          平台配置（bizDomain / bizModule 决定 promptCode 前缀）
     */
    public PromptTemplateProvider(CachedPromptTemplateService cachedPromptTemplateService,
                                  AgentPlatformProperties platformProperties) {
        this.cachedPromptTemplateService = cachedPromptTemplateService;
        this.platformProperties = platformProperties;
    }

    /** 重新预热全部 ACTIVE 提示词模板到 JetCache。 */
    public void reload() {
        cachedPromptTemplateService.warmup();
    }

    /**
     * 获取框架模式 System 提示词。
     *
     * @return 模板正文
     */
    public String getFrameworkSystem() {
        return require(promptCode(PromptTemplateCodes.SCENE_CHAT,
            PromptTemplateCodes.AGENT_MODE_FRAMEWORK, PromptTemplateCodes.ROLE_SYSTEM));
    }

    /**
     * 获取手写模式 System 提示词。
     *
     * @return 模板正文
     */
    public String getManualSystem() {
        return require(promptCode(PromptTemplateCodes.SCENE_CHAT,
            PromptTemplateCodes.AGENT_MODE_MANUAL, PromptTemplateCodes.ROLE_SYSTEM));
    }

    /**
     * 渲染框架模式 User 提示词。
     *
     * @param question 当前用户问题
     * @param history  历史对话文本
     * @return 替换占位符后的 User 提示词
     */
    public String frameworkUserPrompt(String question, String history) {
        return render(promptCode(PromptTemplateCodes.SCENE_CHAT,
            PromptTemplateCodes.AGENT_MODE_FRAMEWORK, PromptTemplateCodes.ROLE_USER), Map.of(
            "history", historyBlock(history),
            "question", safe(question)
        ));
    }

    /**
     * 渲染框架模式「未调工具重试」提示词（User + 重试后缀）。
     *
     * @param question 当前用户问题
     * @param history  历史对话文本
     * @return 带强制工具调用后缀的 User 提示词
     */
    public String frameworkToolRetryPrompt(String question, String history) {
        return frameworkUserPrompt(question, history)
            + require(promptCode(PromptTemplateCodes.SCENE_CHAT,
                PromptTemplateCodes.AGENT_MODE_FRAMEWORK, PromptTemplateCodes.ROLE_TOOL_RETRY));
    }

    /**
     * 渲染手写模式 User 提示词。
     *
     * @param question       当前用户问题
     * @param history        历史对话文本
     * @param bizContext     业务查询上下文（订单事实 / 员工事实等）
     * @param policyContext  RAG 策略上下文
     * @return 替换占位符后的 User 提示词
     */
    public String manualUserPrompt(String question, String history, String bizContext, String policyContext) {
        return render(promptCode(PromptTemplateCodes.SCENE_CHAT,
            PromptTemplateCodes.AGENT_MODE_MANUAL, PromptTemplateCodes.ROLE_USER), Map.of(
            "history", historyBlock(history),
            "question", safe(question),
            "bizContext", safe(bizContext),
            "policyContext", safe(policyContext)
        ));
    }

    /** 按当前 Demo 的 bizDomain.bizModule 拼接五段式 promptCode。 */
    private String promptCode(String scene, String agentMode, String promptRole) {
        return PromptCode.of(
            platformProperties.getBizDomain(),
            platformProperties.getBizModule(),
            scene,
            agentMode,
            promptRole);
    }

    /** 从缓存服务读取指定 promptCode 的模板正文。 */
    private String require(String promptCode) {
        return cachedPromptTemplateService.getContent(promptCode);
    }

    /** 加载模板并按 key 替换 {@code {key}} 占位符。 */
    private String render(String promptCode, Map<String, String> variables) {
        String template = require(promptCode);
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return rendered;
    }

    /** 格式化历史对话块，空历史返回空串。 */
    private String historyBlock(String history) {
        if (history == null || history.isBlank()) {
            return "";
        }
        return history + "\n";
    }

    /** null 安全字符串转换。 */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}
