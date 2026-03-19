package com.biosense.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.biosense.dto.SensorDataDto;
import com.biosense.entity.Usuario;
import com.biosense.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttDataProcessor {

    private final SensorService sensorService;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public void process(String payload) {
        Mono.fromCallable(() -> objectMapper.readTree(payload))
                .flatMap(root -> {
                    // Si el mensaje es solo de estado, ignorar
                    if (root.has("estado") && root.get("estado").asText().equals("conectado")) {
                        log.info("[MQTT] Dispositivo conectado: {}",
                                root.has("dispositivo") ? root.get("dispositivo").asText() : "unknown");
                        return Mono.empty();
                    }

                    if (!root.has("dispositivo")) {
                        return Mono.error(new RuntimeException("Mensaje MQTT sin dispositivoId"));
                    }

                    String dispositivoId = root.get("dispositivo").asText();
                    log.debug("[MQTT] Procesando datos del dispositivo: {}", dispositivoId);

                    // Buscar usuario por nombre de dispositivo
                    return usuarioRepository.findByNombreDispositivo(dispositivoId)
                            .switchIfEmpty(Mono.defer(() -> {
                                // Lógica temporal para asociar ESP32_BIOSENSE_001 al primer usuario
                                if ("ESP32_BIOSENSE_001".equals(dispositivoId)) {
                                    log.info("[MQTT_DEBUG] Forzando asociación de dispositivo {} al primer usuario encontrado en la BD.",
                                            dispositivoId);
                                    return usuarioRepository.findAll().next();
                                }
                                log.warn("[MQTT] Dispositivo {} no asociado a ningún usuario.", dispositivoId);
                                return Mono.empty();
                            }))
                            .flatMap(usuario -> {
                                SensorDataDto dto = SensorDataDto.builder()
                                        .mq4(new BigDecimal(root.get("mq4").asInt()))
                                        .mq7(new BigDecimal(root.get("mq7").asInt()))
                                        .mq135(new BigDecimal(root.get("mq135").asInt()))
                                        .timestamp(LocalDateTime.now()) // Usamos la hora del servidor
                                        .build();

                                return sensorService.saveSensorData(usuario.getId(), dto)
                                        .doOnSuccess(saved -> log.info(
                                                "[MQTT] Datos guardados exitosamente para el usuario: {} (Dispositivo: {})",
                                                usuario.getEmail(), dispositivoId));
                            })
                            .switchIfEmpty(Mono.fromRunnable(() -> log.error(
                                    "[MQTT] Error: No se pudo asociar el dispositivo {} a ningún usuario.",
                                    dispositivoId)));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        null,
                        e -> log.error("[MQTT] Error al procesar el mensaje: {}", e.getMessage()));
    }
}
