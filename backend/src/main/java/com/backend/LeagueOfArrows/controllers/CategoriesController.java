package com.backend.LeagueOfArrows.controllers;

import com.backend.LeagueOfArrows.repositories.CategoriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoriesController {

    private final CategoriesRepository categoriesRepository;

    // listar categorias (publico)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        return ResponseEntity.ok(categoriesRepository.findAll());
    }

    // Crear categoria (admin)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio"));
        }
        try {
            Long id = categoriesRepository.save(name.trim());
            return ResponseEntity.status(201).body(Map.of("id_category", id, "name", name.trim()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(Map.of("error", "Ya existe una categoría con ese nombre"));
        }
    }

    // Editar categoria (admin)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio"));
        }
        try {
            int rows = categoriesRepository.update(id, name.trim());
            if (rows == 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Categoría no encontrada"));
            }
            return ResponseEntity.ok(Map.of("id_category", id, "name", name.trim()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(Map.of("error", "Ya existe una categoría con ese nombre"));
        }
    }

    // Borrar categoria (admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            int rows = categoriesRepository.deleteById(id);
            if (rows == 0) {
                return ResponseEntity.status(404).body(Map.of("error", "Categoría no encontrada"));
            }
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(Map.of("error", "No se puede borrar: la categoría está en uso por torneos o inscripciones"));
        }
    }
}