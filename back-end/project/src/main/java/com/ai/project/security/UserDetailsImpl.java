package com.ai.project.security;

import com.ai.project.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * Adapts the MindOps {@link User} entity to Spring Security's {@link UserDetails} contract.
 *
 * <p>Exposes the internal {@code User} entity via {@link #getUser()} so that downstream
 * services (e.g. {@code AuthService}) can access application-specific fields such as
 * {@code publicId} and {@code role} without an additional database round-trip.</p>
 */
public class UserDetailsImpl implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final User user;

    private final String email;
    private final String passwordHash;
    private final boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(User user) {
        this.user         = user;
        this.email        = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.isActive     = Boolean.TRUE.equals(user.getIsActive());
        this.authorities  = List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    // ── UserDetails contract ─────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** Spring Security uses {@code username} as the principal identifier. We use email. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}