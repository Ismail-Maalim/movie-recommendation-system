package com.movies.recommendation;

import com.movies.recommendation.service.RecommendationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RecommendationServiceTest {

    private final RecommendationService recommendationService = new RecommendationService();

    @Test
    void returnsRecommendationsForKnownGenre() {
        assertFalse(recommendationService.recommendByGenre("Sci-Fi").isEmpty());
    }
}
