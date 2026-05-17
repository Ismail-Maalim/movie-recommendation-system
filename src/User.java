//package com.recommendation.model;

import java.util.HashMap;
import java.util.Map;

public class User {
    private final int id;
    // Maps Movie ID -> Rating Value
    private final Map<Integer, Double> ratings;

    public User(int id) {
        this.id = id;
        this.ratings = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public void addRating(int movieId, double rating) {
        ratings.put(movieId, rating);
    }

    public Map<Integer, Double> getRatings() {
        return ratings;
    }

    public Double getRatingForMovie(int movieId) {
        return ratings.get(movieId);
    }
}