package com.agentforge.agent.observability;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * {@link AgentSession} 的 MyBatis-Plus Mapper。
 *
 * <p>由 {@link com.agentforge.agent.runtime.AgentExecutionSupport} 写入问答记录，
 * {@link ObservabilityController} 查询最近会话。
 */
public interface AgentSessionMapper extends BaseMapper<AgentSession> {
}
