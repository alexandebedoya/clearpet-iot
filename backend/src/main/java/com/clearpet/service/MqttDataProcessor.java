package com.clearpet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.clearpet.dto.SensorDataDto;
import com.clearpet.entity.Usuario;
import com.clearpet.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttDataProcessor {

    private final SensorService sensorService;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public void process(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            
            // Si el mensaje es solo de estado, ignorar
            if (root.has("estado") && root.get("estado").asText().equals("conectado")) {
                log.info("[MQTT] Dispositivo conectado: {}", root.has("dispositivo") ? root.get("dispositivo").asText() : "unknown");
                return;
            }

            String dispositivoId = root.get("dispositivo").asText();
            log.debug("[MQTT] Procesando datos del dispositivo: {}", dispositivoId);

            // Buscar usuario por nombre de dispositivo
            Usuario usuario = usuarioRepository.findByNombreDispositivo(dispositivoId)
                    .orElseGet(() -> {
                        log.warn("[MQTT] Dispositivo {} no asociado a ningún usuario. Usando usuario por defecto o ignorando.", dispositivoId);
                        // Aquí podrías decidir si crear un usuario por defecto o simplemente retornar
                        return null;
                    });

            if (usuario == null) return;

            SensorDataDto dto = SensorDataDto.builder()
                    .mq4(new BigDecimal(root.get("mq4").asInt()))
                    .mq7(new BigDecimal(root.get("mq7").asInt()))
                    .mq135(new BigDecimal(root.get("mq135").asInt()))
                    .timestamp(LocalDateTime.now()) // Usamos la hora del servidor
                    .build();

            sensorService.saveSensorData(usuario, dto);
            log.info("[MQTT] Datos guardados exitosamente para el usuario: {}", usuario.getEmail());

        } catch (Exception e) {
            log.error("[MQTT] Error al procesar el mensaje: {}", e.getMessage());
        }
    }
}
