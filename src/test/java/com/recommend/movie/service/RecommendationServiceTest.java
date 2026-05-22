package com.recommend.movie.service;

import com.recommend.movie.dto.MovieRecommendation;
import com.recommend.movie.model.Movie;
import com.recommend.movie.model.Rating;
import com.recommend.movie.model.User;
import com.recommend.movie.repository.MovieRepository;
import com.recommend.movie.repository.RatingRepository;
import com.recommend.movie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private UserRepository userRepository;

    private RecommendationService recommendationService;

    private User targetUser;
    private User similarUser;
    private Movie movieSciFi;
    private Movie movieDrama;
    private Movie movieAction;

    @BeforeEach
    public void setup() {
        recommendationService = new RecommendationService(movieRepository, ratingRepository, userRepository);

        // Define users
        targetUser = new User();
        targetUser.setId(1L);
        targetUser.setUsername("alice");
        targetUser.setPreferredGenres(Arrays.asList("Sci-Fi"));

        similarUser = new User();
        similarUser.setId(2L);
        similarUser.setUsername("charlie");
        similarUser.setPreferredGenres(Arrays.asList("Sci-Fi", "Action"));

        // Define movies
        movieSciFi = new Movie("Inception", "Dream heist", 2010, Arrays.asList("Sci-Fi"), "", "", "", "");
        movieSciFi.setId(101L);
        movieSciFi.setAverageRating(4.5);

        movieDrama = new Movie("The Godfather", "Mafia drama", 1972, Arrays.asList("Crime", "Drama"), "", "", "", "");
        movieDrama.setId(102L);
        movieDrama.setAverageRating(5.0);

        movieAction = new Movie("The Dark Knight", "Batman vs Joker", 2008, Arrays.asList("Action"), "", "", "", "");
        movieAction.setId(103L);
        movieAction.setAverageRating(4.8);
    }

    @Test
    public void testPopularFallbackForNewUser() {
        // Mock new user with no preferences and no ratings
        targetUser.setPreferredGenres(Collections.emptyList());
        when(userRepository.findById(1L)).thenReturn(Optional.of(targetUser));
        when(ratingRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        
        // Mock database movies
        when(movieRepository.findAll()).thenReturn(Arrays.asList(movieSciFi, movieDrama, movieAction));

        List<MovieRecommendation> recs = recommendationService.getRecommendations(1L, 5);

        assertNotNull(recs);
        assertEquals(3, recs.size());
        assertEquals("POPULAR", recs.get(0).getRecommendationType());
        // The highest average rating is Godfather (5.0), then Dark Knight (4.8), then Inception (4.5)
        assertEquals("The Godfather", recs.get(0).getMovie().getTitle());
    }

    @Test
    public void testContentBasedRecommendations() {
        // Target user Alice has rated movieSciFi (Inception) 5 stars
        List<Rating> ratings = Arrays.asList(new Rating(1L, 101L, 5));
        
        // Create an unrated Sci-Fi movie to match Alice's preference
        Movie movieSciFi2 = new Movie("Interstellar", "Space exploration", 2014, Arrays.asList("Sci-Fi"), "", "", "", "");
        movieSciFi2.setId(104L);
        movieSciFi2.setAverageRating(4.7);

        // When requesting hybrid recommendations, it should rank Sci-Fi (Inception rated highly + pref)
        // since Sci-Fi is in Alice's preferences and she highly rated it
        // We will assert the content-based algorithm behavior
        Set<Long> ratedIds = Set.of(101L);
        List<MovieRecommendation> cbRecs = recommendationService.getContentRecommendations(targetUser, ratings, 
                Arrays.asList(movieSciFi, movieDrama, movieAction, movieSciFi2), ratedIds);

        assertFalse(cbRecs.isEmpty());
        assertEquals("Interstellar", cbRecs.get(0).getMovie().getTitle());
        assertEquals("CONTENT_BASED", cbRecs.get(0).getRecommendationType());
        assertNotNull(cbRecs);
    }

    @Test
    public void testCollaborativeFilteringRecommendations() {
        // Setup rating vectors
        // Target user (Alice - 1L) rated:
        // Inception (101L) -> 5
        List<Rating> targetRatings = List.of(new Rating(1L, 101L, 5));
        
        // Similar user (Charlie - 2L) rated:
        // Inception (101L) -> 5
        // The Dark Knight (103L) -> 5
        List<Rating> allRatings = List.of(
                new Rating(1L, 101L, 5),
                new Rating(2L, 101L, 5),
                new Rating(2L, 103L, 5)
        );

        when(ratingRepository.findAll()).thenReturn(allRatings);

        Set<Long> ratedMovieIds = Set.of(101L);
        List<MovieRecommendation> cfRecs = recommendationService.getCollaborativeRecommendations(
                1L, targetRatings, List.of(movieSciFi, movieDrama, movieAction), ratedMovieIds
        );

        // Since Charlie is similar to Alice (both rated Inception 5), Charlie's highly rated movie
        // "The Dark Knight" (103L) should be recommended to Alice
        assertFalse(cfRecs.isEmpty());
        MovieRecommendation bestCF = cfRecs.get(0);
        assertEquals("COLLABORATIVE", bestCF.getRecommendationType());
        assertEquals("The Dark Knight", bestCF.getMovie().getTitle());
        assertTrue(bestCF.getMatchPercentage() > 80);
    }
}
