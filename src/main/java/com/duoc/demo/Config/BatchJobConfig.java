package com.duoc.demo.Config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.duoc.demo.Decider.BankJobDecider;
import com.duoc.demo.Listener.CriticalFailureRestartListener;

@Configuration
public class BatchJobConfig {

    private static final String SIN_ERRORES = "SIN_ERRORES";
    private static final String CON_ERRORES = "CON_ERRORES";

    @Bean
    public Job transaccionesJob(
            JobRepository jobRepository,
            @Qualifier("stepLimpiarTransacciones")
            Step stepLimpiarTransacciones,
            @Qualifier("partitionStepTransacciones")
            Step partitionStep,
            @Qualifier("stepResumenTransacciones")
            Step stepResumenTransacciones,
            @Qualifier("bankJobDecider")
            BankJobDecider bankJobDecider,
            CriticalFailureRestartListener criticalFailureRestartListener) {

        return new JobBuilder(
                "reporteTransaccionesDiariasJob",
                jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(criticalFailureRestartListener)
                .start(stepLimpiarTransacciones)
                .next(partitionStep)
                .next(stepResumenTransacciones)
                .next(bankJobDecider)
                    .on(SIN_ERRORES).end()
                .from(bankJobDecider)
                    .on(CON_ERRORES).end()
                .end()
                .build();
    }

    @Bean
    public Job interesesJob(
            JobRepository jobRepository,
            @Qualifier("stepLimpiarIntereses")
            Step stepLimpiarIntereses,
            @Qualifier("partitionStepIntereses")
            Step partitionStep,
            @Qualifier("stepConsolidarIntereses")
            Step stepConsolidarIntereses,
            @Qualifier("bankJobDecider")
            BankJobDecider bankJobDecider,
            CriticalFailureRestartListener criticalFailureRestartListener) {

        return new JobBuilder(
                "calculoInteresesMensualesJob",
                jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(criticalFailureRestartListener)
                .start(stepLimpiarIntereses)
                .next(partitionStep)
                .next(stepConsolidarIntereses)
                .next(bankJobDecider)
                    .on(SIN_ERRORES).end()
                .from(bankJobDecider)
                    .on(CON_ERRORES).end()
                .end()
                .build();
    }

    @Bean
    public Job cuentasJob(
            JobRepository jobRepository,
            @Qualifier("partitionStepCuentas")
            Step stepCuentas,
            @Qualifier("stepResumenAnual")
            Step stepResumenAnual,
            @Qualifier("stepLimpiarMovimientos")
            Step stepLimpiarMovimientos,
            @Qualifier("bankJobDecider")
            BankJobDecider bankJobDecider,
            CriticalFailureRestartListener criticalFailureRestartListener) {

        return new JobBuilder(
                "estadosCuentaAnualesJob",
                jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(criticalFailureRestartListener)
                .start(stepLimpiarMovimientos)
                .next(stepCuentas)
                .next(stepResumenAnual)
                .next(bankJobDecider)
                    .on(SIN_ERRORES).end()
                .from(bankJobDecider)
                    .on(CON_ERRORES).end()
                .end()
                .build();
    }
}