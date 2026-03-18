package com.clearpet.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clearpet.dto.UsuarioDto;
import com.clearpet.entity.Usuario;
import com.clearpet.repository.UsuarioRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;

    // Inyectamos el repositorio por constructor
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Inyectamos el PasswordEncoder con @Lazy para romper el ciclo circular
    @Autowired
    public void setPasswordEncoder(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    public Usuario findById(String id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public boolean existsByEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    public Usuario createUsuario(String email, String nombre, String password, String nombreDispositivo) {
        log.info("[USUARIO] Creando nuevo usuario: {}", email);

        Usuario usuario = Usuario.builder()
                .email(email)
                .nombre(nombre)
                .password(passwordEncoder.encode(password)) // Ya no dará error
                .nombreDispositivo(nombreDispositivo)
                .rol("USER")
                .activo(true)
                .verificado(false)
                .build();

        return usuarioRepository.save(usuario);
    }

    public Usuario createGoogleUsuario(String email, String nombre, String googleId) {
        log.info("[USUARIO] Creando usuario de Google: {}", email);

        Usuario usuario = Usuario.builder()
                .email(email)
                .nombre(nombre)
                .password(passwordEncoder.encode(String.valueOf(System.nanoTime())))
                .rol("USER")
                .googleId(googleId)
                .activo(true)
                .verificado(true)
                .build();

        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> findByGoogleId(String googleId) {
        return usuarioRepository.findByGoogleId(googleId);
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