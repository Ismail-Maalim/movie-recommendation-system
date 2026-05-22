package com.recommend.movie.service;

import com.recommend.movie.model.Movie;
import com.recommend.movie.model.Rating;
import com.recommend.movie.model.Review;
import com.recommend.movie.model.WatchlistItem;
import com.recommend.movie.repository.MovieRepository;
import com.recommend.movie.repository.RatingRepository;
import com.recommend.movie.repository.ReviewRepository;
import com.recommend.movie.repository.WatchlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class MovieService {

    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final ReviewRepository reviewRepository;
    private final WatchlistItemRepository watchlistRepository;

    public MovieService(MovieRepository movieRepository, RatingRepository ratingRepository,
                        ReviewRepository reviewRepository, WatchlistItemRepository watchlistRepository) {
        this.movieRepository = movieRepository;
        this.ratingRepository = ratingRepository;
        this.reviewRepository = reviewRepository;
        this.watchlistRepository = watchlistRepository;
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id);
    }

    public List<Movie> searchMovies(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Movie> getMoviesByGenre(String genre) {
        return movieRepository.findByGenreIgnoreCase(genre);
    }

    @Transactional
    public Rating rateMovie(Long userId, Long movieId, int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Rating score must be between 1 and 5");
        }

        // Check if movie exists
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found"));

        Optional<Rating> existingRating = ratingRepository.findByUserIdAndMovieId(userId, movieId);
        Rating rating;
        if (existingRating.isPresent()) {
            rating = existingRating.get();
            rating.setScore(score);
            rating.setTimestamp(System.currentTimeMillis());
        } else {
            rating = new Rating(userId, movieId, score);
        }
        
        Rating savedRating = ratingRepository.save(rating);
        
        // Recalculate average rating for the movie
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
            // Round to 1 decimal place
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
        
        // Verify movie exists
        movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found"));

        Review review = new Review(userId, username, movieId, reviewText.trim());
        return reviewRepository.save(review);
    }

    public List<Review> getReviewsForMovie(Long movieId) {
        return reviewRepository.findByMovieId(movieId);
    }

    @Transactional
    public WatchlistItem addToWatchlist(Long userId, Long movieId) {
        movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found"));

        Optional<WatchlistItem> existing = watchlistRepository.findByUserIdAndMovieId(userId, movieId);
        if (existing.isPresent()) {
            return existing.get();
        }

        WatchlistItem item = new WatchlistItem(userId, movieId);
        return watchlistRepository.save(item);
    }

    @Transactional
    public void removeFromWatchlist(Long userId, Long movieId) {
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
}
