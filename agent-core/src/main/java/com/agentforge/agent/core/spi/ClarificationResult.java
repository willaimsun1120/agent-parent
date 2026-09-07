package com.agentforge.agent.core.spi;

/**
 * 澄清策略命中时的返回结果，平台直接作为回答返回，不继续调 LLM。
 *
 * @param message 面向用户的追问文案
 * @param reason  内部判定原因，供日志与调试
 */
public record ClarificationResult(String message, String reason) {
}
