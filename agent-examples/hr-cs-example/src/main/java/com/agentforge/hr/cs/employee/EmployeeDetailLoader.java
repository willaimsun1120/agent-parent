package com.agentforge.hr.cs.employee;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeDetailLoader {
    private static final Logger log = LoggerFactory.getLogger(EmployeeDetailLoader.class);

    private final EmployeeMapper employeeMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final PayrollRecordMapper payrollRecordMapper;
    private final HrBenefitMapper hrBenefitMapper;

    public EmployeeDetailLoader(EmployeeMapper employeeMapper,
                                LeaveRequestMapper leaveRequestMapper,
                                PayrollRecordMapper payrollRecordMapper,
                                HrBenefitMapper hrBenefitMapper) {
        this.employeeMapper = employeeMapper;
        this.leaveRequestMapper = leaveRequestMapper;
        this.payrollRecordMapper = payrollRecordMapper;
        this.hrBenefitMapper = hrBenefitMapper;
    }

    public EmployeeDetail load(String empNo) {
        log.info("加载员工详情（无缓存） empNo={}", empNo);
        Employee employee = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>()
            .eq(Employee::getEmpNo, empNo)
            .last("LIMIT 1"));
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + empNo);
        }
        List<String> leaveRequests = leaveRequestMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getEmpNo, empNo)
                .orderByDesc(LeaveRequest::getCreatedAt))
            .stream()
            .map(leave -> leave.getLeaveType() + ":" + leave.getDays() + "d:" + leave.getStatus()
                + (leave.getStartDate() == null ? "" : ":" + leave.getStartDate())
                + (leave.getReason() == null ? "" : "(" + leave.getReason() + ")"))
            .toList();
        List<String> payrollRecords = payrollRecordMapper.selectList(new LambdaQueryWrapper<PayrollRecord>()
                .eq(PayrollRecord::getEmpNo, empNo)
                .orderByDesc(PayrollRecord::getPayMonth))
            .stream()
            .map(payroll -> payroll.getPayMonth() + ":" + payroll.getAmount() + ":" + payroll.getStatus()
                + (payroll.getPaidAt() == null ? "" : ":" + payroll.getPaidAt()))
            .toList();
        List<String> benefits = hrBenefitMapper.selectList(new LambdaQueryWrapper<HrBenefit>()
                .eq(HrBenefit::getEmpNo, empNo)
                .orderByDesc(HrBenefit::getCreatedAt))
            .stream()
            .map(benefit -> benefit.getBenefitType() + ":" + benefit.getStatus())
            .toList();
        return new EmployeeDetail(
            employee.getEmpNo(),
            employee.getName(),
            employee.getDepartment(),
            employee.getAnnualLeaveBalance(),
            employee.getHireDate(),
            leaveRequests,
            payrollRecords,
            benefits
        );
    }
}
