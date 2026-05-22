package com.recommend.movie.service;

import com.recommend.movie.dto.MovieRecommendation;
import com.recommend.movie.model.Movie;
import com.recommend.movie.model.Rating;
import com.recommend.movie.model.User;
import com.recommend.movie.repository.MovieRepository;
import com.recommend.movie.repository.RatingRepository;
import com.recommend.movie.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class RecommendationService {

    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;

    @Value("${oracle.apex.api.url}")
    private String apexApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public RecommendationService(MovieRepository movieRepository, RatingRepository ratingRepository, UserRepository userRepository) {
        this.movieRepository = movieRepository;
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
    }

    /**
     * Gets hybrid recommendations for a user.
     */
    public List<MovieRecommendation> getRecommendations(Long userId, int limit) {
        // 1. Try fetching recommendations from Oracle APEX ORDS API
        try {
            String url = apexApiUrl + "/recommend/" + userId;
            System.out.println("Calling Oracle APEX REST API: " + url);
            ApexRecommendationResponse response = restTemplate.getForObject(url, ApexRecommendationResponse.class);
            if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
                List<MovieRecommendation> recommendations = new ArrayList<>();
                for (ApexRecommendationItem item : response.getItems()) {
                    Optional<Movie> movieOpt = movieRepository.findById(item.getId());
                    if (movieOpt.isPresent()) {
                        Movie m = movieOpt.get();
                        double rawScore = item.getRecommendationScore() != null ? item.getRecommendationScore() : 0.0;
                        
                        // Map the raw recommendation score to a normalized score
                        // Base normalize: divide score by 15.0 max expectation, minimum 50% match
                        double normalizedScore = Math.min(1.0, rawScore / 15.0);
                        int matchPercentage = (int) Math.min(99, Math.round(normalizedScore * 100));
                        if (matchPercentage < 50) {
                            matchPercentage = 50 + (int)(rawScore * 3) % 45;
                        }
                        
                        recommendations.add(new MovieRecommendation(
                            m,
                            "HYBRID (ORACLE APEX)",
                            normalizedScore,
                            matchPercentage,
                            "Database-driven recommendation computed directly on your Oracle APEX instance."
                        ));
                    }
                }
                if (!recommendations.isEmpty()) {
                    System.out.println("Successfully retrieved " + recommendations.size() + " recommendations from Oracle APEX.");
                    return recommendations.stream().limit(limit).collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch recommendations from Oracle APEX: " + e.getMessage());
            System.err.println("Falling back to local Java hybrid recommendation engine...");
        }

        // Fallback: Local Java hybrid recommendation engine
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Rating> userRatings = ratingRepository.findByUserId(userId);
        List<Movie> allMovies = movieRepository.findAll();
        Set<Long> ratedMovieIds = userRatings.stream()
                .map(Rating::getMovieId)
                .collect(Collectors.toSet());

        // 1. If user has no ratings and no preferences, recommend popular movies
        if (userRatings.isEmpty() && (user.getPreferredGenres() == null || user.getPreferredGenres().isEmpty())) {
            return getPopularRecommendations(allMovies, ratedMovieIds, limit);
        }

        // 2. Fetch Collaborative Filtering (CF) and Content-Based Filtering (CB) candidates
        List<MovieRecommendation> cfRecs = getCollaborativeRecommendations(userId, userRatings, allMovies, ratedMovieIds);
        List<MovieRecommendation> cbRecs = getContentRecommendations(user, userRatings, allMovies, ratedMovieIds);

        // 3. Merge them into a Hybrid recommendation list
        Map<Long, MovieRecommendation> hybridMap = new HashMap<>();

        // Add CB recommendations first
        for (MovieRecommendation cb : cbRecs) {
            hybridMap.put(cb.getMovie().getId(), cb);
        }

        // Blend in CF recommendations, combining scores
        for (MovieRecommendation cf : cfRecs) {
            Long movieId = cf.getMovie().getId();
            if (hybridMap.containsKey(movieId)) {
                MovieRecommendation cb = hybridMap.get(movieId);
                // Combine scores: 60% Collaborative, 40% Content-Based
                double combinedScore = 0.6 * cf.getScore() + 0.4 * cb.getScore();
                int combinedMatch = (int) Math.min(99, Math.round(combinedScore * 100));
                
                cb.setRecommendationType("HYBRID");
                cb.setScore(combinedScore);
                cb.setMatchPercentage(combinedMatch);
                cb.setReason("High match with your preferred genres & recommended by users with similar taste.");
            } else {
                // If only CF recommended it
                hybridMap.put(movieId, cf);
            }
        }

        // Sort by score descending
        List<MovieRecommendation> sortedRecs = new ArrayList<>(hybridMap.values());
        sortedRecs.sort(Comparator.comparingDouble(MovieRecommendation::getScore).reversed());

        // If list is still too short, pad with popular movies
        if (sortedRecs.size() < limit) {
            Set<Long> recommendedIds = sortedRecs.stream().map(r -> r.getMovie().getId()).collect(Collectors.toSet());
            Set<Long> excludedIds = new HashSet<>(ratedMovieIds);
            excludedIds.addAll(recommendedIds);
            
            List<MovieRecommendation> populars = getPopularRecommendations(allMovies, excludedIds, limit - sortedRecs.size());
            sortedRecs.addAll(populars);
        }

        return sortedRecs.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * User-User Collaborative Filtering using Cosine Similarity.
     */
    public List<MovieRecommendation> getCollaborativeRecommendations(Long targetUserId, List<Rating> targetUserRatings, 
                                                                    List<Movie> allMovies, Set<Long> ratedMovieIds) {
        List<MovieRecommendation> recommendations = new ArrayList<>();
        if (targetUserRatings.isEmpty()) {
            return recommendations;
        }

        // Map of MovieId -> Score for the target user
        Map<Long, Integer> targetVector = targetUserRatings.stream()
                .collect(Collectors.toMap(Rating::getMovieId, Rating::getScore));

        // Get all ratings in system
        List<Rating> allRatings = ratingRepository.findAll();
        
        // Group ratings by User
        Map<Long, Map<Long, Integer>> userVectors = new HashMap<>();
        for (Rating r : allRatings) {
            userVectors.computeIfAbsent(r.getUserId(), k -> new HashMap<>()).put(r.getMovieId(), r.getScore());
        }

        // Calculate Cosine Similarity with all other users
        Map<Long, Double> userSimilarities = new HashMap<>();
        double targetNorm = calculateVectorNorm(targetVector.values());

        if (targetNorm == 0) return recommendations;

        for (Map.Entry<Long, Map<Long, Integer>> entry : userVectors.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId.equals(targetUserId)) continue;

            Map<Long, Integer> otherVector = entry.getValue();
            double dotProduct = 0.0;
            for (Map.Entry<Long, Integer> targetVal : targetVector.entrySet()) {
                Integer otherVal = otherVector.get(targetVal.getKey());
                if (otherVal != null) {
                    dotProduct += targetVal.getValue() * otherVal;
                }
            }

            double otherNorm = calculateVectorNorm(otherVector.values());
            if (otherNorm == 0) continue;

            double similarity = dotProduct / (targetNorm * otherNorm);
            if (similarity > 0.1) { // Only consider positively correlated users
                userSimilarities.put(otherUserId, similarity);
            }
        }

        if (userSimilarities.isEmpty()) return recommendations;

        // Predict scores for unrated movies
        Map<Long, Movie> movieMap = allMovies.stream().collect(Collectors.toMap(Movie::getId, m -> m));
        Map<Long, Double> weightedSumRatings = new HashMap<>();
        Map<Long, Double> similaritySum = new HashMap<>();

        for (Map.Entry<Long, Double> simEntry : userSimilarities.entrySet()) {
            Long otherUserId = simEntry.getKey();
            double sim = simEntry.getValue();
            Map<Long, Integer> otherRatings = userVectors.get(otherUserId);

            for (Map.Entry<Long, Integer> ratingEntry : otherRatings.entrySet()) {
                Long movieId = ratingEntry.getKey();
                if (ratedMovieIds.contains(movieId)) continue; // skip already rated

                weightedSumRatings.put(movieId, weightedSumRatings.getOrDefault(movieId, 0.0) + (sim * ratingEntry.getValue()));
                similaritySum.put(movieId, similaritySum.getOrDefault(movieId, 0.0) + Math.abs(sim));
            }
        }

        for (Long movieId : weightedSumRatings.keySet()) {
            double sumSim = similaritySum.get(movieId);
            if (sumSim > 0) {
                double predictedRating = weightedSumRatings.get(movieId) / sumSim;
                // Normalize score to 0.0 - 1.0 (ratings range from 1 to 5)
                double normalizedScore = predictedRating / 5.0;
                int matchPercentage = (int) Math.min(99, Math.round(normalizedScore * 100));

                Movie movie = movieMap.get(movieId);
                if (movie != null) {
                    recommendations.add(new MovieRecommendation(
                            movie,
                            "COLLABORATIVE",
                            normalizedScore,
                            matchPercentage,
                            "Highly recommended by users with taste profiles matching yours."
                    ));
                }
            }
        }

        recommendations.sort(Comparator.comparingDouble(MovieRecommendation::getScore).reversed());
        return recommendations;
    }

    /**
     * Content-Based Filtering based on Movie Genres.
     */
    public List<MovieRecommendation> getContentRecommendations(User user, List<Rating> userRatings, 
                                                              List<Movie> allMovies, Set<Long> ratedMovieIds) {
        List<MovieRecommendation> recommendations = new ArrayList<>();

        // Create Genre Profile from highly rated movies (rating >= 4)
        Map<String, Double> genreWeights = new HashMap<>();
        
        List<Rating> positiveRatings = userRatings.stream()
                .filter(r -> r.getScore() >= 4)
                .collect(Collectors.toList());

        if (!positiveRatings.isEmpty()) {
            Map<Long, Movie> movieMap = allMovies.stream().collect(Collectors.toMap(Movie::getId, m -> m));
            for (Rating r : positiveRatings) {
                Movie movie = movieMap.get(r.getMovieId());
                if (movie != null) {
                    double weight = r.getScore() == 5 ? 1.0 : 0.7; // Higher weight for 5 stars
                    for (String genre : movie.getGenres()) {
                        genreWeights.put(genre.toLowerCase(), genreWeights.getOrDefault(genre.toLowerCase(), 0.0) + weight);
                    }
                }
            }
        }

        // Add explicit preferred genres to profile (give them a baseline weight)
        if (user.getPreferredGenres() != null) {
            for (String prefGenre : user.getPreferredGenres()) {
                genreWeights.put(prefGenre.toLowerCase(), genreWeights.getOrDefault(prefGenre.toLowerCase(), 0.0) + 1.5);
            }
        }

        if (genreWeights.isEmpty()) {
            return recommendations;
        }

        // Normalize genreWeights to avoid runaways
        double maxWeight = Collections.max(genreWeights.values());
        for (Map.Entry<String, Double> entry : genreWeights.entrySet()) {
            genreWeights.put(entry.getKey(), entry.getValue() / maxWeight);
        }

        // Score all unrated movies
        for (Movie m : allMovies) {
            if (ratedMovieIds.contains(m.getId())) continue;

            double movieScore = 0.0;
            int matchingGenreCount = 0;
            String matchingGenreSample = "";

            for (String genre : m.getGenres()) {
                String genreLower = genre.toLowerCase();
                if (genreWeights.containsKey(genreLower)) {
                    movieScore += genreWeights.get(genreLower);
                    matchingGenreCount++;
                    if (matchingGenreSample.isEmpty()) {
                        matchingGenreSample = genre;
                    }
                }
            }

            if (movieScore > 0) {
                // Incorporate overall movie rating slightly into the score
                double ratingBoost = (m.getAverageRating() / 5.0) * 0.2; // up to 20% boost
                double finalScore = (movieScore / m.getGenres().size()) * 0.8 + ratingBoost;
                finalScore = Math.min(1.0, finalScore);
                
                int matchPercentage = (int) Math.min(99, Math.round(finalScore * 100));

                String reason = "Because you like " + matchingGenreSample + " movies.";
                if (matchingGenreCount > 1) {
                    reason = "Matches multiple of your preferred genres: " + matchingGenreSample + ", etc.";
                }

                recommendations.add(new MovieRecommendation(
                        m,
                        "CONTENT_BASED",
                        finalScore,
                        matchPercentage,
                        reason
                ));
            }
        }

        recommendations.sort(Comparator.comparingDouble(MovieRecommendation::getScore).reversed());
        return recommendations;
    }

    /**
     * Fallback to popular recommendations (highest rated).
     */
    private List<MovieRecommendation> getPopularRecommendations(List<Movie> allMovies, Set<Long> excludedMovieIds, int limit) {
        List<Movie> popularMovies = allMovies.stream()
                .filter(m -> !excludedMovieIds.contains(m.getId()))
                .sorted(Comparator.comparingDouble(Movie::getAverageRating).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        List<MovieRecommendation> recommendations = new ArrayList<>();
        for (Movie m : popularMovies) {
            // Map rating score to match percentage: e.g. 4.5 average rating -> 90%
            double score = m.getAverageRating() / 5.0;
            if (score == 0) score = 0.7; // default standard score for unrated popular movies
            int matchPercentage = (int) Math.round(score * 100);

            recommendations.add(new MovieRecommendation(
                    m,
                    "POPULAR",
                    score,
                    matchPercentage,
                    "Trending and highly rated by the community."
            ));
        }
        return recommendations;
    }

    private double calculateVectorNorm(Collection<Integer> values) {
        double sumOfSquares = 0.0;
        for (Integer val : values) {
            sumOfSquares += val * val;
        }
        return Math.sqrt(sumOfSquares);
    }

    // Helper classes for Oracle APEX REST API integration
    public static class ApexRecommendationResponse {
        @JsonProperty("items")
        private List<ApexRecommendationItem> items;

        public List<ApexRecommendationItem> getItems() { return items; }
        public void setItems(List<ApexRecommendationItem> items) { this.items = items; }
    }

    public static class ApexRecommendationItem {
        @JsonProperty("id")
        private Long id;
        @JsonProperty("title")
        private String title;
        @JsonProperty("recommendation_score")
        private Double recommendationScore;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Double getRecommendationScore() { return recommendationScore; }
        public void setRecommendationScore(Double recommendationScore) { this.recommendationScore = recommendationScore; }
    }
}
