package com.clearpet.controller;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clearpet.dto.AuthResponse;
import com.clearpet.dto.LoginRequest;
import com.clearpet.dto.RegisterRequest;
import com.clearpet.dto.ResetPasswordRequest;
import com.clearpet.entity.Usuario;
import com.clearpet.service.AuthService;
import com.clearpet.service.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://clearpet-iot-production.up.railway.app" })
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    /**
     * Endpoint de éxito para OAuth2
     * Devuelve un JSON con el token para que la App móvil lo capture fácilmente.
     */
    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> authSuccess(@RequestParam(value = "token", required = false) String token) {
        log.info("[AUTH_CONTROLLER] Procesando endpoint de éxito post-login");
        Map<String, Object> response = new HashMap<>();
        
        if (token == null || token.isEmpty()) {
            log.error("[AUTH_CONTROLLER] Error: El token no fue recibido en la URL");
            response.put("success", false);
            response.put("error", "Token no recibido en la URL");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        log.info("[AUTH_CONTROLLER] Login con Google exitoso, devolviendo JSON de éxito");
        response.put("success", true);
        response.put("message", "Autenticación Exitosa con Google");
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[AUTH_CONTROLLER] Error en login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("[AUTH_CONTROLLER] Error en registro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            authService.forgotPassword(request.get("email"));
            return ResponseEntity.ok(Collections.singletonMap("message", "Email de recuperación enviado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(Collections.singletonMap("message", "Contraseña restablecida con éxito"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            authService.logout(token);
            return ResponseEntity.ok("Logout exitoso");
        } catch (Exception e) {
            log.error("[AUTH_CONTROLLER] Error en logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Usuario usuario = authService.validateToken(token);
            return ResponseEntity.ok(AuthResponse.builder()
                    .message("Token válido")
                    .usuario(usuarioService.toDto(usuario))
                    .build());
        } catch (Exception e) {
            log.error("[AUTH_CONTROLLER] Error validando token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }
}
