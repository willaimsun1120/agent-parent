package com.agentforge.agent.action;

import com.agentforge.agent.core.api.AgentPendingActionView;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HITL 写操作 REST API（人工确认 / 拒绝 Agent 发起的写操作）。
 *
 * <p>与 {@link com.agentforge.agent.api.AgentController} 分离：chat 只创建 PENDING，
 * 本 Controller 负责 confirm / reject 后的真正执行。
 */
@RestController
@RequestMapping("/api/agent/actions")
public class AgentActionController {

    private final AgentActionService actionService;

    /**
     * @param actionService HITL 写操作服务
     */
    public AgentActionController(AgentActionService actionService) {
        this.actionService = actionService;
    }

    /**
     * 查询全局所有待确认写操作。
     *
     * @return 待确认操作列表
     */
    @GetMapping("/pending")
    public List<AgentPendingActionView> pendingActions() {
        return actionService.listAllPending();
    }

    /**
     * 查询指定 session 下的待确认写操作。
     *
     * @param sessionId 会话 ID
     * @return 待确认操作列表
     */
    @GetMapping("/sessions/{sessionId}/pending")
    public List<AgentPendingActionView> pendingBySession(@PathVariable String sessionId) {
        return actionService.listPending(sessionId);
    }

    /**
     * 确认并执行指定写操作。
     *
     * @param actionId 操作 ID
     * @return 执行结果摘要（actionId、status、actionType、orderNo）
     */
    @PostMapping("/{actionId}/confirm")
    public Map<String, Object> confirm(@PathVariable String actionId) {
        AgentActionRequest request = actionService.confirm(actionId);
        return Map.of(
            "actionId", request.getActionId(),
            "status", request.getStatus(),
            "actionType", request.getActionType(),
            "orderNo", request.getOrderNo()
        );
    }

    /**
     * 拒绝指定写操作。
     *
     * @param actionId 操作 ID
     * @param body     可选拒绝原因（字段 reason）
     * @return 拒绝结果摘要（actionId、status、reason）
     */
    @PostMapping("/{actionId}/reject")
    public Map<String, Object> reject(@PathVariable String actionId, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        AgentActionRequest request = actionService.reject(actionId, reason);
        return Map.of(
            "actionId", request.getActionId(),
            "status", request.getStatus(),
            "reason", request.getReason() == null ? "" : request.getReason()
        );
    }
}
