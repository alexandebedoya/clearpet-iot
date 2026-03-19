package com.clearpet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "\"usuario\"") // Coincide con el nombre de Prisma (con comillas para evitar errores de
                             // mayúsculas)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private String rol = "USER";

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "\"nombreDispositivo\"")

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiration")
    private LocalDateTime resetTokenExpiration;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verificado = false;

    @CreationTimestamp
    @Column(name = "\"creadoEn\"", nullable = false, updatable = false) // Ajustado a nombre de Prisma
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "\"actualizadoEn\"") // Ajustado a nombre de Prisma
    private LocalDateTime updatedAt;

    // --- Métodos de UserDetails ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return activo != null && activo;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo != null && activo;
    }
}