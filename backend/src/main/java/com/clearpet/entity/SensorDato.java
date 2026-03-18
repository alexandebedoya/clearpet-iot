package com.clearpet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_datos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal mq4;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal mq7;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal mq135;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AirQualityLevel nivel = AirQualityLevel.NORMAL;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "aqi")
    private Integer aqi;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AirQualityLevel {
        NORMAL, PRECAUCION, PELIGRO
    }
}