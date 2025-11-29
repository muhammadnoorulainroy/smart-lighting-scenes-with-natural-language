package com.example.smart.lighting.scenes.with_natural.language.repository;

import com.example.smart.lighting.scenes.with_natural.language.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderSub(User.OAuthProvider provider, String providerSub);

    boolean existsByEmail(String email);
}
