package com.agentforge.agent.conversation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * {@link AgentConversation} 的 MyBatis-Plus Mapper。
 *
 * <p>由 {@link AgentConversationService} 与 {@link com.agentforge.agent.session.DbSessionStateStore} 使用，
 * 负责会话元数据的 CRUD。
 */
public interface AgentConversationMapper extends BaseMapper<AgentConversation> {
}
