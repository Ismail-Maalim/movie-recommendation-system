package com.recommend.movie.controller;

import com.recommend.movie.model.Movie;
import com.recommend.movie.model.Episode;
import com.recommend.movie.model.Rating;
import com.recommend.movie.model.Review;
import com.recommend.movie.model.WatchlistItem;
import com.recommend.movie.service.MovieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "*")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<List<Movie>> getMovies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String genre) {
        
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(movieService.searchMovies(search));
        } else if (genre != null && !genre.trim().isEmpty()) {
            return ResponseEntity.ok(movieService.getMoviesByGenre(genre));
        }
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Movie not found")));
    }

    @GetMapping("/{id}/episodes")
    public ResponseEntity<?> getMovieEpisodes(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getEpisodesByMovieId(id));
    }

    // Ratings
    @GetMapping("/{id}/rating")
    public ResponseEntity<?> getRating(@PathVariable Long id, @RequestParam Long userId) {
        Optional<Rating> rating = movieService.getUserRatingForMovie(userId, id);
        return rating.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("score", 0)));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<?> rateMovie(@PathVariable Long id, @RequestBody RateRequest request) {
        try {
            Rating rating = movieService.rateMovie(request.getUserId(), id, request.getScore());
            return ResponseEntity.ok(rating);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    // Reviews
    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<Review>> getReviews(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getReviewsForMovie(id));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> addReview(@PathVariable Long id, @RequestBody ReviewRequest request) {
        try {
            Review review = movieService.addReview(
                    request.getUserId(),
                    request.getUsername(),
                    id,
                    request.getReviewText()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(review);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    // Watchlist
    @GetMapping("/watchlist")
    public ResponseEntity<?> getWatchlist(@RequestParam Long userId) {
        return ResponseEntity.ok(movieService.getWatchlistMovies(userId));
    }

    @GetMapping("/{id}/watchlist-status")
    public ResponseEntity<?> getWatchlistStatus(@PathVariable Long id, @RequestParam Long userId) {
        boolean inWatchlist = movieService.isInWatchlist(userId, id);
        return ResponseEntity.ok(Map.of("inWatchlist", inWatchlist));
    }

    @PostMapping("/watchlist/add")
    public ResponseEntity<?> addToWatchlist(@RequestBody WatchlistRequest request) {
        try {
            WatchlistItem item = movieService.addToWatchlist(request.getUserId(), request.getMovieId());
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/watchlist/remove")
    public ResponseEntity<?> removeFromWatchlist(@RequestBody WatchlistRequest request) {
        movieService.removeFromWatchlist(request.getUserId(), request.getMovieId());
        return ResponseEntity.ok(Map.of("message", "Removed from watchlist"));
    }

    // Inner DTO Classes for requests
    public static class RateRequest {
        private Long userId;
        private int score;

        public RateRequest() {}

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }

    public static class ReviewRequest {
        private Long userId;
        private String username;
        private String reviewText;

        public ReviewRequest() {}

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getReviewText() { return reviewText; }
        public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    }

    public static class WatchlistRequest {
        private Long userId;
        private Long movieId;

        public WatchlistRequest() {}

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getMovieId() { return movieId; }
        public void setMovieId(Long movieId) { this.movieId = movieId; }
    }
}
