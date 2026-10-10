package com.duoc.pagos.repository;

import java.sql.Timestamp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.duoc.pagos.model.OperacionPago;

@Repository
public class OperacionPagoRepository {

    private final JdbcTemplate jdbcTemplate;

    public OperacionPagoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int guardar(OperacionPago operacion) {

        String sql = """
                INSERT INTO OPERACIONES_PAGOS (
                    OPERACION_ID,
                    TIPO,
                    CUENTA_ORIGEN_ID,
                    CUENTA_DESTINO_ID,
                    MONTO,
                    ESTADO,
                    DESCRIPCION,
                    FECHA_HORA
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        return jdbcTemplate.update(
                sql,
                operacion.operacionId(),
                operacion.tipo(),
                operacion.cuentaOrigenId(),
                operacion.cuentaDestinoId(),
                operacion.monto(),
                operacion.estado(),
                operacion.descripcion(),
                Timestamp.valueOf(operacion.fechaHora())
        );
    }
}