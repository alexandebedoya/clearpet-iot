package com.clearpet.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.clearpet.entity.SensorDato;
import com.clearpet.entity.Usuario;

@Repository
public interface SensorDatoRepository extends JpaRepository<SensorDato, String> {

    Optional<SensorDato> findFirstByUsuarioOrderByTimestampDesc(Usuario usuario);

    List<SensorDato> findByUsuarioOrderByTimestampDesc(Usuario usuario);

    @Query("SELECT s FROM SensorDato s WHERE s.usuario = :usuario AND s.timestamp BETWEEN :inicio AND :fin ORDER BY s.timestamp DESC")
    List<SensorDato> findByUsuarioAndTimestampBetween(
            @Param("usuario") Usuario usuario,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);
}