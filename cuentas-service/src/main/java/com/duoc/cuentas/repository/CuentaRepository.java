package com.duoc.cuentas.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.duoc.cuentas.dto.CuentaResponse;

@Repository
public class CuentaRepository {

        private final JdbcTemplate jdbcTemplate;

        public CuentaRepository(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        public List<CuentaResponse> findAll() {

                String sql = """
                        SELECT
                        CUENTA_ID,
                        NOMBRE,
                        SALDO_INICIAL,
                        EDAD,
                        TIPO,
                        TASA_INTERES,
                        INTERES_CALCULADO,
                        SALDO_FINAL,
                        ESTADO
                        FROM INTERESES_PROCESADOS
                        ORDER BY CUENTA_ID
                        """;

                return jdbcTemplate.query(sql, this::mapearCuenta);
        }

        public Optional<CuentaResponse> findById(Integer cuentaId) {

                String sql = """
                        SELECT
                        CUENTA_ID,
                        NOMBRE,
                        SALDO_INICIAL,
                        EDAD,
                        TIPO,
                        TASA_INTERES,
                        INTERES_CALCULADO,
                        SALDO_FINAL,
                        ESTADO
                        FROM INTERESES_PROCESADOS
                        WHERE CUENTA_ID = ?
                        """;

                List<CuentaResponse> resultados = jdbcTemplate.query(
                        sql,
                        this::mapearCuenta,
                        cuentaId
                );

                return resultados.stream().findFirst();
        }

        public int crear(
                Integer cuentaId,
                String nombre,
                BigDecimal saldoInicial,
                Integer edad,
                String tipo,
                BigDecimal tasaInteres) {

                String sql = """
                        INSERT INTO INTERESES_PROCESADOS (
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
                        LINEA_ORIGEN,
                        FECHA_PROCESAMIENTO
                        )
                        VALUES (
                        ?, ?, ?, ?, ?, ?,
                        0,
                        ?,
                        'VALIDA',
                        'APERTURA_MICROSERVICIO',
                        ?,
                        SYSTIMESTAMP
                        )
                        """;

                long lineaOrigen = -cuentaId.longValue();

                return jdbcTemplate.update(
                        sql,
                        cuentaId,
                        nombre,
                        saldoInicial,
                        edad,
                        tipo,
                        tasaInteres,
                        saldoInicial,
                        lineaOrigen
                );
        }

        public int actualizar(
                Integer cuentaId,
                String nombre,
                Integer edad,
                String tipo,
                BigDecimal tasaInteres) {

                String sql = """
                        UPDATE INTERESES_PROCESADOS
                        SET
                        NOMBRE = ?,
                        EDAD = ?,
                        TIPO = ?,
                        TASA_INTERES = ?,
                        DETALLE_VALIDACION = 'MANTENIMIENTO_MICROSERVICIO',
                        FECHA_PROCESAMIENTO = SYSTIMESTAMP
                        WHERE CUENTA_ID = ?
                        AND ESTADO <> 'CERRADA'
                        """;

                return jdbcTemplate.update(
                        sql,
                        nombre,
                        edad,
                        tipo,
                        tasaInteres,
                        cuentaId
                );
        }

        public int cerrar(Integer cuentaId) {

                String sql = """
                        UPDATE INTERESES_PROCESADOS
                        SET
                        ESTADO = 'CERRADA',
                        DETALLE_VALIDACION = 'CIERRE_MICROSERVICIO',
                        FECHA_PROCESAMIENTO = SYSTIMESTAMP
                        WHERE CUENTA_ID = ?
                        AND ESTADO = 'VALIDA'
                        """;

                return jdbcTemplate.update(sql, cuentaId);
        }

        public int retirar(Integer cuentaId, BigDecimal monto) {

                String sql = """
                        UPDATE INTERESES_PROCESADOS
                        SET SALDO_FINAL = SALDO_FINAL - ?
                        WHERE CUENTA_ID = ?
                        AND ESTADO = 'VALIDA'
                        AND LOWER(TIPO) = 'ahorro'
                        AND SALDO_FINAL >= ?
                        """;

                return jdbcTemplate.update(
                        sql,
                        monto,
                        cuentaId,
                        monto
                );
        }

        private CuentaResponse mapearCuenta(
                java.sql.ResultSet rs,
                int rowNum) throws java.sql.SQLException {

                return new CuentaResponse(
                        rs.getInt("CUENTA_ID"),
                        rs.getString("NOMBRE"),
                        rs.getBigDecimal("SALDO_INICIAL"),
                        rs.getInt("EDAD"),
                        rs.getString("TIPO"),
                        rs.getBigDecimal("TASA_INTERES"),
                        rs.getBigDecimal("INTERES_CALCULADO"),
                        rs.getBigDecimal("SALDO_FINAL"),
                        rs.getString("ESTADO")
                );
        }
}