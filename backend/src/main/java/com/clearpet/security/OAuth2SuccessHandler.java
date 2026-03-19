package com.clearpet.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.clearpet.entity.Usuario;
import com.clearpet.service.UsuarioService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UsuarioService usuarioService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String picture = oAuth2User.getAttribute("picture");

        log.info("[OAUTH2] Login exitoso con Google: {}. Procesando persistencia...", email);

        // 1. Sincronización de usuario en BD
        Usuario usuario = usuarioService.processOAuthPostLogin(email, name, googleId, picture);

        // 2. Generación del JWT
        String token = tokenProvider.generateToken(usuario);

        // 3. Construcción de URL relativa para evitar problemas de protocolo (http/https)
        // Redirigimos internamente al controlador de éxito
        String targetUrl = "/api/auth/success?token=" + token;

        log.info("DEBUG: Redirigiendo a: {}", targetUrl);
        System.out.println("DEBUG: Redirigiendo a: " + targetUrl);
        
        // Limpiamos los atributos de sesión de autenticación previos
        clearAuthenticationAttributes(request);
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
