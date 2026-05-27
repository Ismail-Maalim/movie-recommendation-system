package com.recommend.movie.service;

import com.recommend.movie.model.User;
import com.recommend.movie.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${oracle.apex.api.url}")
    private String apexApiUrl;

    @Value("${oracle.apex.fallback-to-local:true}")
    private boolean fallbackToLocal;

    public UserService(UserRepository userRepository,
                       @org.springframework.beans.factory.annotation.Qualifier("oracleRestTemplate") RestTemplate restTemplate,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.emailService = emailService;
    }

    public Optional<User> getUserById(Long id) {
        // 1. Try fetching from Oracle ORDS API
        // Map local ID to Oracle User ID
        Long oracleId = id;
        Optional<User> localUserOpt = userRepository.findById(id);
        if (localUserOpt.isPresent()) {
            User localUser = localUserOpt.get();
            if (localUser.getOracleUserId() != null) {
                oracleId = localUser.getOracleUserId();
            }
        }
        try {
            String url = apexApiUrl + "/users/" + oracleId;
            log.info("Calling Oracle APEX REST API (GetUserById): {}", url);
            ApexUserQueryResponse response = restTemplate.getForObject(url, ApexUserQueryResponse.class);
            if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
                ApexUserItem item = response.getItems().get(0);
                
                // Fetch user preferences too
                List<String> prefs = fetchPreferredGenresFromApex(oracleId);
                
                // Update local H2 cache
                User user;
                if (localUserOpt.isPresent()) {
                    User localUser = localUserOpt.get();
                    localUser.setUsername(item.getUsername());
                    localUser.setEmail(item.getEmail());
                    localUser.setPreferredGenres(prefs);
                    localUser.setOracleUserId(item.getId());
                    if (item.getAvatar() != null) localUser.setAvatar(item.getAvatar());
                    if (item.getOauthProvider() != null) localUser.setOauthProvider(item.getOauthProvider());
                    if (item.getEmailVerified() != null) localUser.setEmailVerified(item.getEmailVerified() == 1);
                    user = userRepository.save(localUser);
                } else {
                    User newUser = new User(item.getUsername(), item.getEmail(), "", prefs);
                    newUser.setId(item.getId());
                    newUser.setOracleUserId(item.getId());
                    if (item.getAvatar() != null) newUser.setAvatar(item.getAvatar());
                    if (item.getOauthProvider() != null) newUser.setOauthProvider(item.getOauthProvider());
                    if (item.getEmailVerified() != null) newUser.setEmailVerified(item.getEmailVerified() == 1);
                    user = userRepository.save(newUser);
                }
                return Optional.of(user);
            }
        } catch (Exception e) {
            log.error("Failed to fetch user from Oracle APEX: {}", e.getMessage());
            if (!fallbackToLocal) {
                throw new RuntimeException("Failed to fetch user from Oracle APEX and fallback to local is disabled.", e);
            }
        }

        // Fallback to H2
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User registerUser(String username, String email, String password, List<String> preferredGenres) {
        if (!isStrongPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        Long userId = null;
        Exception registerException = null;
        // 1. Try registering user on Oracle APEX ORDS (routing to /register)
        try {
            String url = apexApiUrl + "/register";
            log.info("Calling Oracle APEX REST API (RegisterUser): {}", url);
            
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("email", email);
            payload.put("password", password);
            
            ApexRegisterResponse response = restTemplate.postForObject(url, payload, ApexRegisterResponse.class);
            if (response != null && response.getUserId() != null) {
                Long candidateId = response.getUserId();
                log.info("Successfully registered user on Oracle APEX. ID: {}", candidateId);
                
                // Validate registration immediately
                validateOracleRegistration(candidateId);
                
                userId = candidateId;
                
                // Submit preferred genres
                if (preferredGenres != null && !preferredGenres.isEmpty()) {
                    updatePreferencesOnApex(userId, preferredGenres);
                }
            } else {
                registerException = new RuntimeException("Empty registration response or missing user ID from Oracle APEX");
            }
        } catch (Exception e) {
            log.error("Failed to register user on Oracle APEX: {}", e.getMessage());
            registerException = e;
        }

        if (userId == null) {
            if (!fallbackToLocal) {
                throw new RuntimeException("Registration failed on Oracle APEX and fallback to local is disabled.", registerException);
            }
            log.warn("Registering user in local H2 database instead...");
            userId = userRepository.findMaxId().orElse(5L) + 1;
        }

        // Save locally to H2
        User user = new User(username, email, passwordEncoder.encode(password), preferredGenres);
        user.setId(userId);
        if (registerException == null) {
            user.setOracleUserId(userId);
        }
        User savedUser = userRepository.save(user);
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername());
        return savedUser;
    }

    public Optional<User> login(String username, String password) {
        // 1. Try login on Oracle APEX ORDS
        Exception loginException = null;
        try {
            String url = apexApiUrl + "/login";
            log.info("Calling Oracle APEX REST API (Login): {}", url);
            
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("password", password);
            
            // Deserialize flat ApexUserItem response directly from /login
            ApexUserItem item = restTemplate.postForObject(url, payload, ApexUserItem.class);
            if (item != null && item.getId() != null) {
                log.info("Successful login on Oracle APEX for user: {}", username);
                
                List<String> prefs = fetchPreferredGenresFromApex(item.getId());
                
                // Sync to H2 cache
                Optional<User> localUserOpt = userRepository.findById(item.getId());
                User user;
                if (localUserOpt.isPresent()) {
                    User localUser = localUserOpt.get();
                    localUser.setUsername(item.getUsername());
                    localUser.setEmail(item.getEmail());
                    localUser.setPassword(passwordEncoder.encode(password));
                    localUser.setPreferredGenres(prefs);
                    localUser.setOracleUserId(item.getId());
                    if (item.getAvatar() != null) localUser.setAvatar(item.getAvatar());
                    if (item.getOauthProvider() != null) localUser.setOauthProvider(item.getOauthProvider());
                    if (item.getEmailVerified() != null) localUser.setEmailVerified(item.getEmailVerified() == 1);
                    user = userRepository.save(localUser);
                } else {
                    User newUser = new User(item.getUsername(), item.getEmail(), passwordEncoder.encode(password), prefs);
                    newUser.setId(item.getId());
                    newUser.setOracleUserId(item.getId());
                    if (item.getAvatar() != null) newUser.setAvatar(item.getAvatar());
                    if (item.getOauthProvider() != null) newUser.setOauthProvider(item.getOauthProvider());
                    if (item.getEmailVerified() != null) newUser.setEmailVerified(item.getEmailVerified() == 1);
                    user = userRepository.save(newUser);
                }
                return Optional.of(user);
            } else {
                loginException = new RuntimeException("Empty login response or missing user ID from Oracle APEX");
            }
        } catch (Exception e) {
            log.error("Failed to login via Oracle APEX: {}", e.getMessage());
            loginException = e;
        }

        if (!fallbackToLocal) {
            if (loginException != null) {
                throw new RuntimeException("Failed to login via Oracle APEX and fallback to local is disabled.", loginException);
            } else {
                return Optional.empty();
            }
        }

        log.warn("Falling back to local H2 login...");
        // Fallback: Local H2 login
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()));
    }

    @Transactional
    public User updatePreferredGenres(Long userId, List<String> genres) {
        // Map local ID to Oracle User ID
        Long oracleUserId = userId;
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent() && userOpt.get().getOracleUserId() != null) {
            oracleUserId = userOpt.get().getOracleUserId();
        }

        // 1. Try updating on Oracle APEX ORDS
        try {
            updatePreferencesOnApex(oracleUserId, genres);
            log.info("Successfully updated user preferences on Oracle APEX for user: {}", oracleUserId);
        } catch (Exception e) {
            log.error("Failed to update user preferences on Oracle APEX: {}", e.getMessage());
            if (!fallbackToLocal) {
                throw new RuntimeException("Failed to update user preferences on Oracle APEX and fallback to local is disabled.", e);
            }
        }

        // Update locally in H2
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPreferredGenres(genres);
        return userRepository.save(user);
    }

    @Transactional
    public User registerOrLoginOAuth(String email, String username, String provider, String avatar) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(username);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setOauthProvider(provider);
            user.setEmailVerified(true);
            if (avatar != null && !avatar.trim().isEmpty()) {
                user.setAvatar(avatar);
            }
            userRepository.save(user);
            syncOAuthUserToApex(user);
            return user;
        }

        String rawPassword = UUID.randomUUID().toString() + "aA1!";
        Long userId = null;
        Exception registerException = null;

        try {
            String url = apexApiUrl + "/register";
            log.info("Registering OAuth user on Oracle APEX: {}", url);
            
            Map<String, String> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("email", email);
            payload.put("password", rawPassword);
            
            ApexRegisterResponse response = restTemplate.postForObject(url, payload, ApexRegisterResponse.class);
            if (response != null && response.getUserId() != null) {
                userId = response.getUserId();
                log.info("Successfully registered OAuth user on Oracle APEX. ID: {}", userId);
                validateOracleRegistration(userId);
            } else {
                registerException = new RuntimeException("Missing userId in Oracle APEX OAuth register response");
            }
        } catch (Exception e) {
            log.error("Failed to register OAuth user on Oracle APEX: {}", e.getMessage());
            registerException = e;
        }

        if (userId == null) {
            if (!fallbackToLocal) {
                throw new RuntimeException("OAuth registration failed on Oracle APEX and fallback to local is disabled.", registerException);
            }
            userId = userRepository.findMaxId().orElse(5L) + 1;
        }

        User user = new User(username, email, passwordEncoder.encode(rawPassword), Arrays.asList("Sci-Fi", "Action"));
        user.setId(userId);
        user.setOauthProvider(provider);
        user.setEmailVerified(true);
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        if (registerException == null) {
            user.setOracleUserId(userId);
        }
        
        User savedUser = userRepository.save(user);
        syncOAuthUserToApex(savedUser);
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername());
        
        return savedUser;
    }

    private void syncOAuthUserToApex(User user) {
        if (user.getOracleUserId() == null) return;
        try {
            String url = apexApiUrl + "/users/" + user.getOracleUserId();
            Map<String, Object> payload = new HashMap<>();
            payload.put("username", user.getUsername());
            payload.put("email", user.getEmail());
            payload.put("avatar", user.getAvatar());
            payload.put("email_verified", user.isEmailVerified() ? 1 : 0);
            payload.put("oauth_provider", user.getOauthProvider());
            
            restTemplate.put(url, payload);
            log.info("Successfully synced OAuth user columns to Oracle APEX for user ID: {}", user.getOracleUserId());
        } catch (Exception e) {
            log.error("Failed to sync OAuth user columns to Oracle APEX: {}", e.getMessage());
        }
    }

    @Transactional
    public User updateSettings(Long userId, String username, String email, String avatar) {
        User localUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Local User record missing"));
        
        localUser.setUsername(username);
        localUser.setEmail(email);
        localUser.setAvatar(avatar);
        User savedUser = userRepository.save(localUser);

        if (localUser.getOracleUserId() != null) {
            String url = apexApiUrl + "/users/" + localUser.getOracleUserId();
            Map<String, Object> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("email", email);
            payload.put("avatar", avatar);
            payload.put("email_verified", localUser.isEmailVerified() ? 1 : 0);
            payload.put("oauth_provider", localUser.getOauthProvider());

            try {
                restTemplate.put(url, payload);
                log.info("Successfully updated settings on Oracle APEX for user: {}", localUser.getOracleUserId());
            } catch (Exception e) {
                log.error("Failed to sync settings updates to Oracle APEX", e);
                if (!fallbackToLocal) {
                    throw new RuntimeException("Cloud synchronization boundary failure", e);
                }
            }
        }
        return savedUser;
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
            log.error("Failed to fetch preferences from Oracle APEX: {}", e.getMessage());
            if (!fallbackToLocal) {
                throw new RuntimeException("Failed to fetch preferences from Oracle APEX and fallback to local is disabled.", e);
            }
        }
        return new ArrayList<>();
    }

    private void validateOracleRegistration(Long oracleUserId) {
        String url = apexApiUrl + "/users/" + oracleUserId;
        log.info("Validating registration on Oracle APEX (GetUserById): {}", url);
        ApexUserQueryResponse response = restTemplate.getForObject(url, ApexUserQueryResponse.class);
        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            throw new RuntimeException("Validation failed: User registered but not found on Oracle APEX for ID: " + oracleUserId);
        }
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        String specialChars = "!@#$%^&*()-_=+[]{}|;:',.<>?/`~";

        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isUpperCase(ch)) {
                hasUpper = true;
            } else if (Character.isLowerCase(ch)) {
                hasLower = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (specialChars.indexOf(ch) >= 0) {
                hasSpecial = true;
            }
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
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
        @JsonProperty("avatar")
        private String avatar;
        @JsonProperty("email_verified")
        private Integer emailVerified;
        @JsonProperty("oauth_provider")
        private String oauthProvider;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public Integer getEmailVerified() { return emailVerified; }
        public void setEmailVerified(Integer emailVerified) { this.emailVerified = emailVerified; }
        public String getOauthProvider() { return oauthProvider; }
        public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }
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
