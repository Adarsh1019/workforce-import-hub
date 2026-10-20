package com.workforce.service;

import com.workforce.dto.ImportJobResponse;
import com.workforce.dto.SummaryResponse;
import com.workforce.repository.DepartmentRepository;
import com.workforce.repository.EmployeeRepository;
import com.workforce.repository.ImportJobRepository;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final ImportJobRepository imports;

    public DashboardService(EmployeeRepository employees, DepartmentRepository departments, ImportJobRepository imports) {
        this.employees = employees;
        this.departments = departments;
        this.imports = imports;
    }

    public SummaryResponse summary() {
        var byDept = departments.findAllWithCounts().stream()
                .map(d -> new SummaryResponse.DepartmentCount(d.name(), d.employeeCount())).toList();
        return new SummaryResponse(
                employees.count(), departments.count(), imports.count(),
                employees.averageSalary().setScale(2, RoundingMode.HALF_UP),
                byDept,
                imports.findTop5ByOrderByIdDesc().stream().map(ImportJobResponse::from).toList());
    }
}
