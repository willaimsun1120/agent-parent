package com.agentforge.agent.conversation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * {@link AgentConversationTurn} 的 MyBatis-Plus Mapper。
 *
 * <p>由 {@link AgentConversationService} 使用，负责对话轮次的插入与历史查询。
 */
public interface AgentConversationTurnMapper extends BaseMapper<AgentConversationTurn> {
}
