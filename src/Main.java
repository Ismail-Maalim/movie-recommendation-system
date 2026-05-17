//package com.recommendation;
//
//import com.recommendation.model.Movie;
//import com.recommendation.model.User;
//import com.recommendation.service.RecommendationService;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        // Initialize Movies
        Map<Integer, Movie> movieMap = new HashMap<>();
        movieMap.put(1, new Movie(1, "The Matrix"));
        movieMap.put(2, new Movie(2, "Inception"));
        movieMap.put(3, new Movie(3, "Interstellar"));
        movieMap.put(4, new Movie(4, "Toy Story"));
        movieMap.put(5, new Movie(5, "Finding Nemo"));

        // Initialize Users and Ratings
        List<User> users = new ArrayList<>();

        // User 1: High Sci-Fi preference
        User u1 = new User(1);
        u1.addRating(1, 5.0); // The Matrix
        u1.addRating(2, 5.0); // Inception
        u1.addRating(3, 4.0); // Interstellar
        users.add(u1);

        // User 2: Sci-Fi and minor Animation preference
        User u2 = new User(2);
        u2.addRating(1, 4.0); // The Matrix
        u2.addRating(2, 4.5); // Inception
        u2.addRating(4, 2.0); // Toy Story
        users.add(u2);

        // User 3: High Animation preference
        User u3 = new User(3);
        u3.addRating(4, 5.0); // Toy Story
        u3.addRating(5, 5.0); // Finding Nemo
        users.add(u3);

        // Target User: Prefers Matrix and Inception, missing Interstellar and Animation
        User targetUser = new User(4);
        targetUser.addRating(1, 4.5); // The Matrix
        targetUser.addRating(2, 4.0); // Inception
        users.add(targetUser);

        // Instantiate Service and Compute Recommendations
        RecommendationService service = new RecommendationService();
        int limit = 2;
        List<Movie> recommendations = service.getRecommendations(targetUser, users, movieMap, limit);

        System.out.println("--- Movie Recommendations for User " + targetUser.getId() + " ---");
        if (recommendations.isEmpty()) {
            System.out.println("No recommendations found.");
        } else {
            for (int i = 0; i < recommendations.size(); i++) {
                System.out.println((i + 1) + ". " + recommendations.get(i));
            }
        }
    }
}