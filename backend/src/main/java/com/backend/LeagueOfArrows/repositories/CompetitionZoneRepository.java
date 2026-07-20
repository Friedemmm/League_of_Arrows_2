package com.backend.LeagueOfArrows.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CompetitionZoneRepository {

    private final JdbcTemplate jdbc;

    // Zonas de competencia de un torneo
    public String findByTournamentAsGeoJson(Long tournamentId) {
        String sql = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', COALESCE(json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'properties', json_build_object(
                            'id_zone', id_zone,
                            'id_tournament', id_tournament,
                            'name', name
                        ),
                        'geometry', ST_AsGeoJSON(zone_polygon)::json
                    )
                ), '[]'::json)
            )::text
            FROM competition_zones
            WHERE id_tournament = ?
            """;
        return jdbc.queryForObject(sql, String.class, tournamentId);
    }

    // Crea una zona a partir de un body JSON con formato({idTournament, name, geometry}).
    public Long save(String body) {
        return jdbc.queryForObject("""
            WITH input AS (SELECT ?::json AS j)
            INSERT INTO competition_zones (id_tournament, name, zone_polygon)
            SELECT
                (j ->> 'idTournament')::bigint,
                j ->> 'name',
                ST_GeomFromGeoJSON(j -> 'geometry')
            FROM input
            RETURNING id_zone
            """, Long.class, body);
    }

    // Actualiza name y polígono de una zona ya existente
    public int update(Long id, String body) {
        return jdbc.update("""
            WITH input AS (SELECT ?::json AS j)
            UPDATE competition_zones
            SET name = j ->> 'name',
                zone_polygon = ST_GeomFromGeoJSON(j -> 'geometry')
            FROM input
            WHERE id_zone = ?
            """, body, id);
    }

    // Elimina una zona por id.
    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM competition_zones WHERE id_zone = ?", id);
    }
}