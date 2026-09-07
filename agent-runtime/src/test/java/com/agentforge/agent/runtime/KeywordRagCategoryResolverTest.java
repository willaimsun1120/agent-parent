package com.agentforge.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentforge.agent.core.config.RagCategoryProperties;
import com.agentforge.agent.core.config.RagCategoryProperties.CategoryMapping;
import com.agentforge.agent.runtime.RagCategoryResolverConfiguration.KeywordRagCategoryResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeywordRagCategoryResolverTest {

    private KeywordRagCategoryResolver resolver;

    @BeforeEach
    void setUp() {
        RagCategoryProperties properties = new RagCategoryProperties();
        properties.setEnabled(true);
        properties.setDefaultCategory("general");

        Map<String, CategoryMapping> categories = new LinkedHashMap<>();

        CategoryMapping refund = new CategoryMapping();
        refund.setKeywords(List.of("退款", "refund", "退钱"));
        refund.setRegex("退[款费钱]|refund");
        categories.put("refund", refund);

        CategoryMapping payment = new CategoryMapping();
        payment.setKeywords(List.of("支付", "payment", "回调"));
        payment.setRegex("支付|付款|回调|payment");
        categories.put("payment", payment);

        properties.setCategories(categories);
        resolver = new KeywordRagCategoryResolver(properties);
    }

    @Test
    void explicitCategoryTakesPrecedence() {
        assertThat(resolver.resolve("refund", "随便问")).contains("refund");
    }

    @Test
    void keywordMatchRefund() {
        assertThat(resolver.resolve(null, "退款多久到账")).contains("refund");
    }

    @Test
    void keywordMatchPayment() {
        assertThat(resolver.resolve(null, "支付成功但状态不对")).contains("payment");
    }

    @Test
    void regexMatchRefund() {
        assertThat(resolver.resolve(null, "退费流程是什么")).contains("refund");
    }

    @Test
    void regexMatchPayment() {
        assertThat(resolver.resolve(null, "付款后没反应")).contains("payment");
    }

    @Test
    void noMatchReturnsDefault() {
        assertThat(resolver.resolve(null, "你好")).contains("general");
    }

    @Test
    void nullQuestionReturnsDefault() {
        assertThat(resolver.resolve(null, null)).contains("general");
    }

    @Test
    void blankQuestionReturnsDefault() {
        assertThat(resolver.resolve(null, "  ")).contains("general");
    }

    @Test
    void keywordPriorityOverRegex() {
        assertThat(resolver.resolve(null, "退款")).contains("refund");
    }

    @Test
    void explicitCategoryTrimmed() {
        assertThat(resolver.resolve("  REFUND ", "xxx")).contains("refund");
    }
}
