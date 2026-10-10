package com.duoc.demo.Writer;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import com.duoc.demo.Model.ModernIntereses;

public class InteresesWriter implements ItemWriter<ModernIntereses> {

    private static final String MERGE_DETALLE_SQL = """
            MERGE INTO INTERESES_DETALLE_PROCESADOS destino
            USING (
                SELECT
                    ? AS LINEA_ORIGEN,
                    ? AS CUENTA_ID,
                    ? AS NOMBRE,
                    ? AS SALDO_INICIAL,
                    ? AS EDAD,
                    ? AS TIPO,
                    ? AS TASA_INTERES,
                    ? AS INTERES_CALCULADO,
                    ? AS SALDO_FINAL,
                    ? AS ESTADO,
                    ? AS DETALLE_VALIDACION
                FROM DUAL
            ) origen

            ON (destino.LINEA_ORIGEN = origen.LINEA_ORIGEN)

            WHEN MATCHED THEN
                UPDATE SET
                    destino.CUENTA_ID = origen.CUENTA_ID,
                    destino.NOMBRE = origen.NOMBRE,
                    destino.SALDO_INICIAL = origen.SALDO_INICIAL,
                    destino.EDAD = origen.EDAD,
                    destino.TIPO = origen.TIPO,
                    destino.TASA_INTERES = origen.TASA_INTERES,
                    destino.INTERES_CALCULADO = origen.INTERES_CALCULADO,
                    destino.SALDO_FINAL = origen.SALDO_FINAL,
                    destino.ESTADO = origen.ESTADO,
                    destino.DETALLE_VALIDACION =
                        origen.DETALLE_VALIDACION,
                    destino.FECHA_PROCESAMIENTO =
                        SYSTIMESTAMP

            WHEN NOT MATCHED THEN
                INSERT (
                    LINEA_ORIGEN,
                    CUENTA_ID,
                    NOMBRE,
                    SALDO_INICIAL,
                    EDAD,
                    TIPO,
                    TASA_INTERES,
                    INTERES_CALCULADO,
                    SALDO_FINAL,
                    ESTADO,
                    DETALLE_VALIDACION
                )
                VALUES (
                    origen.LINEA_ORIGEN,
                    origen.CUENTA_ID,
                    origen.NOMBRE,
                    origen.SALDO_INICIAL,
                    origen.EDAD,
                    origen.TIPO,
                    origen.TASA_INTERES,
                    origen.INTERES_CALCULADO,
                    origen.SALDO_FINAL,
                    origen.ESTADO,
                    origen.DETALLE_VALIDACION
                )
            """;

    private final JdbcTemplate jdbcTemplate;

    public InteresesWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(
            Chunk<? extends ModernIntereses> items)
            throws Exception {

        for (ModernIntereses item : items) {

            jdbcTemplate.update(
                    MERGE_DETALLE_SQL,
                    item.lineaOrigen(),
                    item.cuentaId(),
                    item.nombre(),
                    item.saldoInicial(),
                    item.edad(),
                    item.tipo(),
                    item.tasaInteres(),
                    item.interesCalculado(),
                    item.saldoFinal(),
                    item.estado(),
                    item.detalleValidacion()
            );
        }
    }
}