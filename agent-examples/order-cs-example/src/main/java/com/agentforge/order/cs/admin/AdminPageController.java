package com.agentforge.order.cs.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 管理页面入口控制器。
 *
 * <p>将 {@code /admin/} 请求转发至静态 HTML 管理页，供运营维护订单、知识库与 Agent 动作；
 * 与平台 SPI 无直接关系，仅为 Demo UI 入口。
 */
@Controller
public class AdminPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminPageController.class);

    /**
     * 转发至订单客服助手管理静态页面。
     *
     * @return 转发目标路径
     */
    @GetMapping("/admin/")
    public String adminIndex() {
        // 关键入口：让用户直接访问 /admin/ 时进入同一个静态管理页面。
        log.info("访问订单客服助手管理页面");
        return "forward:/admin/index.html";
    }
}
