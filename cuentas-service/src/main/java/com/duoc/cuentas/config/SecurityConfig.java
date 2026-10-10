package com.duoc.cuentas.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        private static final String SCOPE_WEB = "SCOPE_WEB";
        private static final String SCOPE_MOBILE = "SCOPE_MOBILE";
        private static final String SCOPE_ATM = "SCOPE_ATM";

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http)
                throws Exception {

                http
                        .csrf(csrf -> csrf.disable())

                        .authorizeHttpRequests(auth -> auth

                                .dispatcherTypeMatchers(
                                        DispatcherType.ERROR
                                ).permitAll()

                                .requestMatchers(
                                        "/actuator/health",
                                        "/actuator/info"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/cuentas/**"
                                ).hasAnyAuthority(
                                        SCOPE_WEB,
                                        SCOPE_MOBILE,
                                        SCOPE_ATM
                                )

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/cuentas/*/retiro"
                                ).hasAuthority(SCOPE_ATM)

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/cuentas"
                                ).hasAuthority(SCOPE_WEB)

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/cuentas/*"
                                ).hasAuthority(SCOPE_WEB)

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/cuentas/*/cerrar"
                                ).hasAuthority(SCOPE_WEB)

                                .anyRequest().denyAll()
                        )

                        .oauth2ResourceServer(oauth2 ->
                                oauth2.jwt(Customizer.withDefaults())
                        );

                return http.build();
        }
}