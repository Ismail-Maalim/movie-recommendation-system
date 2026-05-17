//package com.recommendation.service;
//
//import com.recommendation.model.Movie;
//import com.recommendation.model.User;

import java.util.*;

public class RecommendationService {

    /**
     * Calculates the Cosine Similarity between two users based on shared movie ratings.
     */
    public double calculateUserSimilarity(User u1, User u2) {
        Map<Integer, Double> r1 = u1.getRatings();
        Map<Integer, Double> r2 = u2.getRatings();

        Set<Integer> commonMovies = new HashSet<>(r1.keySet());
        commonMovies.retainAll(r2.keySet());

        if (commonMovies.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        for (int movieId : commonMovies) {
            dotProduct += r1.get(movieId) * r2.get(movieId);
        }

        double normA = 0.0;
        for (double rating : r1.values()) {
            normA += Math.pow(rating, 2);
        }

        double normB = 0.0;
        for (double rating : r2.values()) {
            normB += Math.pow(rating, 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Generates top movie recommendations for a target user.
     */
    public List<Movie> getRecommendations(User targetUser, List<User> allUsers, Map<Integer, Movie> movieMap, int topN) {
        Map<Integer, Double> scoreWeightedRatings = new HashMap<>();
        Map<Integer, Double> scoreSimSums = new HashMap<>();

        for (User current : allUsers) {
            if (current.getId() == targetUser.getId()) {
                continue;
            }

            double similarity = calculateUserSimilarity(targetUser, current);
            if (similarity <= 0) {
                continue;
            }

            for (Map.Entry<Integer, Double> entry : current.getRatings().entrySet()) {
                int movieId = entry.getKey();
                double rating = entry.getValue();

                // Only recommend movies the target user has not rated yet
                if (!targetUser.getRatings().containsKey(movieId)) {
                    scoreWeightedRatings.put(movieId, scoreWeightedRatings.getOrDefault(movieId, 0.0) + (rating * similarity));
                    scoreSimSums.put(movieId, scoreSimSums.getOrDefault(movieId, 0.0) + similarity);
                }
            }
        }

        // Calculate final predicted ratings
        Map<Integer, Double> predictedRatings = new HashMap<>();
        for (int movieId : scoreWeightedRatings.keySet()) {
            double totalSim = scoreSimSums.get(movieId);
            if (totalSim > 0) {
                predictedRatings.put(movieId, scoreWeightedRatings.get(movieId) / totalSim);
            }
        }

        // Sort movies by predicted rating in descending order
        List<Map.Entry<Integer, Double>> list = new ArrayList<>(predictedRatings.entrySet());
        list.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        List<Movie> recommendations = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, list.size()); i++) {
            recommendations.add(movieMap.get(list.get(i).getKey()));
        }

        return recommendations;
    }
}