package com.opensurveys.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// Single source of truth for which routes need a signed-in user. This is what actually
// implements "sign in / register only required to create a form": AuthController's
// endpoints and the form-fetch GET are public, while form creation is gated.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Injected so it can be wired into the filter chain below; JwtAuthFilter itself only
    // *populates* the SecurityContext, this config decides what to do with it per route.
    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // No cookies/sessions - auth state travels entirely in the JWT on each request.
                .csrf(csrf -> csrf.disable())
                // Lets the React dev server (a different origin: localhost:3000 vs 8080) call
                // this API from the browser - without this the browser blocks every request
                // before it even reaches the controllers below.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // The built React app is served by this same app (see WebConfig) so it can
                        // be reached at http://localhost:8080 directly. Loading the page/its assets
                        // must never require a token - React's own ProtectedRoute (client-side)
                        // is what actually gates page content; the rules below still gate the
                        // real data/actions underneath it.
                        .requestMatchers(HttpMethod.GET,
                                "/", "/index.html", "/static/**", "/favicon.ico",
                                "/manifest.json", "/asset-manifest.json", "/robots.txt", "/logo*.png")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/home", "/auth/login", "/auth/signup", "/user-view", "/login-home",
                                "/surveys", "/admin", "/editor", "/editor/*", "/settings")
                        .permitAll()
                        // AuthController: account creation and sign-in must be reachable without a token.
                        // Path must match AuthController's actual @PostMapping("/signup"), not "/register".
                        .requestMatchers(HttpMethod.POST, "/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/google").permitAll()
                        .requestMatchers(HttpMethod.GET, "/forms/*/responses").permitAll()
                        // Responses stay permitAll so public surveys work anonymously; FormController
                        // enforces owner-only access when the form's responsesPublic flag is false
                        // (JWT is still parsed by JwtAuthFilter when a token is sent).
                        // FormController#getForm: anonymous users fetch a form (and its
                        // creator's username) to fill it out, so this must stay public.
                        // Use /forms/* (one path segment) so GET /forms (list mine) is not public.
                        .requestMatchers(HttpMethod.GET, "/forms/*").permitAll()
                        // FormController#submitAnswers: whoever filled the form out anonymously
                        // (via the GET above) must be able to submit their answers without a
                        // token too - this must be listed before the plain "/forms" rule below
                        // so its POST method doesn't fall through to requiring authentication.
                        .requestMatchers(HttpMethod.POST, "/forms/*/answers").permitAll()
                        // FormController#listMyForms / #createForm / #updateForm: only signed-in users.
                        .requestMatchers(HttpMethod.GET, "/forms").authenticated()
                        .requestMatchers(HttpMethod.POST, "/forms").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/forms/*").authenticated()
                        // UserController: admin-only account directory and management.
                        .requestMatchers("/users", "/users/**").hasRole("ADMIN")
                        // Any other unlisted route defaults to requiring authentication.
                        .anyRequest().authenticated()
                )
                // Runs JwtAuthFilter ahead of Spring Security's built-in filter so a valid
                // bearer token is resolved into a SecurityContext authentication before the
                // rules above are evaluated.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Used by AuthController to hash passwords on register and verify them on login.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Origins allowed to call this API from a browser. Add the deployed frontend's URL
    // here too once it's hosted somewhere other than localhost.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
