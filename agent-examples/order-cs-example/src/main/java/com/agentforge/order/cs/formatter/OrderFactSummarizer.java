package com.agentforge.order.cs.formatter;

import com.agentforge.agent.core.spi.FactSummarizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 订单客服场景的事实摘要实现：将工具返回的 JSON 转为通俗中文摘要。
 *
 * <p>原 {@code CustomerServicePlainFormatter.summarizeOrderFacts} 中的订单特定逻辑已迁入此类。
 */
@Component
public class OrderFactSummarizer implements FactSummarizer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String summarize(String factJson) {
        if (factJson == null || factJson.isBlank()) {
            return "暂无订单数据。";
        }
        StringBuilder builder = new StringBuilder();
        for (String chunk : extractJsonObjects(factJson)) {
            try {
                JsonNode node = MAPPER.readTree(chunk);
                String line = describeJson(node);
                if (!line.isBlank()) {
                    builder.append(line).append('\n');
                }
            } catch (Exception ignored) {
            }
        }
        return builder.isEmpty() ? "暂无订单数据。" : builder.toString().trim();
    }

    private static List<String> extractJsonObjects(String raw) {
        List<String> objects = new ArrayList<>();
        int index = 0;
        while (index < raw.length()) {
            int start = raw.indexOf('{', index);
            if (start < 0) {
                break;
            }
            int depth = 0;
            int end = -1;
            for (int i = start; i < raw.length(); i++) {
                char ch = raw.charAt(i);
                if (ch == '{') {
                    depth++;
                } else if (ch == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
            if (end < 0) {
                break;
            }
            objects.add(raw.substring(start, end + 1));
            index = end + 1;
        }
        return objects;
    }

    private static String describeJson(JsonNode node) {
        if (node.has("orderNo") && node.has("status") && node.has("amount")) {
            return describeOrderDetail(node);
        }
        if (node.has("paymentStatus") && node.size() <= 4) {
            return "支付情况：" + zhPayment(node.get("paymentStatus").asText())
                + "，支付结果确认：" + zhCallback(node.path("paymentCallbackStatus").asText(null));
        }
        if (node.has("refundStatus")) {
            String reason = node.path("refundReason").asText("");
            return "退款情况：" + zhRefund(node.get("refundStatus").asText())
                + (reason.isBlank() ? "" : "，原因：" + reason);
        }
        if (node.has("benefits")) {
            return "权益情况：" + formatBenefits(node.get("benefits"));
        }
        return "";
    }

    private static String describeOrderDetail(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append("订单 ").append(node.get("orderNo").asText());
        sb.append("，金额 ").append(node.get("amount").asText()).append(" 元");
        sb.append("，当前进度：").append(zhOrderStatus(node.get("status").asText()));
        if (node.hasNonNull("paidAt")) {
            sb.append("，已于 ").append(formatTime(node.get("paidAt").asText())).append(" 完成支付");
        }
        if (node.hasNonNull("completedAt")) {
            sb.append("，已于 ").append(formatTime(node.get("completedAt").asText())).append(" 订单完成");
        }
        sb.append("；支付：").append(zhPayment(node.path("paymentStatus").asText("未知")));
        sb.append("，支付确认：").append(zhCallback(node.path("paymentCallbackStatus").asText(null)));
        sb.append("；退款：").append(zhRefund(node.path("refundStatus").asText("NONE")));
        String refundReason = node.path("refundReason").asText("");
        if (!refundReason.isBlank()) {
            sb.append("（").append(refundReason).append("）");
        }
        if (node.has("benefits") && node.get("benefits").isArray() && !node.get("benefits").isEmpty()) {
            sb.append("；权益：").append(formatBenefits(node.get("benefits")));
        }
        return sb.toString();
    }

    private static String formatBenefits(JsonNode benefits) {
        if (!benefits.isArray() || benefits.isEmpty()) {
            return "暂无权益记录";
        }
        return benefits.toString()
            .replace("ISSUED", "已发放")
            .replace("FAILED", "发放失败")
            .replace("PENDING", "发放中");
    }

    private static String formatTime(String iso) {
        return iso.length() >= 16 ? iso.substring(0, 16).replace('T', ' ') : iso;
    }

    private static String zhOrderStatus(String status) {
        return switch (status) {
            case "CREATED" -> "已下单，待支付";
            case "PENDING_PAYMENT_CALLBACK" -> "已支付，等待支付结果确认";
            case "COMPLETED" -> "已完成";
            case "BENEFIT_FAILED" -> "已完成，但权益发放失败";
            default -> status;
        };
    }

    private static String zhPayment(String status) {
        return switch (status) {
            case "SUCCESS" -> "已成功";
            case "PENDING" -> "待支付";
            case "FAILED" -> "失败";
            default -> "未知";
        };
    }

    private static String zhCallback(String status) {
        if (status == null || status.isBlank()) {
            return "暂无";
        }
        return switch (status) {
            case "SUCCESS" -> "已确认";
            case "FAILED" -> "确认失败";
            case "PENDING" -> "待确认";
            default -> status;
        };
    }

    private static String zhRefund(String status) {
        return switch (status) {
            case "NONE" -> "未申请";
            case "REQUESTED" -> "已申请，处理中";
            case "REJECTED" -> "已拒绝";
            case "APPROVED" -> "已通过";
            default -> status;
        };
    }
}
