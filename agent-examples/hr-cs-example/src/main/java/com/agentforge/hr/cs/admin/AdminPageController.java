package com.agentforge.hr.cs.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HR 管理页面入口控制器。
 */
@Controller
public class AdminPageController {
    private static final Logger log = LoggerFactory.getLogger(AdminPageController.class);

    @GetMapping("/admin/")
    public String adminIndex() {
        log.info("访问 HR 客服助手管理页面");
        return "forward:/admin/index.html";
    }
}
