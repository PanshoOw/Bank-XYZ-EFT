package com.duoc.cuentas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http)
                throws Exception {

                http
                        .csrf(csrf -> csrf.disable())

                        .authorizeHttpRequests(auth -> auth

                                // Actuator necesario para health checks
                                .requestMatchers(
                                        "/actuator/health",
                                        "/actuator/info"
                                ).permitAll()

                                // Consultas de cuentas:
                                // Web, Mobile y ATM pueden consultar
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/cuentas/**"
                                ).hasAnyAuthority(
                                        "SCOPE_WEB",
                                        "SCOPE_MOBILE",
                                        "SCOPE_ATM"
                                )

                                // Solo el canal ATM puede retirar
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/cuentas/*/retiro"
                                ).hasAuthority("SCOPE_ATM")

                                .anyRequest().authenticated()
                        )

                        .oauth2ResourceServer(oauth2 ->
                                oauth2.jwt(Customizer.withDefaults())
                        );

                return http.build();
        }
}