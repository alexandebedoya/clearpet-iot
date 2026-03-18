package com.clearpet.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clearpet.dto.AuthResponse;
import com.clearpet.dto.LoginRequest;
import com.clearpet.dto.RegisterRequest;
import com.clearpet.dto.ResetPasswordRequest;
import com.clearpet.entity.SesionUsuario;
import com.clearpet.entity.Usuario;
import com.clearpet.repository.SesionUsuarioRepository;
import com.clearpet.repository.UsuarioRepository;
import com.clearpet.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UsuarioService usuarioService;
    private final SesionUsuarioRepository sesionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final UsuarioRepository usuarioRepository;

    public void forgotPassword(String email) {
        log.info("[AUTH] Forgot password request for: {}", email);
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ese email"));

        String resetToken = UUID.randomUUID().toString();
        usuario.setResetToken(resetToken);
        usuario.setResetTokenExpiration(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(usuario);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("ClearPet - Recuperación de contraseña");
            message.setText("Usa este token para restablecer tu contraseña: " + resetToken +
                    "\nExpira en 1 hora.");
            mailSender.send(message);
            log.info("[AUTH] Email de recuperación enviado a: {}", email);
        } catch (Exception e) {
            log.error("[AUTH] Error al enviar email: {}", e.getMessage());
            throw new RuntimeException("No se pudo enviar el email de recuperación");
        }
    }

    public void resetPassword(ResetPasswordRequest request) {
        log.info("[AUTH] Reset password attempt");

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        Usuario usuario = usuarioRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token de recuperación inválido"));

        if (usuario.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El token de recuperación ha expirado");
        }

        usuario.setPassword(passwordEncoder.encode(request.getNewPassword()));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiration(null);
        usuarioRepository.save(usuario);

        log.info("[AUTH] ✓ Contraseña restablecida con éxito para: {}", usuario.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        log.info("[AUTH] Login intent: {}", request.getEmail());

        Usuario usuario = usuarioService.findByEmail(request.getEmail());

        if (usuario == null || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            log.warn("[AUTH] Login failed for: {}", request.getEmail());
            throw new RuntimeException("Email o contraseña inválidos");
        }

        if (!usuario.getActivo()) {
            log.warn("[AUTH] Login attempt for inactive user: {}", request.getEmail());
            throw new RuntimeException("Usuario inactivo");
        }

        String token = jwtTokenProvider.generateToken(usuario);

        SesionUsuario sesion = SesionUsuario.builder()
                .usuario(usuario)
                .token(token)
                .expiracion(LocalDateTime.now().plusDays(7))
                .activo(true)
                .build();

        sesionRepository.save(sesion);

        log.info("[AUTH] ✓ Login exitoso para: {}", request.getEmail());

        return AuthResponse.builder()
                .token(token)
                .usuario(usuarioService.toDto(usuario))
                .message("Login exitoso")
                .build();
    }

    public AuthResponse register(RegisterRequest request) {
        log.info("[AUTH] Registro intent: {}", request.getEmail());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        if (request.getPassword().length() < 8) {
            throw new RuntimeException("La contraseña debe tener mínimo 8 caracteres");
        }

        if (usuarioService.existsByEmail(request.getEmail())) {
            log.warn("[AUTH] Email ya existe: {}", request.getEmail());
            throw new RuntimeException("El email ya está registrado");
        }

        Usuario usuario = usuarioService.createUsuario(
                request.getEmail(),
                request.getNombre(),
                request.getPassword(),
                request.getNombreDispositivo());

        String token = jwtTokenProvider.generateToken(usuario);

        SesionUsuario sesion = SesionUsuario.builder()
                .usuario(usuario)
                .token(token)
                .expiracion(LocalDateTime.now().plusDays(7))
                .activo(true)
                .build();

        sesionRepository.save(sesion);

        log.info("[AUTH] ✓ Registro exitoso para: {}", request.getEmail());

        return AuthResponse.builder()
                .token(token)
                .usuario(usuarioService.toDto(usuario))
                .message("Registro exitoso")
                .build();
    }

    public void logout(String token) {
        log.info("[AUTH] Logout");
        SesionUsuario sesion = sesionRepository.findByToken(token).orElse(null);
        if (sesion != null) {
            sesion.setActivo(false);
            sesionRepository.save(sesion);
        }
    }

    public Usuario validateToken(String token) {
        if (!jwtTokenProvider.isTokenValid(token)) {
            throw new RuntimeException("Token inválido o expirado");
        }

        String usuarioId = jwtTokenProvider.getUsuarioIdFromToken(token);
        SesionUsuario sesion = sesionRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        if (!sesion.getActivo() || sesion.getExpiracion().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Sesión expirada");
        }

        return sesion.getUsuario();
    }
}
