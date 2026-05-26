package com.carddemo.batch.config;

import com.carddemo.batch.job.AuthorizationPurgeJob;
import com.carddemo.batch.job.DailyTransactionPostingJob;
import com.carddemo.batch.job.DailyTransactionReportJob;
import com.carddemo.batch.job.InterestCalculationJob;
import com.carddemo.batch.job.StatementGenerationJob;
import com.carddemo.batch.job.TransactionInitJob;
import com.carddemo.batch.listener.BatchJobListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Spring Batch configuration defining all batch jobs and steps.
 * Replaces JCL job definitions for the batch processing chain:
 *   CBTRN01C → TransactionInitJob
 *   CBTRN02C → DailyTransactionPostingJob (POSTTRAN)
 *   CBACT04C → InterestCalculationJob (INTCALC)
 *   CBTRN03C → DailyTransactionReportJob (TRANREPT)
 *   CBSTM03A/B → StatementGenerationJob (CREASTMT)
 */
@Configuration
public class BatchConfig {

    @Value("${batch.input.dailytran:#{null}}")
    private String dailyTranPath;

    @Value("${batch.output.reports:#{null}}")
    private String reportOutputPath;

    @Value("${batch.output.statements:#{null}}")
    private String statementOutputPath;

    @Value("${batch.parm.date:#{null}}")
    private String parmDate;

    // ── Transaction Init Job (CBTRN01C) ──

    @Bean
    public Job transactionInitBatchJob(JobRepository jobRepository, Step transactionInitStep,
                                   BatchJobListener listener) {
        return new JobBuilder("transactionInitJob", jobRepository)
                .listener(listener)
                .start(transactionInitStep)
                .build();
    }

    @Bean
    public Step transactionInitStep(JobRepository jobRepository,
                                     PlatformTransactionManager txManager,
                                     TransactionInitJob job) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            String path = (String) chunkContext.getStepContext()
                    .getJobParameters().get("inputFile");
            if (path == null) {
                path = dailyTranPath;
            }
            if (path == null) {
                throw new IllegalArgumentException("inputFile job parameter or batch.input.dailytran property required");
            }
            job.execute(Paths.get(path));
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("transactionInitStep", jobRepository)
                .tasklet(tasklet, txManager)
                .build();
    }

    // ── Daily Transaction Posting Job (CBTRN02C / POSTTRAN) ──

    @Bean
    public Job dailyTransactionPostingBatchJob(JobRepository jobRepository, Step postingStep,
                                           BatchJobListener listener) {
        return new JobBuilder("dailyTransactionPostingJob", jobRepository)
                .listener(listener)
                .start(postingStep)
                .build();
    }

    @Bean
    public Step postingStep(JobRepository jobRepository,
                            PlatformTransactionManager txManager,
                            DailyTransactionPostingJob job) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            job.execute();
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("postingStep", jobRepository)
                .tasklet(tasklet, txManager)
                .build();
    }

    // ── Interest Calculation Job (CBACT04C / INTCALC) ──

    @Bean
    public Job interestCalculationBatchJob(JobRepository jobRepository, Step interestStep,
                                       BatchJobListener listener) {
        return new JobBuilder("interestCalculationJob", jobRepository)
                .listener(listener)
                .start(interestStep)
                .build();
    }

    @Bean
    public Step interestStep(JobRepository jobRepository,
                             PlatformTransactionManager txManager,
                             InterestCalculationJob job) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            String dateParam = (String) chunkContext.getStepContext()
                    .getJobParameters().get("parmDate");
            if (dateParam == null) {
                dateParam = parmDate;
            }
            LocalDate date = dateParam != null
                    ? LocalDate.parse(dateParam, DateTimeFormatter.ISO_LOCAL_DATE)
                    : LocalDate.now();
            job.execute(date);
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("interestStep", jobRepository)
                .tasklet(tasklet, txManager)
                .build();
    }

    // ── Daily Transaction Report Job (CBTRN03C / TRANREPT) ──

    @Bean
    public Job dailyTransactionReportBatchJob(JobRepository jobRepository, Step reportStep,
                                          BatchJobListener listener) {
        return new JobBuilder("dailyTransactionReportJob", jobRepository)
                .listener(listener)
                .start(reportStep)
                .build();
    }

    @Bean
    public Step reportStep(JobRepository jobRepository,
                           PlatformTransactionManager txManager,
                           DailyTransactionReportJob job) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            String path = (String) chunkContext.getStepContext()
                    .getJobParameters().get("outputFile");
            if (path == null) {
                path = reportOutputPath;
            }
            Path output = path != null ? Paths.get(path) : Paths.get("reports", "tranrept.txt");
            job.execute(output);
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("reportStep", jobRepository)
                .tasklet(tasklet, txManager)
                .build();
    }

    // ── Statement Generation Job (CBSTM03A/B / CREASTMT) ──

    @Bean
    public Job statementGenerationBatchJob(JobRepository jobRepository, Step statementStep,
                                       BatchJobListener listener) {
        return new JobBuilder("statementGenerationJob", jobRepository)
                .listener(listener)
                .start(statementStep)
                .build();
    }

    @Bean
    public Step statementStep(JobRepository jobRepository,
                              PlatformTransactionManager txManager,
                              StatementGenerationJob job) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            String path = (String) chunkContext.getStepContext()
                    .getJobParameters().get("outputDir");
            if (path == null) {
                path = statementOutputPath;
            }
            Path output = path != null ? Paths.get(path) : Paths.get("statements");
            job.execute(output);
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("statementStep", jobRepository)
                .tasklet(tasklet, txManager)
                .build();
    }

    // ── Authorization Purge Job (CBPAUP0C) ──

    @Bean
    public Job authorizationPurgeBatchJob(JobRepository jobRepository, Step purgeStep,
                                       BatchJobListener listener) {
        return new JobBuilder("authorizationPurgeJob", jobRepository)
                .listener(listener)
                .start(purgeStep)
                .build();
    }

    @Bean
    public Step purgeStep(JobRepository jobRepository,
                          PlatformTransactionManager txManager,
                          AuthorizationPurgeJob job) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            String daysParam = (String) chunkContext.getStepContext()
                    .getJobParameters().get("expiryDays");
            Integer expiryDays = daysParam != null ? Integer.parseInt(daysParam) : null;
            job.execute(expiryDays);
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("purgeStep", jobRepository)
                .tasklet(tasklet, txManager)
                .build();
    }
}
