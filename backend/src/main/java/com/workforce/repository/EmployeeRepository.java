package com.workforce.repository;

import com.workforce.entity.Employee;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    long countByDepartmentId(Long departmentId);
    long countByImportJobId(Long importJobId);

    @Query("select coalesce(avg(e.salary), 0) from Employee e")
    BigDecimal averageSalary();
}
