package com.opensurveys.service;

import com.opensurveys.model.User;
import com.opensurveys.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Resolves the User behind the current request's JWT. Anonymous requests on permitAll routes
 * carry an {@link AnonymousAuthenticationToken} named "anonymousUser"; those never map to an
 * account, even if one happens to be registered under that username.
 */
@Service
public class CurrentUserService {

    @Autowired
    private UserRepository userRepository;

    public Optional<User> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (authentication.getDetails() instanceof User user) {
            return Optional.of(user);
        }
        return userRepository.findByUsername(authentication.getName());
    }
}
