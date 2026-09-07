package com.agentforge.hr.cs.knowledge;

/**
 * 知识库文章创建/更新请求 DTO。
 *
 * <p>由 {@link KnowledgeController} 接收 JSON 请求体，传递给 {@link KnowledgeArticleService}。
 *
 * @param docCode  文档唯一编码（创建时必填）
 * @param title    文章标题
 * @param category RAG 分类
 * @param content  正文内容
 * @param status   发布状态（可选，默认 DRAFT）
 */
public record KnowledgeArticleRequest(
    String docCode,
    String title,
    String category,
    String content,
    String status
) {
}
