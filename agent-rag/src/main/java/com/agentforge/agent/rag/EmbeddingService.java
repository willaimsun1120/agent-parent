package com.agentforge.agent.rag;

import com.agentforge.agent.core.config.DashScopeProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 文本向量化服务。
 *
 * <p>优先调用阿里云百炼 text-embedding 接口；未配置 API Key 或调用失败时，
 * 退化为基于 SHA-256 的确定性本地向量，便于离线演示。
 */
@Service
public class EmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int VECTOR_SIZE = 1024;

    private final DashScopeProperties properties;
    private final RestClient restClient;

    /**
     * 构造向量化服务。
     *
     * @param properties 百炼配置
     * @param builder    HTTP 客户端构建器
     */
    public EmbeddingService(DashScopeProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        // DashScope 这里同时承载 embedding 和 chat 两类模型服务。
        // 本类只负责 embedding：把文本转成向量，供 Qdrant 做相似度检索。
        this.restClient = builder.baseUrl("https://dashscope.aliyuncs.com").build();
    }

    /**
     * 将文本转换为固定维度（1024）的浮点向量。
     *
     * @param text 待向量化的文本
     * @return 向量分量列表
     */
    public List<Float> embed(String text) {
        if (!properties.enabled()) {
            // 未配置百炼 Key 时，不调用外部模型，直接生成固定长度的本地向量，方便离线跑通 demo。
            return deterministicEmbedding(text);
        }
        try {
            // 调用百炼 text-embedding 接口，把一句话或一个知识片段转换成向量。
            // 入库时传知识片段，检索时传用户问题；两者向量进入同一空间后才能计算相似度。
            Map<String, Object> response = restClient.post()
                .uri("/api/v1/services/embeddings/text-embedding/text-embedding")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "model", properties.embeddingModel(),
                    "input", Map.of("texts", List.of(text))
                ))
                .retrieve()
                .body(Map.class);
            return parseDashScopeEmbedding(response);
        } catch (RuntimeException ex) {
            log.warn("DashScope embedding failed, fallback to deterministic embedding. message={}", ex.getMessage());
            return deterministicEmbedding(text);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Float> parseDashScopeEmbedding(Map<String, Object> response) {
        // DashScope embedding 返回结构是 output.embeddings[0].embedding。
        // 这里抽出真正的浮点向量，交给 Qdrant 写入或检索。
        Map<String, Object> output = (Map<String, Object>) response.get("output");
        List<Object> embeddings = (List<Object>) output.get("embeddings");
        Map<String, Object> first = (Map<String, Object>) embeddings.get(0);
        List<Object> embedding = (List<Object>) first.get("embedding");
        return embedding.stream().map(value -> ((Number) value).floatValue()).toList();
    }

    private List<Float> deterministicEmbedding(String text) {
        byte[] digest = sha256(text);
        List<Float> vector = new ArrayList<>(VECTOR_SIZE);
        // 本地兜底向量使用 SHA-256 重复填充到 1024 维，保证同一文本每次得到同一向量。
        // 这不是语义向量，只是为了在没有 API Key 时让 Qdrant 写入和查询流程可运行。
        for (int i = 0; i < VECTOR_SIZE; i++) {
            int value = digest[i % digest.length] & 0xff;
            vector.add((value - 128) / 128.0f);
        }
        return vector;
    }

    /** 计算文本的 SHA-256 摘要，供确定性向量生成使用。 */
    private byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
