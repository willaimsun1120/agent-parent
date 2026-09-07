package com.agentforge.order.cs.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@link AdminPageController} Web 层单元测试。
 *
 * <p>验证 {@code /admin/} 请求正确转发至静态管理页 {@code /admin/index.html}。
 */
class AdminPageControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPageController()).build();
    }

    @Test
    void forwardsAdminDirectoryToIndexPage() throws Exception {
        mockMvc.perform(get("/admin/"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/admin/index.html"));
    }
}
