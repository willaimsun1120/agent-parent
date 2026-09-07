package com.agentforge.order.cs.mode;

import com.agentforge.agent.action.AgentActionService;
import com.agentforge.agent.core.api.AgentPendingActionView;
import com.agentforge.agent.core.api.AgentRagHitView;
import com.agentforge.agent.core.config.DashScopeProperties;
import com.agentforge.agent.core.api.AgentChatResponse;
import com.agentforge.agent.core.spi.FactSummarizer;
import com.agentforge.agent.core.spi.ManualAgentExecutor;
import com.agentforge.agent.prompt.PromptTemplateProvider;
import com.agentforge.agent.rag.KnowledgeDocument;
import com.agentforge.agent.rag.RagHitViews;
import com.agentforge.agent.runtime.AgentExecutionSupport;
import com.agentforge.agent.runtime.CustomerServicePlainFormatter;
import com.agentforge.agent.rag.CustomerServicePrompts;
import com.agentforge.order.cs.tool.OrderTools;
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

/**
 * 订单客服手写模式 Agent 执行器。
 *
 * <p>实现平台 SPI {@link ManualAgentExecutor}，不依赖 LangChain4j / AgentScope：
 * 按问题关键词主动调用 {@link OrderTools}，再调用 DashScope Qwen 或本地模板生成回答。
 * 适用于 Demo 演示与无框架依赖的轻量部署。
 */
@Component
public class OrderManualAgentExecutor implements ManualAgentExecutor {
    private static final Logger log = LoggerFactory.getLogger(OrderManualAgentExecutor.class);
    /** 订单号正则，与澄清策略保持一致 */
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("ORD-\\d+");

    private static final List<String> REFUND_CONFIRM_KEYWORDS = List.of(
        "需要退款", "要退款", "申请退款", "提交退款", "发起退款", "登记退款",
        "帮我退", "我要退", "需要", "要的", "好的", "确认", "同意", "帮我提交", "帮我登记");
    private static final List<String> BENEFIT_RETRY_KEYWORDS = List.of(
        "补发", "重试", "重新发放", "重发权益", "补发权益");

    private final OrderTools orderTools;
    private final AgentActionService actionService;
    private final DashScopeProperties dashScopeProperties;
    private final AgentExecutionSupport executionSupport;
    private final PromptTemplateProvider promptTemplateProvider;
    private final RestClient dashScopeClient;
    private final Optional<FactSummarizer> factSummarizer;

    /**
     * 构造手写模式执行器。
     *
     * @param orderTools              订单业务工具
     * @param dashScopeProperties     DashScope 配置
     * @param executionSupport        平台响应构建支持
     * @param promptTemplateProvider  提示词模板提供者
     * @param builder                 RestClient 构建器
     * @param factSummarizer          业务事实摘要 SPI（可选）
     */
    public OrderManualAgentExecutor(OrderTools orderTools,
                                    AgentActionService actionService,
                                    DashScopeProperties dashScopeProperties,
                                    AgentExecutionSupport executionSupport,
                                    PromptTemplateProvider promptTemplateProvider,
                                    RestClient.Builder builder,
                                    Optional<FactSummarizer> factSummarizer) {
        this.orderTools = orderTools;
        this.actionService = actionService;
        this.dashScopeProperties = dashScopeProperties;
        this.executionSupport = executionSupport;
        this.promptTemplateProvider = promptTemplateProvider;
        this.dashScopeClient = builder.baseUrl("https://dashscope.aliyuncs.com").build();
        this.factSummarizer = factSummarizer;
    }

    /**
     * 执行手写模式对话：识别订单号 → 调用工具 → 检索知识库 → 生成回答。
     *
     * @param context 平台传入的执行上下文
     * @return 执行结果（含响应、工具调用列表、RAG 命中摘要）
     */
    @Override
    public ManualExecutionResult execute(ManualExecutionContext context) {
        long started = System.currentTimeMillis();
        String traceId = context.traceId();
        String question = context.question();
        log.info("OrderManualAgentExecutor 开始 traceId={} sessionId={}", traceId, context.sessionId());

        List<String> toolCalls = new ArrayList<>();
        List<AgentPendingActionView> pendingActions = new ArrayList<>();
        Optional<String> orderNo = extractOrderNo(question);
        if (orderNo.isEmpty() && context.history() != null && !context.history().isBlank()) {
            orderNo = extractOrderNo(context.history());
            if (orderNo.isPresent()) {
                log.info("当前轮未识别订单号，从历史对话回溯 traceId={} orderNo={}", traceId, orderNo.get());
            }
        }
        String orderContext = "";
        if (orderNo.isPresent()) {
            log.info("识别到订单号 traceId={} orderNo={}", traceId, orderNo.get());
            orderContext = orderTools.getOrderDetail(orderNo.get());
            toolCalls.add("getOrderDetail(" + orderNo.get() + ")");
            if (question.contains("支付")) {
                orderContext += "\n" + orderTools.getPaymentStatus(orderNo.get());
                toolCalls.add("getPaymentStatus(" + orderNo.get() + ")");
            }
            if (question.contains("退款")) {
                orderContext += "\n" + orderTools.getRefundStatus(orderNo.get());
                toolCalls.add("getRefundStatus(" + orderNo.get() + ")");
            }
            if (question.contains("权益") || question.contains("会员")) {
                orderContext += "\n" + orderTools.getBenefitStatus(orderNo.get());
                toolCalls.add("getBenefitStatus(" + orderNo.get() + ")");
            }

            String actionResult = detectAndExecuteAction(question, context.history(), orderNo.get());
            if (actionResult != null) {
                orderContext += "\n" + actionResult;
            }
        }

        String actionHistory = formatActionHistory(context.sessionId());
        if (!actionHistory.isEmpty()) {
            orderContext += "\n" + actionHistory;
        }

        List<KnowledgeDocument> ragHits = orderTools.searchKnowledge(question, context.category());
        toolCalls.add("searchKnowledge(...)");
        List<AgentRagHitView> ragHitDetails = RagHitViews.fromDocuments(ragHits);
        List<String> ragHitSummaries = RagHitViews.summaryLines(ragHitDetails);

        String answer = generateAnswer(question, context.history(), orderContext, ragHits);
        long durationMs = System.currentTimeMillis() - started;
        log.info("OrderManualAgentExecutor 完成 traceId={} durationMs={}", traceId, durationMs);
        AgentChatResponse response = executionSupport.buildResponse(
            traceId, context.sessionId(), context.turnIndex(), question, answer, durationMs,
            toolCalls, ragHitSummaries, "manual-rest + DashScope HTTP", pendingActions, ragHitDetails);
        return new ManualExecutionResult(response, toolCalls, ragHitSummaries);
    }

    private String detectAndExecuteAction(String question, String history, String orderNo) {
        boolean historyMentionsRefund = history != null && history.contains("退款");
        boolean questionConfirmsRefund = REFUND_CONFIRM_KEYWORDS.stream().anyMatch(question::contains);
        if (historyMentionsRefund && questionConfirmsRefund) {
            log.info("检测到退款确认意图 orderNo={}", orderNo);
            String result = orderTools.submitRefundRequest(orderNo, "用户确认申请退款");
            return "【退款申请结果】" + result;
        }

        boolean historyMentionsBenefit = history != null && (history.contains("权益") || history.contains("补发"));
        boolean questionConfirmsBenefitRetry = BENEFIT_RETRY_KEYWORDS.stream().anyMatch(question::contains);
        if (historyMentionsBenefit && questionConfirmsBenefitRetry) {
            log.info("检测到权益重试意图 orderNo={}", orderNo);
            String result = orderTools.retryBenefitIssue(orderNo);
            return "【权益重试结果】" + result;
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
                case "SUBMIT_REFUND" -> "退款申请";
                case "RETRY_BENEFIT" -> "权益重试";
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

    /** 从问题文本中提取第一个 ORD- 订单号。 */
    private Optional<String> extractOrderNo(String question) {
        Matcher matcher = ORDER_NO_PATTERN.matcher(question);
        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
    }

    /**
     * 生成客服回答：优先 DashScope Qwen，失败或未配置时回退本地模板。
     */
    private String generateAnswer(String question, String history, String orderContext, List<KnowledgeDocument> ragHits) {
        String policyContext = CustomerServicePrompts.formatPolicyContext(ragHits);
        if (!dashScopeProperties.enabled()) {
            log.info("未配置 DASHSCOPE_API_KEY，手写模式使用本地模板回答");
            return fallbackAnswer(question, orderContext, policyContext);
        }
        try {
            log.info("手写模式开始调用 DashScope Qwen model={}", dashScopeProperties.chatModel());
            // 直接 HTTP 调用 DashScope 文本生成 API
            Map<String, Object> response = dashScopeClient.post()
                .uri("/api/v1/services/aigc/text-generation/generation")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + dashScopeProperties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "model", dashScopeProperties.chatModel(),
                    "input", Map.of("messages", List.of(
                        Map.of("role", "system", "content", promptTemplateProvider.getManualSystem()),
                        Map.of("role", "user", "content",
                            promptTemplateProvider.manualUserPrompt(question, history, orderContext, policyContext))
                    )),
                    "parameters", Map.of("result_format", "message")
                ))
                .retrieve()
                .body(Map.class);
            String answer = parseDashScopeAnswer(response);
            log.info("手写模式 DashScope Qwen 调用成功 answerLength={}", answer.length());
            return answer;
        } catch (RuntimeException ex) {
            log.warn("手写模式 DashScope Qwen 调用失败，使用本地模板回答 message={}", ex.getMessage());
            return fallbackAnswer(question, orderContext, policyContext);
        }
    }

    /** 解析 DashScope 响应体中的 message.content 字段。 */
    @SuppressWarnings("unchecked")
    private String parseDashScopeAnswer(Map<String, Object> response) {
        Map<String, Object> output = (Map<String, Object>) response.get("output");
        Map<String, Object> choices0 = (Map<String, Object>) ((List<Object>) output.get("choices")).get(0);
        Map<String, Object> message = (Map<String, Object>) choices0.get("message");
        return String.valueOf(message.get("content"));
    }

    /**
     * 本地模板回退回答：按问题类型（退款/支付/权益）选用不同话术模板。
     */
    private String fallbackAnswer(String question, String orderContext, String policyContext) {
        if (orderContext.isBlank()) {
            return "请先提供订单号（如 ORD-1001），我才能查询订单相关信息。";
        }
        String facts = CustomerServicePlainFormatter.summarizeFacts(orderContext, factSummarizer);
        if (question.contains("退款")) {
            return """
                根据目前查到的信息：
                %s

                建议回复用户：订单完成超过 7 天的，一般无法再申请普通退款；若仍在 7 天内，可协助提交退款申请（需人工确认）。
                请先核对订单完成时间，再向用户解释具体原因。
                """.formatted(facts);
        }
        if (question.contains("支付")) {
            return """
                根据目前查到的信息：
                %s

                建议回复用户：若钱已扣但订单仍显示待确认，可能是支付结果同步延迟，请勿重复付款；请联系运营核实支付记录并补做确认。
                """.formatted(facts);
        }
        if (question.contains("权益") || question.contains("会员")) {
            return """
                根据目前查到的信息：
                %s

                建议回复用户：若权益显示发放失败，请先确认支付是否成功；如因系统超时导致失败，可申请补发（需人工确认）。
                """.formatted(facts);
        }
        return """
            根据目前查到的信息：
            %s

            请结合上述情况，用通俗语言向用户说明下一步处理方式。
            """.formatted(facts);
    }
}
