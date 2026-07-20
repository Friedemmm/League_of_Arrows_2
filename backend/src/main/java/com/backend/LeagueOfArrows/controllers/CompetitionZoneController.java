package com.backend.LeagueOfArrows.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/competition-zones")
@RequiredArgsConstructor
public class CompetitionZoneController {

    private final JdbcTemplate jdbc;

    // GET: obtener todas las zonas de competencia de un torneo en formato GeoJSON.
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<String> getByTournament(@PathVariable Long tournamentId) {
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
        String geojson = jdbc.queryForObject(sql, String.class, tournamentId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(geojson);
    }

    // POST: crear una zona de competencia (solo ADMIN).
    @PostMapping
    public ResponseEntity<?> create(@RequestBody String body) {
        try {
            Long id = jdbc.queryForObject("""
                WITH input AS (SELECT ?::json AS j)
                INSERT INTO competition_zones (id_tournament, name, zone_polygon)
                SELECT
                    (j ->> 'idTournament')::bigint,
                    j ->> 'name',
                    ST_GeomFromGeoJSON(j -> 'geometry')
                FROM input
                RETURNING id_zone
                """, Long.class, body);
            return ResponseEntity.status(201).body(Map.of("id_zone", id, "message", "Zona creada"));
        } catch (DataAccessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "No se pudo crear la zona: revisa que envíes idTournament, name y un GeoJSON de polígono válido en 'geometry'"));
        }
    }
}