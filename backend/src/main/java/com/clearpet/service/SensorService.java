package com.clearpet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clearpet.dto.SensorDataDto;
import com.clearpet.entity.SensorDato;
import com.clearpet.entity.Usuario;
import com.clearpet.repository.SensorDatoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SensorService {

    private final SensorDatoRepository sensorRepository;

    public SensorDataDto saveSensorData(Usuario usuario, SensorDataDto data) {
        log.info("[SENSOR] Guardando datos de sensores para usuario: {}", usuario.getEmail());

        Integer aqi = calculateAQI(data.getMq4(), data.getMq7(), data.getMq135());
        SensorDato.AirQualityLevel nivel = determineAirQuality(data.getMq4(), data.getMq7(), data.getMq135());

        SensorDato sensorDato = SensorDato.builder()
                .usuario(usuario)
                .mq4(data.getMq4())
                .mq7(data.getMq7())
                .mq135(data.getMq135())
                .aqi(aqi)
                .nivel(nivel)
                .timestamp(data.getTimestamp() != null ? data.getTimestamp() : LocalDateTime.now())
                .build();

        SensorDato saved = sensorRepository.save(sensorDato);

        return toDto(saved);
    }

    public Optional<SensorDataDto> getLatestSensorData(Usuario usuario) {
        log.info("[SENSOR] Obteniendo última lectura para: {}", usuario.getEmail());
        return sensorRepository.findFirstByUsuarioOrderByTimestampDesc(usuario)
                .map(this::toDto);
    }

    public List<SensorDataDto> getSensorHistory(Usuario usuario) {
        log.info("[SENSOR] Obteniendo historial de sensores para: {}", usuario.getEmail());
        return sensorRepository.findByUsuarioOrderByTimestampDesc(usuario)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<SensorDataDto> getSensorDataByDateRange(Usuario usuario, LocalDateTime inicio, LocalDateTime fin) {
        log.info("[SENSOR] Obteniendo datos entre {} y {} para: {}", inicio, fin, usuario.getEmail());
        return sensorRepository.findByUsuarioAndTimestampBetween(usuario, inicio, fin)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private Integer calculateAQI(BigDecimal mq4, BigDecimal mq7, BigDecimal mq135) {
        // Fórmula AQI simplificada
        BigDecimal aqi = (mq4.divide(BigDecimal.valueOf(50), 2, java.math.RoundingMode.HALF_UP)
                .add(mq7.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP))
                .add(mq135.divide(BigDecimal.valueOf(75), 2, java.math.RoundingMode.HALF_UP)))
                .multiply(BigDecimal.valueOf(50));

        return aqi.intValue();
    }

    private SensorDato.AirQualityLevel determineAirQuality(BigDecimal mq4, BigDecimal mq7, BigDecimal mq135) {
        // Umbrales de calidad del aire
        if (mq4.compareTo(BigDecimal.valueOf(500)) > 0 ||
                mq7.compareTo(BigDecimal.valueOf(400)) > 0 ||
                mq135.compareTo(BigDecimal.valueOf(600)) > 0) {
            return SensorDato.AirQualityLevel.PELIGRO;
        }

        if (mq4.compareTo(BigDecimal.valueOf(300)) > 0 ||
                mq7.compareTo(BigDecimal.valueOf(200)) > 0 ||
                mq135.compareTo(BigDecimal.valueOf(350)) > 0) {
            return SensorDato.AirQualityLevel.PRECAUCION;
        }

        return SensorDato.AirQualityLevel.NORMAL;
    }

    public SensorDataDto toDto(SensorDato sensorDato) {
        return SensorDataDto.builder()
                .mq4(sensorDato.getMq4())
                .mq7(sensorDato.getMq7())
                .mq135(sensorDato.getMq135())
                .nivel(sensorDato.getNivel().toString())
                .aqi(sensorDato.getAqi())
                .timestamp(sensorDato.getTimestamp())
                .build();
    }
}