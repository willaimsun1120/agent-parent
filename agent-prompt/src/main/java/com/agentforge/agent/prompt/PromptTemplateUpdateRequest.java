package com.agentforge.agent.prompt;

/**
 * 提示词模板更新请求体。
 *
 * @param title       可选标题
 * @param description 可选描述
 * @param content     模板正文（必填）
 * @param status      可选状态（ACTIVE / DRAFT）
 */
public record PromptTemplateUpdateRequest(
    String title,
    String description,
    String content,
    String status
) {
}
