package com.gym.management.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

public class GymUserDetails extends User {

    private final UUID id;

    public GymUserDetails(UUID id, String email, String passwordHash, boolean enabled,
                          Collection<? extends GrantedAuthority> authorities) {
        super(email, passwordHash, enabled, true, true, true, authorities);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}