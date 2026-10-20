package com.workforce.repository;

import com.workforce.dto.DepartmentResponse;
import com.workforce.entity.Department;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    @Query("""
        select new com.workforce.dto.DepartmentResponse(d.id, d.name, d.description, count(e.id))
        from Department d left join Employee e on e.department = d
        group by d.id, d.name, d.description
        order by d.name""")
    List<DepartmentResponse> findAllWithCounts();
}
