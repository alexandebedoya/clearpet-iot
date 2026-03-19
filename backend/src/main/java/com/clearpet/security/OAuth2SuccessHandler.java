package com.clearpet.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.clearpet.entity.Usuario;
import com.clearpet.repository.UsuarioRepository;

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
    private final UsuarioRepository usuarioRepository;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");

        log.info("[OAUTH2] Login exitoso con Google: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .map(u -> {
                    if (u.getGoogleId() == null) {
                        u.setGoogleId(googleId);
                        return usuarioRepository.save(u);
                    }
                    return u;
                })
                .orElseGet(() -> {
                    Usuario newUser = Usuario.builder()
                            .email(email)
                            .nombre(name != null ? name : email.split("@")[0])
                            .googleId(googleId)
                            .password("") 
                            .rol("USER")
                            .activo(true)
                            .verificado(true)
                            .build();
                    return usuarioRepository.save(newUser);
                });

        String token = tokenProvider.generateToken(usuario);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
