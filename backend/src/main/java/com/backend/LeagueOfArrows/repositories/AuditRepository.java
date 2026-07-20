package com.backend.LeagueOfArrows.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AuditRepository {

    private final JdbcTemplate jdbc;

    // Historial completo de modificaciones de puntajes
    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList("""
            SELECT
                al.id_audit        AS "idAudit",
                al.id_archer       AS "idArcher",
                ar.name            AS "archerName",
                al.id_tournament   AS "idTournament",
                t.name             AS "tournamentName",
                al.old_score       AS "oldScore",
                al.new_score       AS "newScore",
                al.modified_by     AS "modifiedBy",
                al.modified_at     AS "modifiedAt"
            FROM audit_log al
            LEFT JOIN archers     ar ON ar.id_archer     = al.id_archer
            LEFT JOIN tournaments t  ON t.id_tournament  = al.id_tournament
            ORDER BY al.id_audit DESC
            """);
    }
}
