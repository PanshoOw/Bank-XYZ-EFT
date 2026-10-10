package com.duoc.pagos.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.duoc.pagos.model.CuentaSaldo;

@Repository
public class CuentaSaldoRepository {

    private final JdbcTemplate jdbcTemplate;

    public CuentaSaldoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CuentaSaldo> findById(Integer cuentaId) {

        String sql = """
                SELECT
                    CUENTA_ID,
                    SALDO_FINAL,
                    ESTADO,
                    TIPO
                FROM INTERESES_PROCESADOS
                WHERE CUENTA_ID = ?
                """;

        List<CuentaSaldo> resultados = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new CuentaSaldo(
                        rs.getInt("CUENTA_ID"),
                        rs.getBigDecimal("SALDO_FINAL"),
                        rs.getString("ESTADO"),
                        rs.getString("TIPO")
                ),
                cuentaId
        );

        return resultados.stream().findFirst();
    }

    public int debitar(Integer cuentaId, BigDecimal monto) {

        String sql = """
                UPDATE INTERESES_PROCESADOS
                SET SALDO_FINAL = SALDO_FINAL - ?
                WHERE CUENTA_ID = ?
                AND ESTADO = 'VALIDA'
                AND SALDO_FINAL >= ?
                """;

        return jdbcTemplate.update(
                sql,
                monto,
                cuentaId,
                monto
        );
    }

    public int acreditar(Integer cuentaId, BigDecimal monto) {

        String sql = """
                UPDATE INTERESES_PROCESADOS
                SET SALDO_FINAL = SALDO_FINAL + ?
                WHERE CUENTA_ID = ?
                AND ESTADO = 'VALIDA'
                """;

        return jdbcTemplate.update(
                sql,
                monto,
                cuentaId
        );
    }
}