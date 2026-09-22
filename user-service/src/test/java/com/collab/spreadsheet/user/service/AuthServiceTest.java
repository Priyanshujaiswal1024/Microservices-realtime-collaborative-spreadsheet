package com.collab.spreadsheet.user.service;

import com.collab.spreadsheet.common.dto.UserRole;
import com.collab.spreadsheet.common.exception.UnauthorizedException;
import com.collab.spreadsheet.common.exception.ValidationException;
import com.collab.spreadsheet.user.dto.AuthResponse;
import com.collab.spreadsheet.user.dto.LoginRequest;
import com.collab.spreadsheet.user.dto.RegisterRequest;
import com.collab.spreadsheet.user.dto.UserDto;
import com.collab.spreadsheet.user.entity.User;
import com.collab.spreadsheet.user.mapper.UserMapper;
import com.collab.spreadsheet.user.repository.RefreshTokenRepository;
import com.collab.spreadsheet.user.repository.UserRepository;
import com.collab.spreadsheet.user.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private UserDto sampleUserDto;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("u-12345")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPass")
                .fullName("Test User")
                .role(UserRole.USER)
                .enabled(true)
                .colorHex("#4A90E2")
                .build();

        sampleUserDto = UserDto.builder()
                .id("u-12345")
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .role(UserRole.USER)
                .enabled(true)
                .colorHex("#4A90E2")
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedSecret");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtil.generateAccessToken(any(), any(), any(), any())).thenReturn("access.jwt.token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh.jwt.token");
        when(jwtUtil.getAccessTokenValiditySeconds()).thenReturn(900L);
        when(jwtUtil.getRefreshTokenValiditySeconds()).thenReturn(604800L);
        when(userMapper.toDto(any(User.class))).thenReturn(sampleUserDto);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.jwt.token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.jwt.token");
        assertThat(response.getUser()).isEqualTo(sampleUserDto);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when email already exists during registration")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("existing@example.com")
                .password("Password123!")
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("Should successfully login user with valid credentials")
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .emailOrUsername("testuser")
                .password("Password123!")
                .build();

        when(userRepository.findByEmailOrUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123!", "hashedPass")).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(), any(), any(), any())).thenReturn("access.jwt.token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh.jwt.token");
        when(jwtUtil.getAccessTokenValiditySeconds()).thenReturn(900L);
        when(jwtUtil.getRefreshTokenValiditySeconds()).thenReturn(604800L);
        when(userMapper.toDto(any(User.class))).thenReturn(sampleUserDto);

        AuthResponse response = authService.login(request, "Mozilla/5.0", "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.jwt.token");
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException for invalid password")
    void testLoginInvalidPassword() {
        LoginRequest request = LoginRequest.builder()
                .emailOrUsername("testuser")
                .password("WrongPassword")
                .build();

        when(userRepository.findByEmailOrUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", "hashedPass")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, "agent", "ip"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
