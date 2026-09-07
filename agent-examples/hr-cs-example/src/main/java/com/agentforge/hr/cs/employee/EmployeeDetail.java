package com.agentforge.hr.cs.employee;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 员工聚合视图（只读 DTO）。
 */
public record EmployeeDetail(
    String empNo,
    String name,
    String department,
    Integer annualLeaveBalance,
    LocalDate hireDate,
    List<String> leaveRequests,
    List<String> payrollRecords,
    List<String> benefits
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
