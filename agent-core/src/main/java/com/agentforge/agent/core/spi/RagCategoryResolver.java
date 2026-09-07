package com.agentforge.agent.core.spi;

import java.util.Optional;

/**
 * RAG 检索 category 解析 SPI。
 *
 * <p>用于在向量检索时按业务分类过滤（如 refund / payment / benefit），提高命中率。
 * 请求体可显式传 category；未传时由业务从问题文本推断。
 *
 * <p>订单 Demo 参考：{@code com.agentforge.order.cs.rag.OrderRagCategoryResolver}
 */
public interface RagCategoryResolver {

    /**
     * 解析 RAG 检索用的业务分类。
     *
     * @param explicitCategory 请求体显式传入的分类，可为空
     * @param question         用户问题，用于推断分类
     * @return 解析到的分类；无法推断时为空
     */
    Optional<String> resolve(String explicitCategory, String question);
}
