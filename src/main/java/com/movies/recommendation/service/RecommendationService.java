package com.movies.recommendation.service;

import com.movies.recommendation.model.Movie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class RecommendationService {

    private static final List<Movie> CATALOG = List.of(
            new Movie("Inception", "Sci-Fi", 2010, 8.8),
            new Movie("Interstellar", "Sci-Fi", 2014, 8.7),
            new Movie("The Dark Knight", "Action", 2008, 9.0),
            new Movie("Parasite", "Thriller", 2019, 8.5),
            new Movie("La La Land", "Romance", 2016, 8.0),
            new Movie("Whiplash", "Drama", 2014, 8.5),
            new Movie("Mad Max: Fury Road", "Action", 2015, 8.1),
            new Movie("Spider-Man: Into the Spider-Verse", "Animation", 2018, 8.4)
    );

    public List<Movie> recommendByGenre(String genre) {
        if (genre == null || genre.isBlank()) {
            return topRated(6);
        }

        String normalized = genre.trim().toLowerCase(Locale.ROOT);
        List<Movie> filtered = CATALOG.stream()
                .filter(movie -> movie.genre().toLowerCase(Locale.ROOT).contains(normalized))
                .sorted((a, b) -> Double.compare(b.rating(), a.rating()))
                .limit(6)
                .toList();

        return filtered.isEmpty() ? topRated(6) : filtered;
    }

    public List<Movie> topRated(int limit) {
        return CATALOG.stream()
                .sorted((a, b) -> Double.compare(b.rating(), a.rating()))
                .limit(limit)
                .toList();
    }
}
