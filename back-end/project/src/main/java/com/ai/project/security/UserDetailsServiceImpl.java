package com.ai.project.security;

import com.ai.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a {@link UserDetailsImpl} from the database by email address.
 *
 * <p>Called by Spring Security's {@code AuthenticationManager} during the
 * login flow. Also used by {@link JwtAuthenticationFilter} to reconstruct
 * the security context from a validated JWT on subsequent requests.</p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * @param email the email address used as the Spring Security principal identifier
     * @throws UsernameNotFoundException if no active or inactive user exists with this email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
            .map(UserDetailsImpl::new)
            .orElseThrow(() -> new UsernameNotFoundException(
                "No user account found for email: " + email
            ));
    }
}