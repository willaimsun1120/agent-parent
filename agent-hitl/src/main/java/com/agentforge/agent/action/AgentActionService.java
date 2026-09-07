package com.agentforge.agent.action;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.agentforge.agent.core.api.AgentPendingActionView;
import com.agentforge.agent.core.exception.AgentErrorCode;
import com.agentforge.agent.core.exception.AgentException;
import com.agentforge.agent.core.spi.ActionExecutionContext;
import com.agentforge.agent.core.spi.ActionExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * HITL（Human-in-the-Loop）写操作服务。
 *
 * <p>Agent 不应直接改库/发起退款等写操作，而是：
 * <ol>
 *   <li>工具层调用 {@link #createPending} 创建 PENDING 记录</li>
 *   <li>问答响应携带 {@code pendingActions}，前端 / Admin 展示待确认项</li>
 *   <li>人工调用 {@link #confirm} / {@link #reject} 后，按 actionType 路由到对应 {@link ActionExecutor} SPI</li>
 * </ol>
 *
 * <p>actionType 与 Executor 在启动时注册到 Map，重复 actionType 会启动失败。
 */
@Service
public class AgentActionService {
    private static final Logger log = LoggerFactory.getLogger(AgentActionService.class);

    private final AgentActionRequestMapper actionMapper;
    private final Map<String, ActionExecutor> executors;
    private final ObjectMapper objectMapper;

    /**
     * @param actionMapper 写操作请求 Mapper
     * @param executorList 按 actionType 注册的 ActionExecutor SPI 列表
     * @param objectMapper JSON 序列化器
     */
    public AgentActionService(AgentActionRequestMapper actionMapper,
                              List<ActionExecutor> executorList,
                              ObjectMapper objectMapper) {
        this.actionMapper = actionMapper;
        this.executors = executorList.stream()
            .collect(Collectors.toMap(ActionExecutor::actionType, Function.identity(), (a, b) -> {
                throw new IllegalStateException("重复的 ActionExecutor actionType=" + a.actionType());
            }));
        this.objectMapper = objectMapper;
        log.info("AgentActionService 初始化 executors={}", this.executors.keySet());
    }

    /**
     * 由业务工具在 Agent 推理过程中调用，创建 PENDING 写操作。
     *
     * @param actionType 操作类型（对应 ActionExecutor.actionType）
     * @param orderNo    关联业务订单号
     * @param payload    操作参数
     * @param sessionId  会话 ID
     * @param traceId    链路追踪 ID
     * @return 待确认操作视图，随 chat 响应返回前端
     */
    public AgentPendingActionView createPending(String actionType, String orderNo, Map<String, Object> payload,
                                                String sessionId, String traceId) {
        String actionId = "act-" + UUID.randomUUID();
        AgentActionRequest request = new AgentActionRequest();
        request.setActionId(actionId);
        request.setSessionId(sessionId);
        request.setTraceId(traceId);
        request.setActionType(actionType);
        request.setOrderNo(orderNo);
        request.setPayloadJson(toJson(payload == null ? Map.of() : payload));
        request.setStatus("PENDING");
        actionMapper.insert(request);
        log.info("创建待确认写操作 actionId={} actionType={} orderNo={}", actionId, actionType, orderNo);
        return toView(request);
    }

    /**
     * 确认并执行待处理的写操作。
     *
     * @param actionId 操作 ID
     * @return 更新后的请求实体（status=EXECUTED）
     */
    public AgentActionRequest confirm(String actionId) {
        AgentActionRequest request = requirePending(actionId);
        ActionExecutor executor = executors.get(request.getActionType());
        if (executor == null) {
            throw new AgentException(AgentErrorCode.ACTION_UNSUPPORTED,
                "Unsupported action type: " + request.getActionType());
        }
        log.info("开始执行待确认写操作 actionId={} actionType={} orderNo={}",
            actionId, request.getActionType(), request.getOrderNo());
        executor.execute(new ActionExecutionContext(
            request.getActionId(),
            request.getOrderNo(),
            request.getPayloadJson(),
            request.getSessionId(),
            request.getTraceId()
        ));
        request.setStatus("EXECUTED");
        actionMapper.updateById(request);
        log.info("写操作已确认执行 actionId={} actionType={}", actionId, request.getActionType());
        return request;
    }

    /**
     * 拒绝待处理的写操作。
     *
     * @param actionId 操作 ID
     * @param reason   拒绝原因，可为 null
     * @return 更新后的请求实体（status=REJECTED）
     */
    public AgentActionRequest reject(String actionId, String reason) {
        AgentActionRequest request = requirePending(actionId);
        request.setStatus("REJECTED");
        request.setReason(reason == null ? "" : reason.trim());
        actionMapper.updateById(request);
        log.info("写操作已拒绝 actionId={}", actionId);
        return request;
    }

    /**
     * 列出指定 session 下所有 PENDING 写操作。
     *
     * @param sessionId 会话 ID
     * @return 待确认操作视图列表
     */
    public List<AgentPendingActionView> listPending(String sessionId) {
        return actionMapper.selectList(new LambdaQueryWrapper<AgentActionRequest>()
                .eq(AgentActionRequest::getSessionId, sessionId)
                .eq(AgentActionRequest::getStatus, "PENDING")
                .orderByDesc(AgentActionRequest::getCreatedAt))
            .stream()
            .map(this::toView)
            .toList();
    }

    /**
     * 列出指定 session 下全部写操作（含已确认/已拒绝），供 Agent 上下文注入。
     *
     * @param sessionId 会话 ID
     * @return 全部操作视图列表（按创建时间降序）
     */
    public List<AgentPendingActionView> listBySession(String sessionId) {
        return actionMapper.selectList(new LambdaQueryWrapper<AgentActionRequest>()
                .eq(AgentActionRequest::getSessionId, sessionId)
                .orderByDesc(AgentActionRequest::getCreatedAt))
            .stream()
            .map(this::toView)
            .toList();
    }

    /**
     * 列出全局所有 PENDING 写操作（Admin 用）。
     *
     * @return 待确认操作视图列表
     */
    public List<AgentPendingActionView> listAllPending() {
        return actionMapper.selectList(new LambdaQueryWrapper<AgentActionRequest>()
                .eq(AgentActionRequest::getStatus, "PENDING")
                .orderByDesc(AgentActionRequest::getCreatedAt))
            .stream()
            .map(this::toView)
            .toList();
    }

    private AgentActionRequest requirePending(String actionId) {
        AgentActionRequest request = actionMapper.selectOne(new LambdaQueryWrapper<AgentActionRequest>()
            .eq(AgentActionRequest::getActionId, actionId)
            .last("LIMIT 1"));
        if (request == null) {
            throw new AgentException(AgentErrorCode.ACTION_NOT_FOUND, "Action not found: " + actionId);
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new AgentException(AgentErrorCode.ACTION_NOT_PENDING,
                "Action is not pending: " + request.getStatus());
        }
        return request;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON serialization failed", ex);
        }
    }

    private AgentPendingActionView toView(AgentActionRequest request) {
        String summary = request.getActionType() + "：" + request.getOrderNo();
        return new AgentPendingActionView(
            request.getActionId(),
            request.getActionType(),
            request.getOrderNo(),
            request.getStatus(),
            summary
        );
    }
}
