package com.opensurveys.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.opensurveys.model.User;
import com.opensurveys.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

// Runs once per request, before Spring Security's own auth filters (wired via
// SecurityConfig#securityFilterChain -> addFilterBefore). It never blocks a request itself -
// it only *optionally* populates SecurityContextHolder when a valid bearer token is present.
// SecurityConfig then decides, per route, whether that resulting authentication is required
// (e.g. POST /forms) or ignored (e.g. GET /forms/**, /auth/**), which is what lets anonymous
// users hit the public form-viewing endpoint while form creation stays gated.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTH_HEADER);

        // No/other header format (e.g. anonymous GET /forms/{id} request) -> just continue
        // the chain unauthenticated; SecurityConfig's permitAll() rules allow that.
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());

            if (jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.extractUsername(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Optional<User> userOpt = userRepository.findByUsername(username);

                    if (userOpt.isPresent()) {
                        // Principal is the plain username String (not the User entity) -
                        // FormController later reads it back via
                        // SecurityContextHolder.getContext().getAuthentication().getName()
                        // to resolve the form creator.
                        // Authority comes from User.role (USER/ADMIN) so SecurityConfig
                        // can gate admin-only routes like GET /users.
                        User user = userOpt.get();
                        String authority = "ROLE_" + user.getRole();
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority(authority))
                        );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
            // Invalid/expired token: silently fall through as unauthenticated rather than
            // rejecting outright, so SecurityConfig's per-route rules are the single source
            // of truth for what actually requires auth.
        }

        filterChain.doFilter(request, response);
    }
}
