package com.agentforge.hr.cs.formatter;

import com.agentforge.agent.core.spi.FactSummarizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * HR 客服场景的事实摘要实现：将工具返回的 JSON 转为通俗中文摘要。
 */
@Component
public class HrFactSummarizer implements FactSummarizer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String summarize(String factJson) {
        if (factJson == null || factJson.isBlank()) {
            return "暂无员工数据。";
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
        return builder.isEmpty() ? "暂无员工数据。" : builder.toString().trim();
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
        if (node.has("empNo") && node.has("name")) {
            return describeEmployee(node);
        }
        if (node.has("annualLeaveBalance")) {
            return "年假余额：" + node.get("annualLeaveBalance").asText() + " 天";
        }
        if (node.has("payrollRecords")) {
            return "薪资记录已查询，共 " + node.get("payrollRecords").size() + " 条";
        }
        if (node.has("benefits")) {
            return "福利信息已查询，共 " + node.get("benefits").size() + " 项";
        }
        return "";
    }

    private static String describeEmployee(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append("员工 ").append(node.get("empNo").asText());
        sb.append("（").append(node.get("name").asText()).append("）");
        if (node.has("department")) {
            sb.append("，部门：").append(node.get("department").asText());
        }
        if (node.has("position")) {
            sb.append("，职位：").append(node.get("position").asText());
        }
        return sb.toString();
    }
}
