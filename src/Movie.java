
//package com.recommendation.model;

public class Movie {
    private final int id;
    private final String title;

    public Movie(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title + " (ID: " + id + ")";
    }
}