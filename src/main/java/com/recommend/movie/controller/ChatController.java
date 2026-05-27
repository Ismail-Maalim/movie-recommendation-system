package com.recommend.movie.controller;

import com.recommend.movie.model.Movie;
import com.recommend.movie.repository.MovieRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final MovieRepository movieRepository;

    public ChatController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> handleChatMessage(@RequestBody ChatRequest request) {
        String msg = request.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return ResponseEntity.ok(new ChatResponse("Please tell me something about your movie preferences! 🍿", Collections.emptyList()));
        }

        String lower = msg.toLowerCase().trim();
        List<Movie> suggestions = new ArrayList<>();
        String reply;

        // 1. GREETING INTENT
        if (lower.matches(".*\\b(hi|hello|hey|greetings|yo)\\b.*")) {
            reply = "Hello there! I'm CineBot. I can recommend movies, check what's trending, list latest releases, or help you navigate your feed. Try asking: \"Recommend a Sci-Fi movie\" or \"What is trending now?\" 🎬";
        }
        // 2. HELP / INFO INTENT
        else if (lower.contains("help") || lower.contains("features") || lower.contains("how to") || lower.contains("recommendations work")) {
            reply = "CineMatch uses a Hybrid Recommendation Engine. It combines Content-Based Affinity (scoring genres you selected during settings/onboarding) and Collaborative Filtering (matching your ratings with similar community tastes). You can search for movies, toggle genres on the Discover tab, add movies to your Watchlist, and submit star ratings and text reviews!";
        }
        // 3. TRENDING INTENT
        else if (lower.contains("trending") || lower.contains("popular") || lower.contains("top rated")) {
            reply = "Here are the top trending and highest-rated movies in our catalog right now:";
            suggestions = movieRepository.findAll().stream()
                    .sorted(Comparator.comparing(Movie::getAverageRating, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Movie::getImdbRating, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(5)
                    .collect(Collectors.toList());
        }
        // 4. LATEST INTENT
        else if (lower.contains("latest") || lower.contains("recent") || lower.contains("new release") || lower.contains("2024") || lower.contains("2025") || lower.contains("2026")) {
            reply = "Check out these recently released or slated titles in CineMatch:";
            suggestions = movieRepository.findAll().stream()
                    .sorted(Comparator.comparing(Movie::getReleaseYear, Comparator.reverseOrder())
                            .thenComparing(Movie::getImdbRating, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(5)
                    .collect(Collectors.toList());
        }
        // 5. GENRE INTENT
        else if (hasGenreKeyword(lower)) {
            String detectedGenre = detectGenre(lower);
            reply = "I found these highly-rated matches in the **" + detectedGenre + "** genre:";
            suggestions = movieRepository.findByGenreIgnoreCase(detectedGenre).stream()
                    .sorted(Comparator.comparing(Movie::getImdbRating, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(5)
                    .collect(Collectors.toList());
        }
        // 6. SPECIFIC MOVIE SEARCH INTENT
        else {
            // Check if user is referencing a title in our database
            Optional<Movie> directMatch = findSubstringMovieMatch(lower);
            if (directMatch.isPresent()) {
                Movie m = directMatch.get();
                reply = "Yes, we have \"" + m.getTitle() + "\" in our database! Directed by " + m.getDirector() + ". Here are its details:";
                suggestions = Collections.singletonList(m);
            } else {
                // Default fallback
                reply = "I'm not sure I completely understand. You can ask for recommendations in genres like Sci-Fi, Action, Drama, Comedy, or Thriller, check \"trending\" movies, or type a specific movie title!";
                // Suggest some general highly rated hits
                suggestions = movieRepository.findAll().stream()
                        .sorted(Comparator.comparing(Movie::getImdbRating, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(3)
                        .collect(Collectors.toList());
            }
        }

        return ResponseEntity.ok(new ChatResponse(reply, suggestions));
    }

    private boolean hasGenreKeyword(String text) {
        return text.contains("sci-fi") || text.contains("science fiction") || text.contains("action") 
                || text.contains("drama") || text.contains("comedy") || text.contains("funny") 
                || text.contains("thriller") || text.contains("suspense") || text.contains("romance") 
                || text.contains("love") || text.contains("adventure") || text.contains("crime") 
                || text.contains("fantasy") || text.contains("animation") || text.contains("cartoon")
                || text.contains("horror") || text.contains("scary");
    }

    private String detectGenre(String text) {
        if (text.contains("sci-fi") || text.contains("science fiction")) return "Sci-Fi";
        if (text.contains("action")) return "Action";
        if (text.contains("drama")) return "Drama";
        if (text.contains("comedy") || text.contains("funny")) return "Comedy";
        if (text.contains("thriller") || text.contains("suspense")) return "Thriller";
        if (text.contains("romance") || text.contains("love")) return "Romance";
        if (text.contains("adventure")) return "Adventure";
        if (text.contains("crime")) return "Crime";
        if (text.contains("fantasy")) return "Fantasy";
        if (text.contains("animation") || text.contains("cartoon")) return "Animation";
        if (text.contains("horror") || text.contains("scary")) return "Horror";
        return "Sci-Fi"; // default
    }

    private Optional<Movie> findSubstringMovieMatch(String text) {
        List<Movie> all = movieRepository.findAll();
        for (Movie m : all) {
            String title = m.getTitle().toLowerCase();
            if (text.contains(title) && title.length() > 2) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    // DTO Classes
    public static class ChatRequest {
        private String message;
        public ChatRequest() {}
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ChatResponse {
        private String reply;
        private List<Movie> suggestedMovies;

        public ChatResponse(String reply, List<Movie> suggestedMovies) {
            this.reply = reply;
            this.suggestedMovies = suggestedMovies;
        }

        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
        public List<Movie> getSuggestedMovies() { return suggestedMovies; }
        public void setSuggestedMovies(List<Movie> suggestedMovies) { this.suggestedMovies = suggestedMovies; }
    }
}
