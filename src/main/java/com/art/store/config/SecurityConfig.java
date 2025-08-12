package com.art.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:8080,http://localhost:3000,http://localhost:*}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for API and web endpoints (using Stripe for payment security)
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Set session management to stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization
            .authorizeHttpRequests(authz -> authz
                // Allow public access to web pages
                .requestMatchers("/", "/success", "/cancel").permitAll()
                
                // Allow public access to static resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/fire.jpeg").permitAll()
                
                // Allow public access to product endpoints (read-only)
                .requestMatchers("/api/products/**").permitAll()
                
                // Allow public access to order creation (POST) but secure individual order access
                .requestMatchers("/api/orders").permitAll()  // POST only for creating orders
                .requestMatchers("/api/orders/*").permitAll()  // GET individual orders - TODO: secure with customer auth
                
                // Allow public access to payment endpoints
                .requestMatchers("/api/payments/config").permitAll()
                .requestMatchers("/api/payments/create-payment-intent").permitAll()
                .requestMatchers("/api/payments/create-checkout-session").permitAll()
                .requestMatchers("/api/payments/confirm-payment").permitAll()
                .requestMatchers("/api/payments/status/**").permitAll()
                .requestMatchers("/api/payments/session/**").permitAll()
                .requestMatchers("/api/payments/webhook").permitAll()
                
                // Allow access to health check endpoints
                .requestMatchers("/actuator/health").permitAll()
                
                // Allow access to API documentation (if using Swagger)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // All admin/sensitive order endpoints are now commented out in controller
                // If re-enabled, they should require admin authentication:
                // .requestMatchers("/api/orders/stats/**").authenticated()
                // .requestMatchers("/api/orders/{id}/status").authenticated()
                // .requestMatchers("/api/orders/{id}/cancel").authenticated()
                // .requestMatchers("/api/orders/customer/**").authenticated()
                
                // Deny all other requests by default (security-first approach)
                .anyRequest().denyAll()
            )
            
            // Configure security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> contentTypeOptions.and())
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow origins from environment variable (supports freesticker.org in production)
        String[] origins = allowedOrigins.split(",");
        configuration.setAllowedOriginPatterns(Arrays.asList(origins));
        
        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}