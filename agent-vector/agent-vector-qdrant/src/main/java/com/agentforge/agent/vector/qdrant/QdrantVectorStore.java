package com.agentforge.agent.vector.qdrant;

import com.agentforge.agent.config.QdrantProperties;
import com.agentforge.agent.vector.core.VectorPoint;
import com.agentforge.agent.vector.core.VectorSearchHit;
import com.agentforge.agent.vector.core.VectorStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Qdrant 向量库实现（REST API）。
 */
@Component
@ConditionalOnProperty(name = "agent.vector-store.type", havingValue = "qdrant", matchIfMissing = true)
public class QdrantVectorStore implements VectorStore {
    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStore.class);

    private final QdrantProperties properties;
    private final RestClient restClient;

    public QdrantVectorStore(QdrantProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public String type() {
        return "qdrant";
    }

    @Override
    public String collectionName() {
        return properties.collection();
    }

    @Override
    public void ensureCollection() {
        try {
            restClient.put()
                .uri("/collections/{collection}", properties.collection())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("vectors", Map.of("size", properties.vectorSize(), "distance", "Cosine")))
                .retrieve()
                .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Qdrant collection ensure failed. message={}", ex.getMessage());
        }
    }

    @Override
    public void upsert(List<VectorPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        List<Map<String, Object>> bodyPoints = points.stream()
            .map(point -> Map.<String, Object>of(
                "id", point.id(),
                "vector", point.vector(),
                "payload", point.payload()
            ))
            .toList();
        restClient.put()
            .uri("/collections/{collection}/points?wait=true", properties.collection())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("points", bodyPoints))
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public void deleteByDocCode(String docCode) {
        try {
            restClient.post()
                .uri("/collections/{collection}/points/delete?wait=true", properties.collection())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("filter", Map.of(
                    "must", List.of(Map.of(
                        "key", "doc_code",
                        "match", Map.of("value", docCode)
                    ))
                )))
                .retrieve()
                .toBodilessEntity();
            log.info("已删除 Qdrant 中 docCode={} 的旧向量", docCode);
        } catch (RuntimeException ex) {
            log.warn("删除 Qdrant 向量失败 docCode={} message={}", docCode, ex.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VectorSearchHit> search(List<Float> queryVector, int topK, Optional<String> category) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("vector", queryVector);
        body.put("limit", topK);
        body.put("with_payload", true);
        category.ifPresent(value -> body.put("filter", Map.of(
            "must", List.of(Map.of(
                "key", "category",
                "match", Map.of("value", value)
            ))
        )));

        Map<String, Object> response = restClient.post()
            .uri("/collections/{collection}/points/search", properties.collection())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map.class);
        List<Map<String, Object>> result = (List<Map<String, Object>>) response.get("result");
        return result.stream()
            .map(item -> {
                Map<String, Object> payload = (Map<String, Object>) item.get("payload");
                return new VectorSearchHit(
                    String.valueOf(item.get("id")),
                    String.valueOf(payload.get("source")),
                    String.valueOf(payload.get("content")),
                    ((Number) item.get("score")).doubleValue(),
                    String.valueOf(payload.get("doc_code")),
                    String.valueOf(payload.get("title")),
                    String.valueOf(payload.get("category")),
                    payload.get("version") == null ? 0 : ((Number) payload.get("version")).intValue()
                );
            })
            .toList();
    }
}
