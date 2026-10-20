package com.workforce.service;

import com.workforce.entity.ImportJob;
import com.workforce.repository.EmployeeRepository;
import com.workforce.repository.ImportErrorRepository;
import com.workforce.repository.ImportJobRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps import_jobs in sync with the running batch job. Counts are derived from the
 * database (imported employees + stored errors) so they stay correct across restarts.
 */
@Service
public class ImportJobTracker {
    private final ImportJobRepository jobs;
    private final EmployeeRepository employees;
    private final ImportErrorRepository errors;

    public ImportJobTracker(ImportJobRepository jobs, EmployeeRepository employees, ImportErrorRepository errors) {
        this.jobs = jobs;
        this.employees = employees;
        this.errors = errors;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStarted(Long importJobId, Long executionId) {
        jobs.findById(importJobId).ifPresent(j -> {
            j.setStatus("STARTED");
            j.setBatchExecutionId(executionId);
            j.setErrorMessage(null);
            if (j.getStartedAt() == null) j.setStartedAt(Instant.now());
            j.setFinishedAt(null);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshCounts(Long importJobId) {
        jobs.findById(importJobId).ifPresent(this::applyCounts);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFinished(Long importJobId, String status, String errorMessage) {
        jobs.findById(importJobId).ifPresent(j -> {
            applyCounts(j);
            j.setStatus(status);
            j.setErrorMessage(errorMessage);
            j.setFinishedAt(Instant.now());
        });
    }

    private void applyCounts(ImportJob j) {
        int ok = (int) employees.countByImportJobId(j.getId());
        int bad = (int) errors.countByImportJobId(j.getId());
        j.setSuccessCount(ok);
        j.setFailureCount(bad);
        j.setProcessedRecords(ok + bad);
    }
}
