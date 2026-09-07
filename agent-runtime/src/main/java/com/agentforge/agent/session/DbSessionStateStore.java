package com.agentforge.agent.session;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.agentforge.agent.conversation.AgentConversation;
import com.agentforge.agent.conversation.AgentConversationMapper;
import com.agentforge.agent.core.spi.SessionState;
import com.agentforge.agent.core.spi.SessionStateStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 基于数据库的会话状态存储实现。
 *
 * <p>实现 {@link SessionStateStore} SPI，将业务自定义的 session 属性（如澄清上下文、槽位）
 * 序列化为 JSON 存入 {@code agent_conversations.session_state} 列。
 * 供 Manual / 框架模式在跨轮次间共享结构化状态。
 */
@Service
public class DbSessionStateStore implements SessionStateStore {
    private static final Logger log = LoggerFactory.getLogger(DbSessionStateStore.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AgentConversationMapper conversationMapper;
    private final ObjectMapper objectMapper;

    /**
     * @param conversationMapper 会话元数据 Mapper
     * @param objectMapper JSON 序列化器
     */
    public DbSessionStateStore(AgentConversationMapper conversationMapper, ObjectMapper objectMapper) {
        this.conversationMapper = conversationMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 从数据库加载指定 session 的状态属性。
     *
     * @param sessionId 会话 ID
     * @return 解析成功时含 attributes 的 SessionState，不存在或解析失败为空
     */
    @Override
    public Optional<SessionState> load(String sessionId) {
        AgentConversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<AgentConversation>()
            .eq(AgentConversation::getSessionId, sessionId)
            .last("LIMIT 1"));
        if (conversation == null || conversation.getSessionStateJson() == null || conversation.getSessionStateJson().isBlank()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> attributes = objectMapper.readValue(conversation.getSessionStateJson(), MAP_TYPE);
            log.debug("加载 session state sessionId={} keys={}", sessionId, attributes.keySet());
            return Optional.of(new SessionState(attributes));
        } catch (JsonProcessingException ex) {
            log.warn("解析 session_state 失败 sessionId={} message={}", sessionId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 将 session 状态属性持久化到数据库（不存在则 insert，存在则 update）。
     *
     * @param sessionId 会话 ID
     * @param state 待保存的状态
     */
    @Override
    public void save(String sessionId, SessionState state) {
        AgentConversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<AgentConversation>()
            .eq(AgentConversation::getSessionId, sessionId)
            .last("LIMIT 1"));
        if (conversation == null) {
            conversation = new AgentConversation();
            conversation.setSessionId(sessionId);
        }
        try {
            conversation.setSessionStateJson(objectMapper.writeValueAsString(state.attributes()));
            if (conversation.getId() == null) {
                conversationMapper.insert(conversation);
            } else {
                conversationMapper.updateById(conversation);
            }
            log.debug("保存 session state sessionId={} keys={}", sessionId, state.attributes().keySet());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("session_state JSON 序列化失败", ex);
        }
    }
}
