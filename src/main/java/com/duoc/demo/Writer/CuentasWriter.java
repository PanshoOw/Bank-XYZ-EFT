package com.duoc.demo.Writer;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import com.duoc.demo.Model.ModernCuentas;

public class CuentasWriter implements ItemWriter<ModernCuentas> {

    private static final String MERGE_MOVIMIENTO_SQL = """
            MERGE INTO MOVIMIENTOS_ANUALES_PROCESADOS destino
            USING (
                SELECT
                    ? AS LINEA_ORIGEN,
                    ? AS CUENTA_ID,
                    ? AS FECHA,
                    ? AS TRANSACCION,
                    ? AS MONTO,
                    ? AS DESCRIPCION,
                    ? AS ESTADO,
                    ? AS DETALLE_VALIDACION
                FROM DUAL
            ) origen

            ON (
                destino.LINEA_ORIGEN =
                    origen.LINEA_ORIGEN
            )

            WHEN MATCHED THEN
                UPDATE SET
                    destino.CUENTA_ID =
                        origen.CUENTA_ID,
                    destino.FECHA =
                        origen.FECHA,
                    destino.TRANSACCION =
                        origen.TRANSACCION,
                    destino.MONTO =
                        origen.MONTO,
                    destino.DESCRIPCION =
                        origen.DESCRIPCION,
                    destino.ESTADO =
                        origen.ESTADO,
                    destino.DETALLE_VALIDACION =
                        origen.DETALLE_VALIDACION,
                    destino.FECHA_PROCESAMIENTO =
                        SYSTIMESTAMP

            WHEN NOT MATCHED THEN
                INSERT (
                    LINEA_ORIGEN,
                    CUENTA_ID,
                    FECHA,
                    TRANSACCION,
                    MONTO,
                    DESCRIPCION,
                    ESTADO,
                    DETALLE_VALIDACION
                )
                VALUES (
                    origen.LINEA_ORIGEN,
                    origen.CUENTA_ID,
                    origen.FECHA,
                    origen.TRANSACCION,
                    origen.MONTO,
                    origen.DESCRIPCION,
                    origen.ESTADO,
                    origen.DETALLE_VALIDACION
                )
            """;

    private final JdbcTemplate jdbcTemplate;

    public CuentasWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(
            Chunk<? extends ModernCuentas> items)
            throws Exception {

        for (ModernCuentas item : items) {

            jdbcTemplate.update(
                    MERGE_MOVIMIENTO_SQL,
                    item.lineaOrigen(),
                    item.cuentaId(),
                    item.fecha(),
                    item.transaccion(),
                    item.monto(),
                    item.descripcion(),
                    item.estado(),
                    item.detalleValidacion()
            );
        }
    }
}