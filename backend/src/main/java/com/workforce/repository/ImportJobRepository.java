package com.workforce.repository;

import com.workforce.entity.ImportJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    List<ImportJob> findTop5ByOrderByIdDesc();
}
