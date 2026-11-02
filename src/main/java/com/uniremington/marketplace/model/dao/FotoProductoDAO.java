package com.uniremington.marketplace.model.dao;

import com.uniremington.marketplace.model.vo.FotoProductoVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class FotoProductoDAO {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<FotoProductoVO> rowMapper = (resultSet, rowNumber) ->
            new FotoProductoVO(
                    resultSet.getLong("id"),
                    resultSet.getLong("producto_id"),
                    resultSet.getString("url"),
                    resultSet.getBoolean("principal")
            );

    public FotoProductoDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Long crear(FotoProductoVO foto) {
        if (foto.principal()) {
            jdbcTemplate.update(
                    "UPDATE fotos_productos SET principal = FALSE WHERE producto_id = ?",
                    foto.productoId()
            );
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO fotos_productos (producto_id, url, principal) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, foto.productoId());
            statement.setString(2, foto.url());
            statement.setBoolean(3, foto.principal());
            return statement;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey(), "No se generó el id de la foto").longValue();
    }

    public List<FotoProductoVO> listarPorProducto(Long productoId) {
        return jdbcTemplate.query(
                """
                SELECT *
                FROM fotos_productos
                WHERE producto_id = ?
                  AND LOWER(url) NOT LIKE '%placehold.co/%'
                  AND LOWER(url) NOT LIKE '%via.placeholder.com/%'
                ORDER BY principal DESC, id
                """,
                rowMapper,
                productoId
        );
    }

    public Map<Long, String> listarFotosPrincipalesPorTienda(Long tiendaId) {
        return jdbcTemplate.query(
                """
                SELECT p.id AS producto_id,
                       (
                           SELECT fp.url
                           FROM fotos_productos fp
                           WHERE fp.producto_id = p.id
                             AND LOWER(fp.url) NOT LIKE '%placehold.co/%'
                             AND LOWER(fp.url) NOT LIKE '%via.placeholder.com/%'
                           ORDER BY fp.principal DESC, fp.id
                           LIMIT 1
                       ) AS foto_url
                FROM productos p
                WHERE p.tienda_id = ?
                """,
                resultSet -> {
                    Map<Long, String> fotosPorProducto = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        fotosPorProducto.put(
                                resultSet.getLong("producto_id"),
                                resultSet.getString("foto_url")
                        );
                    }
                    return fotosPorProducto;
                },
                tiendaId
        );
    }

    @Transactional
    public boolean marcarComoPrincipal(Long productoId, Long fotoId) {
        jdbcTemplate.update(
                "UPDATE fotos_productos SET principal = FALSE WHERE producto_id = ?",
                productoId
        );
        int filasActualizadas = jdbcTemplate.update(
                "UPDATE fotos_productos SET principal = TRUE WHERE producto_id = ? AND id = ?",
                productoId,
                fotoId
        );
        if (filasActualizadas == 0) {
            throw new IllegalArgumentException("La foto no existe para el producto indicado");
        }
        return true;
    }

    public boolean actualizarUrl(Long id, String url) {
        return jdbcTemplate.update("UPDATE fotos_productos SET url = ? WHERE id = ?", url, id) > 0;
    }

    public boolean eliminar(Long id) {
        return jdbcTemplate.update("DELETE FROM fotos_productos WHERE id = ?", id) > 0;
    }
}
