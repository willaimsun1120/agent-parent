package com.agentforge.agent.runtime;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 框架模式工具可能在 Reactor/线程池线程执行，{@link AgentRequestContext} 的 request scope 会失效。
 * 每次工具调用前通过 holder 注入当前请求的 sessionId / traceId / category。
 *
 * <p>同时作为 SSE 异步线程的上下文载体：request scope 不可用时，由本 Holder 贯穿整次 chat。
 */
public final class AgentRequestContextHolder {

    private static final ThreadLocal<AgentExecutionSupport.PreparedChat> CURRENT = new ThreadLocal<>();

    private AgentRequestContextHolder() {
    }

    /**
     * 将当前线程绑定到指定 PreparedChat。
     *
     * @param prepared 预处理结果
     */
    public static void set(AgentExecutionSupport.PreparedChat prepared) {
        CURRENT.set(prepared);
    }

    /** 清除当前线程的上下文绑定。 */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * @return 当前线程绑定的 PreparedChat，未绑定则为 empty
     */
    public static Optional<AgentExecutionSupport.PreparedChat> peek() {
        return Optional.ofNullable(CURRENT.get());
    }

    /**
     * 在指定上下文中执行 action；支持嵌套，结束后恢复外层上下文。
     *
     * @param prepared 预处理结果
     * @param action 待执行逻辑
     * @param <T> 返回值类型
     * @return action 的执行结果
     */
    public static <T> T callWith(AgentExecutionSupport.PreparedChat prepared, Supplier<T> action) {
        AgentExecutionSupport.PreparedChat previous = CURRENT.get();
        set(prepared);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                clear();
            } else {
                set(previous);
            }
        }
    }

    /**
     * @return 当前线程绑定的 sessionId，未绑定时为 null
     */
    public static String sessionId() {
        AgentExecutionSupport.PreparedChat prepared = CURRENT.get();
        return prepared == null ? null : prepared.sessionId();
    }

    /**
     * @return 当前线程绑定的 traceId，未绑定时为 null
     */
    public static String traceId() {
        AgentExecutionSupport.PreparedChat prepared = CURRENT.get();
        return prepared == null ? null : prepared.traceId();
    }

    /**
     * @return 当前线程绑定的 RAG category，未绑定时为空 Optional
     */
    public static Optional<String> category() {
        AgentExecutionSupport.PreparedChat prepared = CURRENT.get();
        return prepared == null ? Optional.empty() : prepared.category();
    }
}
