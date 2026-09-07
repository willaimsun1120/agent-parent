package com.agentforge.order.cs.mode;

import com.agentforge.agent.core.spi.FrameworkToolRegistrar;
import com.agentforge.agent.core.spi.ToolCallCollector;
import com.agentforge.agent.runtime.AgentExecutionSupport;
import com.agentforge.order.cs.mode.agentscope.AgentScopeOrderToolAdapter;
import com.agentforge.order.cs.mode.langchain4j.LangChain4jOrderToolAdapter;
import com.agentforge.order.cs.tool.OrderTools;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 订单业务：LangChain4j / AgentScope 工具注册实现。
 *
 * <p>实现平台 SPI {@link FrameworkToolRegistrar}，为两种框架分别创建 Tool 适配器，
 * 底层统一委托 {@link OrderTools}，保证三种 Agent 模式共用同一套业务逻辑。
 */
@Component
public class OrderFrameworkToolRegistrar implements FrameworkToolRegistrar {
    private static final Pattern ORDER_NO_IN_QUESTION = Pattern.compile("ORD-\\d+");

    private final OrderTools orderTools;

    /**
     * 构造框架工具注册器。
     *
     * @param orderTools 订单业务工具集
     */
    public OrderFrameworkToolRegistrar(OrderTools orderTools) {
        this.orderTools = orderTools;
    }

    /**
     * 创建 LangChain4j 工具适配器实例。
     *
     * @param collector 工具调用收集器
     * @param context   当前请求上下文
     * @return LangChain4j 工具对象
     */
    @Override
    public Object createLangChain4jTools(ToolCallCollector collector, ToolContext context) {
        return new LangChain4jOrderToolAdapter(orderTools, collector.toolCalls(), toPreparedChat(context));
    }

    /**
     * 创建 AgentScope 工具适配器实例。
     *
     * @param collector 工具调用收集器
     * @param context   当前请求上下文
     * @return AgentScope 工具对象
     */
    @Override
    public Object createAgentScopeTools(ToolCallCollector collector, ToolContext context) {
        return new AgentScopeOrderToolAdapter(orderTools, collector.toolCalls(), toPreparedChat(context));
    }

    /**
     * 返回本 Demo 注册的工具名称提示，供平台日志与调试展示。
     *
     * @return 工具名称说明列表
     */
    @Override
    public List<String> toolNamesHint() {
        return List.of("LangChain4j/AgentScope 工具：get_order_detail/get_payment_status/get_refund_status/get_benefit_status/search_knowledge/submit_refund_request/retry_benefit_issue");
    }

    /**
     * 判断问题是否可能触发工具调用（含订单号或业务关键词）。
     *
     * @param question 用户问题
     * @return 是否期望调用工具
     */
    @Override
    public boolean expectsToolCalls(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        return ORDER_NO_IN_QUESTION.matcher(question).find()
            || question.contains("支付")
            || question.contains("退款")
            || question.contains("权益")
            || question.contains("会员");
    }

    /** 将 SPI 上下文转换为平台 PreparedChat，供 ThreadLocal 传递 sessionId/traceId。 */
    private AgentExecutionSupport.PreparedChat toPreparedChat(ToolContext context) {
        return new AgentExecutionSupport.PreparedChat(
            context.traceId(),
            context.sessionId(),
            context.turnIndex(),
            context.question(),
            context.category(),
            ""
        );
    }
}
