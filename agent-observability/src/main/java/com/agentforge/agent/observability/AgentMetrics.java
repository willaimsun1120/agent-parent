package com.agentforge.agent.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Agent 平台 Micrometer 指标采集组件。
 *
 * <p>位于 agent-observability 模块，为 chat、工具调用、RAG 检索提供 Counter 与 Timer，
 * 供 Prometheus / Actuator 暴露，{@link com.agentforge.agent.runtime.AgentExecutionSupport} 等在运行时调用。
 */
@Component
public class AgentMetrics {
    private final Counter chatCounter;
    private final Counter toolCounter;
    private final Counter ragCounter;
    private final Timer chatTimer;
    private final Timer toolTimer;
    private final Timer ragTimer;

    /**
     * 注册 agent_chat_total、agent_tool_call_total、rag_search_total 及对应 duration Timer。
     *
     * @param registry Micrometer 注册表
     */
    public AgentMetrics(MeterRegistry registry) {
        this.chatCounter = Counter.builder("agent_chat_total").register(registry);
        this.toolCounter = Counter.builder("agent_tool_call_total").register(registry);
        this.ragCounter = Counter.builder("rag_search_total").register(registry);
        this.chatTimer = Timer.builder("agent_chat_duration").register(registry);
        this.toolTimer = Timer.builder("agent_tool_call_duration").register(registry);
        this.ragTimer = Timer.builder("rag_search_duration").register(registry);
    }

    /** 问答请求计数 +1。 */
    public void incrementChat() { chatCounter.increment(); }

    /** 工具调用计数 +1。 */
    public void incrementToolCall() { toolCounter.increment(); }

    /** RAG 检索计数 +1。 */
    public void incrementRagSearch() { ragCounter.increment(); }

    /**
     * 记录单次问答耗时。
     *
     * @param millis 耗时毫秒
     */
    public void recordChatDuration(long millis) { chatTimer.record(millis, TimeUnit.MILLISECONDS); }

    /**
     * 记录单次工具调用耗时。
     *
     * @param millis 耗时毫秒
     */
    public void recordToolDuration(long millis) { toolTimer.record(millis, TimeUnit.MILLISECONDS); }

    /**
     * 记录单次 RAG 检索耗时。
     *
     * @param millis 耗时毫秒
     */
    public void recordRagDuration(long millis) { ragTimer.record(millis, TimeUnit.MILLISECONDS); }
}
