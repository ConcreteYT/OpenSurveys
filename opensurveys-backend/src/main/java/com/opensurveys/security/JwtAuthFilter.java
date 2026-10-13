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

    @Autowired
    private AuthCookieService authCookieService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = null;
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        } else {
            token = authCookieService.read(request).orElse(null);
        }

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<ParsedToken> parsed = jwtUtil.parse(token);
            if (parsed.isPresent()) {
                Optional<User> userOpt = userRepository.findByUsername(parsed.get().username());
                if (userOpt.isPresent() && userOpt.get().getTokenVersion() == parsed.get().version()) {
                    User user = userOpt.get();
                    String authority = "ROLE_" + user.getRole();
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            parsed.get().username(),
                            null,
                            List.of(new SimpleGrantedAuthority(authority))
                    );
                    authToken.setDetails(user);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
