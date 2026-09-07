package com.agentforge.hr.cs.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 知识库文章切片服务。
 *
 * <p>将文章正文按固定窗口（500 字符，50 字符重叠）切分，生成 {@link KnowledgeChunk} 并持久化；
 * 向量点 ID 由 docCode、序号、版本与内容确定性生成，供 {@link KnowledgeIngestionService} 写入 Qdrant。
 */
@Service
public class KnowledgeChunkService {
    /** 每个切片的最大字符数 */
    private static final int CHUNK_SIZE = 500;
    /** 相邻切片之间的重叠字符数，避免语义截断 */
    private static final int OVERLAP = 50;

    private final KnowledgeChunkMapper chunkMapper;

    /**
     * 构造切片服务。
     *
     * @param chunkMapper 切片 Mapper
     */
    public KnowledgeChunkService(KnowledgeChunkMapper chunkMapper) {
        this.chunkMapper = chunkMapper;
    }

    /**
     * 重建指定文章的全部切片（先删后插）。
     *
     * @param article 源文章
     * @return 新建的切片列表
     */
    public List<KnowledgeChunk> rebuildChunks(KnowledgeArticle article) {
        chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
            .eq(KnowledgeChunk::getDocCode, article.getDocCode()));

        List<String> parts = split(article.getContent(), CHUNK_SIZE, OVERLAP);
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (int index = 0; index < parts.size(); index++) {
            String content = parts.get(index);
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocCode(article.getDocCode());
            chunk.setChunkIndex(index);
            chunk.setVectorPointId(buildPointId(article.getDocCode(), index, article.getVersion(), content));
            chunk.setContent(content);
            chunk.setArticleVersion(article.getVersion());
            chunkMapper.insert(chunk);
            chunks.add(chunk);
        }
        return chunks;
    }

    /**
     * 按 docCode 查询全部切片，按序号升序。
     *
     * @param docCode 文档编码
     * @return 切片列表
     */
    public List<KnowledgeChunk> listByDocCode(String docCode) {
        return chunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
            .eq(KnowledgeChunk::getDocCode, docCode)
            .orderByAsc(KnowledgeChunk::getChunkIndex));
    }

    /**
     * 汇总多篇已发布文章的全部切片。
     *
     * @param articles 文章列表
     * @return 全部切片
     */
    public List<KnowledgeChunk> listPublishedChunks(List<KnowledgeArticle> articles) {
        List<KnowledgeChunk> all = new ArrayList<>();
        for (KnowledgeArticle article : articles) {
            all.addAll(listByDocCode(article.getDocCode()));
        }
        return all;
    }

    /**
     * 删除指定文档的全部切片。
     *
     * @param docCode 文档编码
     */
    public void deleteByDocCode(String docCode) {
        chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
            .eq(KnowledgeChunk::getDocCode, docCode));
    }

    /**
     * 将文本按固定窗口与重叠量切分。
     *
     * @param text      原始文本
     * @param maxLength 每片最大长度
     * @param overlap   重叠长度
     * @return 切片字符串列表
     */
    public List<String> split(String text, int maxLength, int overlap) {
        List<String> chunks = new ArrayList<>();
        String normalized = text == null ? "" : text.replace("\r\n", "\n").trim();
        if (normalized.isEmpty()) {
            chunks.add("");
            return chunks;
        }
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxLength, normalized.length());
            chunks.add(normalized.substring(start, end));
            if (end >= normalized.length()) {
                break;
            }
            // 下一片起点回退 overlap 字符，保证上下文连续
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    /**
     * 生成 Qdrant 向量点的确定性 UUID。
     *
     * @param docCode     文档编码
     * @param chunkIndex  切片序号
     * @param version     文章版本
     * @param content     切片内容
     * @return UUID 字符串
     */
    public String buildPointId(String docCode, int chunkIndex, int version, String content) {
        String seed = docCode + "#" + chunkIndex + "#v" + version + "#" + content;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
