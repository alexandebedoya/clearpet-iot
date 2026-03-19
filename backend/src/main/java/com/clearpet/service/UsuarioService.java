package com.clearpet.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clearpet.dto.UsuarioDto;
import com.clearpet.entity.Usuario;
import com.clearpet.exception.DatabaseConflictException;
import com.clearpet.repository.UsuarioRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Autowired
    public void setPasswordEncoder(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    @Transactional(readOnly = true)
    public Usuario findById(String id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    /**
     * Procesa el login de OAuth2. Busca al usuario por email:
     * - Si existe: actualiza googleId y fotoUrl si son nulos o han cambiado.
     * - Si no existe: lo crea.
     */
    public Usuario processOAuthPostLogin(String email, String nombre, String googleId, String fotoUrl) {
        log.info("[USUARIO] Procesando post-login OAuth2 para: {}", email);

        return usuarioRepository.findByEmail(email)
                .map(usuario -> {
                    log.debug("[USUARIO] Usuario existente encontrado, actualizando datos de Google");
                    boolean modified = false;
                    if (usuario.getGoogleId() == null) {
                        usuario.setGoogleId(googleId);
                        modified = true;
                    }
                    if (fotoUrl != null && !fotoUrl.equals(usuario.getFotoUrl())) {
                        usuario.setFotoUrl(fotoUrl);
                        modified = true;
                    }
                    return modified ? usuarioRepository.save(usuario) : usuario;
                })
                .orElseGet(() -> {
                    log.info("[USUARIO] Usuario no encontrado, creando nueva cuenta de Google para: {}", email);
                    Usuario nuevoUsuario = Usuario.builder()
                            .email(email)
                            .nombre(nombre != null ? nombre : email.split("@")[0])
                            .googleId(googleId)
                            .fotoUrl(fotoUrl)
                            .password(passwordEncoder.encode("OAUTH2_USER_" + System.nanoTime()))
                            .rol("USER")
                            .activo(true)
                            .verificado(true)
                            .build();
                    try {
                        return usuarioRepository.save(nuevoUsuario);
                    } catch (Exception e) {
                        log.error("[USUARIO] Error al guardar nuevo usuario de Google: {}", e.getMessage());
                        throw new DatabaseConflictException(
                                "No se pudo crear el usuario debido a un conflicto en la base de datos.");
                    }
                });
    }

    public Usuario createUsuario(String email, String nombre, String password, String nombreDispositivo) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new DatabaseConflictException("El email ya está registrado.");
        }

        Usuario usuario = Usuario.builder()
                .email(email)
                .nombre(nombre)
                .password(passwordEncoder.encode(password))
                .nombreDispositivo(nombreDispositivo)
                .rol("USER")
                .activo(true)
                .verificado(false)
                .build();

        return usuarioRepository.save(usuario);
    }

    public UsuarioDto toDto(Usuario usuario) {
        if (usuario == null)
            return null;

        UsuarioDto dto = new UsuarioDto();
        dto.setId(usuario.getId());
        dto.setEmail(usuario.getEmail());
        dto.setNombre(usuario.getNombre());
        dto.setRol(usuario.getRol());
        dto.setActivo(usuario.getActivo());
        dto.setVerificado(usuario.getVerificado());
        dto.setNombreDispositivo(usuario.getNombreDispositivo());

        return dto;
    }
}
