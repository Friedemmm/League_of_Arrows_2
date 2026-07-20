package com.backend.LeagueOfArrows.repositories;

import com.backend.LeagueOfArrows.dtos.EnvironmentalCorrelationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class EnvironmentalRepository {

    private final JdbcTemplate jdbc;

    // ── Correlación entre condición ambiental de la zona del blanco y
    //    la precisión de tiro (promedio de puntaje por flecha por ronda)
    public List<EnvironmentalCorrelationDTO> getCorrelationByTournament(Long tournamentId) {
        String breakdownSql = """
            WITH precision_por_ronda AS (
                SELECT ez.zone_type, ez.intensity, r.id_round, AVG(a.score) AS precision_promedio
                FROM rounds r
                JOIN targets tg            ON tg.id_target = r.id_target
                JOIN arrows a               ON a.id_round = r.id_round
                JOIN environmental_zones ez ON ez.id_tournament = r.id_tournament
                                            AND ST_Contains(ez.zone_polygon, tg.location)
                WHERE r.id_tournament = ?
                GROUP BY ez.zone_type, ez.intensity, r.id_round
            )
            SELECT zone_type, intensity,
                   COUNT(*)                              AS rondas_evaluadas,
                   ROUND(AVG(precision_promedio)::numeric, 3) AS promedio_precision
            FROM precision_por_ronda
            GROUP BY zone_type, intensity
            ORDER BY zone_type, intensity
        """;

        String correlationSql = """
            WITH precision_por_ronda AS (
                SELECT ez.zone_type, ez.intensity, r.id_round, AVG(a.score) AS precision_promedio
                FROM rounds r
                JOIN targets tg            ON tg.id_target = r.id_target
                JOIN arrows a               ON a.id_round = r.id_round
                JOIN environmental_zones ez ON ez.id_tournament = r.id_tournament
                                            AND ST_Contains(ez.zone_polygon, tg.location)
                WHERE r.id_tournament = ?
                GROUP BY ez.zone_type, ez.intensity, r.id_round
            )
            SELECT zone_type,
                   corr(
                       CASE intensity WHEN 'LEVE' THEN 1 WHEN 'MODERADO' THEN 2 WHEN 'FUERTE' THEN 3 END,
                       precision_promedio
                   ) AS coeficiente_correlacion
            FROM precision_por_ronda
            GROUP BY zone_type
        """;

        Map<String, Double> correlationByZone = new HashMap<>();
        jdbc.query(correlationSql, rs -> {
            double corr = rs.getDouble("coeficiente_correlacion");
            correlationByZone.put(rs.getString("zone_type"), rs.wasNull() ? null : corr);
        }, tournamentId);

        return jdbc.query(breakdownSql, (rs, rn) -> {
            String zoneType = rs.getString("zone_type");
            return new EnvironmentalCorrelationDTO(
                    zoneType,
                    rs.getString("intensity"),
                    rs.getLong("rondas_evaluadas"),
                    rs.getBigDecimal("promedio_precision"),
                    correlationByZone.get(zoneType)
            );
        }, tournamentId);
    }
}
