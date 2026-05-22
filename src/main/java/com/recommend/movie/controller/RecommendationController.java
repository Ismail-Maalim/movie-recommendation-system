package com.recommend.movie.controller;

import com.recommend.movie.dto.MovieRecommendation;
import com.recommend.movie.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<List<MovieRecommendation>> getRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "8") int limit) {
        
        List<MovieRecommendation> recommendations = recommendationService.getRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/evaluate")
    public ResponseEntity<Map<String, Object>> evaluateRecommendations() {
        return ResponseEntity.ok(recommendationService.evaluateRecommendations());
    }
}
