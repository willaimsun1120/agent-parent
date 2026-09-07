package com.agentforge.agent.core.exception;

/**
 * Agent 平台统一运行时异常，携带 {@link AgentErrorCode} 供上层统一处理。
 */
public class AgentException extends RuntimeException {

    private final AgentErrorCode errorCode;

    /**
     * @param errorCode 错误码
     * @param message   面向调用方或日志的错误描述
     */
    public AgentException(AgentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * @param errorCode 错误码
     * @param message   错误描述
     * @param cause     原始异常
     */
    public AgentException(AgentErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /** @return 关联的错误码 */
    public AgentErrorCode errorCode() {
        return errorCode;
    }
}
