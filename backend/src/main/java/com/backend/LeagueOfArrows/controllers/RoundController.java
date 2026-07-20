package com.backend.LeagueOfArrows.controllers;

import com.backend.LeagueOfArrows.dtos.ArcherPositionDTO;
import com.backend.LeagueOfArrows.dtos.DistanceCheckDTO;
import com.backend.LeagueOfArrows.dtos.RoundDetailDTO;
import com.backend.LeagueOfArrows.dtos.RoundRequestDTO;
import com.backend.LeagueOfArrows.repositories.RoundRepository;
import com.backend.LeagueOfArrows.services.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/rounds")
@RequiredArgsConstructor
public class RoundController {

    private final RoundRepository roundRepository;
    private final JwtService      jwtService;

    // Register a new round 
    @PostMapping
    public ResponseEntity<?> registerRound(
            @RequestBody RoundRequestDTO dto,
            HttpServletRequest request) {

        Long adminUserId = extractUserId(request);
        roundRepository.registerRound(dto, adminUserId);
        return ResponseEntity.ok(Map.of("message", "Ronda registrada exitosamente"));
    }

    // ── Get all rounds (with arrows) for an archer in a tournament ───
    @GetMapping
    public ResponseEntity<List<RoundDetailDTO>> getRounds(
            @RequestParam Long tournamentId,
            @RequestParam Long archerId) {

        return ResponseEntity.ok(roundRepository.getRoundsByTournamentAndArcher(tournamentId, archerId));
    }

    // Update a single arrow score 
    @PutMapping("/{roundId}/arrows/{arrowId}")
    public ResponseEntity<?> updateArrow(
            @PathVariable Long roundId,
            @PathVariable Long arrowId,
            @RequestBody Map<String, Integer> body,
            HttpServletRequest request) {

        Integer newScore = body.get("score");
        if (newScore == null || newScore < 0 || newScore > 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El puntaje debe estar entre 0 y 10."));
        }

        Long adminUserId = extractUserId(request);
        roundRepository.updateArrowScore(roundId, arrowId, newScore, adminUserId);
        return ResponseEntity.ok(Map.of("message", "Puntaje actualizado correctamente"));
    }

    // Delete a full round (its arrows cascade automatically) 
    @DeleteMapping("/{roundId}")
    public ResponseEntity<?> deleteRound(@PathVariable Long roundId) {
        try {
            int rows = roundRepository.deleteRound(roundId);
            if (rows == 0) throw new NoSuchElementException("Ronda no encontrada");
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }


    // Delete a single arrow 
    @DeleteMapping("/{roundId}/arrows/{arrowId}")
    public ResponseEntity<?> deleteArrow(@PathVariable Long roundId, @PathVariable Long arrowId) {
        try {
            int rows = roundRepository.deleteArrow(roundId, arrowId);
            if (rows == 0) throw new NoSuchElementException("Flecha no encontrada");
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Register/update archer GPS position for a round ───────────────
    //    Runs the PostGIS triggers (zona de competencia, geocerca, distancia
    //    de seguridad) and returns the real-vs-normative distance check.
    @PutMapping("/{roundId}/position")
    public ResponseEntity<?> updatePosition(
            @PathVariable Long roundId,
            @RequestBody ArcherPositionDTO dto,
            HttpServletRequest request) {

        if (dto.getLon() == null || dto.getLat() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Se requieren las coordenadas 'lon' y 'lat'."));
        }

        Long adminUserId = extractUserId(request);
        try {
            int rows = roundRepository.updateArcherPosition(
                    roundId, dto.getLon(), dto.getLat(), dto.getTargetId(), adminUserId);
            if (rows == 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Ronda no encontrada"));
            }
        } catch (DataAccessException e) {
            Throwable cause = e.getMostSpecificCause();
            return ResponseEntity.badRequest().body(Map.of("error", cause.getMessage()));
        }

        DistanceCheckDTO check = null;
        try {
            check = roundRepository.checkDistanceNormativa(roundId).orElse(null);
        } catch (DataAccessException ignored) {
            // La ronda todavía no tiene blanco asignado: no hay distancia que verificar.
        }
        return ResponseEntity.ok(Map.of(
                "message", "Posición registrada correctamente",
                "distanceCheck", check == null ? Map.of() : check
        ));
    }

    // Helper
    private Long extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try { return jwtService.extractUserId(authHeader.substring(7)); }
            catch (Exception ignored) {}
        }
        return null;
    }
}
