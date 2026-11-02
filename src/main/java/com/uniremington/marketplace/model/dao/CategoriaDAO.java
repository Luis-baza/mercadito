package com.uniremington.marketplace.model.dao;

import com.uniremington.marketplace.model.vo.CategoriaVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class CategoriaDAO {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<CategoriaVO> rowMapper = (resultSet, rowNumber) ->
            new CategoriaVO(resultSet.getLong("id"), resultSet.getString("nombre"));

    public CategoriaDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long crear(CategoriaVO categoria) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO categorias (nombre) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, categoria.nombre());
            return statement;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey(), "No se generó el id de la categoría").longValue();
    }

    public Optional<CategoriaVO> buscarPorId(Long id) {
        return jdbcTemplate.query("SELECT * FROM categorias WHERE id = ?", rowMapper, id)
                .stream()
                .findFirst();
    }

    public List<CategoriaVO> listar() {
        return jdbcTemplate.query("SELECT * FROM categorias ORDER BY nombre", rowMapper);
    }

    public boolean actualizar(CategoriaVO categoria) {
        return jdbcTemplate.update(
                "UPDATE categorias SET nombre = ? WHERE id = ?",
                categoria.nombre(),
                categoria.id()
        ) > 0;
    }

    public boolean eliminar(Long id) {
        return jdbcTemplate.update("DELETE FROM categorias WHERE id = ?", id) > 0;
    }
}
