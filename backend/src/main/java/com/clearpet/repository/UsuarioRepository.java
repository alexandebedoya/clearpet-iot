package com.clearpet.repository;

import com.clearpet.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByGoogleId(String googleId);

    Optional<Usuario> findByNombreDispositivo(String nombreDispositivo);

    Optional<Usuario> findByResetToken(String resetToken);

    boolean existsByEmail(String email);
}