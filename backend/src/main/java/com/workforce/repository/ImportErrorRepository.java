package com.workforce.repository;

import com.workforce.entity.ImportError;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportErrorRepository extends JpaRepository<ImportError, Long> {
    List<ImportError> findByImportJobIdOrderByLineNumber(Long importJobId);
    long countByImportJobId(Long importJobId);
    boolean existsByImportJobIdAndLineNumber(Long importJobId, int lineNumber);
}
