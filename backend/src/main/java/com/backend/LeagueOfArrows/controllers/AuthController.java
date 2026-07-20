package com.backend.LeagueOfArrows.controllers;

import com.backend.LeagueOfArrows.dtos.ArcherDTO;
import com.backend.LeagueOfArrows.dtos.LoginRequest;
import com.backend.LeagueOfArrows.dtos.LoginResponse;
import com.backend.LeagueOfArrows.entities.ArcherEntity;
import com.backend.LeagueOfArrows.entities.UserEntity;
import com.backend.LeagueOfArrows.repositories.UserRepository;
import com.backend.LeagueOfArrows.services.ArcherService;
import com.backend.LeagueOfArrows.services.JwtService;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final ArcherService archerService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request){

        // Buscar el usuario por email
        UserEntity user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // Verificar si el usuario existe y si la contraseña es correcta
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())){
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales incorrectas"));
        }

        // Generar el token si logra iniciar sesion
        String token = jwtService.generateToken(user.getEmail(), user.getRol(), user.getUserId());

        return ResponseEntity.ok(new LoginResponse(token, user.getRol(), user.getUserId()));
    }

    // Registro publico de arqueros: cualquiera puede crearse una cuenta de ARQUERO
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody ArcherDTO request){

        // Validaciones de campos obligatorios
        if (request.getName() == null || request.getName().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()){
            return ResponseEntity.badRequest().body(Map.of("error", "Nombre, email y contraseña son obligatorios"));
        }

        // Email debe ser unico
        if (userRepository.findByEmail(request.getEmail()).isPresent()){
            return ResponseEntity.status(409).body(Map.of("error", "El email ya está registrado"));
        }

        // Crear el arquero (rol ARQUERO forzado dentro del servicio)
        ArcherEntity archer = archerService.save(request);

        // Generar token para dejar al arquero logueado inmediatamente
        String token = jwtService.generateToken(request.getEmail(), "ARQUERO", archer.getUserId());

        return ResponseEntity.status(201).body(new LoginResponse(token, "ARQUERO", archer.getUserId()));
    }

}