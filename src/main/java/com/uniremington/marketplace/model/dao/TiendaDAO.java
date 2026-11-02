package com.uniremington.marketplace.model.dao;

import com.uniremington.marketplace.model.vo.EstadoTienda;
import com.uniremington.marketplace.model.vo.TiendaVO;
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
public class TiendaDAO {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TiendaVO> rowMapper = (resultSet, rowNumber) -> {
        Timestamp creadaEn = resultSet.getTimestamp("creada_en");
        return new TiendaVO(
                resultSet.getLong("id"),
                resultSet.getLong("usuario_id"),
                resultSet.getString("nombre"),
                resultSet.getString("descripcion"),
                resultSet.getString("telefono"),
                resultSet.getString("whatsapp"),
                resultSet.getString("correo_contacto"),
                EstadoTienda.valueOf(resultSet.getString("estado").toUpperCase(Locale.ROOT)),
                creadaEn == null ? null : creadaEn.toLocalDateTime()
        );
    };

    public TiendaDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long crear(TiendaVO tienda) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO tiendas
                        (usuario_id, nombre, descripcion, telefono, whatsapp, correo_contacto, estado)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, tienda.usuarioId());
            statement.setString(2, tienda.nombre());
            statement.setString(3, tienda.descripcion());
            statement.setString(4, tienda.telefono());
            statement.setString(5, tienda.whatsapp());
            statement.setString(6, tienda.correoContacto());
            statement.setString(7, estadoParaBaseDeDatos(tienda.estado()));
            return statement;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey(), "No se generó el id de la tienda").longValue();
    }

    public Optional<TiendaVO> buscarPorId(Long id) {
        return jdbcTemplate.query("SELECT * FROM tiendas WHERE id = ?", rowMapper, id)
                .stream()
                .findFirst();
    }

    public Optional<TiendaVO> buscarPorUsuarioId(Long usuarioId) {
        return jdbcTemplate.query("SELECT * FROM tiendas WHERE usuario_id = ?", rowMapper, usuarioId)
                .stream()
                .findFirst();
    }

    public List<TiendaVO> listarPorEstado(EstadoTienda estado) {
        return jdbcTemplate.query(
                "SELECT * FROM tiendas WHERE estado = ? ORDER BY creada_en",
                rowMapper,
                estadoParaBaseDeDatos(estado)
        );
    }

    public List<TiendaVO> listarAprobadas() {
        return listarPorEstado(EstadoTienda.APROBADA);
    }

    public boolean actualizar(TiendaVO tienda) {
        return jdbcTemplate.update(
                """
                UPDATE tiendas
                SET nombre = ?, descripcion = ?, telefono = ?, whatsapp = ?,
                    correo_contacto = ?, estado = ?
                WHERE id = ?
                """,
                tienda.nombre(),
                tienda.descripcion(),
                tienda.telefono(),
                tienda.whatsapp(),
                tienda.correoContacto(),
                estadoParaBaseDeDatos(tienda.estado()),
                tienda.id()
        ) > 0;
    }

    public boolean actualizarEstado(Long id, EstadoTienda estado) {
        return jdbcTemplate.update(
                "UPDATE tiendas SET estado = ? WHERE id = ?",
                estadoParaBaseDeDatos(estado),
                id
        ) > 0;
    }

    public boolean eliminar(Long id) {
        return jdbcTemplate.update("DELETE FROM tiendas WHERE id = ?", id) > 0;
    }

    private String estadoParaBaseDeDatos(EstadoTienda estado) {
        EstadoTienda valor = estado == null ? EstadoTienda.PENDIENTE : estado;
        return valor.name().toLowerCase(Locale.ROOT);
    }
}
