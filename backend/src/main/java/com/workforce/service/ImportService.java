package com.workforce.service;

import com.workforce.batch.EmployeeImportJobConfig;
import com.workforce.dto.ImportJobResponse;
import com.workforce.dto.PageResponse;
import com.workforce.entity.ImportError;
import com.workforce.entity.ImportJob;
import com.workforce.exception.ConflictException;
import com.workforce.exception.NotFoundException;
import com.workforce.repository.ImportErrorRepository;
import com.workforce.repository.ImportJobRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportService {
    private final ImportJobRepository jobs;
    private final ImportErrorRepository errors;
    private final JobLauncher launcher;
    private final Job importJob;
    private final JobExplorer explorer;
    private final JobRepository jobRepository;
    private final Path uploadDir;

    public ImportService(ImportJobRepository jobs, ImportErrorRepository errors,
                         @Qualifier("asyncJobLauncher") JobLauncher launcher,
                         @Qualifier("employeeImportJob") Job importJob,
                         JobExplorer explorer, JobRepository jobRepository,
                         @Value("${app.import.upload-dir}") String uploadDir) {
        this.jobs = jobs;
        this.errors = errors;
        this.launcher = launcher;
        this.importJob = importJob;
        this.explorer = explorer;
        this.jobRepository = jobRepository;
        this.uploadDir = Path.of(uploadDir);
    }

    public ImportJobResponse upload(MultipartFile file, String username) {
        if (file.isEmpty()) throw new IllegalArgumentException("The uploaded file is empty");
        String original = file.getOriginalFilename() == null ? "upload.csv" : Path.of(file.getOriginalFilename()).getFileName().toString();
        if (!original.toLowerCase().endsWith(".csv")) throw new IllegalArgumentException("Only .csv files are accepted");
        try {
            Files.createDirectories(uploadDir);
            Path stored = uploadDir.resolve(UUID.randomUUID() + ".csv");
            file.transferTo(stored.toAbsolutePath());
            String header = Files.lines(stored, StandardCharsets.UTF_8).findFirst().orElse("").replace("\uFEFF", "").trim();
            if (!header.equalsIgnoreCase(String.join(",", EmployeeImportJobConfig.COLUMNS))) {
                Files.deleteIfExists(stored);
                throw new IllegalArgumentException("CSV header must be: " + String.join(",", EmployeeImportJobConfig.COLUMNS));
            }
            ImportJob job = new ImportJob();
            job.setFileName(original);
            job.setStoredPath(stored.toAbsolutePath().toString());
            job.setStatus("STARTING");
            job.setCreatedBy(username);
            job.setTotalRecords(countDataLines(stored));
            job = jobs.save(job);
            launch(job);
            return ImportJobResponse.from(jobs.findById(job.getId()).orElseThrow());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Restarts a FAILED/STOPPED job. Spring Batch resumes from the last committed chunk because the
     * job parameters are identical; already-imported rows are never inserted twice.
     */
    public ImportJobResponse restart(Long id) {
        ImportJob job = find(id);
        if (job.getBatchExecutionId() != null) {
            JobExecution last = explorer.getJobExecution(job.getBatchExecutionId());
            if (last != null && last.isRunning()) {
                throw new ConflictException("Job is still running");
            }
            if (last != null && last.getStatus() == BatchStatus.COMPLETED) {
                throw new ConflictException("Job already completed; nothing to restart");
            }
        }
        launch(job);
        return ImportJobResponse.from(find(id));
    }

    public PageResponse<ImportJobResponse> list(Pageable pageable) {
        return PageResponse.of(jobs.findAll(pageable), ImportJobResponse::from);
    }

    public ImportJobResponse get(Long id) { return ImportJobResponse.from(find(id)); }

    public String errorsCsv(Long id) {
        find(id);
        StringBuilder sb = new StringBuilder("lineNumber,reason,rawLine\n");
        for (ImportError e : errors.findByImportJobIdOrderByLineNumber(id)) {
            sb.append(e.getLineNumber()).append(',').append(csv(e.getReason())).append(',').append(csv(e.getRawLine())).append('\n');
        }
        return sb.toString();
    }

    /** After a crash, executions left in STARTED can never finish; mark them FAILED so they can be restarted. */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOrphanedExecutions() {
        for (JobExecution je : explorer.findRunningJobExecutions(EmployeeImportJobConfig.JOB_NAME)) {
            je.getStepExecutions().forEach(se -> {
                se.setStatus(BatchStatus.FAILED);
                se.setEndTime(LocalDateTime.now());
                jobRepository.update(se);
            });
            je.setStatus(BatchStatus.FAILED);
            je.setEndTime(LocalDateTime.now());
            jobRepository.update(je);
            Long importJobId = je.getJobParameters().getLong("importJobId");
            if (importJobId != null) jobs.findById(importJobId).ifPresent(j -> {
                j.setStatus("FAILED");
                j.setErrorMessage("Server stopped while the import was running. Restart to resume.");
                jobs.save(j);
            });
        }
    }

    private void launch(ImportJob job) {
        JobParameters params = new JobParametersBuilder()
                .addLong("importJobId", job.getId())
                .addString("filePath", job.getStoredPath())
                .toJobParameters();
        try {
            launcher.run(importJob, params);
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            jobs.save(job);
            throw new ConflictException("Could not start import: " + e.getMessage());
        }
    }

    private ImportJob find(Long id) {
        return jobs.findById(id).orElseThrow(() -> new NotFoundException("Import job " + id + " not found"));
    }

    private static int countDataLines(Path p) throws IOException {
        try (Stream<String> lines = Files.lines(p, StandardCharsets.UTF_8)) {
            return (int) Math.max(0, lines.skip(1).filter(l -> !l.isBlank()).count());
        }
    }

    private static String csv(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
