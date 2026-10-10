package com.duoc.demo.Config;

import java.io.IOException;

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

import com.duoc.demo.Listener.InteresesSkipListener;
import com.duoc.demo.Model.LegacyIntereses;
import com.duoc.demo.Model.ModernIntereses;
import com.duoc.demo.Partition.BankPartitioner;
import com.duoc.demo.Policy.BankSkipPolicy;
import com.duoc.demo.Processor.LegacyInteresesProcessor;
import com.duoc.demo.Reader.InteresesReader;
import com.duoc.demo.Writer.InteresesWriter;

@Configuration
public class InteresesConfig {

        private static final String DELETE_INTERESES_SQL =
                "DELETE FROM INTERESES_PROCESADOS";

        private static final String DELETE_INTERESES_DETALLE_SQL =
                "DELETE FROM INTERESES_DETALLE_PROCESADOS";

private static final String MERGE_INTERESES_CONSOLIDADOS_SQL = """
                MERGE INTO INTERESES_PROCESADOS destino
                        USING (
                        SELECT
                                CUENTA_ID,
                                NOMBRE,
                                SALDO_INICIAL,
                                EDAD,
                                TIPO,
                                TASA_INTERES,
                                INTERES_CALCULADO,
                                SALDO_FINAL,
                                ESTADO,
                                DETALLE_VALIDACION,
                                LINEA_ORIGEN
                        FROM (
                                SELECT
                                detalle.CUENTA_ID,
                                detalle.NOMBRE,
                                detalle.SALDO_INICIAL,
                                detalle.EDAD,
                                detalle.TIPO,
                                detalle.TASA_INTERES,
                                detalle.INTERES_CALCULADO,
                                detalle.SALDO_FINAL,
                                detalle.ESTADO,
                                detalle.DETALLE_VALIDACION,
                                detalle.LINEA_ORIGEN,
                                ROW_NUMBER() OVER (
                                        PARTITION BY detalle.CUENTA_ID
                                        ORDER BY
                                        CASE
                                                WHEN LOWER(detalle.TIPO)
                                                IN ('ahorro', 'prestamo')
                                                THEN 0
                                                ELSE 1
                                        END,
                                        detalle.LINEA_ORIGEN
                                ) AS RN
                                FROM INTERESES_DETALLE_PROCESADOS detalle
                        )
                        WHERE RN = 1
                        ) origen

                        ON (destino.CUENTA_ID = origen.CUENTA_ID)

                        WHEN MATCHED THEN
                        UPDATE SET
                                destino.NOMBRE = origen.NOMBRE,
                                destino.SALDO_INICIAL = origen.SALDO_INICIAL,
                                destino.EDAD = origen.EDAD,
                                destino.TIPO = origen.TIPO,
                                destino.TASA_INTERES = origen.TASA_INTERES,
                                destino.INTERES_CALCULADO =
                                origen.INTERES_CALCULADO,
                                destino.SALDO_FINAL = origen.SALDO_FINAL,
                                destino.ESTADO = origen.ESTADO,
                                destino.DETALLE_VALIDACION =
                                origen.DETALLE_VALIDACION,
                                destino.LINEA_ORIGEN = origen.LINEA_ORIGEN,
                                destino.FECHA_PROCESAMIENTO = SYSTIMESTAMP

                        WHEN NOT MATCHED THEN
                        INSERT (
                                CUENTA_ID,
                                NOMBRE,
                                SALDO_INICIAL,
                                EDAD,
                                TIPO,
                                TASA_INTERES,
                                INTERES_CALCULADO,
                                SALDO_FINAL,
                                ESTADO,
                                DETALLE_VALIDACION,
                                LINEA_ORIGEN
                        )
                        VALUES (
                                origen.CUENTA_ID,
                                origen.NOMBRE,
                                origen.SALDO_INICIAL,
                                origen.EDAD,
                                origen.TIPO,
                                origen.TASA_INTERES,
                                origen.INTERES_CALCULADO,
                                origen.SALDO_FINAL,
                                origen.ESTADO,
                                origen.DETALLE_VALIDACION,
                                origen.LINEA_ORIGEN
                        )
                """;

        private static final String INTERESES_INPUT_PATH =
        "data/fin_legacy_data/semana_3/intereses_trimestrales.csv";

        @Bean
        @StepScope
        public InteresesReader interesesReader(
                @Value("#{stepExecutionContext['start']}") Integer start,
                @Value("#{stepExecutionContext['end']}") Integer end)
                throws IOException {

        return new InteresesReader(
                INTERESES_INPUT_PATH,
                start,
                end);
        }

        @Bean
        @StepScope
        public LegacyInteresesProcessor interesesProcessor() {
                return new LegacyInteresesProcessor();
        }

        @Bean
        public InteresesWriter interesesWriter(JdbcTemplate jdbcTemplate) {
                return new InteresesWriter(jdbcTemplate);
        }

        @Bean
        Step stepLimpiarIntereses(
                JobRepository jobRepository,
                JdbcTemplate jdbcTemplate,
                PlatformTransactionManager transactionManager) {

        return new StepBuilder(
                "stepLimpiarIntereses",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                        jdbcTemplate.update(
                                DELETE_INTERESES_DETALLE_SQL);

                        jdbcTemplate.update(
                                DELETE_INTERESES_SQL);

                        return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
        }

        @Bean
        Step stepIntereses(
                JobRepository jobRepository,
                InteresesReader reader,
                LegacyInteresesProcessor processor,
                InteresesWriter writer,
                BankSkipPolicy bankSkipPolicy,
                InteresesSkipListener interesesSkipListener) {

                return new StepBuilder("stepIntereses", jobRepository)
                        .<LegacyIntereses, ModernIntereses>chunk(5)
                        .reader(reader)
                        .processor(processor)
                        .writer(writer)
                        .faultTolerant()
                        .retry(TransientDataAccessException.class)
                        .retryLimit(2)
                        .skipPolicy(bankSkipPolicy)
                        .listener(interesesSkipListener)
                        .build();
        }

        @Bean
        Step stepConsolidarIntereses(
                JobRepository jobRepository,
                JdbcTemplate jdbcTemplate,
                PlatformTransactionManager transactionManager) {

        return new StepBuilder(
                "stepConsolidarIntereses",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                        jdbcTemplate.update(
                                MERGE_INTERESES_CONSOLIDADOS_SQL);

                        return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
        }

        @Bean
        public TaskExecutorPartitionHandler partitionHandlerIntereses(
                Step stepIntereses,
                TaskExecutor taskExecutor) {

                TaskExecutorPartitionHandler handler =
                        new TaskExecutorPartitionHandler();

                handler.setStep(stepIntereses);
                handler.setTaskExecutor(taskExecutor);
                handler.setGridSize(5);

                return handler;
        }

        @Bean
        public Step partitionStepIntereses(
                JobRepository jobRepository,
                @Qualifier("partitionHandlerIntereses")
                TaskExecutorPartitionHandler partitionHandler) {

        BankPartitioner dynamicPartitioner =
                new BankPartitioner(
                        new ClassPathResource(
                                INTERESES_INPUT_PATH));

        return new StepBuilder(
                "partitionStepIntereses",
                jobRepository)
                .partitioner(
                        "stepIntereses",
                        dynamicPartitioner)
                .partitionHandler(partitionHandler)
                .build();
        }
}