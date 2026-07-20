package com.backend.LeagueOfArrows.controllers;

import com.backend.LeagueOfArrows.repositories.TargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
public class TargetController {

    private final TargetRepository targetRepository;

    // GET: obtener todos los blancos de un torneo en formato GeoJSON.
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<String> getByTournament(@PathVariable Long tournamentId) {
        String geojson = targetRepository.findByTournamentAsGeoJson(tournamentId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(geojson);
    }

    // POST: crear un blanco (solo ADMIN).
    @PostMapping
    public ResponseEntity<?> create(@RequestBody String body) {
        try {
            Long id = targetRepository.save(body);
            return ResponseEntity.status(201).body(Map.of("id_target", id, "message", "Blanco creado"));
        } catch (DataAccessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "No se pudo crear el blanco: revisa que envíes idTournament, idCategory, label, requiredDistanceM y un GeoJSON de punto válido en 'geometry'"));
        }
    }

    // PUT: editar un blanco (solo ADMIN).
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody String body) {
        try {
            int rows = targetRepository.update(id, body);
            if (rows == 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Blanco no encontrado"));
            }
            return ResponseEntity.ok(Map.of("id_target", id, "message", "Blanco actualizado"));
        } catch (DataAccessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "No se pudo actualizar el blanco: revisa que envíes idCategory, label, requiredDistanceM y un GeoJSON de punto válido en 'geometry'"));
        }
    }

    // DELETE: eliminar un blanco (solo ADMIN).
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        int rows = targetRepository.deleteById(id);
        if (rows == 0) {
            return ResponseEntity.status(404).body(Map.of("error", "Blanco no encontrado"));
        }
        return ResponseEntity.noContent().build();
    }
}
