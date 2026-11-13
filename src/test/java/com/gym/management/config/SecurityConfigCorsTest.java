package com.gym.management.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

    @Test
    void parseOrigins_missingOrBlankMeansAnyOrigin() {
        assertThat(SecurityConfig.parseOrigins(null)).containsExactly("*");
        assertThat(SecurityConfig.parseOrigins("   ")).containsExactly("*");
        assertThat(SecurityConfig.parseOrigins(" , ,")).containsExactly("*");
    }

    @Test
    void parseOrigins_splitsOnCommaAndTrims() {
        assertThat(SecurityConfig.parseOrigins(" https://gms.vercel.app , https://gms.example.com,"))
                .containsExactly("https://gms.vercel.app", "https://gms.example.com");
    }

    @Test
    void corsConfigurationSource_usesTheConfiguredOrigins() {
        SecurityConfig config = new SecurityConfig(null, null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://gms.vercel.app");

        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfigurations().get("/**");

        assertThat(cors.getAllowedOriginPatterns()).isEqualTo(List.of("https://gms.vercel.app"));
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getAllowedMethods()).contains("PATCH", "OPTIONS");
    }
}
