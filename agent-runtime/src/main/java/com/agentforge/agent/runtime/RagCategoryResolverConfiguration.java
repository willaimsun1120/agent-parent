package com.agentforge.agent.runtime;

import com.agentforge.agent.core.config.DashScopeProperties;
import com.agentforge.agent.core.config.RagCategoryProperties;
import com.agentforge.agent.core.config.RagCategoryProperties.CategoryMapping;
import com.agentforge.agent.core.spi.RagCategoryResolver;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * RAG 分类解析器自动装配。
 *
 * <p>根据配置动态选择实现策略：
 * <ul>
 *   <li>{@code strategy=keyword}（默认）— 关键词 + 正则匹配</li>
 *   <li>{@code strategy=llm} — 调用 LLM 做意图分类，失败时降级为关键词匹配</li>
 * </ul>
 *
 * <p>当 {@code enabled=false} 或未配置时，注册空实现（始终返回 empty）。
 * 若业务自定义了 {@link RagCategoryResolver} Bean，则所有自动装配均不生效。
 */
@Configuration
public class RagCategoryResolverConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RagCategoryResolverConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "agent.platform.rag-category", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(RagCategoryResolver.class)
    RagCategoryResolver ragCategoryResolver(RagCategoryProperties properties,
                                            DashScopeProperties dashScopeProperties,
                                            RestClient.Builder restClientBuilder) {
        KeywordRagCategoryResolver keywordFallback = new KeywordRagCategoryResolver(properties);
        if (RagCategoryProperties.Strategy.LLM == properties.getStrategy()) {
            log.info("RAG 分类解析器已装配 strategy=llm categories={} defaultCategory={}",
                properties.getCategories().keySet(), properties.getDefaultCategory());
            return new LlmRagCategoryResolverImpl(properties, dashScopeProperties,
                restClientBuilder, keywordFallback);
        }
        log.info("RAG 分类解析器已装配 strategy=keyword categories={} defaultCategory={}",
            properties.getCategories().keySet(), properties.getDefaultCategory());
        return keywordFallback;
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.platform.rag-category", name = "enabled",
        havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean(RagCategoryResolver.class)
    RagCategoryResolver noOpRagCategoryResolver() {
        log.info("RAG 分类解析器未启用，使用空实现");
        return (explicitCategory, question) -> {
            if (explicitCategory != null && !explicitCategory.isBlank()) {
                return Optional.of(explicitCategory.trim().toLowerCase());
            }
            return Optional.empty();
        };
    }

    static class KeywordRagCategoryResolver implements RagCategoryResolver {

        private final RagCategoryProperties properties;
        private final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();

        KeywordRagCategoryResolver(RagCategoryProperties properties) {
            this.properties = properties;
        }

        @Override
        public Optional<String> resolve(String explicitCategory, String question) {
            if (explicitCategory != null && !explicitCategory.isBlank()) {
                return Optional.of(explicitCategory.trim().toLowerCase());
            }
            if (question == null || question.isBlank()) {
                return defaultOrEmpty();
            }

            Optional<String> keywordResult = matchKeywords(question);
            if (keywordResult.isPresent()) {
                log.debug("RAG 分类关键词命中 category={} question={}", keywordResult.get(), question);
                return keywordResult;
            }

            Optional<String> regexResult = matchRegex(question);
            if (regexResult.isPresent()) {
                log.debug("RAG 分类正则命中 category={} question={}", regexResult.get(), question);
                return regexResult;
            }

            log.debug("RAG 分类未命中 question={} fallback={}", question,
                properties.getDefaultCategory() != null ? properties.getDefaultCategory() : "empty");
            return defaultOrEmpty();
        }

        private Optional<String> matchKeywords(String question) {
            String lower = question.toLowerCase();
            for (Map.Entry<String, CategoryMapping> entry : properties.getCategories().entrySet()) {
                for (String keyword : entry.getValue().getKeywords()) {
                    if (lower.contains(keyword.toLowerCase())) {
                        return Optional.of(entry.getKey());
                    }
                }
            }
            return Optional.empty();
        }

        private Optional<String> matchRegex(String question) {
            for (Map.Entry<String, CategoryMapping> entry : properties.getCategories().entrySet()) {
                String regex = entry.getValue().getRegex();
                if (regex != null && !regex.isBlank()) {
                    Pattern pattern = compiledPatterns.computeIfAbsent(
                        entry.getKey(),
                        k -> Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
                    );
                    if (pattern.matcher(question).find()) {
                        return Optional.of(entry.getKey());
                    }
                }
            }
            return Optional.empty();
        }

        private Optional<String> defaultOrEmpty() {
            if (properties.getDefaultCategory() != null && !properties.getDefaultCategory().isBlank()) {
                return Optional.of(properties.getDefaultCategory());
            }
            return Optional.empty();
        }
    }

    static class LlmRagCategoryResolverImpl implements RagCategoryResolver {

        private static final String SYSTEM_TEMPLATE = """
            你是一个分类器。根据用户问题，从以下分类中选择最匹配的一个，只返回分类名称，不要返回任何其他内容。
            如果无法判断，返回 unknown。

            可选分类：
            %s
            """;

        private final RagCategoryProperties properties;
        private final DashScopeProperties dashScopeProperties;
        private final KeywordRagCategoryResolver keywordFallback;
        private final RestClient dashScopeClient;
        private final String categoryListText;

        LlmRagCategoryResolverImpl(RagCategoryProperties properties,
                                   DashScopeProperties dashScopeProperties,
                                   RestClient.Builder builder,
                                   KeywordRagCategoryResolver keywordFallback) {
            this.properties = properties;
            this.dashScopeProperties = dashScopeProperties;
            this.keywordFallback = keywordFallback;
            this.dashScopeClient = builder.baseUrl("https://dashscope.aliyuncs.com").build();
            this.categoryListText = buildCategoryListText();
        }

        @Override
        public Optional<String> resolve(String explicitCategory, String question) {
            if (explicitCategory != null && !explicitCategory.isBlank()) {
                return Optional.of(explicitCategory.trim().toLowerCase());
            }
            if (question == null || question.isBlank()) {
                return defaultOrEmpty();
            }

            if (!dashScopeProperties.enabled()) {
                log.debug("DASHSCOPE_API_KEY 未配置，LLM 分类降级为关键词匹配 question={}", question);
                return keywordFallback.resolve(null, question);
            }

            try {
                return resolveByLlm(question);
            } catch (Exception e) {
                log.warn("LLM 分类调用失败，降级为关键词匹配 question={} error={}", question, e.getMessage());
                return keywordFallback.resolve(null, question);
            }
        }

        @SuppressWarnings("unchecked")
        private Optional<String> resolveByLlm(String question) {
            String systemPrompt = SYSTEM_TEMPLATE.formatted(categoryListText);

            Map<String, Object> response = dashScopeClient.post()
                .uri("/api/v1/services/aigc/text-generation/generation")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + dashScopeProperties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "model", dashScopeProperties.chatModel(),
                    "input", Map.of("messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", question)
                    )),
                    "parameters", Map.of(
                        "result_format", "message",
                        "temperature", 0.1
                    )
                ))
                .retrieve()
                .body(Map.class);

            String rawCategory = parseDashScopeAnswer(response);
            String category = normalizeCategory(rawCategory);

            if (category != null) {
                log.debug("LLM 分类结果 raw={} normalized={} question={}", rawCategory, category, question);
                return Optional.of(category);
            }

            log.debug("LLM 分类结果无法识别 raw={} question={}", rawCategory, question);
            return defaultOrEmpty();
        }

        private String normalizeCategory(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String trimmed = raw.trim().toLowerCase();
            if (properties.getCategories().containsKey(trimmed)) {
                return trimmed;
            }
            for (String key : properties.getCategories().keySet()) {
                if (trimmed.contains(key)) {
                    return key;
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private String parseDashScopeAnswer(Map<String, Object> response) {
            if (response == null) return null;
            Map<String, Object> output = (Map<String, Object>) response.get("output");
            if (output == null) return null;
            List<Object> choices = (List<Object>) output.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<String, Object> choices0 = (Map<String, Object>) choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choices0.get("message");
            if (message == null) return null;
            return String.valueOf(message.get("content"));
        }

        private String buildCategoryListText() {
            return properties.getCategories().entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    String desc = entry.getValue().getDescription();
                    if (desc != null && !desc.isBlank()) {
                        return "- " + key + "：" + desc;
                    }
                    return "- " + key;
                })
                .collect(Collectors.joining("\n"));
        }

        private Optional<String> defaultOrEmpty() {
            if (properties.getDefaultCategory() != null && !properties.getDefaultCategory().isBlank()) {
                return Optional.of(properties.getDefaultCategory());
            }
            return Optional.empty();
        }
    }
}
