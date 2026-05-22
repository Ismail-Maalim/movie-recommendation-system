package com.recommend.movie.service;

import com.recommend.movie.model.User;
import com.recommend.movie.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${oracle.apex.api.url}")
    private String apexApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUserById(Long id) {
        // 1. Try fetching from Oracle ORDS API
        try {
            String url = apexApiUrl + "/users/" + id;
            System.out.println("Calling Oracle APEX REST API (GetUserById): " + url);
            ApexUserQueryResponse response = restTemplate.getForObject(url, ApexUserQueryResponse.class);
            if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
                ApexUserItem item = response.getItems().get(0);
                
                // Fetch user preferences too
                List<String> prefs = fetchPreferredGenresFromApex(id);
                
                // Update local H2 cache
                User user = new User(item.getUsername(), item.getEmail(), "", prefs);
                user.setId(item.getId());
                
                Optional<User> localUserOpt = userRepository.findById(id);
                if (localUserOpt.isPresent()) {
                    User localUser = localUserOpt.get();
                    localUser.setUsername(item.getUsername());
                    localUser.setEmail(item.getEmail());
                    localUser.setPreferredGenres(prefs);
                    userRepository.save(localUser);
                } else {
                    userRepository.save(user);
                }
                return Optional.of(user);
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch user from Oracle APEX: " + e.getMessage());
        }

        // Fallback to H2
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User registerUser(String username, String email, String password, List<String> preferredGenres) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        Long userId = null;
        // 1. Try registering user on Oracle APEX ORDS
        try {
            String url = apexApiUrl + "/users";
            System.out.println("Calling Oracle APEX REST API (RegisterUser): " + url);
            
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("email", email);
            payload.put("password", password);
            
            ApexRegisterResponse response = restTemplate.postForObject(url, payload, ApexRegisterResponse.class);
            if (response != null && response.getUserId() != null) {
                userId = response.getUserId();
                System.out.println("Successfully registered user on Oracle APEX. ID: " + userId);
                
                // Submit preferred genres
                if (preferredGenres != null && !preferredGenres.isEmpty()) {
                    updatePreferencesOnApex(userId, preferredGenres);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to register user on Oracle APEX: " + e.getMessage());
            System.err.println("Registering user in local H2 database instead...");
        }

        // Save locally to H2 (as cache / primary depending on above success)
        User user = new User(username, email, passwordEncoder.encode(password), preferredGenres);
        if (userId != null) {
            user.setId(userId); // Use the same ID as Oracle APEX
        }
        return userRepository.save(user);
    }

    public Optional<User> login(String username, String password) {
        // 1. Try login on Oracle APEX ORDS
        try {
            String url = apexApiUrl + "/users/login";
            System.out.println("Calling Oracle APEX REST API (Login): " + url);
            
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("password", password);
            
            ApexUserQueryResponse response = restTemplate.postForObject(url, payload, ApexUserQueryResponse.class);
            if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
                ApexUserItem item = response.getItems().get(0);
                System.out.println("Successful login on Oracle APEX for user: " + username);
                
                List<String> prefs = fetchPreferredGenresFromApex(item.getId());
                
                // Sync to H2 cache
                User user = new User(item.getUsername(), item.getEmail(), passwordEncoder.encode(password), prefs);
                user.setId(item.getId());
                
                // Save/update locally
                userRepository.save(user);
                return Optional.of(user);
            }
        } catch (Exception e) {
            System.err.println("Failed to login via Oracle APEX: " + e.getMessage());
            System.err.println("Falling back to local H2 login...");
        }

        // Fallback: Local H2 login
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()));
    }

    @Transactional
    public User updatePreferredGenres(Long userId, List<String> genres) {
        // 1. Try updating on Oracle APEX ORDS
        try {
            updatePreferencesOnApex(userId, genres);
            System.out.println("Successfully updated user preferences on Oracle APEX for user: " + userId);
        } catch (Exception e) {
            System.err.println("Failed to update user preferences on Oracle APEX: " + e.getMessage());
        }

        // Update locally in H2
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPreferredGenres(genres);
        return userRepository.save(user);
    }

    // Helper method to update preferences on Oracle APEX
    private void updatePreferencesOnApex(Long userId, List<String> genres) {
        String url = apexApiUrl + "/users/" + userId + "/preferences";
        String genresCommaSeparated = String.join(",", genres);
        
        Map<String, String> payload = new HashMap<>();
        payload.put("genres", genresCommaSeparated);
        
        restTemplate.put(url, payload);
    }

    // Helper method to fetch preferences from Oracle APEX
    private List<String> fetchPreferredGenresFromApex(Long userId) {
        try {
            String url = apexApiUrl + "/users/" + userId + "/preferences";
            ApexPreferencesResponse response = restTemplate.getForObject(url, ApexPreferencesResponse.class);
            if (response != null && response.getItems() != null) {
                return response.getItems().stream()
                        .map(ApexPreferenceItem::getPreferredGenre)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch preferences from Oracle APEX: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // ORDS API Mapping Classes
    public static class ApexUserQueryResponse {
        @JsonProperty("items")
        private List<ApexUserItem> items;

        public List<ApexUserItem> getItems() { return items; }
        public void setItems(List<ApexUserItem> items) { this.items = items; }
    }

    public static class ApexUserItem {
        @JsonProperty("id")
        private Long id;
        @JsonProperty("username")
        private String username;
        @JsonProperty("email")
        private String email;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ApexRegisterResponse {
        @JsonProperty("user_id")
        private Long userId;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class ApexPreferencesResponse {
        @JsonProperty("items")
        private List<ApexPreferenceItem> items;

        public List<ApexPreferenceItem> getItems() { return items; }
        public void setItems(List<ApexPreferenceItem> items) { this.items = items; }
    }

    public static class ApexPreferenceItem {
        @JsonProperty("preferred_genre")
        private String preferredGenre;

        public String getPreferredGenre() { return preferredGenre; }
        public void setPreferredGenre(String preferredGenre) { this.preferredGenre = preferredGenre; }
    }
}
