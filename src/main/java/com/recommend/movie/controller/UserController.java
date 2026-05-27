package com.recommend.movie.controller;

import com.recommend.movie.model.User;
import com.recommend.movie.service.UserService;
import com.recommend.movie.service.MovieService;
import com.recommend.movie.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final MovieService movieService;
    private final RecommendationService recommendationService;

    public UserController(UserService userService, MovieService movieService, RecommendationService recommendationService) {
        this.userService = userService;
        this.movieService = movieService;
        this.recommendationService = recommendationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getPreferredGenres()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userService.login(request.getUsername(), request.getPassword())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid username or password")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        return userService.getUserById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found")));
    }

    @PutMapping("/{id}/preferences")
    public ResponseEntity<?> updatePreferences(@PathVariable Long id, @RequestBody List<String> genres) {
        try {
            User user = userService.updatePreferredGenres(id, genres);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/oauth")
    public ResponseEntity<?> oauthLogin(@RequestBody OAuthRequest request) {
        try {
            User user = userService.registerOrLoginOAuth(
                    request.getEmail(),
                    request.getUsername(),
                    request.getProvider(),
                    request.getAvatar()
            );
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "OAuth authentication failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/settings")
    public ResponseEntity<?> updateSettings(@PathVariable Long id, @RequestBody SettingsRequest request) {
        try {
            User user = userService.updateSettings(
                    id,
                    request.getUsername(),
                    request.getEmail(),
                    request.getAvatar()
            );
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Settings update failed: " + e.getMessage()));
        }
    }

    // Inner DTO Classes for requests
    public static class LoginRequest {
        private String username;
        private String password;

        public LoginRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private List<String> preferredGenres;

        public RegisterRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public List<String> getPreferredGenres() { return preferredGenres; }
        public void setPreferredGenres(List<String> preferredGenres) { this.preferredGenres = preferredGenres; }
    }

    @PostMapping("/onboarding")
    public ResponseEntity<?> onboarding(@RequestBody OnboardingRequest request) {
        try {
            // 1. Update preferred genres
            User user = userService.updatePreferredGenres(request.getUserId(), request.getPreferredGenres());
            
            // 2. Process all ratings
            if (request.getRatings() != null) {
                for (Map.Entry<Long, Integer> entry : request.getRatings().entrySet()) {
                    movieService.rateMovie(request.getUserId(), entry.getKey(), entry.getValue());
                }
            }
            
            // 3. Evict cache in RecommendationService to trigger fresh generation
            recommendationService.evictCache(request.getUserId());
            
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    public static class OnboardingRequest {
        private Long userId;
        private List<String> preferredGenres;
        private Map<Long, Integer> ratings;

        public OnboardingRequest() {}

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public List<String> getPreferredGenres() { return preferredGenres; }
        public void setPreferredGenres(List<String> preferredGenres) { this.preferredGenres = preferredGenres; }
        public Map<Long, Integer> getRatings() { return ratings; }
        public void setRatings(Map<Long, Integer> ratings) { this.ratings = ratings; }
    }

    public static class OAuthRequest {
        private String email;
        private String username;
        private String provider;
        private String avatar;

        public OAuthRequest() {}

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
    }

    public static class SettingsRequest {
        private String username;
        private String email;
        private String avatar;

        public SettingsRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
    }
}
