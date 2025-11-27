package com.example.smart.lighting.scenes.with_natural.language.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User Entity Tests")
class UserTest {

    @Nested
    @DisplayName("User Builder")
    class UserBuilder {

        @Test
        @DisplayName("should create user with all fields")
        void shouldCreateUserWithAllFields() {
            UUID id = UUID.randomUUID();

            User user = User.builder()
                    .id(id)
                    .email("test@example.com")
                    .name("Test User")
                    .pictureUrl("https://example.com/photo.jpg")
                    .role(User.UserRole.RESIDENT)
                    .provider(User.OAuthProvider.GOOGLE)
                    .providerSub("google-sub-123")
                    .isActive(true)
                    .build();

            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getEmail()).isEqualTo("test@example.com");
            assertThat(user.getName()).isEqualTo("Test User");
            assertThat(user.getPictureUrl()).isEqualTo("https://example.com/photo.jpg");
            assertThat(user.getRole()).isEqualTo(User.UserRole.RESIDENT);
            assertThat(user.getProvider()).isEqualTo(User.OAuthProvider.GOOGLE);
            assertThat(user.getProviderSub()).isEqualTo("google-sub-123");
            assertThat(user.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("should default isActive to true")
        void shouldDefaultIsActiveToTrue() {
            User user = User.builder()
                    .email("test@example.com")
                    .name("Test User")
                    .role(User.UserRole.GUEST)
                    .provider(User.OAuthProvider.GOOGLE)
                    .build();

            assertThat(user.getIsActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("UserRole Enum")
    class UserRoleEnum {

        @Test
        @DisplayName("should have OWNER role")
        void shouldHaveOwnerRole() {
            User.UserRole role = User.UserRole.OWNER;
            assertThat(role.name()).isEqualTo("OWNER");
        }

        @Test
        @DisplayName("should have RESIDENT role")
        void shouldHaveResidentRole() {
            User.UserRole role = User.UserRole.RESIDENT;
            assertThat(role.name()).isEqualTo("RESIDENT");
        }

        @Test
        @DisplayName("should have GUEST role")
        void shouldHaveGuestRole() {
            User.UserRole role = User.UserRole.GUEST;
            assertThat(role.name()).isEqualTo("GUEST");
        }

        @Test
        @DisplayName("should have exactly 3 roles")
        void shouldHaveExactlyThreeRoles() {
            assertThat(User.UserRole.values()).hasSize(3);
        }

        @Test
        @DisplayName("should parse role from string")
        void shouldParseRoleFromString() {
            User.UserRole owner = User.UserRole.valueOf("OWNER");
            User.UserRole resident = User.UserRole.valueOf("RESIDENT");
            User.UserRole guest = User.UserRole.valueOf("GUEST");

            assertThat(owner).isEqualTo(User.UserRole.OWNER);
            assertThat(resident).isEqualTo(User.UserRole.RESIDENT);
            assertThat(guest).isEqualTo(User.UserRole.GUEST);
        }
    }

    @Nested
    @DisplayName("OAuthProvider Enum")
    class OAuthProviderEnum {

        @Test
        @DisplayName("should have GOOGLE provider")
        void shouldHaveGoogleProvider() {
            User.OAuthProvider provider = User.OAuthProvider.GOOGLE;
            assertThat(provider.name()).isEqualTo("GOOGLE");
        }

        @Test
        @DisplayName("should have LOCAL provider")
        void shouldHaveLocalProvider() {
            User.OAuthProvider provider = User.OAuthProvider.LOCAL;
            assertThat(provider.name()).isEqualTo("LOCAL");
        }

        @Test
        @DisplayName("should have exactly 2 providers")
        void shouldHaveExactlyTwoProviders() {
            assertThat(User.OAuthProvider.values()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("User Equality")
    class UserEquality {

        @Test
        @DisplayName("should be equal when same id")
        void shouldBeEqualWhenSameId() {
            UUID id = UUID.randomUUID();

            User user1 = User.builder()
                    .id(id)
                    .email("test@example.com")
                    .name("Test User")
                    .role(User.UserRole.GUEST)
                    .provider(User.OAuthProvider.GOOGLE)
                    .build();

            User user2 = User.builder()
                    .id(id)
                    .email("test@example.com")
                    .name("Test User")
                    .role(User.UserRole.GUEST)
                    .provider(User.OAuthProvider.GOOGLE)
                    .build();

            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when different id")
        void shouldNotBeEqualWhenDifferentId() {
            User user1 = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .name("Test User")
                    .role(User.UserRole.GUEST)
                    .provider(User.OAuthProvider.GOOGLE)
                    .build();

            User user2 = User.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .name("Test User")
                    .role(User.UserRole.GUEST)
                    .provider(User.OAuthProvider.GOOGLE)
                    .build();

            assertThat(user1).isNotEqualTo(user2);
        }
    }
}

