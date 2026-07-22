package com.backend.LeagueOfArrows.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class FieldRepository {

    private final JdbcTemplate jdbc;

    // Campo oficial asociado al torneo (0 o 1 resultado)
    public String findByTournamentAsGeoJson(Long tournamentId) {
        String sql = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'properties', json_build_object(
                            'id_field', f.id_field,
                            'name', f.name
                        ),
                        'geometry', ST_AsGeoJSON(f.boundary)::json
                    )
                ), '[]'::json)
            )::text
            FROM fields f
            JOIN tournaments t ON t.id_field = f.id_field
            WHERE t.id_tournament = ?
            """;
        return jdbc.queryForObject(sql, String.class, tournamentId);
    }

    // Crea (o reemplaza si el torneo ya tenía uno) el campo oficial de un torneo.
    // body: JSON con formato ({idTournament, name, geometry}).
    @Transactional
    public Long save(String body) {
        Long tournamentId = jdbc.queryForObject(
                "SELECT (?::json ->> 'idTournament')::bigint", Long.class, body);

        Long existingFieldId = jdbc.query(
                "SELECT id_field FROM tournaments WHERE id_tournament = ?",
                (rs, rn) -> rs.getLong("id_field"),
                tournamentId
        ).stream().findFirst().orElse(null);

        if (existingFieldId != null && existingFieldId != 0) {
            jdbc.update("""
                WITH input AS (SELECT ?::json AS j)
                UPDATE fields
                SET name = j ->> 'name',
                    boundary = ST_GeomFromGeoJSON(j -> 'geometry')
                FROM input
                WHERE id_field = ?
                """, body, existingFieldId);
            return existingFieldId;
        }

        Long newFieldId = jdbc.queryForObject("""
            WITH input AS (SELECT ?::json AS j)
            INSERT INTO fields (name, boundary)
            SELECT j ->> 'name', ST_GeomFromGeoJSON(j -> 'geometry')
            FROM input
            RETURNING id_field
            """, Long.class, body);
        jdbc.update("UPDATE tournaments SET id_field = ? WHERE id_tournament = ?", newFieldId, tournamentId);
        return newFieldId;
    }
}
