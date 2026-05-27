package com.recommend.movie.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    // Temporary storage matching Email -> OTP code
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();

    @PostMapping("/oauth/initiate")
    public ResponseEntity<?> initiateOAuth(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Email is required."));
        }
        
        // Generate a cryptographically secure 6-digit verification code
        String secureOtp = String.format("%06d", new SecureRandom().nextInt(1000000));
        otpStorage.put(email, secureOtp);
        
        // Return the OTP in the payload so the frontend toast engine can catch it 
        // (Simulating a secure transactional email dispatch)
        return ResponseEntity.ok(Map.of(
            "message", "Verification code generated",
            "simulatedToastOtp", secureOtp 
        ));
    }

    @PostMapping("/oauth/verify")
    public ResponseEntity<?> verifyOAuth(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        
        if (email == null || code == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Email and code are required."));
        }
        
        if (otpStorage.containsKey(email) && otpStorage.get(email).equals(code.trim())) {
            otpStorage.remove(email); // Burn the token immediately upon use
            return ResponseEntity.ok(Map.of("verified", true));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token submission."));
    }
}
