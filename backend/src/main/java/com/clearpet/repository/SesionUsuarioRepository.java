package com.clearpet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.clearpet.entity.SesionUsuario;
import com.clearpet.entity.Usuario;

@Repository
public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, String> {
    Optional<SesionUsuario> findByToken(String token);

    List<SesionUsuario> findByUsuarioAndActivo(Usuario usuario, Boolean activo);

    void deleteByUsuario(Usuario usuario);
}