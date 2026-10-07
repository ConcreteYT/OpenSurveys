package com.opensurveys.service;

import com.opensurveys.model.User;
import com.opensurveys.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrentUserServiceTest {

    private UserRepository userRepository;
    private CurrentUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new CurrentUserService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static User user(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        return user;
    }

    @Test
    void emptyWithoutAuthentication() {
        assertTrue(service.get().isEmpty());
    }

    @Test
    void anonymousTokenNeverResolvesToAnAccount() {
        when(userRepository.findByUsername("anonymousUser")).thenReturn(Optional.of(user("anonymousUser")));
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertTrue(service.get().isEmpty());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void usesUserAttachedByJwtFilter() {
        User alice = user("alice");
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        token.setDetails(alice);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertSame(alice, service.get().orElseThrow());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void fallsBackToLookupByName() {
        User alice = user("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertEquals(Optional.of(alice), service.get());
    }

    @Test
    void realAccountNamedAnonymousUserStillWorksWithItsOwnToken() {
        User account = user("anonymousUser");
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        token.setDetails(account);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertSame(account, service.get().orElseThrow());
    }
}
