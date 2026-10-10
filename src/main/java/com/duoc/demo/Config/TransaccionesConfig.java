package com.duoc.demo.Config;

import java.io.IOException;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.duoc.demo.Listener.TransaccionesSkipListener;
import com.duoc.demo.Model.LegacyTransacciones;
import com.duoc.demo.Model.ModernTransacciones;
import com.duoc.demo.Partition.BankPartitioner;
import com.duoc.demo.Policy.BankSkipPolicy;
import com.duoc.demo.Processor.LegacyTransaccionesProcessor;
import com.duoc.demo.Reader.TransaccionesReader;
import com.duoc.demo.Writer.TransaccionesWriter;

@Configuration
public class TransaccionesConfig {

    private static final String TRANSACCIONES_INPUT_PATH =
        "data/fin_legacy_data/semana_3/movimientos_financieros_diarios.csv";

    private static final String DELETE_TRANSACCIONES_SQL =
            "DELETE FROM TRANSACCIONES_PROCESADAS";

    private static final String DELETE_RESUMEN_TRANSACCIONES_SQL =
            "DELETE FROM RESUMEN_TRANSACCIONES_DIARIAS";

    private static final String INSERT_RESUMEN_TRANSACCIONES_SQL = """
            INSERT INTO RESUMEN_TRANSACCIONES_DIARIAS (
                FECHA,
                TOTAL_TRANSACCIONES,
                TOTAL_VALIDAS,
                TOTAL_ANOMALIAS,
                CANTIDAD_DEBITOS,
                CANTIDAD_CREDITOS,
                CANTIDAD_DESCONOCIDOS,
                MONTO_DEBITOS,
                MONTO_CREDITOS,
                MONTO_TOTAL,
                FECHA_GENERACION
            )
            SELECT
                FECHA,
                COUNT(*),
                SUM(CASE
                    WHEN ESTADO = 'VALIDA'
                    THEN 1 ELSE 0
                END),
                SUM(CASE
                    WHEN ESTADO = 'ANOMALIA'
                    THEN 1 ELSE 0
                END),
                SUM(CASE
                    WHEN TIPO = 'debito'
                    THEN 1 ELSE 0
                END),
                SUM(CASE
                    WHEN TIPO = 'credito'
                    THEN 1 ELSE 0
                END),
                SUM(CASE
                    WHEN TIPO = 'desconocido'
                    THEN 1 ELSE 0
                END),
                SUM(CASE
                    WHEN TIPO = 'debito'
                    THEN MONTO ELSE 0
                END),
                SUM(CASE
                    WHEN TIPO = 'credito'
                    THEN MONTO ELSE 0
                END),
                SUM(MONTO),
                SYSTIMESTAMP
            FROM TRANSACCIONES_PROCESADAS
            GROUP BY FECHA
            """;

    @Bean
    @StepScope
    public TransaccionesReader transaccionesReader(
            @Value("#{stepExecutionContext['start']}") Integer start,
            @Value("#{stepExecutionContext['end']}") Integer end)
            throws IOException {

        return new TransaccionesReader(
                TRANSACCIONES_INPUT_PATH,
                start,
                end);
    }

    @Bean
    public LegacyTransaccionesProcessor transaccionesProcessor() {
        return new LegacyTransaccionesProcessor();
    }

    @Bean
    public TransaccionesWriter transaccionesWriter(JdbcTemplate jdbcTemplate) {
        return new TransaccionesWriter(jdbcTemplate);
    }

    @Bean
    Step stepTransacciones(
            JobRepository jobRepository,
            TransaccionesReader reader,
            LegacyTransaccionesProcessor processor,
            TransaccionesWriter writer,
            BankSkipPolicy bankSkipPolicy,
            TransaccionesSkipListener transaccionesSkipListener) {

        return new StepBuilder("stepTransacciones", jobRepository)
                .<LegacyTransacciones, ModernTransacciones>chunk(5)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(transaccionesSkipListener)
                .faultTolerant()
                .retry(TransientDataAccessException.class)
                .retryLimit(2)
                .skipPolicy(bankSkipPolicy)
                .build();
    }

    @Bean
    Step stepLimpiarTransacciones(
            JobRepository jobRepository,
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            LegacyTransaccionesProcessor processor) {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return new StepBuilder(
                "stepLimpiarTransacciones",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    jdbcTemplate.update(DELETE_TRANSACCIONES_SQL);

                    processor.reset();

                    return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

    @Bean
    Step stepResumenTransacciones(
            JobRepository jobRepository,
            DataSource dataSource,
            PlatformTransactionManager transactionManager) {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return new StepBuilder(
                "stepResumenTransacciones",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    jdbcTemplate.update(DELETE_RESUMEN_TRANSACCIONES_SQL);
                    jdbcTemplate.update(INSERT_RESUMEN_TRANSACCIONES_SQL);

                    return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

    @Bean
    public TaskExecutorPartitionHandler partitionHandlerTransacciones(
            Step stepTransacciones,
            TaskExecutor taskExecutor) {

        TaskExecutorPartitionHandler handler =
                new TaskExecutorPartitionHandler();

        handler.setStep(stepTransacciones);
        handler.setTaskExecutor(taskExecutor);
        handler.setGridSize(5);

        return handler;
    }

    @Bean
    public Step partitionStepTransacciones(
            JobRepository jobRepository,
            @Qualifier("partitionHandlerTransacciones")
            TaskExecutorPartitionHandler partitionHandler) {

        BankPartitioner dynamicPartitioner =
                new BankPartitioner(
                        new ClassPathResource(
                                TRANSACCIONES_INPUT_PATH));

        return new StepBuilder(
                "partitionStepTransacciones",
                jobRepository)
                .partitioner(
                        "stepTransacciones",
                        dynamicPartitioner)
                .partitionHandler(partitionHandler)
                .build();
    }
}