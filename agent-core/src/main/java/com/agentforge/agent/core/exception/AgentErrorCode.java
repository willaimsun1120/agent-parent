package com.agentforge.agent.core.exception;

/**
 * Agent 平台统一错误码，由 {@link AgentException} 携带，供 API 层映射 HTTP 状态与提示文案。
 */
public enum AgentErrorCode {

    /** 请求的 mode 未注册对应 {@link com.agentforge.agent.core.mode.AgentModeHandler} */
    MODE_NOT_REGISTERED,

    /** 澄清策略判定需追问，不继续调 LLM */
    CLARIFICATION_REQUIRED,

    /** HITL 动作 ID 不存在 */
    ACTION_NOT_FOUND,

    /** 动作已非 pending 状态，不可重复确认 */
    ACTION_NOT_PENDING,

    /** 动作类型无对应 {@link com.agentforge.agent.core.spi.ActionExecutor} 实现 */
    ACTION_UNSUPPORTED,

    /** 动作执行过程中发生业务冲突（如重复退款） */
    ACTION_CONFLICT,

    /** 动作执行过程中发生业务或系统错误 */
    EXECUTION_FAILED
}
