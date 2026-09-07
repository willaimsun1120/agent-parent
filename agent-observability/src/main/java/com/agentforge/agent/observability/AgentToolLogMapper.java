package com.agentforge.agent.observability;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * {@link AgentToolLog} 的 MyBatis-Plus Mapper。
 *
 * <p>由工具执行层写入调用记录，{@link ObservabilityController} 按 traceId 查询。
 */
public interface AgentToolLogMapper extends BaseMapper<AgentToolLog> {
}
