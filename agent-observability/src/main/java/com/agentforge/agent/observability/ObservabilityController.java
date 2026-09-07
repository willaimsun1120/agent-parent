package com.agentforge.agent.observability;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 可观测性 REST API（会话、工具调用、RAG 命中查询）。
 *
 * <p>位于 agent-observability 模块，为 Admin / 调试提供按 traceId 串联的审计数据，
 * 与 {@link com.agentforge.agent.api.AgentController} 的问答入口互补。
 */
@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {
    private static final Logger log = LoggerFactory.getLogger(ObservabilityController.class);

    private final AgentSessionMapper sessionMapper;
    private final AgentToolLogMapper toolLogMapper;
    private final RagHitLogMapper ragHitLogMapper;

    /**
     * @param sessionMapper 问答会话 Mapper
     * @param toolLogMapper 工具调用日志 Mapper
     * @param ragHitLogMapper RAG 命中日志 Mapper
     */
    public ObservabilityController(AgentSessionMapper sessionMapper, AgentToolLogMapper toolLogMapper,
                                   RagHitLogMapper ragHitLogMapper) {
        this.sessionMapper = sessionMapper;
        this.toolLogMapper = toolLogMapper;
        this.ragHitLogMapper = ragHitLogMapper;
    }

    /**
     * 查询最近 20 条 Agent 问答会话。
     *
     * @return 按创建时间降序的会话列表
     */
    @GetMapping("/sessions")
    public List<AgentSession> sessions() {
        log.info("收到最近 Agent 会话查询请求");
        List<AgentSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<AgentSession>()
            .orderByDesc(AgentSession::getCreatedAt)
            .last("LIMIT 20"));
        log.info("最近 Agent 会话查询完成 count={}", sessions.size());
        return sessions;
    }

    /**
     * 按 traceId 聚合会话快照 + 工具调用 + RAG 命中，供控制台点击历史消息回放观测。
     */
    @GetMapping("/sessions/{traceId}")
    public ObservabilityTraceView traceDetail(@PathVariable String traceId) {
        log.info("收到观测详情查询请求 traceId={}", traceId);
        AgentSession session = sessionMapper.selectOne(new LambdaQueryWrapper<AgentSession>()
            .eq(AgentSession::getTraceId, traceId)
            .last("LIMIT 1"));
        List<AgentToolLog> tools = toolLogMapper.selectList(new LambdaQueryWrapper<AgentToolLog>()
            .eq(AgentToolLog::getTraceId, traceId)
            .orderByAsc(AgentToolLog::getCreatedAt));
        List<RagHitLog> hits = ragHitLogMapper.selectList(new LambdaQueryWrapper<RagHitLog>()
            .eq(RagHitLog::getTraceId, traceId)
            .orderByDesc(RagHitLog::getScore));

        List<String> toolCalls = new ArrayList<>();
        if (tools != null) {
            for (AgentToolLog tool : tools) {
                String name = tool.getToolName() == null ? "tool" : tool.getToolName();
                String args = tool.getInputJson() == null ? "" : tool.getInputJson().trim();
                toolCalls.add(args.isEmpty() ? name + "()" : name + "(" + args + ")");
            }
        }
        List<ObservabilityTraceView.RagHitDetailView> ragDetails = new ArrayList<>();
        if (hits != null) {
            for (RagHitLog hit : hits) {
                Double score = hit.getScore() == null ? null : hit.getScore().doubleValue();
                ragDetails.add(new ObservabilityTraceView.RagHitDetailView(
                    hit.getSource(),
                    null,
                    score,
                    hit.getContentPreview()
                ));
            }
        }
        ObservabilityTraceView view = new ObservabilityTraceView(
            traceId,
            session == null ? null : session.getSessionId(),
            session == null ? null : session.getTurnIndex(),
            session == null ? null : session.getDurationMs(),
            toolCalls,
            ragDetails
        );
        log.info("观测详情查询完成 traceId={} tools={} ragHits={}",
            traceId, toolCalls.size(), ragDetails.size());
        return view;
    }

    /**
     * 按 traceId 查询该次问答中的工具调用日志。
     *
     * @param traceId 链路追踪 ID
     * @return 按时间升序的工具调用记录
     */
    @GetMapping("/sessions/{traceId}/tools")
    public List<AgentToolLog> tools(@PathVariable String traceId) {
        log.info("收到 Agent 工具调用日志查询请求 traceId={}", traceId);
        // 关键查询：按 traceId 串起一次 Agent 回答中调用过的订单工具和知识库工具。
        List<AgentToolLog> logs = toolLogMapper.selectList(new LambdaQueryWrapper<AgentToolLog>()
            .eq(AgentToolLog::getTraceId, traceId)
            .orderByAsc(AgentToolLog::getCreatedAt));
        log.info("Agent 工具调用日志查询完成 traceId={} count={}", traceId, logs.size());
        return logs;
    }

    /**
     * 按 traceId 查询 RAG 检索命中记录。
     *
     * @param traceId 链路追踪 ID
     * @return 按相似度得分降序的命中记录
     */
    @GetMapping("/sessions/{traceId}/rag-hits")
    public List<RagHitLog> ragHits(@PathVariable String traceId) {
        log.info("收到 RAG 命中日志查询请求 traceId={}", traceId);
        List<RagHitLog> logs = ragHitLogMapper.selectList(new LambdaQueryWrapper<RagHitLog>()
            .eq(RagHitLog::getTraceId, traceId)
            .orderByDesc(RagHitLog::getScore));
        log.info("RAG 命中日志查询完成 traceId={} count={}", traceId, logs.size());
        return logs;
    }
}
