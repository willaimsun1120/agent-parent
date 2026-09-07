package com.agentforge.hr.cs.employee;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("leave_requests")
public class LeaveRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String empNo;
    private String leaveType;
    private Integer days;
    private String status;
    private String reason;
    private LocalDate startDate;
    private LocalDateTime createdAt;

    public LeaveRequest() {
    }

    public LeaveRequest(String empNo, String leaveType, Integer days, String status, String reason, LocalDate startDate) {
        this.empNo = empNo;
        this.leaveType = leaveType;
        this.days = days;
        this.status = status;
        this.reason = reason;
        this.startDate = startDate;
    }

    public Long getId() { return id; }
    public String getEmpNo() { return empNo; }
    public String getLeaveType() { return leaveType; }
    public Integer getDays() { return days; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setEmpNo(String empNo) { this.empNo = empNo; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
    public void setDays(Integer days) { this.days = days; }
    public void setStatus(String status) { this.status = status; }
    public void setReason(String reason) { this.reason = reason; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
