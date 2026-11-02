package com.uniremington.marketplace.model.dao;

import com.uniremington.marketplace.model.vo.DisponibilidadProducto;
import com.uniremington.marketplace.model.vo.EstadoProducto;
import com.uniremington.marketplace.model.vo.ProductoVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Repository
public class ProductoDAO {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ProductoVO> rowMapper = (resultSet, rowNumber) -> {
        Timestamp creadoEn = resultSet.getTimestamp("creado_en");
        Timestamp actualizadoEn = resultSet.getTimestamp("actualizado_en");
        return new ProductoVO(
                resultSet.getLong("id"),
                resultSet.getLong("tienda_id"),
                resultSet.getObject("categoria_id", Long.class),
                resultSet.getString("nombre"),
                resultSet.getString("descripcion"),
                resultSet.getBigDecimal("precio"),
                DisponibilidadProducto.valueOf(
                        resultSet.getString("disponibilidad").toUpperCase(Locale.ROOT)
                ),
                EstadoProducto.valueOf(resultSet.getString("estado").toUpperCase(Locale.ROOT)),
                creadoEn == null ? null : creadoEn.toLocalDateTime(),
                actualizadoEn == null ? null : actualizadoEn.toLocalDateTime()
        );
    };

    public ProductoDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long crear(ProductoVO producto) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO productos
                        (tienda_id, categoria_id, nombre, descripcion, precio, disponibilidad, estado)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, producto.tiendaId());
            if (producto.categoriaId() == null) {
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(2, producto.categoriaId());
            }
            statement.setString(3, producto.nombre());
            statement.setString(4, producto.descripcion());
            statement.setBigDecimal(5, producto.precio());
            statement.setString(6, disponibilidadParaBaseDeDatos(producto.disponibilidad()));
            statement.setString(7, estadoParaBaseDeDatos(producto.estado()));
            return statement;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey(), "No se generó el id del producto").longValue();
    }

    public Optional<ProductoVO> buscarPorId(Long id) {
        return jdbcTemplate.query("SELECT * FROM productos WHERE id = ?", rowMapper, id)
                .stream()
                .findFirst();
    }

    public List<ProductoVO> listarCatalogo(
            String texto,
            Long categoriaId,
            Long tiendaId,
            java.math.BigDecimal precioMinimo,
            java.math.BigDecimal precioMaximo,
            boolean ordenarPorPrecioAscendente
    ) {
        List<Object> parametros = new ArrayList<>();
        StringBuilder sql = crearConsultaCatalogo("p.*", texto, categoriaId, tiendaId,
                precioMinimo, precioMaximo, ordenarPorPrecioAscendente, parametros);
        return jdbcTemplate.query(sql.toString(), rowMapper, parametros.toArray());
    }

    public List<com.uniremington.marketplace.model.vo.ProductoCatalogoVO> listarCatalogoConFotos(
            String texto,
            Long categoriaId,
            Long tiendaId,
            java.math.BigDecimal precioMinimo,
            java.math.BigDecimal precioMaximo,
            boolean ordenarPorPrecioAscendente
    ) {
        List<Object> parametros = new ArrayList<>();
        String select = """
                p.*,
                (
                    SELECT fp.url
                    FROM fotos_productos fp
                    WHERE fp.producto_id = p.id
                      AND LOWER(fp.url) NOT LIKE '%placehold.co/%'
                      AND LOWER(fp.url) NOT LIKE '%via.placeholder.com/%'
                    ORDER BY fp.principal DESC, fp.id
                    LIMIT 1
                ) AS foto_url
                """;
        StringBuilder sql = crearConsultaCatalogo(
                select,
                texto,
                categoriaId,
                tiendaId,
                precioMinimo,
                precioMaximo,
                ordenarPorPrecioAscendente,
                parametros
        );
        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) ->
                new com.uniremington.marketplace.model.vo.ProductoCatalogoVO(
                        rowMapper.mapRow(resultSet, rowNumber),
                        resultSet.getString("foto_url")
                ), parametros.toArray());
    }

    private StringBuilder crearConsultaCatalogo(
            String columnas,
            String texto,
            Long categoriaId,
            Long tiendaId,
            java.math.BigDecimal precioMinimo,
            java.math.BigDecimal precioMaximo,
            boolean ordenarPorPrecioAscendente,
            List<Object> parametros
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT %s
                FROM productos p
                JOIN tiendas t ON t.id = p.tienda_id
                WHERE p.estado = 'publicado' AND t.estado = 'aprobada'
                """.formatted(columnas));

        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (p.nombre LIKE ? OR p.descripcion LIKE ?)");
            String patron = "%" + texto.trim() + "%";
            parametros.add(patron);
            parametros.add(patron);
        }
        if (categoriaId != null) {
            sql.append(" AND p.categoria_id = ?");
            parametros.add(categoriaId);
        }
        if (tiendaId != null) {
            sql.append(" AND p.tienda_id = ?");
            parametros.add(tiendaId);
        }
        if (precioMinimo != null) {
            sql.append(" AND p.precio >= ?");
            parametros.add(precioMinimo);
        }
        if (precioMaximo != null) {
            sql.append(" AND p.precio <= ?");
            parametros.add(precioMaximo);
        }

        sql.append(ordenarPorPrecioAscendente ? " ORDER BY p.precio ASC" : " ORDER BY p.creado_en DESC");
        return sql;
    }

    public List<ProductoVO> listarPorTienda(Long tiendaId) {
        return jdbcTemplate.query(
                "SELECT * FROM productos WHERE tienda_id = ? ORDER BY creado_en DESC",
                rowMapper,
                tiendaId
        );
    }

    public List<ProductoVO> listarPendientesModeracion() {
        return jdbcTemplate.query(
                "SELECT * FROM productos WHERE estado = 'pendiente' ORDER BY creado_en",
                rowMapper
        );
    }

    public boolean actualizar(ProductoVO producto) {
        return jdbcTemplate.update(
                """
                UPDATE productos
                SET tienda_id = ?, categoria_id = ?, nombre = ?, descripcion = ?, precio = ?,
                    disponibilidad = ?, estado = ?
                WHERE id = ?
                """,
                producto.tiendaId(),
                producto.categoriaId(),
                producto.nombre(),
                producto.descripcion(),
                producto.precio(),
                disponibilidadParaBaseDeDatos(producto.disponibilidad()),
                estadoParaBaseDeDatos(producto.estado()),
                producto.id()
        ) > 0;
    }

    public boolean actualizarDisponibilidad(Long id, DisponibilidadProducto disponibilidad) {
        return jdbcTemplate.update(
                "UPDATE productos SET disponibilidad = ? WHERE id = ?",
                disponibilidadParaBaseDeDatos(disponibilidad),
                id
        ) > 0;
    }

    public boolean actualizarEstado(Long id, EstadoProducto estado) {
        return jdbcTemplate.update(
                "UPDATE productos SET estado = ? WHERE id = ?",
                estadoParaBaseDeDatos(estado),
                id
        ) > 0;
    }

    public boolean eliminar(Long id) {
        return jdbcTemplate.update("DELETE FROM productos WHERE id = ?", id) > 0;
    }

    private String disponibilidadParaBaseDeDatos(DisponibilidadProducto disponibilidad) {
        DisponibilidadProducto valor = disponibilidad == null
                ? DisponibilidadProducto.DISPONIBLE
                : disponibilidad;
        return valor.name().toLowerCase(Locale.ROOT);
    }

    private String estadoParaBaseDeDatos(EstadoProducto estado) {
        EstadoProducto valor = estado == null ? EstadoProducto.PENDIENTE : estado;
        return valor.name().toLowerCase(Locale.ROOT);
    }
}
