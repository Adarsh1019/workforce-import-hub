package com.workforce.batch;

import com.workforce.entity.Employee;
import com.workforce.repository.DepartmentRepository;
import com.workforce.repository.EmployeeRepository;
import com.workforce.repository.ImportErrorRepository;
import com.workforce.service.ImportJobTracker;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class EmployeeImportJobConfig {
    public static final String JOB_NAME = "employeeImportJob";
    public static final String[] COLUMNS = {"name", "email", "department", "role", "salary", "joiningDate"};

    /** Runs jobs on a background thread so the upload request returns immediately. */
    @Bean
    JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("import-"));
        launcher.afterPropertiesSet();
        return launcher;
    }

    @Bean
    Job employeeImportJob(JobRepository jobRepository, Step importStep, ImportJobTracker tracker) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(importStep)
                .listener(new JobExecutionListener() {
                    @Override public void beforeJob(JobExecution je) {
                        tracker.markStarted(je.getJobParameters().getLong("importJobId"), je.getId());
                    }
                    @Override public void afterJob(JobExecution je) {
                        String msg = je.getAllFailureExceptions().stream().findFirst().map(Throwable::getMessage).orElse(null);
                        tracker.markFinished(je.getJobParameters().getLong("importJobId"), je.getStatus().name(), msg);
                    }
                })
                .build();
    }

    @Bean
    Step importStep(JobRepository jobRepository, PlatformTransactionManager tx,
                    FlatFileItemReader<EmployeeCsvRow> employeeReader,
                    EmployeeRowProcessor employeeProcessor,
                    ImportProgressListener importListener,
                    JpaItemWriter<Employee> employeeWriter,
                    @Value("${app.import.chunk-size:500}") int chunkSize) {
        return new StepBuilder("importStep", jobRepository)
                .<EmployeeCsvRow, Employee>chunk(chunkSize, tx)
                .reader(employeeReader)
                .processor(employeeProcessor)
                .writer(employeeWriter)
                .faultTolerant()
                .skip(RowValidationException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(Integer.MAX_VALUE)
                .noRollback(RowValidationException.class)
                .processorNonTransactional()
                .listener((org.springframework.batch.core.SkipListener<EmployeeCsvRow, Employee>) importListener)
                .listener((org.springframework.batch.core.ChunkListener) importListener)
                .build();
    }

    @Bean
    @StepScope
    FlatFileItemReader<EmployeeCsvRow> employeeReader(@Value("#{jobParameters['filePath']}") String filePath) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(COLUMNS);
        tokenizer.setStrict(true);
        BeanWrapperFieldSetMapper<EmployeeCsvRow> fieldMapper = new BeanWrapperFieldSetMapper<>();
        fieldMapper.setTargetType(EmployeeCsvRow.class);
        DefaultLineMapper<EmployeeCsvRow> delegate = new DefaultLineMapper<>();
        delegate.setLineTokenizer(tokenizer);
        delegate.setFieldSetMapper(fieldMapper);
        LineMapper<EmployeeCsvRow> mapper = (line, lineNumber) -> {
            EmployeeCsvRow row = delegate.mapLine(line, lineNumber);
            row.setLineNumber(lineNumber);
            row.setRawLine(line);
            return row;
        };
        return new FlatFileItemReaderBuilder<EmployeeCsvRow>()
                .name("employeeReader")       // saveState: lets a restarted job resume after the last committed chunk
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .lineMapper(mapper)
                .build();
    }

    @Bean
    @StepScope
    EmployeeRowProcessor employeeProcessor(@Value("#{jobParameters['importJobId']}") Long importJobId,
                                           EmployeeRepository employees, DepartmentRepository departments) {
        return new EmployeeRowProcessor(importJobId, employees, departments);
    }

    @Bean
    @StepScope
    ImportProgressListener importListener(@Value("#{jobParameters['importJobId']}") Long importJobId,
                                          ImportErrorRepository errors, ImportJobTracker tracker) {
        return new ImportProgressListener(importJobId, errors, tracker);
    }

    @Bean
    JpaItemWriter<Employee> employeeWriter(EntityManagerFactory emf) {
        return new JpaItemWriterBuilder<Employee>().entityManagerFactory(emf).usePersist(true).build();
    }
}
