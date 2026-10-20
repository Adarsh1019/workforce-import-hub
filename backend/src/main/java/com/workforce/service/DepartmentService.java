package com.workforce.service;

import com.workforce.dto.DepartmentRequest;
import com.workforce.dto.DepartmentResponse;
import com.workforce.entity.Department;
import com.workforce.exception.ConflictException;
import com.workforce.exception.NotFoundException;
import com.workforce.repository.DepartmentRepository;
import com.workforce.repository.EmployeeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DepartmentService {
    private final DepartmentRepository departments;
    private final EmployeeRepository employees;

    public DepartmentService(DepartmentRepository departments, EmployeeRepository employees) {
        this.departments = departments;
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() { return departments.findAllWithCounts(); }

    public DepartmentResponse create(DepartmentRequest req) {
        if (departments.existsByNameIgnoreCase(req.name().trim()))
            throw new ConflictException("Department '" + req.name() + "' already exists");
        Department d = departments.save(new Department(req.name().trim(), req.description()));
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription(), 0);
    }

    public DepartmentResponse update(Long id, DepartmentRequest req) {
        Department d = departments.findById(id).orElseThrow(() -> new NotFoundException("Department not found"));
        departments.findByNameIgnoreCase(req.name().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ConflictException("Department '" + req.name() + "' already exists"); });
        d.setName(req.name().trim());
        d.setDescription(req.description());
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription(), employees.countByDepartmentId(id));
    }

    public void delete(Long id) {
        if (!departments.existsById(id)) throw new NotFoundException("Department not found");
        long count = employees.countByDepartmentId(id);
        if (count > 0) throw new ConflictException("Cannot delete: department still has " + count + " employees");
        departments.deleteById(id);
    }
}
