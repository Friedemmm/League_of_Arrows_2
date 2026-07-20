package com.backend.LeagueOfArrows.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CategoriesRepository {

    private final JdbcTemplate jdbc;

    // listar categorias
    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList("SELECT id_category, name FROM categories ORDER BY name");
    }

    // crear categoria, devuelve el id
    public Long save(String name) {
        return jdbc.queryForObject(
            "INSERT INTO categories (name) VALUES (?) RETURNING id_category",
            Long.class, name);
    }

    // editar categoria, devuelve filas afectadas
    public int update(Long id, String name) {
        return jdbc.update("UPDATE categories SET name = ? WHERE id_category = ?", name, id);
    }

    // borrar categoria, devuelve filas afectadas
    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM categories WHERE id_category = ?", id);
    }
}