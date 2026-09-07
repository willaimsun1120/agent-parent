package com.agentforge.agent.core.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 分类解析配置，绑定 {@code agent.platform.rag-category.*}。
 *
 * <p>支持两种策略：
 * <ul>
 *   <li>{@code keyword}（默认）— 关键词 + 正则匹配，零延迟</li>
 *   <li>{@code llm} — 调用 LLM 做意图分类，准确率更高但有额外延迟</li>
 * </ul>
 *
 * <p>业务 Demo 在 application.yml 中配置分类映射规则，无需写 Java 代码。
 * 若业务自定义了 {@link com.agentforge.agent.core.spi.RagCategoryResolver} Bean，
 * 则本配置不生效（{@code @ConditionalOnMissingBean}）。
 */
@ConfigurationProperties(prefix = "agent.platform.rag-category")
public class RagCategoryProperties {

    private boolean enabled = false;
    private Strategy strategy = Strategy.KEYWORD;
    private String defaultCategory;
    private Map<String, CategoryMapping> categories = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public String getDefaultCategory() {
        return defaultCategory;
    }

    public void setDefaultCategory(String defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    public Map<String, CategoryMapping> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, CategoryMapping> categories) {
        this.categories = categories;
    }

    public enum Strategy {
        KEYWORD,
        LLM
    }

    public static class CategoryMapping {
        private List<String> keywords = List.of();
        private String regex;
        private String description;

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        public String getRegex() {
            return regex;
        }

        public void setRegex(String regex) {
            this.regex = regex;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
