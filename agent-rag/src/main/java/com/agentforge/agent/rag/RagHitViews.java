package com.agentforge.agent.rag;

import com.agentforge.agent.core.api.AgentRagHitView;
import java.util.List;

/**
 * {@link KnowledgeDocument} → 观测用 {@link AgentRagHitView} 转换。
 */
public final class RagHitViews {

    private static final int SNIPPET_MAX = 160;

    private RagHitViews() {
    }

    public static AgentRagHitView from(KnowledgeDocument doc) {
        if (doc == null) {
            return new AgentRagHitView("", "", 0, "");
        }
        String title = doc.title();
        if (title == null || title.isBlank()) {
            title = doc.source();
        }
        return new AgentRagHitView(
            nullToEmpty(doc.docCode()),
            nullToEmpty(title),
            doc.score(),
            snippet(doc.content())
        );
    }

    public static List<AgentRagHitView> fromDocuments(List<KnowledgeDocument> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }
        return docs.stream().map(RagHitViews::from).toList();
    }

    public static List<String> summaryLines(List<AgentRagHitView> details) {
        if (details == null || details.isEmpty()) {
            return List.of();
        }
        return details.stream().map(AgentRagHitView::summaryLine).toList();
    }

    private static String snippet(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= SNIPPET_MAX) {
            return normalized;
        }
        return normalized.substring(0, SNIPPET_MAX) + "…";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
