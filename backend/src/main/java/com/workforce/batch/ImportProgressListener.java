package com.workforce.batch;

import com.workforce.entity.ImportError;
import com.workforce.entity.Employee;
import com.workforce.repository.ImportErrorRepository;
import com.workforce.service.ImportJobTracker;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.file.FlatFileParseException;

/** Records rejected rows into import_errors and refreshes job progress after each chunk. */
public class ImportProgressListener implements SkipListener<EmployeeCsvRow, Employee>, ChunkListener {
    private final Long importJobId;
    private final ImportErrorRepository errors;
    private final ImportJobTracker tracker;

    public ImportProgressListener(Long importJobId, ImportErrorRepository errors, ImportJobTracker tracker) {
        this.importJobId = importJobId;
        this.errors = errors;
        this.tracker = tracker;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        if (t instanceof FlatFileParseException p) {
            record(p.getLineNumber(), p.getInput(), "malformed CSV line: wrong number of columns or bad quoting");
        }
    }

    @Override
    public void onSkipInProcess(EmployeeCsvRow row, Throwable t) {
        record(row.getLineNumber(), row.getRawLine(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(Employee item, Throwable t) {
        record(-1, item.getEmail(), "database rejected row: " + t.getMessage());
    }

    @Override
    public void afterChunk(ChunkContext context) {
        tracker.refreshCounts(importJobId);
    }

    private void record(int line, String raw, String reason) {
        // Idempotent: a restarted job never stores the same rejected line twice.
        if (line > 0 && errors.existsByImportJobIdAndLineNumber(importJobId, line)) return;
        errors.save(new ImportError(importJobId, line, raw, reason));
    }
}
