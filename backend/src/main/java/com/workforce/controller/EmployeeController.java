package com.workforce.controller;

import com.workforce.dto.EmployeeRequest;
import com.workforce.dto.EmployeeResponse;
import com.workforce.dto.PageResponse;
import com.workforce.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees")
public class EmployeeController {
    private static final Set<String> SORTABLE = Set.of("name", "email", "role", "salary", "joiningDate", "department.name", "id");
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Search employees", description = "sort format: field,asc|desc")
    public PageResponse<EmployeeResponse> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(defaultValue = "name,asc") String sort,
                                               @RequestParam(required = false) String search,
                                               @RequestParam(required = false) Long departmentId) {
        String[] parts = sort.split(",");
        String field = SORTABLE.contains(parts[0]) ? parts[0] : "name";
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(dir, field).and(Sort.by("id")));
        return service.search(search, departmentId, pageable);
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest req) { return service.create(req); }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest req) { return service.update(id, req); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
