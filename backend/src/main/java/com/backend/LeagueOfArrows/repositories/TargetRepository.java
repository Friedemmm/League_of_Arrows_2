package com.backend.LeagueOfArrows.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TargetRepository {

    private final JdbcTemplate jdbc;

    // Blancos de un torneo
    public String findByTournamentAsGeoJson(Long tournamentId) {
        String sql = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'properties', json_build_object(
                            'id_target', id_target,
                            'id_tournament', id_tournament,
                            'id_category', id_category,
                            'label', label,
                            'required_distance_m', required_distance_m
                        ),
                        'geometry', ST_AsGeoJSON(location)::json
                    )
                ), '[]'::json)
            )::text
            FROM targets
            WHERE id_tournament = ?
            """;
        return jdbc.queryForObject(sql, String.class, tournamentId);
    }

    // Crea un blanco a partir de un body JSON con formato ({idTournament, idCategory, label, requiredDistanceM, geometry}).
    public Long save(String body) {
        return jdbc.queryForObject("""
            WITH input AS (SELECT ?::json AS j)
            INSERT INTO targets (id_tournament, id_category, label, location, required_distance_m)
            SELECT
                (j ->> 'idTournament')::bigint,
                (j ->> 'idCategory')::bigint,
                j ->> 'label',
                ST_GeomFromGeoJSON(j -> 'geometry'),
                (j ->> 'requiredDistanceM')::numeric
            FROM input
            RETURNING id_target
            """, Long.class, body);
    }

    // Actualiza categoría, label, ubicación y distancia normativa de un blanco ya existente
    public int update(Long id, String body) {
        return jdbc.update("""
            WITH input AS (SELECT ?::json AS j)
            UPDATE targets
            SET id_category = (j ->> 'idCategory')::bigint,
                label = j ->> 'label',
                location = ST_GeomFromGeoJSON(j -> 'geometry'),
                required_distance_m = (j ->> 'requiredDistanceM')::numeric
            FROM input
            WHERE id_target = ?
            """, body, id);
    }

    // Elimina un blanco por id.
    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM targets WHERE id_target = ?", id);
    }
}
