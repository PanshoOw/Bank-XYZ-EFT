package com.duoc.demo.Listener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class CriticalFailureRestartListener
        implements JobExecutionListener {

        private static final Logger log =
                LoggerFactory.getLogger(
                        CriticalFailureRestartListener.class);

        private final JobOperator jobOperator;
        private final JobRepository jobRepository;
        private final boolean autoRestartEnabled;
        private final int maxRestartAttempts;

        public CriticalFailureRestartListener(
                @Lazy JobOperator jobOperator,
                JobRepository jobRepository,
                @Value("${app.batch.auto-restart.enabled:true}")
                boolean autoRestartEnabled,
                @Value("${app.batch.auto-restart.max-attempts:1}")
                int maxRestartAttempts) {

                this.jobOperator = jobOperator;
                this.jobRepository = jobRepository;
                this.autoRestartEnabled = autoRestartEnabled;
                this.maxRestartAttempts = maxRestartAttempts;
        }

        @Override
        public void afterJob(JobExecution jobExecution) {

                if (!autoRestartEnabled) {
                return;
                }

                if (jobExecution.getStatus() != BatchStatus.FAILED) {
                return;
                }

                JobInstance jobInstance =
                        jobExecution.getJobInstance();

                List<JobExecution> executions =
                        jobRepository.getJobExecutions(jobInstance);

                int restartAttempts =
                        Math.max(0, executions.size() - 1);

                if (restartAttempts >= maxRestartAttempts) {

                log.error(
                        "Job {} fallo y alcanzo el maximo de "
                                + "reintentos automaticos: {}",
                        jobInstance.getJobName(),
                        maxRestartAttempts);

                return;
                }

                try {

                log.warn(
                        "Job {} fallo. Reinicio automatico "
                                + "{}/{} para JobExecution {}",
                        jobInstance.getJobName(),
                        restartAttempts + 1,
                        maxRestartAttempts,
                        jobExecution.getId());

                JobExecution restartedExecution =
                        jobOperator.restart(jobExecution);

                log.info(
                        "Reinicio automatico finalizado | "
                                + "JobExecution {} | estado {}",
                        restartedExecution.getId(),
                        restartedExecution.getStatus());

                } catch (JobRestartException ex) {

                log.error(
                        "No fue posible reiniciar automaticamente "
                                + "el Job {}",
                        jobInstance.getJobName(),
                        ex);
                }
        }
}