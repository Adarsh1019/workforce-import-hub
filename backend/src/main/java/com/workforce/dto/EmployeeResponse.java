package com.workforce.dto;

import com.workforce.entity.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id, String name, String email, Long departmentId, String departmentName,
        String role, BigDecimal salary, LocalDate joiningDate) {

    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(e.getId(), e.getName(), e.getEmail(), e.getDepartment().getId(),
                e.getDepartment().getName(), e.getRole(), e.getSalary(), e.getJoiningDate());
    }
}
