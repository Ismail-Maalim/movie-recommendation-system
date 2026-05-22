package com.recommend.movie.dto;

import com.recommend.movie.model.Movie;

public class MovieRecommendation {
    private Movie movie;
    private String recommendationType; // COLLABORATIVE, CONTENT_BASED, HYBRID, POPULAR
    private double score;
    private int matchPercentage; // e.g. 92
    private String reason; // E.g., "Because you rated 'Inception' highly" or "Popular among users like you"

    public MovieRecommendation() {
    }

    public MovieRecommendation(Movie movie, String recommendationType, double score, int matchPercentage, String reason) {
        this.movie = movie;
        this.recommendationType = recommendationType;
        this.score = score;
        this.matchPercentage = matchPercentage;
        this.reason = reason;
    }

    // Getters and Setters
    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public String getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(String recommendationType) {
        this.recommendationType = recommendationType;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(int matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
