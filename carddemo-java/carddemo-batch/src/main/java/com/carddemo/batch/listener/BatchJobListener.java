package com.carddemo.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BatchJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("START OF EXECUTION OF JOB {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("END OF EXECUTION OF JOB {} - COMPLETED",
                    jobExecution.getJobInstance().getJobName());
        } else {
            log.error("JOB {} FINISHED WITH STATUS: {}",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getStatus());
        }
    }
}
