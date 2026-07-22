package com.backend.LeagueOfArrows.controllers;

import com.backend.LeagueOfArrows.repositories.FieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fields")
@RequiredArgsConstructor
public class FieldController {

    private final FieldRepository fieldRepository;

    // GET: campo oficial de un torneo en formato GeoJSON (0 o 1 feature).
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<String> getByTournament(@PathVariable Long tournamentId) {
        String geojson = fieldRepository.findByTournamentAsGeoJson(tournamentId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(geojson);
    }

    // POST: crea o reemplaza el campo oficial de un torneo (solo ADMIN).
    @PostMapping
    public ResponseEntity<?> save(@RequestBody String body) {
        try {
            Long id = fieldRepository.save(body);
            return ResponseEntity.status(201).body(Map.of("id_field", id, "message", "Campo oficial guardado"));
        } catch (DataAccessException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "No se pudo guardar el campo: revisa que envíes idTournament, name y un GeoJSON de polígono válido en 'geometry'"));
        }
    }
}
