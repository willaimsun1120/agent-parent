package com.agentforge.agent.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.agentforge.agent.core.api.AgentConversationMessageView;
import com.agentforge.agent.core.api.AgentConversationPageView;
import com.agentforge.agent.core.config.AgentPlatformProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 多轮对话会话服务。
 *
 * <p>数据模型：
 * <ul>
 *   <li>{@code agent_conversations} — session 元数据</li>
 *   <li>{@code agent_conversation_turns} — 每轮 user/assistant 消息</li>
 * </ul>
 *
 * <p>客户端传 {@code sessionId} 续聊；不传则自动生成 sess-uuid。
 * {@link #formatHistory} 将最近 N 轮（配置 max-history-turns）格式化为文本注入 prompt。
 */
@Service
public class AgentConversationService {

    private static final DateTimeFormatter CREATED_AT_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final AgentConversationMapper conversationMapper;
    private final AgentConversationTurnMapper turnMapper;
    private final int maxHistoryTurns;

    /**
     * @param conversationMapper 会话元数据 Mapper
     * @param turnMapper 对话轮次 Mapper
     * @param properties 平台配置（含 max-history-turns）
     */
    public AgentConversationService(AgentConversationMapper conversationMapper,
                                      AgentConversationTurnMapper turnMapper,
                                      AgentPlatformProperties properties) {
        this.conversationMapper = conversationMapper;
        this.turnMapper = turnMapper;
        this.maxHistoryTurns = properties.getConversation().getMaxHistoryTurns();
    }

    /**
     * 解析或创建 sessionId。客户端传入则复用，否则生成 sess-uuid。
     *
     * @param sessionId 客户端传入的会话 ID，可为 null/空
     * @return 有效的 sessionId
     */
    public String resolveSessionId(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            ensureConversation(sessionId.trim());
            return sessionId.trim();
        }
        String created = "sess-" + UUID.randomUUID();
        ensureConversation(created);
        return created;
    }

    /**
     * 计算下一轮 turnIndex（当前最大轮次 + 1，无历史则从 1 开始）。
     *
     * @param sessionId 会话 ID
     * @return 下一轮序号
     */
    public int nextTurnIndex(String sessionId) {
        AgentConversationTurn latest = turnMapper.selectOne(new LambdaQueryWrapper<AgentConversationTurn>()
            .eq(AgentConversationTurn::getSessionId, sessionId)
            .orderByDesc(AgentConversationTurn::getTurnIndex)
            .last("LIMIT 1"));
        if (latest == null) {
            return 1;
        }
        return latest.getTurnIndex() + 1;
    }

    /**
     * 追加一条对话轮次记录（幂等：若唯一键已存在则跳过）。
     *
     * <p>同一 session 的并发请求可能导致 {@code nextTurnIndex} 返回相同值，
     * 唯一键 {@code (session_id, turn_index, role)} 会拒绝重复插入。
     * 本方法先查再插，避免 {@code SQLIntegrityConstraintViolationException}。
     *
     * @param sessionId 会话 ID
     * @param turnIndex 轮次序号
     * @param traceId 链路追踪 ID
     * @param role 角色（user / assistant）
     * @param content 消息内容
     */
    public void appendTurn(String sessionId, int turnIndex, String traceId, String role, String content) {
        boolean exists = turnMapper.selectCount(new LambdaQueryWrapper<AgentConversationTurn>()
            .eq(AgentConversationTurn::getSessionId, sessionId)
            .eq(AgentConversationTurn::getTurnIndex, turnIndex)
            .eq(AgentConversationTurn::getRole, role)) > 0;
        if (exists) {
            return;
        }
        AgentConversationTurn turn = new AgentConversationTurn();
        turn.setSessionId(sessionId);
        turn.setTurnIndex(turnIndex);
        turn.setTraceId(traceId);
        turn.setRole(role);
        turn.setContent(content);
        turn.setCreatedAt(LocalDateTime.now());
        turnMapper.insert(turn);
    }

    /**
     * 获取会话全部对话轮次（供控制台刷新后恢复展示，默认最多 200 条消息）。
     *
     * @param sessionId 会话 ID
     * @return 按时间正序的轮次列表
     */
    public List<AgentConversationTurn> listTurns(String sessionId) {
        return pageTurns(sessionId, null, 200).items().stream()
            .map(view -> {
                AgentConversationTurn turn = new AgentConversationTurn();
                turn.setId(view.id());
                turn.setSessionId(sessionId);
                turn.setTurnIndex(view.turnIndex());
                turn.setTraceId(view.traceId());
                turn.setRole(view.role());
                turn.setContent(view.content());
                if (view.createdAt() != null && !view.createdAt().isBlank()) {
                    turn.setCreatedAt(LocalDateTime.parse(view.createdAt(), CREATED_AT_FORMAT));
                }
                return turn;
            })
            .toList();
    }

    /**
     * 聊天记录分页查询：默认取最新一页；传 beforeId 继续向上加载更早消息。
     *
     * @param sessionId 会话 ID
     * @param beforeId  仅返回 id 小于该值的消息；null 表示从最新开始
     * @param limit     每页条数（1~100）
     * @return 分页结果（items 为正序）
     */
    public AgentConversationPageView pageTurns(String sessionId, Long beforeId, int limit) {
        if (sessionId == null || sessionId.isBlank()) {
            return new AgentConversationPageView(List.of(), false, null);
        }
        int pageSize = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        LambdaQueryWrapper<AgentConversationTurn> query = new LambdaQueryWrapper<AgentConversationTurn>()
            .eq(AgentConversationTurn::getSessionId, sessionId.trim())
            .orderByDesc(AgentConversationTurn::getId)
            .last("LIMIT " + (pageSize + 1));
        if (beforeId != null && beforeId > 0) {
            query.lt(AgentConversationTurn::getId, beforeId);
        }
        List<AgentConversationTurn> fetched = turnMapper.selectList(query);
        if (fetched == null || fetched.isEmpty()) {
            return new AgentConversationPageView(List.of(), false, null);
        }
        boolean hasMore = fetched.size() > pageSize;
        List<AgentConversationTurn> page = hasMore ? fetched.subList(0, pageSize) : fetched;
        List<AgentConversationTurn> ordered = new ArrayList<>(page);
        Collections.reverse(ordered);
        List<AgentConversationMessageView> items = ordered.stream()
            .map(turn -> new AgentConversationMessageView(
                turn.getId(),
                turn.getRole(),
                turn.getContent(),
                turn.getTurnIndex(),
                turn.getTraceId(),
                formatCreatedAt(turn.getCreatedAt())
            ))
            .toList();
        Long nextBeforeId = hasMore && !items.isEmpty() ? items.get(0).id() : null;
        return new AgentConversationPageView(items, hasMore, nextBeforeId);
    }

    private static String formatCreatedAt(LocalDateTime createdAt) {
        return createdAt == null ? null : createdAt.format(CREATED_AT_FORMAT);
    }

    /**
     * 获取最近 N 轮对话（按 turnIndex 升序）。
     *
     * @param sessionId 会话 ID
     * @return 有序轮次列表
     */
    public List<AgentConversationTurn> recentHistory(String sessionId) {
        // 每轮含 user + assistant，故 limit = maxHistoryTurns * 2
        List<AgentConversationTurn> turns = turnMapper.selectList(new LambdaQueryWrapper<AgentConversationTurn>()
            .eq(AgentConversationTurn::getSessionId, sessionId)
            .orderByDesc(AgentConversationTurn::getTurnIndex)
            .orderByDesc(AgentConversationTurn::getId)
            .last("LIMIT " + (maxHistoryTurns * 2)));
        List<AgentConversationTurn> ordered = new ArrayList<>(turns);
        Collections.reverse(ordered); // 查询为降序，反转为时间正序
        return ordered;
    }

    /**
     * 将最近对话格式化为可注入 prompt 的文本。
     *
     * @param sessionId 会话 ID
     * @return 格式化历史，无记录时返回空串
     */
    public String formatHistory(String sessionId) {
        List<AgentConversationTurn> history = recentHistory(sessionId);
        if (history.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("历史对话（最近几轮）：\n");
        for (AgentConversationTurn turn : history) {
            builder.append(turn.getRole()).append("：").append(turn.getContent()).append("\n");
        }
        return builder.toString();
    }

    /** 确保 agent_conversations 中存在该 session 的元数据行。 */
    private void ensureConversation(String sessionId) {
        AgentConversation existing = conversationMapper.selectOne(new LambdaQueryWrapper<AgentConversation>()
            .eq(AgentConversation::getSessionId, sessionId)
            .last("LIMIT 1"));
        if (existing == null) {
            AgentConversation conversation = new AgentConversation();
            conversation.setSessionId(sessionId);
            conversationMapper.insert(conversation);
        }
    }
}
