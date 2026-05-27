package com.recommend.movie.controller;

import com.recommend.movie.model.Movie;
import com.recommend.movie.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChatControllerTest {

    private MovieRepository movieRepository;
    private ChatController chatController;

    @BeforeEach
    public void setUp() {
        movieRepository = mock(MovieRepository.class);
        chatController = new ChatController(movieRepository);
    }

    @Test
    public void testGreetingIntent() {
        ChatController.ChatRequest req = new ChatController.ChatRequest();
        req.setMessage("Hello, CineBot!");

        ResponseEntity<ChatController.ChatResponse> response = chatController.handleChatMessage(req);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().getReply().toLowerCase().contains("hello") || response.getBody().getReply().toLowerCase().contains("there"));
    }

    @Test
    public void testGenreIntent() {
        Movie mockMovie = new Movie("Dune", "Sci-Fi movie", 2021, Arrays.asList("Sci-Fi"), "poster", "backdrop", "Villeneuve", "Timothee");
        when(movieRepository.findByGenreIgnoreCase("Sci-Fi")).thenReturn(Collections.singletonList(mockMovie));

        ChatController.ChatRequest req = new ChatController.ChatRequest();
        req.setMessage("Can you recommend some sci-fi movies?");

        ResponseEntity<ChatController.ChatResponse> response = chatController.handleChatMessage(req);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().getReply().contains("Sci-Fi"));
        assertEquals(1, response.getBody().getSuggestedMovies().size());
        assertEquals("Dune", response.getBody().getSuggestedMovies().get(0).getTitle());
    }

    @Test
    public void testMovieTitleSubstringMatch() {
        Movie mockMovie = new Movie("Inception", "Nolan dream movie", 2010, Arrays.asList("Sci-Fi", "Action"), "poster", "backdrop", "Nolan", "DiCaprio");
        when(movieRepository.findAll()).thenReturn(Collections.singletonList(mockMovie));

        ChatController.ChatRequest req = new ChatController.ChatRequest();
        req.setMessage("Tell me about inception please");

        ResponseEntity<ChatController.ChatResponse> response = chatController.handleChatMessage(req);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().getReply().contains("Inception"));
        assertEquals(1, response.getBody().getSuggestedMovies().size());
    }
}
