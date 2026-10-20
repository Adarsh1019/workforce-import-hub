package com.workforce.batch;

import com.workforce.entity.Department;
import com.workforce.entity.Employee;
import com.workforce.repository.DepartmentRepository;
import com.workforce.repository.EmployeeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.batch.item.ItemProcessor;

/**
 * Validates a row, resolves its department and prevents duplicates
 * (already in the database, or repeated earlier in the same file).
 */
public class EmployeeRowProcessor implements ItemProcessor<EmployeeCsvRow, Employee> {
    private final Long importJobId;
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final Map<String, Department> departmentCache = new HashMap<>();
    /** email -> first line it appeared on (keyed by line so reprocessing the same row is not a duplicate). */
    private final Map<String, Integer> seenEmails = new HashMap<>();

    public EmployeeRowProcessor(Long importJobId, EmployeeRepository employees, DepartmentRepository departments) {
        this.importJobId = importJobId;
        this.employees = employees;
        this.departments = departments;
    }

    @Override
    public Employee process(EmployeeCsvRow row) {
        List<String> errors = EmployeeRowValidator.validate(row, LocalDate.now());
        if (!errors.isEmpty()) throw new RowValidationException(String.join("; ", errors));

        String email = row.getEmail().trim().toLowerCase();
        Integer firstLine = seenEmails.putIfAbsent(email, row.getLineNumber());
        if (firstLine != null && firstLine != row.getLineNumber())
            throw new RowValidationException("duplicate email in file (first seen on line " + firstLine + ")");
        if (employees.existsByEmailIgnoreCase(email))
            throw new RowValidationException("employee with email " + email + " already exists");

        String deptName = row.getDepartment().trim();
        Department dept = departmentCache.computeIfAbsent(deptName.toLowerCase(),
                k -> departments.findByNameIgnoreCase(deptName).orElse(null));
        if (dept == null) {
            departmentCache.remove(deptName.toLowerCase());
            throw new RowValidationException("department '" + deptName + "' does not exist");
        }

        Employee e = new Employee();
        e.setName(row.getName().trim());
        e.setEmail(email);
        e.setDepartment(dept);
        e.setRole(row.getRole().trim());
        e.setSalary(new BigDecimal(row.getSalary().trim()));
        e.setJoiningDate(LocalDate.parse(row.getJoiningDate().trim()));
        e.setImportJobId(importJobId);
        return e;
    }
}
