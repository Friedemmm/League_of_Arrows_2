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
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // Mismo patrón que el CHECK chk_email_format en la base de datos
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

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

        // Formato de email válido (usuario@dominio.com)
        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()){
            return ResponseEntity.badRequest().body(Map.of("error", "El correo debe tener un formato válido, ej: usuario@dominio.com"));
        }

        // Email debe ser unico
        if (userRepository.findByEmail(request.getEmail()).isPresent()){
            return ResponseEntity.status(409).body(Map.of("error", "El email ya está registrado"));
        }

        // Crear el arquero
        ArcherEntity archer;
        try {
            archer = archerService.save(request);
        } catch (DataAccessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo registrar el usuario. Verifica que los datos sean válidos."));
        }

        // Generar token para dejar al arquero logueado inmediatamente
        String token = jwtService.generateToken(request.getEmail(), "ARQUERO", archer.getUserId());

        return ResponseEntity.status(201).body(new LoginResponse(token, "ARQUERO", archer.getUserId()));
    }

}