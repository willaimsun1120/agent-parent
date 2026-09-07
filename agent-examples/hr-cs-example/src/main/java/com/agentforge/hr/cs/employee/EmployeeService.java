package com.agentforge.hr.cs.employee;

import com.agentforge.hr.cs.cache.CachedEmployeeDetailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeMapper employeeMapper;
    private final CachedEmployeeDetailService cachedEmployeeDetailService;

    public EmployeeService(EmployeeMapper employeeMapper,
                           CachedEmployeeDetailService cachedEmployeeDetailService) {
        this.employeeMapper = employeeMapper;
        this.cachedEmployeeDetailService = cachedEmployeeDetailService;
    }

    public List<EmployeeDetail> listEmployees() {
        log.info("开始查询员工列表");
        return employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                .orderByAsc(Employee::getEmpNo))
            .stream()
            .map(employee -> getEmployeeDetail(employee.getEmpNo()))
            .toList();
    }

    public EmployeeDetail getEmployeeDetail(String empNo) {
        log.info("开始查询员工详情 empNo={}", empNo);
        try {
            return cachedEmployeeDetailService.getEmployeeDetail(empNo);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("员工不存在 empNo={}", empNo);
            }
            throw ex;
        }
    }
}
