package com.agentforge.hr.cs.tool;

import com.agentforge.agent.action.AgentActionService;
import com.agentforge.agent.core.api.AgentPendingActionView;
import com.agentforge.agent.core.spi.RagCategoryResolver;
import com.agentforge.agent.observability.AgentMetrics;
import com.agentforge.agent.observability.AgentToolLog;
import com.agentforge.agent.observability.AgentToolLogMapper;
import com.agentforge.agent.rag.KnowledgeDocument;
import com.agentforge.agent.rag.RagSearchService;
import com.agentforge.agent.runtime.AgentRequestContext;
import com.agentforge.agent.runtime.AgentRequestContextHolder;
import com.agentforge.hr.cs.employee.EmployeeDetail;
import com.agentforge.hr.cs.employee.EmployeeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * HR 客服业务工具集。
 *
 * <p>平台 {@link com.agentforge.agent.core.spi.ActionExecutionContext#orderNo()} 字段在 HR Demo 中存储 empNo。
 */
@Component
public class HrTools {
    private static final Logger log = LoggerFactory.getLogger(HrTools.class);

    private final EmployeeService employeeService;
    private final RagSearchService ragSearchService;
    private final AgentActionService actionService;
    private final AgentRequestContext requestContext;
    private final AgentToolLogMapper toolLogMapper;
    private final AgentMetrics metrics;
    private final ObjectMapper objectMapper;
    private final RagCategoryResolver ragCategoryResolver;

    public HrTools(EmployeeService employeeService, RagSearchService ragSearchService,
                   AgentActionService actionService, AgentRequestContext requestContext,
                   AgentToolLogMapper toolLogMapper, AgentMetrics metrics,
                   ObjectMapper objectMapper, RagCategoryResolver ragCategoryResolver) {
        this.employeeService = employeeService;
        this.ragSearchService = ragSearchService;
        this.actionService = actionService;
        this.requestContext = requestContext;
        this.toolLogMapper = toolLogMapper;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.ragCategoryResolver = ragCategoryResolver;
    }

    public String getEmployeeDetail(String empNo) {
        return recordToolCall("getEmployeeDetail", Map.of("empNo", empNo),
            () -> toJson(employeeService.getEmployeeDetail(empNo)));
    }

    public String getLeaveStatus(String empNo) {
        return recordToolCall("getLeaveStatus", Map.of("empNo", empNo), () -> {
            EmployeeDetail detail = employeeService.getEmployeeDetail(empNo);
            return toJson(Map.of(
                "empNo", empNo,
                "annualLeaveBalance", detail.annualLeaveBalance(),
                "leaveRequests", detail.leaveRequests()
            ));
        });
    }

    public String getPayrollStatus(String empNo) {
        return recordToolCall("getPayrollStatus", Map.of("empNo", empNo), () -> {
            EmployeeDetail detail = employeeService.getEmployeeDetail(empNo);
            return toJson(Map.of("empNo", empNo, "payrollRecords", detail.payrollRecords()));
        });
    }

    public String getBenefitStatus(String empNo) {
        return recordToolCall("getBenefitStatus", Map.of("empNo", empNo), () -> {
            EmployeeDetail detail = employeeService.getEmployeeDetail(empNo);
            return toJson(Map.of("empNo", empNo, "benefits", detail.benefits()));
        });
    }

    public List<KnowledgeDocument> searchKnowledge(String question) {
        return searchKnowledge(question, resolveCategory(question));
    }

    public List<KnowledgeDocument> searchKnowledge(String question, Optional<String> category) {
        long started = System.currentTimeMillis();
        log.info("开始调用知识库检索工具 question={} category={}", question, category.orElse("-"));
        List<KnowledgeDocument> hits = ragSearchService.search(question, 3, category);
        saveToolLog("searchKnowledge", toJson(Map.of("question", question, "category", category.orElse(""))),
            "命中文档数: " + hits.size(), System.currentTimeMillis() - started, true);
        return hits;
    }

    /**
     * 创建待人工确认的请假申请。平台 action 的 orderNo 字段存储 empNo。
     */
    public String submitLeaveRequest(String empNo, String leaveType, int days, String reason, String startDate) {
        return recordToolCall("submitLeaveRequest", Map.of(
            "empNo", empNo, "leaveType", leaveType, "days", days, "reason", reason, "startDate", startDate), () -> {
            employeeService.getEmployeeDetail(empNo);
            AgentPendingActionView action = actionService.createPending(
                "SUBMIT_LEAVE", empNo,
                Map.of(
                    "leaveType", leaveType == null ? "ANNUAL" : leaveType,
                    "days", days,
                    "reason", reason == null ? "" : reason,
                    "startDate", startDate == null ? "" : startDate
                ),
                resolveSessionId(), resolveTraceId());
            return toJson(Map.of(
                "actionId", action.actionId(),
                "status", action.status(),
                "message", "请假申请已创建，等待人工确认后才会写入请假表。"
            ));
        });
    }

    /**
     * 创建待人工确认的福利 enrollment 变更。平台 action 的 orderNo 字段存储 empNo。
     */
    public String updateBenefitEnrollment(String empNo, String benefitType, String targetStatus) {
        return recordToolCall("updateBenefitEnrollment", Map.of(
            "empNo", empNo, "benefitType", benefitType, "targetStatus", targetStatus), () -> {
            try {
                employeeService.getEmployeeDetail(empNo);
                AgentPendingActionView action = actionService.createPending(
                    "UPDATE_BENEFIT", empNo,
                    Map.of(
                        "benefitType", benefitType == null ? "" : benefitType,
                        "targetStatus", targetStatus == null ? "ACTIVE" : targetStatus
                    ),
                    resolveSessionId(), resolveTraceId());
                return toJson(Map.of(
                    "success", true,
                    "actionId", action.actionId(),
                    "status", action.status(),
                    "message", "福利变更申请已创建，等待人工确认后才会生效。"
                ));
            } catch (ResponseStatusException ex) {
                return toJson(Map.of("success", false, "message", ex.getReason()));
            }
        });
    }

    private Optional<String> resolveCategory(String question) {
        Optional<String> fromHolder = AgentRequestContextHolder.category();
        if (fromHolder.isPresent()) {
            return fromHolder;
        }
        try {
            if (requestContext.category() != null && !requestContext.category().isBlank()) {
                return Optional.of(requestContext.category());
            }
        } catch (RuntimeException ignored) {
            // SSE 异步线程 request scope 已失效
        }
        return ragCategoryResolver.resolve(null, question);
    }

    private String recordToolCall(String toolName, Map<String, Object> input, ToolInvoker invoker) {
        long started = System.currentTimeMillis();
        metrics.incrementToolCall();
        String inputJson = toJson(input);
        log.info("开始调用 HR 业务工具 toolName={} input={}", toolName, inputJson);
        try {
            String output = invoker.invoke();
            long durationMs = System.currentTimeMillis() - started;
            metrics.recordToolDuration(durationMs);
            saveToolLog(toolName, inputJson, preview(output), durationMs, true);
            return output;
        } catch (RuntimeException ex) {
            long durationMs = System.currentTimeMillis() - started;
            metrics.recordToolDuration(durationMs);
            saveToolLog(toolName, inputJson, ex.getMessage(), durationMs, false);
            throw ex;
        }
    }

    private void saveToolLog(String toolName, String inputJson, String outputSummary, long durationMs, boolean success) {
        String traceId = resolveTraceId();
        if (traceId == null) {
            return;
        }
        toolLogMapper.insert(new AgentToolLog(traceId, toolName, inputJson, outputSummary, durationMs, success));
    }

    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            return traceId;
        }
        traceId = AgentRequestContextHolder.traceId();
        if (traceId != null) {
            return traceId;
        }
        try {
            return requestContext.traceId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String resolveSessionId() {
        String sessionId = AgentRequestContextHolder.sessionId();
        if (sessionId != null) {
            return sessionId;
        }
        try {
            return requestContext.sessionId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON serialization failed", ex);
        }
    }

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 500 ? text : text.substring(0, 500);
    }

    @FunctionalInterface
    private interface ToolInvoker {
        String invoke();
    }
}
