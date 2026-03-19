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
                        // Lógica temporal para asociar ESP32_CLEARPET_001 al primer usuario
                        if ("ESP32_CLEARPET_001".equals(dispositivoId)) {
                            log.info("[MQTT_DEBUG] Forzando asociación de dispositivo {} al primer usuario encontrado en la BD.", dispositivoId);
                            return usuarioRepository.findAll().stream().findFirst().orElse(null);
                        }
                        log.warn("[MQTT] Dispositivo {} no asociado a ningún usuario.", dispositivoId);
                        return null;
                    });

            if (usuario == null) {
                log.error("[MQTT] Error: No se pudo asociar el dispositivo {} a ningún usuario.", dispositivoId);
                return;
            }

            SensorDataDto dto = SensorDataDto.builder()
                    .mq4(new BigDecimal(root.get("mq4").asInt()))
                    .mq7(new BigDecimal(root.get("mq7").asInt()))
                    .mq135(new BigDecimal(root.get("mq135").asInt()))
                    .timestamp(LocalDateTime.now()) // Usamos la hora del servidor
                    .build();

            sensorService.saveSensorData(usuario, dto);
            log.info("[MQTT] Datos guardados exitosamente para el usuario: {} (Dispositivo: {})", usuario.getEmail(), dispositivoId);

        } catch (Exception e) {
            log.error("[MQTT] Error al procesar el mensaje: {}", e.getMessage());
        }
    }
}
