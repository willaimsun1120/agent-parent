package com.agentforge.hr.cs.mode;

import com.agentforge.agent.action.AgentActionService;
import com.agentforge.agent.core.api.AgentPendingActionView;
import com.agentforge.agent.core.api.AgentRagHitView;
import com.agentforge.agent.core.config.DashScopeProperties;
import com.agentforge.agent.core.api.AgentChatResponse;
import com.agentforge.agent.core.spi.ManualAgentExecutor;
import com.agentforge.agent.prompt.PromptTemplateProvider;
import com.agentforge.agent.rag.CustomerServicePrompts;
import com.agentforge.agent.rag.KnowledgeDocument;
import com.agentforge.agent.rag.RagHitViews;
import com.agentforge.agent.runtime.AgentExecutionSupport;
import com.agentforge.hr.cs.tool.HrTools;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HrManualAgentExecutor implements ManualAgentExecutor {
    private static final Logger log = LoggerFactory.getLogger(HrManualAgentExecutor.class);
    private static final Pattern EMP_NO_PATTERN = Pattern.compile("EMP-\\d+");
    private static final List<String> LEAVE_CONFIRM_KEYWORDS = List.of(
        "请假", "申请请假", "提交请假", "年假", "要请假", "帮我请假");
    private static final List<String> BENEFIT_CHANGE_KEYWORDS = List.of(
        "补发", "重试", "变更福利", "开通福利", "修改福利", "重新发放");

    private final HrTools hrTools;
    private final AgentActionService actionService;
    private final DashScopeProperties dashScopeProperties;
    private final AgentExecutionSupport executionSupport;
    private final PromptTemplateProvider promptTemplateProvider;
    private final RestClient dashScopeClient;

    public HrManualAgentExecutor(HrTools hrTools,
                                 AgentActionService actionService,
                                 DashScopeProperties dashScopeProperties,
                                 AgentExecutionSupport executionSupport,
                                 PromptTemplateProvider promptTemplateProvider,
                                 RestClient.Builder builder) {
        this.hrTools = hrTools;
        this.actionService = actionService;
        this.dashScopeProperties = dashScopeProperties;
        this.executionSupport = executionSupport;
        this.promptTemplateProvider = promptTemplateProvider;
        this.dashScopeClient = builder.baseUrl("https://dashscope.aliyuncs.com").build();
    }

    @Override
    public ManualExecutionResult execute(ManualExecutionContext context) {
        long started = System.currentTimeMillis();
        String traceId = context.traceId();
        String question = context.question();
        log.info("HrManualAgentExecutor 开始 traceId={} sessionId={}", traceId, context.sessionId());

        List<String> toolCalls = new ArrayList<>();
        List<AgentPendingActionView> pendingActions = new ArrayList<>();
        Optional<String> empNo = extractEmpNo(question);
        if (empNo.isEmpty() && context.history() != null && !context.history().isBlank()) {
            empNo = extractEmpNo(context.history());
            if (empNo.isPresent()) {
                log.info("当前轮未识别工号，从历史对话回溯 traceId={} empNo={}", traceId, empNo.get());
            }
        }
        String bizContext = "";
        if (empNo.isPresent()) {
            log.info("识别到员工工号 traceId={} empNo={}", traceId, empNo.get());
            bizContext = hrTools.getEmployeeDetail(empNo.get());
            toolCalls.add("getEmployeeDetail(" + empNo.get() + ")");
            if (question.contains("请假") || question.contains("年假")) {
                bizContext += "\n" + hrTools.getLeaveStatus(empNo.get());
                toolCalls.add("getLeaveStatus(" + empNo.get() + ")");
            }
            if (question.contains("工资") || question.contains("薪资") || question.contains("发薪")) {
                bizContext += "\n" + hrTools.getPayrollStatus(empNo.get());
                toolCalls.add("getPayrollStatus(" + empNo.get() + ")");
            }
            if (question.contains("社保") || question.contains("福利") || question.contains("医保")) {
                bizContext += "\n" + hrTools.getBenefitStatus(empNo.get());
                toolCalls.add("getBenefitStatus(" + empNo.get() + ")");
            }

            String actionResult = detectAndExecuteAction(question, context.history(), empNo.get());
            if (actionResult != null) {
                bizContext += "\n" + actionResult;
            }
        }

        String actionHistory = formatActionHistory(context.sessionId());
        if (!actionHistory.isEmpty()) {
            bizContext += "\n" + actionHistory;
        }

        List<KnowledgeDocument> ragHits = hrTools.searchKnowledge(question, context.category());
        toolCalls.add("searchKnowledge(...)");
        List<AgentRagHitView> ragHitDetails = RagHitViews.fromDocuments(ragHits);
        List<String> ragHitSummaries = RagHitViews.summaryLines(ragHitDetails);

        String answer = generateAnswer(question, context.history(), bizContext, ragHits);
        long durationMs = System.currentTimeMillis() - started;
        log.info("HrManualAgentExecutor 完成 traceId={} durationMs={}", traceId, durationMs);
        AgentChatResponse response = executionSupport.buildResponse(
            traceId, context.sessionId(), context.turnIndex(), question, answer, durationMs,
            toolCalls, ragHitSummaries, "manual-rest + DashScope HTTP", pendingActions, ragHitDetails);
        return new ManualExecutionResult(response, toolCalls, ragHitSummaries);
    }

    private String detectAndExecuteAction(String question, String history, String empNo) {
        boolean historyMentionsLeave = history != null && (history.contains("请假") || history.contains("年假"));
        boolean questionConfirmsLeave = LEAVE_CONFIRM_KEYWORDS.stream().anyMatch(question::contains);
        if (historyMentionsLeave && questionConfirmsLeave) {
            log.info("检测到请假确认意图 empNo={}", empNo);
            String result = hrTools.submitLeaveRequest(empNo, "ANNUAL", 1, "用户确认申请请假", "");
            return "【请假申请结果】" + result;
        }

        boolean historyMentionsBenefit = history != null && (history.contains("福利") || history.contains("社保") || history.contains("补发"));
        boolean questionConfirmsBenefit = BENEFIT_CHANGE_KEYWORDS.stream().anyMatch(question::contains);
        if (historyMentionsBenefit && questionConfirmsBenefit) {
            log.info("检测到福利变更意图 empNo={}", empNo);
            String result = hrTools.updateBenefitEnrollment(empNo, "", "ACTIVE");
            return "【福利变更结果】" + result;
        }

        return null;
    }

    private String formatActionHistory(String sessionId) {
        List<AgentPendingActionView> actions = actionService.listBySession(sessionId);
        if (actions.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("【本会话操作记录】\n");
        for (AgentPendingActionView action : actions) {
            String statusLabel = switch (action.status()) {
                case "PENDING" -> "待人工确认";
                case "EXECUTED" -> "已确认执行";
                case "REJECTED" -> "已拒绝";
                default -> action.status();
            };
            String typeLabel = switch (action.actionType()) {
                case "SUBMIT_LEAVE" -> "请假申请";
                case "UPDATE_BENEFIT" -> "福利变更";
                default -> action.actionType();
            };
            sb.append("- ").append(typeLabel)
                .append("（").append(action.businessKey()).append("）：").append(statusLabel);
            if ("REJECTED".equals(action.status())) {
                sb.append("，原因：").append(action.summary());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private Optional<String> extractEmpNo(String question) {
        Matcher matcher = EMP_NO_PATTERN.matcher(question);
        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
    }

    private String generateAnswer(String question, String history, String bizContext, List<KnowledgeDocument> ragHits) {
        String policyContext = CustomerServicePrompts.formatPolicyContext(ragHits);
        if (!dashScopeProperties.enabled()) {
            log.info("未配置 DASHSCOPE_API_KEY，手写模式使用本地模板回答");
            return fallbackAnswer(question, bizContext, policyContext);
        }
        try {
            Map<String, Object> response = dashScopeClient.post()
                .uri("/api/v1/services/aigc/text-generation/generation")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + dashScopeProperties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "model", dashScopeProperties.chatModel(),
                    "input", Map.of("messages", List.of(
                        Map.of("role", "system", "content", promptTemplateProvider.getManualSystem()),
                        Map.of("role", "user", "content",
                            promptTemplateProvider.manualUserPrompt(question, history, bizContext, policyContext))
                    )),
                    "parameters", Map.of("result_format", "message")
                ))
                .retrieve()
                .body(Map.class);
            return parseDashScopeAnswer(response);
        } catch (RuntimeException ex) {
            log.warn("手写模式 DashScope Qwen 调用失败，使用本地模板回答 message={}", ex.getMessage());
            return fallbackAnswer(question, bizContext, policyContext);
        }
    }

    @SuppressWarnings("unchecked")
    private String parseDashScopeAnswer(Map<String, Object> response) {
        Map<String, Object> output = (Map<String, Object>) response.get("output");
        Map<String, Object> choices0 = (Map<String, Object>) ((List<Object>) output.get("choices")).get(0);
        Map<String, Object> message = (Map<String, Object>) choices0.get("message");
        return String.valueOf(message.get("content"));
    }

    private String fallbackAnswer(String question, String bizContext, String policyContext) {
        if (bizContext.isBlank()) {
            return "请先提供员工工号（如 EMP-1001），我才能查询 HR 相关信息。";
        }
        if (question.contains("请假") || question.contains("年假")) {
            return """
                根据目前查到的信息：
                %s

                建议回复员工：请先核对年假余额与已有请假记录；余额不足时申请会被拒绝，可改用事假或调休。
                """.formatted(bizContext);
        }
        if (question.contains("工资") || question.contains("薪资")) {
            return """
                根据目前查到的信息：
                %s

                建议回复员工：若工资状态为延迟发放，常见原因是考勤异常或银行账号问题，请联系 HR 薪酬组核实。
                """.formatted(bizContext);
        }
        if (question.contains("社保") || question.contains("福利")) {
            return """
                根据目前查到的信息：
                %s

                建议回复员工：若福利显示待确认，请引导员工在福利平台完成 enrollment；未开通项需 HR 人工审核变更。
                """.formatted(bizContext);
        }
        return """
            根据目前查到的信息：
            %s

            请结合上述情况，用通俗语言向员工说明下一步处理方式。
            """.formatted(bizContext);
    }
}
