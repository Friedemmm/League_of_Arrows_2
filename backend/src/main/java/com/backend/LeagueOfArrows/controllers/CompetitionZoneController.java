package com.backend.LeagueOfArrows.controllers;

import com.backend.LeagueOfArrows.repositories.CompetitionZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/competition-zones")
@RequiredArgsConstructor
public class CompetitionZoneController {

    private final CompetitionZoneRepository competitionZoneRepository;

    // GET: obtener todas las zonas de competencia de un torneo en formato GeoJSON.
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<String> getByTournament(@PathVariable Long tournamentId) {
        String geojson = competitionZoneRepository.findByTournamentAsGeoJson(tournamentId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(geojson);
    }

    // POST: crear una zona de competencia (solo ADMIN).
    @PostMapping
    public ResponseEntity<?> create(@RequestBody String body) {
        try {
            Long id = competitionZoneRepository.save(body);
            return ResponseEntity.status(201).body(Map.of("id_zone", id, "message", "Zona creada"));
        } catch (DataAccessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "No se pudo crear la zona: revisa que envíes idTournament, name y un GeoJSON de polígono válido en 'geometry'"));
        }
    }

    // PUT: editar una zona de competencia (solo ADMIN).
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody String body) {
        try {
            int rows = competitionZoneRepository.update(id, body);
            if (rows == 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Zona no encontrada"));
            }
            return ResponseEntity.ok(Map.of("id_zone", id, "message", "Zona actualizada"));
        } catch (DataAccessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "No se pudo actualizar la zona: revisa que envíes name y un GeoJSON de polígono válido en 'geometry'"));
        }
    }

    // DELETE: eliminar una zona de competencia (solo ADMIN).
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        int rows = competitionZoneRepository.deleteById(id);
        if (rows == 0) {
            return ResponseEntity.status(404).body(Map.of("error", "Zona no encontrada"));
        }
        return ResponseEntity.noContent().build();
    }
}