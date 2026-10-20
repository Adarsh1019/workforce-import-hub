package com.workforce.dto;

import com.workforce.entity.ImportJob;
import java.time.Instant;

public record ImportJobResponse(
        Long id, String fileName, String status, int totalRecords, int processedRecords,
        int successCount, int failureCount, String createdBy, Instant startedAt, Instant finishedAt,
        String errorMessage) {

    public static ImportJobResponse from(ImportJob j) {
        return new ImportJobResponse(j.getId(), j.getFileName(), j.getStatus(), j.getTotalRecords(),
                j.getProcessedRecords(), j.getSuccessCount(), j.getFailureCount(), j.getCreatedBy(),
                j.getStartedAt(), j.getFinishedAt(), j.getErrorMessage());
    }
}
