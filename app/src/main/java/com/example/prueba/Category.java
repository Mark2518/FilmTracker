package com.example.prueba;

import java.util.Set;

public class Category {
    private String title;
    private Set<Movie> movies;

    private Runnable onRefresh;

    public Category(String title, Set<Movie> movies) {
        this(title, movies, null);
    }

    public Category(String title, Set<Movie> movies, Runnable onRefresh) {
        this.title = title;
        this.movies = movies;
        this.onRefresh = onRefresh;
    }

    public String getTitle() { return title; }
    public Set<Movie> getMovies() { return movies; }
    public Runnable getOnRefresh() { return onRefresh; }
}
