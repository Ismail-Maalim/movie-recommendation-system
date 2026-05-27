package com.recommend.movie.service;

import com.recommend.movie.model.Movie;
import com.recommend.movie.model.Episode;
import com.recommend.movie.model.Rating;
import com.recommend.movie.model.Review;
import com.recommend.movie.model.WatchlistItem;
import com.recommend.movie.model.User;
import com.recommend.movie.repository.MovieRepository;
import com.recommend.movie.repository.EpisodeRepository;
import com.recommend.movie.repository.RatingRepository;
import com.recommend.movie.repository.ReviewRepository;
import com.recommend.movie.repository.WatchlistItemRepository;
import com.recommend.movie.repository.UserRepository;
import com.recommend.movie.config.OracleSyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final ReviewRepository reviewRepository;
    private final WatchlistItemRepository watchlistRepository;
    private final UserRepository userRepository;
    private final EpisodeRepository episodeRepository;
    private final RestTemplate restTemplate;
    private final OracleSyncProperties syncProperties;

    @Value("${oracle.apex.fallback-to-local:true}")
    private boolean fallbackToLocal;

    public MovieService(MovieRepository movieRepository, RatingRepository ratingRepository,
                        ReviewRepository reviewRepository, WatchlistItemRepository watchlistRepository,
                        UserRepository userRepository,
                        EpisodeRepository episodeRepository,
                        @org.springframework.beans.factory.annotation.Qualifier("oracleRestTemplate") RestTemplate restTemplate,
                        OracleSyncProperties syncProperties) {
        this.movieRepository = movieRepository;
        this.ratingRepository = ratingRepository;
        this.reviewRepository = reviewRepository;
        this.watchlistRepository = watchlistRepository;
        this.userRepository = userRepository;
        this.episodeRepository = episodeRepository;
        this.restTemplate = restTemplate;
        this.syncProperties = syncProperties;
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id);
    }

    public List<Episode> getEpisodesByMovieId(Long movieId) {
        return episodeRepository.findByMovieIdOrderBySeasonNumberAscEpisodeNumberAsc(movieId);
    }

    public List<Movie> searchMovies(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Movie> getMoviesByGenre(String genre) {
        return movieRepository.findByGenreIgnoreCase(genre);
    }

    private Long getOracleUserId(Long localUserId) {
        User user = userRepository.findById(localUserId)
                .orElseThrow(() -> new RuntimeException("Local user not found"));

        if (user.getOracleUserId() == null) {
            throw new RuntimeException("Oracle user ID missing for local user: " + localUserId);
        }

        return user.getOracleUserId();
    }

    @Transactional
    public Rating rateMovie(Long userId, Long movieId, int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Rating score must be between 1 and 5");
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found"));

        long timestamp = System.currentTimeMillis();

        try {
            Long oracleUserId = getOracleUserId(userId);
            String url = syncProperties.getApiUrl() + "/ratings";
            log.info("Calling Oracle APEX REST API (RateMovie): {}", url);
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", oracleUserId);
            payload.put("movieId", movieId);
            payload.put("score", score);
            payload.put("timestamp", timestamp);

            restTemplate.postForObject(url, payload, String.class);
            log.info("Successfully saved rating to Oracle APEX.");
        } catch (Exception e) {
            log.error("Failed to save rating to Oracle APEX: {}", e.getMessage());
            if (!fallbackToLocal) {
                throw new RuntimeException("Failed to save rating to Oracle APEX and fallback to local is disabled.", e);
            }
            log.warn("Falling back to local H2 cache...");
        }

        Optional<Rating> existingRating = ratingRepository.findByUserIdAndMovieId(userId, movieId);
        Rating rating;
        if (existingRating.isPresent()) {
            rating = existingRating.get();
            rating.setScore(score);
            rating.setTimestamp(timestamp);
        } else {
            rating = new Rating(userId, movieId, score);
            rating.setTimestamp(timestamp);
        }
        
        Rating savedRating = ratingRepository.save(rating);
        recalculateAverageRating(movie);

        return savedRating;
    }

    private void recalculateAverageRating(Movie movie) {
        List<Rating> ratings = ratingRepository.findByMovieId(movie.getId());
        if (ratings.isEmpty()) {
            movie.setAverageRating(0.0);
        } else {
            double sum = ratings.stream().mapToDouble(Rating::getScore).sum();
            double avg = sum / ratings.size();
            avg = Math.round(avg * 10.0) / 10.0;
            movie.setAverageRating(avg);
        }
        movieRepository.save(movie);
    }

    public Optional<Rating> getUserRatingForMovie(Long userId, Long movieId) {
        return ratingRepository.findByUserIdAndMovieId(userId, movieId);
    }

    @Transactional
    public Review addReview(Long userId, String username, Long movieId, String reviewText) {
        if (reviewText == null || reviewText.trim().isEmpty()) {
            throw new IllegalArgumentException("Review text cannot be empty");
        }
        
        movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found"));

        long timestamp = System.currentTimeMillis();

        try {
            Long oracleUserId = getOracleUserId(userId);
            String url = syncProperties.getApiUrl() + "/reviews";
            log.info("Calling Oracle APEX REST API (AddReview): {}", url);
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", oracleUserId);
            payload.put("username", username);
            payload.put("movieId", movieId);
            payload.put("reviewText", reviewText.trim());
            payload.put("timestamp", timestamp);

            restTemplate.postForObject(url, payload, String.class);
            log.info("Successfully saved review to Oracle APEX.");
        } catch (Exception e) {
            log.error("Failed to save review to Oracle APEX: {}", e.getMessage());
            if (!fallbackToLocal) {
                throw new RuntimeException("Failed to save review to Oracle APEX and fallback to local is disabled.", e);
            }
            log.warn("Falling back to local H2 cache...");
        }

        Review review = new Review(userId, username, movieId, reviewText.trim());
        review.setTimestamp(timestamp);
        return reviewRepository.save(review);
    }

    public List<Review> getReviewsForMovie(Long movieId) {
        return reviewRepository.findByMovieId(movieId);
    }

    @Transactional
    public WatchlistItem addToWatchlist(Long userId, Long movieId) {
        movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found"));

        try {
            Long oracleUserId = getOracleUserId(userId);
            String url = syncProperties.getApiUrl() + "/watchlist_items";
            log.info("Calling Oracle APEX REST API (AddToWatchlist): {}", url);
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", oracleUserId);
            payload.put("movieId", movieId);

            restTemplate.postForObject(url, payload, String.class);
            log.info("Successfully added to watchlist on Oracle APEX.");
        } catch (Exception e) {
            log.error("Failed to add to watchlist on Oracle APEX: {}", e.getMessage());
            if (!fallbackToLocal) {
                throw new RuntimeException("Failed to add to watchlist on Oracle APEX and fallback to local is disabled.", e);
            }
            log.warn("Falling back to local H2 cache...");
        }

        Optional<WatchlistItem> existing = watchlistRepository.findByUserIdAndMovieId(userId, movieId);
        if (existing.isPresent()) {
            return existing.get();
        }

        WatchlistItem item = new WatchlistItem(userId, movieId);
        return watchlistRepository.save(item);
    }

    @Transactional
    public void removeFromWatchlist(Long userId, Long movieId) {
        try {
            Long oracleUserId = getOracleUserId(userId);
            String url = syncProperties.getApiUrl() + "/watchlist_items?userId=" + oracleUserId + "&movieId=" + movieId;
            log.info("Calling Oracle APEX REST API (RemoveFromWatchlist): {}", url);
            restTemplate.delete(url);
            log.info("Successfully removed from watchlist on Oracle APEX.");
        } catch (Exception e) {
            log.error("Failed to remove from watchlist on Oracle APEX: {}", e.getMessage());
            if (!fallbackToLocal) {
                throw new RuntimeException("Failed to remove from watchlist on Oracle APEX and fallback to local is disabled.", e);
            }
            log.warn("Falling back to local H2 cache...");
        }

        watchlistRepository.findByUserIdAndMovieId(userId, movieId)
                .ifPresent(watchlistRepository::delete);
    }

    public List<Movie> getWatchlistMovies(Long userId) {
        List<WatchlistItem> items = watchlistRepository.findByUserId(userId);
        List<Long> movieIds = items.stream().map(WatchlistItem::getMovieId).collect(Collectors.toList());
        return movieRepository.findAllById(movieIds);
    }

    public boolean isInWatchlist(Long userId, Long movieId) {
        return watchlistRepository.findByUserIdAndMovieId(userId, movieId).isPresent();
    }

    @Transactional
    public void syncMoviesFromOracle() {
        if (!syncProperties.getSync().isEnabled()) {
            log.info("Oracle APEX synchronization is disabled in configuration.");
            return;
        }

        log.info("Starting Oracle APEX synchronization...");
        long startTime = System.currentTimeMillis();

        try {
            String baseUrl = syncProperties.getApiUrl();
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                log.warn("Oracle APEX API URL is not configured. Skipping synchronization.");
                return;
            }

            // 1. Fetch movies
            List<ApexMovieItem> movies = fetchMoviesFromApex(baseUrl);
            if (movies.isEmpty()) {
                log.warn("No movies returned from Oracle APEX ORDS endpoint.");
                return;
            }

            // 2. Fetch genres mapping
            Map<Long, List<String>> genreMap = fetchGenresFromApex(baseUrl);

            // 3. Upsert movies
            int movieSyncCount = 0;
            for (ApexMovieItem item : movies) {
                if (item.getId() == null || item.getTitle() == null) continue;
                List<String> genres = genreMap.get(item.getId());
                if (genres == null || genres.isEmpty() || (genres.size() == 1 && "Sci-Fi".equals(genres.get(0)))) {
                    if (item.getDescription() != null && item.getDescription().contains("|")) {
                        genres = new java.util.ArrayList<>(Arrays.asList(item.getDescription().split("\\|")));
                    } else if (genres == null || genres.isEmpty()) {
                        genres = new java.util.ArrayList<>(Arrays.asList("Sci-Fi"));
                    }
                }
                upsertMovie(item, genres);
                movieSyncCount++;
            }
            log.info("Successfully synchronized {} movies from Oracle APEX to local H2 cache.", movieSyncCount);

            // 4. Fetch and Sync episodes
            List<ApexEpisodeItem> episodes = fetchEpisodesFromApex(baseUrl);
            int episodeSyncCount = 0;
            for (ApexEpisodeItem item : episodes) {
                if (item.getMovieId() == null || item.getSeasonNumber() == null || item.getEpisodeNumber() == null) continue;
                upsertEpisode(item);
                episodeSyncCount++;
            }
            if (episodeSyncCount > 0) {
                log.info("Successfully synchronized {} episodes from Oracle APEX.", episodeSyncCount);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Oracle APEX synchronization completed in {} ms.", duration);

        } catch (Exception e) {
            log.error("Failed to synchronize movies from Oracle APEX", e);
            log.warn("Falling back to local H2 cache / seeded records.");
        }
    }

    private List<ApexMovieItem> fetchMoviesFromApex(String baseUrl) {
        String url = baseUrl + "/movies";
        log.info("Syncing movies catalog from Oracle APEX ORDS: {}", url);
        try {
            ApexMovieQueryResponse response = restTemplate.getForObject(url, ApexMovieQueryResponse.class);
            if (response != null && response.getItems() != null) {
                return response.getItems();
            }
        } catch (Exception e) {
            log.error("Failed to fetch movies from Oracle APEX: {}", e.getMessage());
        }
        return java.util.Collections.emptyList();
    }

    private Map<Long, List<String>> fetchGenresFromApex(String baseUrl) {
        String url = baseUrl + "/movie_genres";
        log.info("Syncing genres mapping from Oracle APEX ORDS: {}", url);
        Map<Long, List<String>> genreMap = new HashMap<>();
        try {
            ApexGenreQueryResponse response = restTemplate.getForObject(url, ApexGenreQueryResponse.class);
            if (response != null && response.getItems() != null) {
                for (ApexGenreItem genreItem : response.getItems()) {
                    if (genreItem.getMovieId() != null && genreItem.getGenre() != null) {
                        genreMap.computeIfAbsent(genreItem.getMovieId(), k -> new java.util.ArrayList<>())
                                .add(genreItem.getGenre());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch genres from Oracle APEX: {}", e.getMessage());
        }
        return genreMap;
    }

    private List<ApexEpisodeItem> fetchEpisodesFromApex(String baseUrl) {
        String url = baseUrl + "/episodes";
        log.info("Syncing episodes catalog from Oracle APEX ORDS: {}", url);
        try {
            ApexEpisodeQueryResponse response = restTemplate.getForObject(url, ApexEpisodeQueryResponse.class);
            if (response != null && response.getItems() != null) {
                return response.getItems();
            }
        } catch (Exception e) {
            log.error("Failed to fetch episodes from Oracle APEX: {}", e.getMessage());
        }
        return java.util.Collections.emptyList();
    }

    private void upsertMovie(ApexMovieItem item, List<String> genres) {
        Optional<Movie> existingOpt = movieRepository.findById(item.getId());
        
        if (existingOpt.isEmpty() && item.getTitle() != null) {
            existingOpt = movieRepository.findByTitleIgnoreCase(item.getTitle());
        }

        String rawDesc = item.getDescription();
        String formattedDesc = rawDesc;
        if (rawDesc != null && rawDesc.contains("|")) {
            formattedDesc = "Classic movie in genres: " + rawDesc.replace("|", ", ") + ".";
        }

        Movie movie;
        if (existingOpt.isPresent()) {
            movie = existingOpt.get();
            movie.setTitle(item.getTitle());
            movie.setDescription(formattedDesc);
            movie.setReleaseYear(item.getReleaseYear() != null ? item.getReleaseYear() : 2024);
            if (movie.getGenres() == null) {
                movie.setGenres(new java.util.ArrayList<>(genres));
            } else {
                movie.getGenres().clear();
                movie.getGenres().addAll(genres);
            }
            movie.setPosterUrl(item.getPosterUrl());
            movie.setBackdropUrl(item.getBackdropUrl());
            movie.setDirector(item.getDirector());
            movie.setCastMembers(item.getCastMembers());
            if (item.getAverageRating() != null) {
                movie.setAverageRating(item.getAverageRating());
            }
            if (item.getImdbRating() != null) {
                movie.setImdbRating(item.getImdbRating());
            }
        } else {
            movie = new Movie(
                    item.getTitle(),
                    formattedDesc,
                    item.getReleaseYear() != null ? item.getReleaseYear() : 2024,
                    new java.util.ArrayList<>(genres),
                    item.getPosterUrl(),
                    item.getBackdropUrl(),
                    item.getDirector(),
                    item.getCastMembers()
            );
            movie.setId(item.getId());
            if (item.getAverageRating() != null) {
                movie.setAverageRating(item.getAverageRating());
            }
            if (item.getImdbRating() != null) {
                movie.setImdbRating(item.getImdbRating());
            }
        }
        movieRepository.save(movie);
    }

    private void upsertEpisode(ApexEpisodeItem item) {
        Optional<Movie> movieOpt = movieRepository.findById(item.getMovieId());
        if (movieOpt.isPresent()) {
            Movie movie = movieOpt.get();
            List<Episode> existingEpisodes = episodeRepository.findByMovieIdOrderBySeasonNumberAscEpisodeNumberAsc(movie.getId());
            Optional<Episode> existingEpisodeOpt = existingEpisodes.stream()
                    .filter(e -> e.getSeasonNumber() == item.getSeasonNumber() && e.getEpisodeNumber() == item.getEpisodeNumber())
                    .findFirst();
                    
            Episode episode;
            if (existingEpisodeOpt.isPresent()) {
                episode = existingEpisodeOpt.get();
                episode.setTitle(item.getTitle() != null ? item.getTitle() : "Episode " + item.getEpisodeNumber());
                episode.setDescription(item.getDescription());
                episode.setAirDate(item.getAirDate());
                episode.setDurationMinutes(item.getDurationMinutes());
            } else {
                episode = new Episode(
                        movie,
                        item.getSeasonNumber(),
                        item.getEpisodeNumber(),
                        item.getTitle() != null ? item.getTitle() : "Episode " + item.getEpisodeNumber(),
                        item.getDescription(),
                        item.getAirDate(),
                        item.getDurationMinutes()
                );
            }
            episodeRepository.save(episode);
        }
    }

    // ORDS Movies and Genres Sync Mapping Classes
    public static class ApexMovieQueryResponse {
        @JsonProperty("items")
        private List<ApexMovieItem> items;

        public List<ApexMovieItem> getItems() { return items; }
        public void setItems(List<ApexMovieItem> items) { this.items = items; }
    }

    public static class ApexMovieItem {
        @JsonProperty("id")
        private Long id;
        @JsonProperty("title")
        private String title;
        @JsonProperty("description")
        private String description;
        @JsonProperty("release_year")
        private Integer releaseYear;
        @JsonProperty("poster_url")
        private String posterUrl;
        @JsonProperty("backdrop_url")
        private String backdropUrl;
        @JsonProperty("director")
        private String director;
        @JsonProperty("cast_members")
        private String castMembers;
        @JsonProperty("average_rating")
        private Double averageRating;
        @JsonProperty("imdb_rating")
        private Double imdbRating;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getReleaseYear() { return releaseYear; }
        public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }
        public String getPosterUrl() { return posterUrl; }
        public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
        public String getBackdropUrl() { return backdropUrl; }
        public void setBackdropUrl(String backdropUrl) { this.backdropUrl = backdropUrl; }
        public String getDirector() { return director; }
        public void setDirector(String director) { this.director = director; }
        public String getCastMembers() { return castMembers; }
        public void setCastMembers(String castMembers) { this.castMembers = castMembers; }
        public Double getAverageRating() { return averageRating; }
        public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
        public Double getImdbRating() { return imdbRating; }
        public void setImdbRating(Double imdbRating) { this.imdbRating = imdbRating; }
    }

    public static class ApexGenreQueryResponse {
        @JsonProperty("items")
        private List<ApexGenreItem> items;

        public List<ApexGenreItem> getItems() { return items; }
        public void setItems(List<ApexGenreItem> items) { this.items = items; }
    }

    public static class ApexGenreItem {
        @JsonProperty("movie_id")
        private Long movieId;
        @JsonProperty("genre")
        private String genre;

        public Long getMovieId() { return movieId; }
        public void setMovieId(Long movieId) { this.movieId = movieId; }
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
    }

    public static class ApexEpisodeQueryResponse {
        @JsonProperty("items")
        private List<ApexEpisodeItem> items;

        public List<ApexEpisodeItem> getItems() { return items; }
        public void setItems(List<ApexEpisodeItem> items) { this.items = items; }
    }

    public static class ApexEpisodeItem {
        @JsonProperty("movie_id")
        private Long movieId;
        @JsonProperty("season_number")
        private Integer seasonNumber;
        @JsonProperty("episode_number")
        private Integer episodeNumber;
        @JsonProperty("title")
        private String title;
        @JsonProperty("description")
        private String description;
        @JsonProperty("air_date")
        private String airDate;
        @JsonProperty("duration_minutes")
        private Integer durationMinutes;

        public Long getMovieId() { return movieId; }
        public void setMovieId(Long movieId) { this.movieId = movieId; }
        public Integer getSeasonNumber() { return seasonNumber; }
        public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }
        public Integer getEpisodeNumber() { return episodeNumber; }
        public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAirDate() { return airDate; }
        public void setAirDate(String airDate) { this.airDate = airDate; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    }
}
