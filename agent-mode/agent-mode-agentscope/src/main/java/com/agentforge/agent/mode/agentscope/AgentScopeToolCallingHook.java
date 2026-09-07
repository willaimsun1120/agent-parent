package com.agentforge.agent.mode.agentscope;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.ToolChoice;
import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Mono;

/**
 * AgentScope 首轮推理 Hook。
 *
 * <p>首轮推理强制 {@link ToolChoice.Required}，避免 Qwen 跳过工具直接编造回答。
 */
final class AgentScopeToolCallingHook implements Hook {

    private final AtomicInteger reasoningRound = new AtomicInteger(0);

    /**
     * 拦截首轮 PreReasoning 事件，注入强制工具调用选项。
     *
     * @param event Hook 事件
     * @param <T>   事件类型
     * @return 处理后的 Mono
     */
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreReasoningEvent preReasoning && reasoningRound.incrementAndGet() == 1) {
            GenerateOptions current = preReasoning.getEffectiveGenerateOptions();
            GenerateOptions forced = GenerateOptions.mergeOptions(
                current,
                GenerateOptions.builder()
                    .toolChoice(new ToolChoice.Required())
                    .parallelToolCalls(false)
                    .build()
            );
            preReasoning.setGenerateOptions(forced);
        }
        return Mono.just(event);
    }
}
