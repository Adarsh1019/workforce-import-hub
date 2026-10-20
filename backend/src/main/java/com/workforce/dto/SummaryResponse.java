package com.workforce.dto;

import java.math.BigDecimal;
import java.util.List;

public record SummaryResponse(
        long totalEmployees, long totalDepartments, long totalImports, BigDecimal averageSalary,
        List<DepartmentCount> employeesByDepartment, List<ImportJobResponse> recentImports) {

    public record DepartmentCount(String name, long count) {}
}
