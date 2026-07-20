package com.backend.LeagueOfArrows.controllers;

import com.backend.LeagueOfArrows.dtos.EnvironmentalCorrelationDTO;
import com.backend.LeagueOfArrows.repositories.EnvironmentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class EnvironmentalController {

    private final EnvironmentalRepository environmentalRepository;

    // ── Correlación entre condición ambiental (zona del blanco) y precisión ──
    @GetMapping("/{tournamentId}/environmental-correlation")
    public ResponseEntity<List<EnvironmentalCorrelationDTO>> getEnvironmentalCorrelation(
            @PathVariable Long tournamentId) {
        return ResponseEntity.ok(environmentalRepository.getCorrelationByTournament(tournamentId));
    }
}
