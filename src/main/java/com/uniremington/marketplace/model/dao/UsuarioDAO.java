package com.uniremington.marketplace.model.dao;

import com.uniremington.marketplace.model.vo.RolUsuario;
import com.uniremington.marketplace.model.vo.UsuarioVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Repository
public class UsuarioDAO {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<UsuarioVO> rowMapper = (resultSet, rowNumber) -> {
        Timestamp creadoEn = resultSet.getTimestamp("creado_en");
        return new UsuarioVO(
                resultSet.getLong("id"),
                resultSet.getString("nombre"),
                resultSet.getString("correo"),
                resultSet.getString("password"),
                RolUsuario.valueOf(resultSet.getString("rol").toUpperCase(Locale.ROOT)),
                resultSet.getBoolean("activo"),
                creadoEn == null ? null : creadoEn.toLocalDateTime()
        );
    };

    public UsuarioDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long crear(UsuarioVO usuario) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO usuarios (nombre, correo, `password`, rol, activo) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, usuario.nombre());
            statement.setString(2, usuario.correo());
            statement.setString(3, usuario.password());
            statement.setString(4, usuario.rol().name().toLowerCase(Locale.ROOT));
            statement.setBoolean(5, usuario.activo());
            return statement;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey(), "No se generó el id del usuario").longValue();
    }

    public Optional<UsuarioVO> buscarPorId(Long id) {
        return jdbcTemplate.query("SELECT * FROM usuarios WHERE id = ?", rowMapper, id)
                .stream()
                .findFirst();
    }

    public Optional<UsuarioVO> buscarPorCorreo(String correo) {
        return jdbcTemplate.query("SELECT * FROM usuarios WHERE correo = ?", rowMapper, correo)
                .stream()
                .findFirst();
    }

    public List<UsuarioVO> listar() {
        return jdbcTemplate.query("SELECT * FROM usuarios ORDER BY nombre", rowMapper);
    }

    public boolean actualizar(UsuarioVO usuario) {
        return jdbcTemplate.update(
                "UPDATE usuarios SET nombre = ?, correo = ?, `password` = ?, rol = ?, activo = ? WHERE id = ?",
                usuario.nombre(),
                usuario.correo(),
                usuario.password(),
                usuario.rol().name().toLowerCase(Locale.ROOT),
                usuario.activo(),
                usuario.id()
        ) > 0;
    }

    public boolean actualizarEstado(Long id, boolean activo) {
        return jdbcTemplate.update("UPDATE usuarios SET activo = ? WHERE id = ?", activo, id) > 0;
    }

    public boolean actualizarPasswordAdministrador(String correo, String passwordHash) {
        return jdbcTemplate.update(
                "UPDATE usuarios SET `password` = ?, activo = TRUE WHERE correo = ? AND rol = 'administrador'",
                passwordHash,
                correo
        ) > 0;
    }

    public boolean eliminar(Long id) {
        return jdbcTemplate.update("DELETE FROM usuarios WHERE id = ?", id) > 0;
    }
}
