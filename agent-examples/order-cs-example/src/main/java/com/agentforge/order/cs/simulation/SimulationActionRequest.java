package com.agentforge.order.cs.simulation;

/**
 * 模拟动作结果请求 DTO。
 *
 * <p>预留结构，可用于扩展模拟 API 的请求体（当前主要使用 query param）。
 *
 * @param result 模拟结果（如 SUCCESS / FAILED）
 */
public record SimulationActionRequest(
    String result
) {
}
