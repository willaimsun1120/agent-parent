package com.agentforge.hr.cs.employee;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("employees")
public class Employee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String empNo;
    private String name;
    private String department;
    private Integer annualLeaveBalance;
    private LocalDate hireDate;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getEmpNo() { return empNo; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public Integer getAnnualLeaveBalance() { return annualLeaveBalance; }
    public LocalDate getHireDate() { return hireDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setEmpNo(String empNo) { this.empNo = empNo; }
    public void setName(String name) { this.name = name; }
    public void setDepartment(String department) { this.department = department; }
    public void setAnnualLeaveBalance(Integer annualLeaveBalance) { this.annualLeaveBalance = annualLeaveBalance; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
