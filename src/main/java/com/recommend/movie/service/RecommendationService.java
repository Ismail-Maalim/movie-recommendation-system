package com.recommend.movie.service;

import com.recommend.movie.dto.MovieRecommendation;
import com.recommend.movie.model.Movie;
import com.recommend.movie.model.Rating;
import com.recommend.movie.model.User;
import com.recommend.movie.repository.MovieRepository;
import com.recommend.movie.repository.RatingRepository;
import com.recommend.movie.repository.UserRepository;
import com.recommend.movie.repository.WatchlistItemRepository;
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
    private final WatchlistItemRepository watchlistRepository;

    @Value("${oracle.apex.api.url}")
    private String apexApiUrl;

    @Value("${recommendation.weight.user-cf:0.35}")
    private double weightUserCf;

    @Value("${recommendation.weight.item-cf:0.35}")
    private double weightItemCf;

    @Value("${recommendation.weight.content-based:0.30}")
    private double weightContentBased;

    @Value("${recommendation.decay.factor:0.98}")
    private double decayFactor;

    private final RestTemplate restTemplate = new RestTemplate();

    // Cache structure for Oracle APEX recommendations
    private static class CachedRecommendations {
        final List<MovieRecommendation> recommendations;
        final long cachedTime;
        CachedRecommendations(List<MovieRecommendation> recommendations) {
            this.recommendations = recommendations;
            this.cachedTime = System.currentTimeMillis();
        }
    }
    private final Map<Long, CachedRecommendations> apexCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes

    // Circuit breaker state
    private static long lastApexFailureTime = 0;
    private static final long APEX_COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

    public RecommendationService(MovieRepository movieRepository, RatingRepository ratingRepository, 
                                 UserRepository userRepository, WatchlistItemRepository watchlistRepository) {
        this.movieRepository = movieRepository;
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.watchlistRepository = watchlistRepository;
    }

    /**
     * Gets hybrid recommendations for a user.
     */
    public List<MovieRecommendation> getRecommendations(Long userId, int limit) {
        // A. Check cache for valid entries
        CachedRecommendations cached = apexCache.get(userId);
        if (cached != null && (System.currentTimeMillis() - cached.cachedTime) < CACHE_TTL_MS) {
            System.out.println("Serving recommendations from local cache for user: " + userId);
            return cached.recommendations.stream().limit(limit).collect(Collectors.toList());
        }

        // B. Check if Oracle APEX is on cooldown
        boolean apexOnCooldown = (System.currentTimeMillis() - lastApexFailureTime) < APEX_COOLDOWN_MS;
        if (!apexOnCooldown) {
            // Try fetching recommendations from Oracle APEX ORDS API
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
                        // Store in cache
                        apexCache.put(userId, new CachedRecommendations(recommendations));
                        return recommendations.stream().limit(limit).collect(Collectors.toList());
                    }
                }
            } catch (Exception e) {
                lastApexFailureTime = System.currentTimeMillis();
                System.err.println("Failed to fetch recommendations from Oracle APEX: " + e.getMessage());
                System.err.println("Enforcing a 5-minute APEX endpoint cooldown. Falling back to local Java hybrid recommendation engine...");
            }
        } else {
            System.out.println("Oracle APEX API is on cooldown due to recent failures. Bypassing directly to local Java engine.");
        }

        // Fallback: Local Java hybrid recommendation engine
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Rating> userRatings = ratingRepository.findByUserId(userId);
        List<com.recommend.movie.model.WatchlistItem> watchlistItems = watchlistRepository.findByUserId(userId);
        List<Movie> allMovies = movieRepository.findAll();
        
        Set<Long> ratedMovieIds = userRatings.stream()
                .map(Rating::getMovieId)
                .collect(Collectors.toSet());
        Set<Long> watchlistMovieIds = watchlistItems.stream()
                .map(com.recommend.movie.model.WatchlistItem::getMovieId)
                .collect(Collectors.toSet());

        Set<Long> excludedMovieIds = new HashSet<>(ratedMovieIds);
        excludedMovieIds.addAll(watchlistMovieIds);

        // 1. If user has no ratings, no watchlist items, and no preferences, recommend popular movies
        if (userRatings.isEmpty() && watchlistItems.isEmpty() && (user.getPreferredGenres() == null || user.getPreferredGenres().isEmpty())) {
            return getPopularRecommendations(allMovies, excludedMovieIds, limit);
        }

        // 2. Fetch User-User CF, Item-Item CF, and Content-Based candidates
        List<MovieRecommendation> userCfRecs = getCollaborativeRecommendations(userId, userRatings, allMovies, excludedMovieIds);
        List<MovieRecommendation> itemCfRecs = getItemCollaborativeRecommendations(userId, userRatings, allMovies, excludedMovieIds);
        List<MovieRecommendation> cbRecs = getContentRecommendations(user, userRatings, allMovies, excludedMovieIds);

        // 3. Merge them into a Hybrid recommendation list
        Map<Long, MovieRecommendation> hybridMap = new HashMap<>();

        // Add CB recommendations with weight Content-Based
        for (MovieRecommendation cb : cbRecs) {
            double weightedScore = cb.getScore() * weightContentBased;
            cb.setScore(weightedScore);
            cb.setMatchPercentage((int) Math.min(99, Math.round(weightedScore * 100)));
            hybridMap.put(cb.getMovie().getId(), cb);
        }

        // Blend in User-CF recommendations
        for (MovieRecommendation ucf : userCfRecs) {
            Long movieId = ucf.getMovie().getId();
            double weightedUserCfScore = ucf.getScore() * weightUserCf;
            if (hybridMap.containsKey(movieId)) {
                MovieRecommendation existing = hybridMap.get(movieId);
                double combinedScore = existing.getScore() + weightedUserCfScore;
                existing.setScore(combinedScore);
                existing.setRecommendationType("HYBRID");
                existing.setReason("Highly recommended based on your preferences and users with similar tastes.");
            } else {
                ucf.setScore(weightedUserCfScore);
                ucf.setRecommendationType("USER_COLLABORATIVE");
                hybridMap.put(movieId, ucf);
            }
        }

        // Blend in Item-CF recommendations
        for (MovieRecommendation icf : itemCfRecs) {
            Long movieId = icf.getMovie().getId();
            double weightedItemCfScore = icf.getScore() * weightItemCf;
            if (hybridMap.containsKey(movieId)) {
                MovieRecommendation existing = hybridMap.get(movieId);
                double combinedScore = existing.getScore() + weightedItemCfScore;
                existing.setScore(combinedScore);
                existing.setRecommendationType("HYBRID");
                existing.setReason("Matches your favorite genres, similar movies, and similar user tastes.");
            } else {
                icf.setScore(weightedItemCfScore);
                icf.setRecommendationType("ITEM_COLLABORATIVE");
                hybridMap.put(movieId, icf);
            }
        }

        // Normalize scores back to standard scale (0.0 to 1.0) since sum of weights = 1.0 (approximately)
        double totalWeight = weightContentBased + weightUserCf + weightItemCf;
        if (totalWeight > 0) {
            for (MovieRecommendation rec : hybridMap.values()) {
                double normalizedScore = rec.getScore() / totalWeight;
                rec.setScore(normalizedScore);
                rec.setMatchPercentage((int) Math.min(99, Math.round(normalizedScore * 100)));
            }
        }

        // Apply popularity/freshness decay based on release year
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (MovieRecommendation rec : hybridMap.values()) {
            Movie movie = rec.getMovie();
            double ageDecay = Math.pow(decayFactor, Math.max(0, currentYear - movie.getReleaseYear()));
            double updatedScore = rec.getScore() * ageDecay;
            rec.setScore(updatedScore);
            rec.setMatchPercentage((int) Math.min(99, Math.round(updatedScore * 100)));
        }

        // Sort by score descending
        List<MovieRecommendation> sortedRecs = new ArrayList<>(hybridMap.values());
        sortedRecs.sort(Comparator.comparingDouble(MovieRecommendation::getScore).reversed());

        // If list is still too short, pad with popular movies
        if (sortedRecs.size() < limit) {
            Set<Long> recommendedIds = sortedRecs.stream().map(r -> r.getMovie().getId()).collect(Collectors.toSet());
            Set<Long> newExcludedIds = new HashSet<>(excludedMovieIds);
            newExcludedIds.addAll(recommendedIds);
            
            List<MovieRecommendation> populars = getPopularRecommendations(allMovies, newExcludedIds, limit - sortedRecs.size());
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
                            "USER_COLLABORATIVE",
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
     * Item-Item Collaborative Filtering using Cosine Similarity.
     */
    public List<MovieRecommendation> getItemCollaborativeRecommendations(Long targetUserId, List<Rating> targetUserRatings, 
                                                                        List<Movie> allMovies, Set<Long> ratedMovieIds) {
        List<MovieRecommendation> recommendations = new ArrayList<>();
        if (targetUserRatings.isEmpty()) {
            return recommendations;
        }

        // Map: MovieId -> Rating value for target user
        Map<Long, Integer> targetUserRatingsMap = targetUserRatings.stream()
                .collect(Collectors.toMap(Rating::getMovieId, Rating::getScore));

        // Get all ratings in the system to calculate item vectors
        List<Rating> allRatings = ratingRepository.findAll();

        // Map: MovieId -> (UserId -> RatingScore)
        Map<Long, Map<Long, Integer>> itemVectors = new HashMap<>();
        for (Rating r : allRatings) {
            itemVectors.computeIfAbsent(r.getMovieId(), k -> new HashMap<>()).put(r.getUserId(), r.getScore());
        }

        // Compute movie map for easy lookup
        Map<Long, Movie> movieMap = allMovies.stream().collect(Collectors.toMap(Movie::getId, m -> m));

        // Predict rating for each movie NOT rated by target user
        for (Movie movie : allMovies) {
            Long targetMovieId = movie.getId();
            if (ratedMovieIds.contains(targetMovieId)) {
                continue;
            }

            Map<Long, Integer> targetItemVector = itemVectors.get(targetMovieId);
            if (targetItemVector == null || targetItemVector.isEmpty()) {
                continue;
            }

            double targetItemNorm = calculateVectorNorm(targetItemVector.values());
            if (targetItemNorm == 0) continue;

            double weightedSum = 0.0;
            double similaritySum = 0.0;

            // Compare with all movies the target user HAS rated
            for (Map.Entry<Long, Integer> ratedEntry : targetUserRatingsMap.entrySet()) {
                Long ratedMovieId = ratedEntry.getKey();
                Integer userScore = ratedEntry.getValue();

                Map<Long, Integer> ratedItemVector = itemVectors.get(ratedMovieId);
                if (ratedItemVector == null || ratedItemVector.isEmpty()) {
                    continue;
                }

                double ratedItemNorm = calculateVectorNorm(ratedItemVector.values());
                if (ratedItemNorm == 0) continue;

                // Compute cosine similarity between targetMovieId and ratedMovieId
                double dotProduct = 0.0;
                for (Map.Entry<Long, Integer> userRatingEntry : targetItemVector.entrySet()) {
                    Long userId = userRatingEntry.getKey();
                    Integer otherRating = ratedItemVector.get(userId);
                    if (otherRating != null) {
                        dotProduct += userRatingEntry.getValue() * otherRating;
                    }
                }

                double similarity = dotProduct / (targetItemNorm * ratedItemNorm);
                if (similarity > 0.1) { // Positive correlation filter
                    weightedSum += similarity * userScore;
                    similaritySum += similarity;
                }
            }

            if (similaritySum > 0) {
                double predictedRating = weightedSum / similaritySum;
                double normalizedScore = predictedRating / 5.0;
                int matchPercentage = (int) Math.min(99, Math.round(normalizedScore * 100));

                recommendations.add(new MovieRecommendation(
                        movie,
                        "ITEM_COLLABORATIVE",
                        normalizedScore,
                        matchPercentage,
                        "Similar to other movies you have rated positively."
                ));
            }
        }

        recommendations.sort(Comparator.comparingDouble(MovieRecommendation::getScore).reversed());
        return recommendations;
    }

    /**
     * Content-Based Filtering based on Movie Genres, Director, and Cast.
     */
    public List<MovieRecommendation> getContentRecommendations(User user, List<Rating> userRatings, 
                                                              List<Movie> allMovies, Set<Long> ratedMovieIds) {
        List<MovieRecommendation> recommendations = new ArrayList<>();

        // Create Genre Profile from highly rated movies (rating >= 4)
        Map<String, Double> genreWeights = new HashMap<>();
        Set<String> favDirectors = new HashSet<>();
        Set<String> favActors = new HashSet<>();

        List<Rating> positiveRatings = userRatings.stream()
                .filter(r -> r.getScore() >= 4)
                .collect(Collectors.toList());

        Map<Long, Movie> movieMap = allMovies.stream().collect(Collectors.toMap(Movie::getId, m -> m));

        if (!positiveRatings.isEmpty()) {
            for (Rating r : positiveRatings) {
                Movie movie = movieMap.get(r.getMovieId());
                if (movie != null) {
                    double weight = r.getScore() == 5 ? 1.0 : 0.7; // Higher weight for 5 stars
                    for (String genre : movie.getGenres()) {
                        genreWeights.put(genre.toLowerCase(), genreWeights.getOrDefault(genre.toLowerCase(), 0.0) + weight);
                    }
                    if (movie.getDirector() != null && !movie.getDirector().isBlank()) {
                        favDirectors.add(movie.getDirector().trim().toLowerCase());
                    }
                    if (movie.getCastMembers() != null && !movie.getCastMembers().isBlank()) {
                        for (String actor : movie.getCastMembers().split(",")) {
                            favActors.add(actor.trim().toLowerCase());
                        }
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

        // Add watchlist items to profile
        List<com.recommend.movie.model.WatchlistItem> watchlistItems = watchlistRepository.findByUserId(user.getId());
        if (watchlistItems != null && !watchlistItems.isEmpty()) {
            for (com.recommend.movie.model.WatchlistItem item : watchlistItems) {
                Movie movie = movieMap.get(item.getMovieId());
                if (movie != null) {
                    double weight = 1.2; // Watchlist items indicate high interest
                    for (String genre : movie.getGenres()) {
                        genreWeights.put(genre.toLowerCase(), genreWeights.getOrDefault(genre.toLowerCase(), 0.0) + weight);
                    }
                    if (movie.getDirector() != null && !movie.getDirector().isBlank()) {
                        favDirectors.add(movie.getDirector().trim().toLowerCase());
                    }
                    if (movie.getCastMembers() != null && !movie.getCastMembers().isBlank()) {
                        for (String actor : movie.getCastMembers().split(",")) {
                            favActors.add(actor.trim().toLowerCase());
                        }
                    }
                }
            }
        }

        if (genreWeights.isEmpty() && favDirectors.isEmpty() && favActors.isEmpty()) {
            return recommendations;
        }

        // Normalize genreWeights to avoid runaways
        double maxWeight = genreWeights.isEmpty() ? 1.0 : Collections.max(genreWeights.values());
        for (Map.Entry<String, Double> entry : genreWeights.entrySet()) {
            genreWeights.put(entry.getKey(), entry.getValue() / maxWeight);
        }

        // Score all unrated movies
        for (Movie m : allMovies) {
            if (ratedMovieIds.contains(m.getId())) continue;

            // 1. Compute Genre Match (60% weight)
            double genreScore = 0.0;
            int matchingGenreCount = 0;
            String matchingGenreSample = "";
            for (String genre : m.getGenres()) {
                String genreLower = genre.toLowerCase();
                if (genreWeights.containsKey(genreLower)) {
                    genreScore += genreWeights.get(genreLower);
                    matchingGenreCount++;
                    if (matchingGenreSample.isEmpty()) {
                        matchingGenreSample = genre;
                    }
                }
            }
            double finalGenreScore = m.getGenres().isEmpty() ? 0.0 : (genreScore / m.getGenres().size());

            // 2. Compute Director Match (20% weight)
            double directorMatch = 0.0;
            if (m.getDirector() != null && favDirectors.contains(m.getDirector().trim().toLowerCase())) {
                directorMatch = 1.0;
            }

            // 3. Compute Cast Match (20% weight)
            double castMatch = 0.0;
            String matchingActorSample = "";
            if (m.getCastMembers() != null && !m.getCastMembers().isBlank()) {
                String[] actors = m.getCastMembers().split(",");
                int matchCount = 0;
                for (String actor : actors) {
                    if (favActors.contains(actor.trim().toLowerCase())) {
                        matchCount++;
                        if (matchingActorSample.isEmpty()) {
                            matchingActorSample = actor.trim();
                        }
                    }
                }
                castMatch = actors.length == 0 ? 0.0 : (double) matchCount / actors.length;
            }

            // Combine Content Similarity signals
            double contentSimilarity = 0.6 * finalGenreScore + 0.2 * directorMatch + 0.2 * castMatch;

            if (contentSimilarity > 0) {
                // Incorporate overall movie rating slightly into the score (up to 20% boost)
                double ratingBoost = (m.getAverageRating() / 5.0) * 0.2;
                double finalScore = contentSimilarity * 0.8 + ratingBoost;
                finalScore = Math.min(1.0, finalScore);
                
                int matchPercentage = (int) Math.min(99, Math.round(finalScore * 100));

                String reason = "Matches your preference for " + matchingGenreSample + " movies.";
                if (directorMatch > 0) {
                    reason = "Directed by one of your favorite directors, " + m.getDirector() + ".";
                } else if (!matchingActorSample.isEmpty()) {
                    reason = "Features actors you enjoy, such as " + matchingActorSample + ".";
                } else if (matchingGenreCount > 1) {
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
            double score = m.getAverageRating() / 5.0;
            if (score == 0) score = 0.7; // default standard score
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

    /**
     * Runs an RMSE validation evaluation on a random 80/20 train-test split of user ratings.
     */
    public Map<String, Object> evaluateRecommendations() {
        List<Rating> allRatings = ratingRepository.findAll();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalRatings", allRatings.size());

        if (allRatings.size() < 5) {
            metrics.put("status", "INSUFFICIENT_DATA");
            metrics.put("message", "At least 5 ratings must exist in the database to run evaluation.");
            metrics.put("trainSize", 0);
            metrics.put("testSize", 0);
            metrics.put("userCfRmse", 0.0);
            metrics.put("itemCfRmse", 0.0);
            metrics.put("hybridRmse", 0.0);
            return metrics;
        }

        // Shuffle with fixed seed for deterministic evaluation runs
        List<Rating> shuffled = new ArrayList<>(allRatings);
        Collections.shuffle(shuffled, new Random(42));

        int trainSize = (int) Math.round(shuffled.size() * 0.8);
        if (trainSize == shuffled.size()) {
            trainSize = shuffled.size() - 1;
        }
        List<Rating> trainRatings = shuffled.subList(0, trainSize);
        List<Rating> testRatings = shuffled.subList(trainSize, shuffled.size());

        metrics.put("status", "SUCCESS");
        metrics.put("trainSize", trainRatings.size());
        metrics.put("testSize", testRatings.size());

        // Build User and Item vectors for the training partition
        Map<Long, Map<Long, Integer>> trainUserVectors = new HashMap<>();
        Map<Long, Map<Long, Integer>> trainItemVectors = new HashMap<>();
        for (Rating r : trainRatings) {
            trainUserVectors.computeIfAbsent(r.getUserId(), k -> new HashMap<>()).put(r.getMovieId(), r.getScore());
            trainItemVectors.computeIfAbsent(r.getMovieId(), k -> new HashMap<>()).put(r.getUserId(), r.getScore());
        }

        // Calculate User similarities in train partition
        Map<Long, Double> userNorms = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Integer>> entry : trainUserVectors.entrySet()) {
            userNorms.put(entry.getKey(), calculateVectorNorm(entry.getValue().values()));
        }

        // Calculate Item similarities in train partition
        Map<Long, Double> itemNorms = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Integer>> entry : trainItemVectors.entrySet()) {
            itemNorms.put(entry.getKey(), calculateVectorNorm(entry.getValue().values()));
        }

        // Retrieve all movies for default fallback predictions
        List<Movie> allMovies = movieRepository.findAll();
        Map<Long, Movie> movieMap = allMovies.stream().collect(Collectors.toMap(Movie::getId, m -> m));
        double globalTrainAverage = trainRatings.stream().mapToInt(Rating::getScore).average().orElse(3.5);

        double userCfSsd = 0.0;
        double itemCfSsd = 0.0;
        double hybridSsd = 0.0;
        int predictionCount = 0;

        for (Rating testRating : testRatings) {
            Long userId = testRating.getUserId();
            Long movieId = testRating.getMovieId();
            double actualScore = testRating.getScore();

            // 1. User-User CF Prediction on Train Set
            double predUserCf = predictUserCfScore(userId, movieId, trainUserVectors, userNorms, movieMap, globalTrainAverage);
            // 2. Item-Item CF Prediction on Train Set
            double predItemCf = predictItemCfScore(userId, movieId, trainUserVectors, trainItemVectors, itemNorms, movieMap, globalTrainAverage);
            // 3. Hybrid blend
            double predHybrid = 0.5 * predUserCf + 0.5 * predItemCf;

            userCfSsd += Math.pow(actualScore - predUserCf, 2);
            itemCfSsd += Math.pow(actualScore - predItemCf, 2);
            hybridSsd += Math.pow(actualScore - predHybrid, 2);
            predictionCount++;
        }

        if (predictionCount > 0) {
            metrics.put("userCfRmse", Math.round(Math.sqrt(userCfSsd / predictionCount) * 1000.0) / 1000.0);
            metrics.put("itemCfRmse", Math.round(Math.sqrt(itemCfSsd / predictionCount) * 1000.0) / 1000.0);
            metrics.put("hybridRmse", Math.round(Math.sqrt(hybridSsd / predictionCount) * 1000.0) / 1000.0);
        } else {
            metrics.put("userCfRmse", 0.0);
            metrics.put("itemCfRmse", 0.0);
            metrics.put("hybridRmse", 0.0);
        }

        return metrics;
    }

    private double predictUserCfScore(Long targetUserId, Long targetMovieId,
                                      Map<Long, Map<Long, Integer>> userVectors,
                                      Map<Long, Double> userNorms,
                                      Map<Long, Movie> movieMap,
                                      double globalAverage) {
        Map<Long, Integer> targetVector = userVectors.get(targetUserId);
        if (targetVector == null || targetVector.isEmpty()) {
            Movie m = movieMap.get(targetMovieId);
            return m != null && m.getAverageRating() > 0 ? m.getAverageRating() : globalAverage;
        }

        double targetNorm = userNorms.getOrDefault(targetUserId, 0.0);
        if (targetNorm == 0) {
            Movie m = movieMap.get(targetMovieId);
            return m != null && m.getAverageRating() > 0 ? m.getAverageRating() : globalAverage;
        }

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (Map.Entry<Long, Map<Long, Integer>> entry : userVectors.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId.equals(targetUserId)) continue;

            Integer otherRatingForMovie = entry.getValue().get(targetMovieId);
            if (otherRatingForMovie == null) continue; // other user hasn't rated this movie

            // Compute similarity with other user
            Map<Long, Integer> otherVector = entry.getValue();
            double dotProduct = 0.0;
            for (Map.Entry<Long, Integer> targetVal : targetVector.entrySet()) {
                Integer otherVal = otherVector.get(targetVal.getKey());
                if (otherVal != null) {
                    dotProduct += targetVal.getValue() * otherVal;
                }
            }

            double otherNorm = userNorms.getOrDefault(otherUserId, 0.0);
            if (otherNorm == 0) continue;

            double similarity = dotProduct / (targetNorm * otherNorm);
            if (similarity > 0.1) {
                weightedSum += similarity * otherRatingForMovie;
                similaritySum += similarity;
            }
        }

        if (similaritySum > 0) {
            double val = weightedSum / similaritySum;
            return Math.max(1.0, Math.min(5.0, val));
        }

        Movie m = movieMap.get(targetMovieId);
        return m != null && m.getAverageRating() > 0 ? m.getAverageRating() : globalAverage;
    }

    private double predictItemCfScore(Long userId, Long targetMovieId,
                                      Map<Long, Map<Long, Integer>> userVectors,
                                      Map<Long, Map<Long, Integer>> itemVectors,
                                      Map<Long, Double> itemNorms,
                                      Map<Long, Movie> movieMap,
                                      double globalAverage) {
        Map<Long, Integer> userRatings = userVectors.get(userId);
        if (userRatings == null || userRatings.isEmpty()) {
            Movie m = movieMap.get(targetMovieId);
            return m != null && m.getAverageRating() > 0 ? m.getAverageRating() : globalAverage;
        }

        Map<Long, Integer> targetItemVector = itemVectors.get(targetMovieId);
        if (targetItemVector == null || targetItemVector.isEmpty()) {
            Movie m = movieMap.get(targetMovieId);
            return m != null && m.getAverageRating() > 0 ? m.getAverageRating() : globalAverage;
        }

        double targetItemNorm = itemNorms.getOrDefault(targetMovieId, 0.0);
        if (targetItemNorm == 0) {
            Movie m = movieMap.get(targetMovieId);
            return m != null && m.getAverageRating() > 0 ? m.getAverageRating() : globalAverage;
        }

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (Map.Entry<Long, Integer> ratedEntry : userRatings.entrySet()) {
            Long ratedMovieId = ratedEntry.getKey();
            Integer userScore = ratedEntry.getValue();

            Map<Long, Integer> ratedItemVector = itemVectors.get(ratedMovieId);
            if (ratedItemVector == null || ratedItemVector.isEmpty()) continue;

            double ratedItemNorm = itemNorms.getOrDefault(ratedMovieId, 0.0);
            if (ratedItemNorm == 0) continue;

            // Cosine similarity between target movie and rated movie
            double dotProduct = 0.0;
            for (Map.Entry<Long, Integer> ratingEntry : targetItemVector.entrySet()) {
                Long rUserId = ratingEntry.getKey();
                Integer otherRating = ratedItemVector.get(rUserId);
                if (otherRating != null) {
                    dotProduct += ratingEntry.getValue() * otherRating;
                }
            }

            double similarity = dotProduct / (targetItemNorm * ratedItemNorm);
            if (similarity > 0.1) {
                weightedSum += similarity * userScore;
                similaritySum += similarity;
            }
        }

        if (similaritySum > 0) {
            double val = weightedSum / similaritySum;
            return Math.max(1.0, Math.min(5.0, val));
        }

        Movie m = movieMap.get(targetMovieId);
        return m != null && m.getAverageRating() > 0 ? m.getAverageRating() : globalAverage;
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
