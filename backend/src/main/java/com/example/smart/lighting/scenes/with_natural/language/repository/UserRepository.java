package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link User} entity persistence operations.
 *
 * <p>Provides lookup methods for both OAuth and local authentication.</p>
 *

 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email address.
     *
     * @param email the user's email
     * @return optional containing the user
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by OAuth provider and provider-specific ID.
     *
     * @param provider the OAuth provider (GOOGLE, LOCAL)
     * @param providerSub the provider's user ID
     * @return optional containing the user
     */
    Optional<User> findByProviderAndProviderSub(User.OAuthProvider provider, String providerSub);

    /**
     * Checks if a user with the given email exists.
     *
     * @param email the email to check
     * @return true if user exists
     */
    boolean existsByEmail(String email);
}
