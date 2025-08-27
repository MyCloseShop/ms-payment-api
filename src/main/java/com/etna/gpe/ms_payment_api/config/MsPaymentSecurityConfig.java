package com.etna.gpe.ms_payment_api.config;

import com.etna.gpe.mycloseshop.security_api.config.CustomAccessDeniedHandler;
import com.etna.gpe.mycloseshop.security_api.config.JwtAuthenticationEntryPoint;
import com.etna.gpe.mycloseshop.security_api.config.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@Order(1)
public class MsPaymentSecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private final JwtRequestFilter jwtRequestFilter;

    private final DaoAuthenticationProvider authenticationProvider;

    @Autowired
    public MsPaymentSecurityConfig(
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtRequestFilter jwtRequestFilter,
            DaoAuthenticationProvider authenticationProvider
    ) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtRequestFilter = jwtRequestFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean(name = "msPaymentAccessDeniedHandler")
    public AccessDeniedHandler msPaymentAccessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }

    @Bean(name = "msPaymentCorsConfigurationSource")
    public CorsConfigurationSource msPaymentCorsConfigurationSource() {
        CorsConfiguration source = new CorsConfiguration();
        source.setAllowedOrigins(List.of("*"));
        source.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        source.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source1 = new UrlBasedCorsConfigurationSource();
        source1.registerCorsConfiguration("/**", source);
        return source1;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(cors -> cors.configurationSource(msPaymentCorsConfigurationSource()));
        http.sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(exceptionHandling -> exceptionHandling
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .accessDeniedHandler(msPaymentAccessDeniedHandler())
        );
        http.authorizeHttpRequests(authorizeRequests ->
            authorizeRequests
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/ms-payment-api/actuator/health").permitAll()
                // Endpoints publics pour les redirections Stripe (pas de JWT possible)
                .requestMatchers("/payment/success", "/payment/cancel").permitAll()
                // Endpoints publics pour les webhooks Stripe (pas de JWT possible)
                .requestMatchers("/stripe/webhook", "/stripe/connect/return", "/stripe/connect/reauth", "/stripe/reauth", "/stripe/return").permitAll()
                .anyRequest().authenticated()
        );
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider);
    }
}
