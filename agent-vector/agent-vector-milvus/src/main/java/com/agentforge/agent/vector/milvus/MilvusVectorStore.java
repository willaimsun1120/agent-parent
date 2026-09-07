package com.agentforge.agent.vector.milvus;

import com.agentforge.agent.config.MilvusProperties;
import com.agentforge.agent.vector.core.VectorPoint;
import com.agentforge.agent.vector.core.VectorSearchHit;
import com.agentforge.agent.vector.core.VectorStore;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Milvus 向量库实现（Java SDK）。
 */
@Component
@ConditionalOnProperty(name = "agent.vector-store.type", havingValue = "milvus")
public class MilvusVectorStore implements VectorStore {
    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStore.class);

    private static final String FIELD_ID = "id";
    private static final String FIELD_VECTOR = "vector";
    private static final String FIELD_DOC_CODE = "doc_code";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_VERSION = "version";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_SOURCE = "source";

    private final MilvusProperties properties;
    private final MilvusServiceClient client;

    public MilvusVectorStore(MilvusProperties properties) {
        this.properties = properties;
        this.client = new MilvusServiceClient(ConnectParam.newBuilder()
            .withUri(properties.uri())
            .build());
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public String type() {
        return "milvus";
    }

    @Override
    public String collectionName() {
        return properties.collection();
    }

    @Override
    public void ensureCollection() {
        String collection = properties.collection();
        R<Boolean> has = client.hasCollection(HasCollectionParam.newBuilder()
            .withCollectionName(collection)
            .build());
        if (Boolean.TRUE.equals(has.getData())) {
            loadCollection(collection);
            return;
        }
        List<FieldType> fields = List.of(
            FieldType.newBuilder().withName(FIELD_ID).withDataType(DataType.VarChar).withMaxLength(64)
                .withPrimaryKey(true).withAutoID(false).build(),
            FieldType.newBuilder().withName(FIELD_DOC_CODE).withDataType(DataType.VarChar).withMaxLength(128).build(),
            FieldType.newBuilder().withName(FIELD_TITLE).withDataType(DataType.VarChar).withMaxLength(255).build(),
            FieldType.newBuilder().withName(FIELD_CATEGORY).withDataType(DataType.VarChar).withMaxLength(64).build(),
            FieldType.newBuilder().withName(FIELD_VERSION).withDataType(DataType.Int64).build(),
            FieldType.newBuilder().withName(FIELD_CONTENT).withDataType(DataType.VarChar).withMaxLength(8192).build(),
            FieldType.newBuilder().withName(FIELD_SOURCE).withDataType(DataType.VarChar).withMaxLength(512).build(),
            FieldType.newBuilder().withName(FIELD_VECTOR).withDataType(DataType.FloatVector)
                .withDimension(properties.vectorSize()).build()
        );
        R<RpcStatus> created = client.createCollection(CreateCollectionParam.newBuilder()
            .withCollectionName(collection)
            .withFieldTypes(fields)
            .build());
        if (created.getStatus() != R.Status.Success.getCode()) {
            throw new IllegalStateException("Milvus createCollection failed: " + created.getMessage());
        }
        client.createIndex(CreateIndexParam.newBuilder()
            .withCollectionName(collection)
            .withFieldName(FIELD_VECTOR)
            .withIndexType(IndexType.AUTOINDEX)
            .withMetricType(MetricType.COSINE)
            .build());
        loadCollection(collection);
        log.info("Milvus collection 已创建 collection={}", collection);
    }

    @Override
    public void upsert(List<VectorPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        ensureCollection();
        String collection = properties.collection();
        List<String> ids = new ArrayList<>();
        List<String> docCodes = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        List<Long> versions = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        List<List<Float>> vectors = new ArrayList<>();
        for (VectorPoint point : points) {
            ids.add(point.id());
            docCodes.add(str(point.payload().get(FIELD_DOC_CODE)));
            titles.add(str(point.payload().get(FIELD_TITLE)));
            categories.add(str(point.payload().get(FIELD_CATEGORY)));
            versions.add(longVal(point.payload().get(FIELD_VERSION)));
            contents.add(truncate(str(point.payload().get(FIELD_CONTENT)), 8192));
            sources.add(truncate(str(point.payload().get(FIELD_SOURCE)), 512));
            vectors.add(point.vector());
        }
        List<InsertParam.Field> fields = List.of(
            new InsertParam.Field(FIELD_ID, ids),
            new InsertParam.Field(FIELD_DOC_CODE, docCodes),
            new InsertParam.Field(FIELD_TITLE, titles),
            new InsertParam.Field(FIELD_CATEGORY, categories),
            new InsertParam.Field(FIELD_VERSION, versions),
            new InsertParam.Field(FIELD_CONTENT, contents),
            new InsertParam.Field(FIELD_SOURCE, sources),
            new InsertParam.Field(FIELD_VECTOR, vectors)
        );
        R<MutationResult> inserted = client.insert(InsertParam.newBuilder()
            .withCollectionName(collection)
            .withFields(fields)
            .build());
        if (inserted.getStatus() != R.Status.Success.getCode()) {
            throw new IllegalStateException("Milvus insert failed: " + inserted.getMessage());
        }
    }

    @Override
    public void deleteByDocCode(String docCode) {
        try {
            ensureCollection();
            String expr = FIELD_DOC_CODE + " == \"" + escapeExpr(docCode) + "\"";
            R<MutationResult> deleted = client.delete(DeleteParam.newBuilder()
                .withCollectionName(properties.collection())
                .withExpr(expr)
                .build());
            if (deleted.getStatus() != R.Status.Success.getCode()) {
                log.warn("Milvus 删除向量失败 docCode={} message={}", docCode, deleted.getMessage());
                return;
            }
            log.info("已删除 Milvus 中 docCode={} 的旧向量", docCode);
        } catch (RuntimeException ex) {
            log.warn("Milvus 删除向量失败 docCode={} message={}", docCode, ex.getMessage());
        }
    }

    @Override
    public List<VectorSearchHit> search(List<Float> queryVector, int topK, Optional<String> category) {
        ensureCollection();
        String collection = properties.collection();
        String expr = category.map(value -> FIELD_CATEGORY + " == \"" + escapeExpr(value) + "\"").orElse("");
        SearchParam.Builder builder = SearchParam.newBuilder()
            .withCollectionName(collection)
            .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
            .withMetricType(MetricType.COSINE)
            .withTopK(topK)
            .withVectors(Collections.singletonList(queryVector))
            .withVectorFieldName(FIELD_VECTOR)
            .withOutFields(List.of(FIELD_ID, FIELD_DOC_CODE, FIELD_TITLE, FIELD_CATEGORY, FIELD_VERSION, FIELD_CONTENT, FIELD_SOURCE));
        if (!expr.isBlank()) {
            builder.withExpr(expr);
        }
        R<SearchResults> response = client.search(builder.build());
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new IllegalStateException("Milvus search failed: " + response.getMessage());
        }
        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
        if (scores == null || scores.isEmpty()) {
            return List.of();
        }
        List<VectorSearchHit> hits = new ArrayList<>();
        for (SearchResultsWrapper.IDScore score : scores) {
            hits.add(new VectorSearchHit(
                str(score.getFieldValues().get(FIELD_ID)),
                str(score.getFieldValues().get(FIELD_SOURCE)),
                str(score.getFieldValues().get(FIELD_CONTENT)),
                score.getScore(),
                str(score.getFieldValues().get(FIELD_DOC_CODE)),
                str(score.getFieldValues().get(FIELD_TITLE)),
                str(score.getFieldValues().get(FIELD_CATEGORY)),
                (int) longVal(score.getFieldValues().get(FIELD_VERSION))
            ));
        }
        return hits;
    }

    private void loadCollection(String collection) {
        client.loadCollection(LoadCollectionParam.newBuilder()
            .withCollectionName(collection)
            .build());
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static long longVal(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    private static String escapeExpr(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
