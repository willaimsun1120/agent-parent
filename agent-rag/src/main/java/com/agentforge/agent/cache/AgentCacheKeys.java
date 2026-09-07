package com.agentforge.agent.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Agent 平台 JetCache 缓存键生成工具。
 */
public final class AgentCacheKeys {

    private AgentCacheKeys() {
    }

    /**
     * 计算文本的 SHA-256 十六进制摘要，用作缓存 key 片段。
     *
     * @param text 输入文本，null 视为空串
     * @return 小写十六进制摘要字符串
     */
    public static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 生成 RAG 检索缓存 key，包含问题、topK、分类与知识库 epoch。
     *
     * @param question        检索问题
     * @param topK            返回条数
     * @param category        分类，null 或空白视为无分类
     * @param knowledgeEpoch  知识库版本号
     * @return SHA-256 摘要作为缓存 key
     */
    public static String ragKey(String question, int topK, String category, long knowledgeEpoch) {
        String normalizedCategory = category == null || category.isBlank() ? "-" : category.trim().toLowerCase();
        return sha256(question + "|k=" + topK + "|c=" + normalizedCategory + "|e=" + knowledgeEpoch);
    }
}
