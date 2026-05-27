package com.recommend.movie.service;

import com.recommend.movie.config.OracleSyncProperties;
import com.recommend.movie.model.Movie;
import com.recommend.movie.repository.MovieRepository;
import com.recommend.movie.repository.EpisodeRepository;
import com.recommend.movie.repository.RatingRepository;
import com.recommend.movie.repository.ReviewRepository;
import com.recommend.movie.repository.WatchlistItemRepository;
import com.recommend.movie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MovieServiceSyncTest {

    private MovieRepository movieRepository;
    private RatingRepository ratingRepository;
    private ReviewRepository reviewRepository;
    private WatchlistItemRepository watchlistRepository;
    private UserRepository userRepository;
    private EpisodeRepository episodeRepository;
    private RestTemplate restTemplate;
    private OracleSyncProperties syncProperties;
    private MovieService movieService;

    @BeforeEach
    public void setUp() {
        movieRepository = mock(MovieRepository.class);
        ratingRepository = mock(RatingRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        watchlistRepository = mock(WatchlistItemRepository.class);
        userRepository = mock(UserRepository.class);
        episodeRepository = mock(EpisodeRepository.class);
        restTemplate = mock(RestTemplate.class);
        syncProperties = new OracleSyncProperties();
        
        syncProperties.getApi().setUrl("http://mock-apex-api/ords/tr_a855_sql_s40/api");
        syncProperties.getSync().setEnabled(true);

        movieService = new MovieService(
                movieRepository,
                ratingRepository,
                reviewRepository,
                watchlistRepository,
                userRepository,
                episodeRepository,
                restTemplate,
                syncProperties
        );
    }

    @Test
    public void testSyncMoviesFromOracle_Success() {
        // Arrange
        MovieService.ApexMovieQueryResponse movieResponse = new MovieService.ApexMovieQueryResponse();
        MovieService.ApexMovieItem movieItem = new MovieService.ApexMovieItem();
        movieItem.setId(99L);
        movieItem.setTitle("Test Movie");
        movieItem.setDescription("A movie for testing");
        movieItem.setReleaseYear(2026);
        movieResponse.setItems(Collections.singletonList(movieItem));

        MovieService.ApexGenreQueryResponse genreResponse = new MovieService.ApexGenreQueryResponse();
        MovieService.ApexGenreItem genreItem = new MovieService.ApexGenreItem();
        genreItem.setMovieId(99L);
        genreItem.setGenre("Sci-Fi");
        genreResponse.setItems(Collections.singletonList(genreItem));

        MovieService.ApexEpisodeQueryResponse episodeResponse = new MovieService.ApexEpisodeQueryResponse();
        episodeResponse.setItems(new ArrayList<>());

        when(restTemplate.getForObject(contains("/movies"), eq(MovieService.ApexMovieQueryResponse.class)))
                .thenReturn(movieResponse);
        when(restTemplate.getForObject(contains("/movie_genres"), eq(MovieService.ApexGenreQueryResponse.class)))
                .thenReturn(genreResponse);
        when(restTemplate.getForObject(contains("/episodes"), eq(MovieService.ApexEpisodeQueryResponse.class)))
                .thenReturn(episodeResponse);

        when(movieRepository.findById(99L)).thenReturn(Optional.empty());
        when(movieRepository.findByTitleIgnoreCase("Test Movie")).thenReturn(Optional.empty());

        // Act
        movieService.syncMoviesFromOracle();

        // Assert
        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        Movie saved = movieCaptor.getValue();
        assertEquals(99L, saved.getId());
        assertEquals("Test Movie", saved.getTitle());
        assertTrue(saved.getGenres().contains("Sci-Fi"));
    }

    @Test
    public void testSyncMoviesFromOracle_NetworkFailureGraceful() {
        // Arrange
        when(restTemplate.getForObject(anyString(), any()))
                .thenThrow(new RestClientException("Network error"));

        // Act & Assert (Should not throw exception)
        assertDoesNotThrow(() -> movieService.syncMoviesFromOracle());
        verify(movieRepository, never()).save(any());
    }

    @Test
    public void testSyncMoviesFromOracle_DuplicatePreventionByTitle() {
        // Arrange
        MovieService.ApexMovieQueryResponse movieResponse = new MovieService.ApexMovieQueryResponse();
        MovieService.ApexMovieItem movieItem = new MovieService.ApexMovieItem();
        movieItem.setId(100L);
        movieItem.setTitle("Existing Movie");
        movieResponse.setItems(Collections.singletonList(movieItem));

        MovieService.ApexGenreQueryResponse genreResponse = new MovieService.ApexGenreQueryResponse();
        genreResponse.setItems(new ArrayList<>());

        MovieService.ApexEpisodeQueryResponse episodeResponse = new MovieService.ApexEpisodeQueryResponse();
        episodeResponse.setItems(new ArrayList<>());

        when(restTemplate.getForObject(contains("/movies"), eq(MovieService.ApexMovieQueryResponse.class)))
                .thenReturn(movieResponse);
        when(restTemplate.getForObject(contains("/movie_genres"), eq(MovieService.ApexGenreQueryResponse.class)))
                .thenReturn(genreResponse);
        when(restTemplate.getForObject(contains("/episodes"), eq(MovieService.ApexEpisodeQueryResponse.class)))
                .thenReturn(episodeResponse);

        Movie existingMovieInDb = new Movie("Existing Movie", "Old desc", 2020, new ArrayList<>(Arrays.asList("Drama")), "url", "url", "dir", "cast");
        existingMovieInDb.setId(1L);

        when(movieRepository.findById(100L)).thenReturn(Optional.empty());
        when(movieRepository.findByTitleIgnoreCase("Existing Movie")).thenReturn(Optional.of(existingMovieInDb));

        // Act
        movieService.syncMoviesFromOracle();

        // Assert
        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        Movie saved = movieCaptor.getValue();
        
        assertEquals(1L, saved.getId());
        assertEquals("Existing Movie", saved.getTitle());
    }
}
