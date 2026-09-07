package com.agentforge.hr.cs.employee;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeDetail> listEmployees() {
        log.info("收到员工列表查询请求");
        return employeeService.listEmployees();
    }

    @GetMapping("/{empNo}")
    public EmployeeDetail getEmployee(@PathVariable String empNo) {
        log.info("收到员工详情查询请求 empNo={}", empNo);
        return employeeService.getEmployeeDetail(empNo);
    }
}
