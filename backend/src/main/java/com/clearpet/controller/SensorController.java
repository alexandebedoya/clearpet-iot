package com.clearpet.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clearpet.dto.SensorDataDto;
import com.clearpet.entity.Usuario;
import com.clearpet.service.SensorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/sensores")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class SensorController {

    private final SensorService sensorService;

    @PostMapping("/data")
    public ResponseEntity<SensorDataDto> saveSensorData(
            @Valid @RequestBody SensorDataDto data,
            Authentication authentication) {
        try {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            SensorDataDto saved = sensorService.saveSensorData(usuario, data);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("[SENSOR_CONTROLLER] Error guardando datos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<SensorDataDto> getLatestSensorData(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Usuario usuario = (Usuario) authentication.getPrincipal();
            Optional<SensorDataDto> data = sensorService.getLatestSensorData(usuario);
            return data.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (Exception e) {
            log.error("[SENSOR_CONTROLLER] Error obteniendo datos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<SensorDataDto>> getSensorHistory(Authentication authentication) {
        try {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            List<SensorDataDto> data = sensorService.getSensorHistory(usuario);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("[SENSOR_CONTROLLER] Error obteniendo historial: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/range")
    public ResponseEntity<List<SensorDataDto>> getSensorDataByRange(
            @RequestParam String inicio,
            @RequestParam String fin,
            Authentication authentication) {
        try {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime inicioDateTime = LocalDateTime.parse(inicio, formatter);
            LocalDateTime finDateTime = LocalDateTime.parse(fin, formatter);

            List<SensorDataDto> data = sensorService.getSensorDataByDateRange(usuario, inicioDateTime, finDateTime);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("[SENSOR_CONTROLLER] Error obteniendo datos por rango: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}