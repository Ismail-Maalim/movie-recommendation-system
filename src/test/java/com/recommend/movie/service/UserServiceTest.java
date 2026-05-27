package com.recommend.movie.service;

import com.recommend.movie.config.OracleSyncProperties;
import com.recommend.movie.model.User;
import com.recommend.movie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserRepository userRepository;
    private RestTemplate restTemplate;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        restTemplate = mock(RestTemplate.class);
        EmailService emailService = mock(EmailService.class);
        userService = new UserService(userRepository, restTemplate, emailService);
        
        // Inject APEX API URL property manually
        org.springframework.test.util.ReflectionTestUtils.setField(
                userService, 
                "apexApiUrl", 
                "http://mock-apex-api/ords/tr_a855_sql_s40/api"
        );
        
        // Inject fallbackToLocal manually
        org.springframework.test.util.ReflectionTestUtils.setField(
                userService, 
                "fallbackToLocal", 
                true
        );
    }

    @Test
    public void testRegisterUser_SuccessWithStrongPassword() {
        // Arrange
        String username = "StrongUser";
        String email = "strong@example.com";
        String strongPassword = "Password123!"; // 1 upper, 1 lower, 1 digit, 1 special, len >= 8
        
        UserService.ApexRegisterResponse mockResponse = new UserService.ApexRegisterResponse();
        mockResponse.setUserId(42L);

        when(restTemplate.postForObject(contains("/register"), any(), eq(UserService.ApexRegisterResponse.class)))
                .thenReturn(mockResponse);

        UserService.ApexUserQueryResponse mockUserQuery = new UserService.ApexUserQueryResponse();
        UserService.ApexUserItem mockItem = new UserService.ApexUserItem();
        mockItem.setId(42L);
        mockItem.setUsername(username);
        mockItem.setEmail(email);
        mockUserQuery.setItems(Collections.singletonList(mockItem));
        
        when(restTemplate.getForObject(contains("/users/42"), eq(UserService.ApexUserQueryResponse.class)))
                .thenReturn(mockUserQuery);

        User mockSavedUser = new User(username, email, "encoded-pwd", new ArrayList<>());
        mockSavedUser.setId(42L);
        when(userRepository.save(any(User.class))).thenReturn(mockSavedUser);

        // Act
        User result = userService.registerUser(username, email, strongPassword, new ArrayList<>());

        // Assert
        assertNotNull(result);
        assertEquals(42L, result.getId());
        assertEquals(username, result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testRegisterUser_WeakPasswordThrows() {
        // Arrange
        String username = "WeakUser";
        String email = "weak@example.com";
        String[] weakPasswords = {
                "short",         // Too short
                "Noupandnodig!", // Missing digit
                "NOLOWER123!",   // Missing lowercase
                "NOUPPERCASE1!", // Missing lowercase
                "NoSpecial123",  // Missing special character
        };

        // Act & Assert
        for (String weakPassword : weakPasswords) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.registerUser(username, email, weakPassword, new ArrayList<>())
            );
            assertTrue(exception.getMessage().contains("Password must be at least 8 characters long"));
        }
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testRegisterUser_StrictNoFallbackThrowsOnFailure() {
        // Arrange
        org.springframework.test.util.ReflectionTestUtils.setField(
                userService, 
                "fallbackToLocal", 
                false
        );
        String username = "StrongUser";
        String email = "strong@example.com";
        String strongPassword = "Password123!";
        
        when(restTemplate.postForObject(contains("/register"), any(), eq(UserService.ApexRegisterResponse.class)))
                .thenThrow(new org.springframework.web.client.RestClientException("Oracle offline"));

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.registerUser(username, email, strongPassword, new ArrayList<>())
        );
        assertTrue(exception.getMessage().contains("fallback to local is disabled"));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testLogin_Success() {
        // Arrange
        String username = "LoginUser";
        String password = "Password123!";
        
        UserService.ApexUserItem mockItem = new UserService.ApexUserItem();
        mockItem.setId(99L);
        mockItem.setUsername(username);
        mockItem.setEmail("login@example.com");

        when(restTemplate.postForObject(contains("/login"), any(), eq(UserService.ApexUserItem.class)))
                .thenReturn(mockItem);
        
        // Preferences endpoint mock
        UserService.ApexPreferencesResponse mockPrefs = new UserService.ApexPreferencesResponse();
        mockPrefs.setItems(new ArrayList<>());
        when(restTemplate.getForObject(contains("/preferences"), eq(UserService.ApexPreferencesResponse.class)))
                .thenReturn(mockPrefs);

        User mockUser = new User(username, "login@example.com", "encoded-pwd", new ArrayList<>());
        mockUser.setId(99L);
        mockUser.setOracleUserId(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        Optional<User> result = userService.login(username, password);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(99L, result.get().getId());
        assertEquals(username, result.get().getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testLogin_FallbackToH2() {
        // Arrange
        String username = "LocalUser";
        String password = "Password123!";
        
        // Oracle APEX fails
        when(restTemplate.postForObject(anyString(), any(), any()))
                .thenThrow(new org.springframework.web.client.RestClientException("Oracle offline"));

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User localUser = new User(username, "local@example.com", encoder.encode(password), new ArrayList<>());
        localUser.setId(5L);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(localUser));

        // Act
        Optional<User> result = userService.login(username, password);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(5L, result.get().getId());
        assertEquals(username, result.get().getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testLogin_StrictNoFallbackThrowsOnFailure() {
        // Arrange
        org.springframework.test.util.ReflectionTestUtils.setField(
                userService, 
                "fallbackToLocal", 
                false
        );
        String username = "LocalUser";
        String password = "Password123!";
        
        // Oracle APEX fails
        when(restTemplate.postForObject(anyString(), any(), any()))
                .thenThrow(new org.springframework.web.client.RestClientException("Oracle offline"));

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.login(username, password)
        );
        assertTrue(exception.getMessage().contains("fallback to local is disabled"));
    }
}
