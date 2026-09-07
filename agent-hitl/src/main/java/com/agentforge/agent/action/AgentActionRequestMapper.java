package com.agentforge.agent.action;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * {@link AgentActionRequest} 的 MyBatis-Plus Mapper。
 *
 * <p>由 {@link AgentActionService} 使用，负责 HITL 写操作请求的持久化与查询。
 */
public interface AgentActionRequestMapper extends BaseMapper<AgentActionRequest> {
}
