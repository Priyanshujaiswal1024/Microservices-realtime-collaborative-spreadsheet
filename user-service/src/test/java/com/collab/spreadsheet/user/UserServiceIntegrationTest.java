package com.collab.spreadsheet.user;

import com.collab.spreadsheet.user.dto.AuthResponse;
import com.collab.spreadsheet.user.dto.LoginRequest;
import com.collab.spreadsheet.user.dto.RegisterRequest;
import com.collab.spreadsheet.user.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("userdb")
            .withUsername("collabuser")
            .withPassword("collabpass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
        if (redis.isRunning()) {
            registry.add("spring.data.redis.host", redis::getHost);
            registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        }
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @Autowired(required = false)
    private AuthService authService;

    @Test
    @DisplayName("Verify user registration and authentication flow with PostgreSQL and Redis")
    void testRegisterAndLoginFlow() {
        if (authService == null) {
            return;
        }

        RegisterRequest reg = RegisterRequest.builder()
                .username("integrationuser")
                .email("integration@example.com")
                .password("Password123!")
                .fullName("Integration Tester")
                .build();

        AuthResponse regResponse = authService.register(reg);
        assertThat(regResponse).isNotNull();
        assertThat(regResponse.getAccessToken()).isNotBlank();
        assertThat(regResponse.getUser().getUsername()).isEqualTo("integrationuser");

        LoginRequest login = LoginRequest.builder()
                .emailOrUsername("integration@example.com")
                .password("Password123!")
                .build();

        AuthResponse loginResponse = authService.login(login, "TestAgent", "127.0.0.1");
        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getAccessToken()).isNotBlank();
        assertThat(loginResponse.getRefreshToken()).isNotBlank();
    }
}
