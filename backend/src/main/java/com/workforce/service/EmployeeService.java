package com.workforce.service;

import com.workforce.dto.EmployeeRequest;
import com.workforce.dto.EmployeeResponse;
import com.workforce.dto.PageResponse;
import com.workforce.entity.Department;
import com.workforce.entity.Employee;
import com.workforce.exception.ConflictException;
import com.workforce.exception.NotFoundException;
import com.workforce.repository.DepartmentRepository;
import com.workforce.repository.EmployeeRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmployeeService {
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;

    public EmployeeService(EmployeeRepository employees, DepartmentRepository departments) {
        this.employees = employees;
        this.departments = departments;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> search(String search, Long departmentId, Pageable pageable) {
        Specification<Employee> spec = (root, query, cb) -> {
            // fetch department in the list query (not in the count query) to avoid N+1
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("department", JoinType.INNER);
            }
            return cb.conjunction();
        };
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("role")), like)));
        }
        if (departmentId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        return PageResponse.of(employees.findAll(spec, pageable), EmployeeResponse::from);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long id) { return EmployeeResponse.from(find(id)); }

    public EmployeeResponse create(EmployeeRequest req) {
        if (employees.existsByEmailIgnoreCase(req.email()))
            throw new ConflictException("An employee with email " + req.email() + " already exists");
        Employee e = new Employee();
        apply(e, req);
        return EmployeeResponse.from(employees.save(e));
    }

    public EmployeeResponse update(Long id, EmployeeRequest req) {
        Employee e = find(id);
        if (employees.existsByEmailIgnoreCaseAndIdNot(req.email(), id))
            throw new ConflictException("An employee with email " + req.email() + " already exists");
        apply(e, req);
        return EmployeeResponse.from(e);
    }

    public void delete(Long id) {
        employees.delete(find(id));
    }

    private Employee find(Long id) {
        return employees.findById(id).orElseThrow(() -> new NotFoundException("Employee " + id + " not found"));
    }

    private void apply(Employee e, EmployeeRequest req) {
        Department d = departments.findById(req.departmentId())
                .orElseThrow(() -> new NotFoundException("Department " + req.departmentId() + " not found"));
        e.setName(req.name().trim());
        e.setEmail(req.email().trim().toLowerCase());
        e.setDepartment(d);
        e.setRole(req.role().trim());
        e.setSalary(req.salary());
        e.setJoiningDate(req.joiningDate());
    }
}
