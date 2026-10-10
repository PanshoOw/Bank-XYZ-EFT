package com.duoc.clientes.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.duoc.clientes.model.Cliente;

@Repository
public class ClienteRepository {

    private final JdbcTemplate jdbcTemplate;

    public ClienteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Cliente> rowMapper =
        (rs, rowNum) -> mapRow(rs, rowNum);

    public List<Cliente> findAll() {

        String sql = """
                SELECT
                    CLIENTE_ID,
                    CUENTA_ID,
                    NOMBRE,
                    EDAD,
                    PERFIL,
                    ESTADO,
                    FECHA_CREACION,
                    FECHA_ACTUALIZACION
                FROM CLIENTES
                ORDER BY CLIENTE_ID
                """;

        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Cliente> findById(Long clienteId) {

        String sql = """
                SELECT
                    CLIENTE_ID,
                    CUENTA_ID,
                    NOMBRE,
                    EDAD,
                    PERFIL,
                    ESTADO,
                    FECHA_CREACION,
                    FECHA_ACTUALIZACION
                FROM CLIENTES
                WHERE CLIENTE_ID = ?
                """;

        return jdbcTemplate.query(sql, rowMapper, clienteId)
                .stream()
                .findFirst();
    }

    public Optional<Cliente> findByCuentaId(Integer cuentaId) {

        String sql = """
                SELECT
                    CLIENTE_ID,
                    CUENTA_ID,
                    NOMBRE,
                    EDAD,
                    PERFIL,
                    ESTADO,
                    FECHA_CREACION,
                    FECHA_ACTUALIZACION
                FROM CLIENTES
                WHERE CUENTA_ID = ?
                """;

        return jdbcTemplate.query(sql, rowMapper, cuentaId)
                .stream()
                .findFirst();
    }

    public boolean existsByCuentaId(Integer cuentaId) {

        String sql = """
                SELECT COUNT(*)
                FROM CLIENTES
                WHERE CUENTA_ID = ?
                """;

        Integer total = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                cuentaId
        );

        return total != null && total > 0;
    }

    public void insertar(
            Integer cuentaId,
            String nombre,
            Integer edad,
            String perfil,
            String estado) {

        String sql = """
                INSERT INTO CLIENTES (
                    CUENTA_ID,
                    NOMBRE,
                    EDAD,
                    PERFIL,
                    ESTADO,
                    FECHA_CREACION,
                    FECHA_ACTUALIZACION
                )
                VALUES (?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)
                """;

        jdbcTemplate.update(
                sql,
                cuentaId,
                nombre,
                edad,
                perfil,
                estado
        );
    }

    public int actualizar(
            Long clienteId,
            String nombre,
            Integer edad,
            String perfil,
            String estado) {

        String sql = """
                UPDATE CLIENTES
                SET
                    NOMBRE = ?,
                    EDAD = ?,
                    PERFIL = ?,
                    ESTADO = ?,
                    FECHA_ACTUALIZACION = SYSTIMESTAMP
                WHERE CLIENTE_ID = ?
                """;

        return jdbcTemplate.update(
                sql,
                nombre,
                edad,
                perfil,
                estado,
                clienteId
        );
    }

    private Cliente mapRow(ResultSet rs, int rowNum)
            throws SQLException {

        return new Cliente(
                rs.getLong("CLIENTE_ID"),
                rs.getInt("CUENTA_ID"),
                rs.getString("NOMBRE"),
                rs.getInt("EDAD"),
                rs.getString("PERFIL"),
                rs.getString("ESTADO"),
                rs.getTimestamp("FECHA_CREACION").toLocalDateTime(),
                rs.getTimestamp("FECHA_ACTUALIZACION").toLocalDateTime()
        );
    }
}