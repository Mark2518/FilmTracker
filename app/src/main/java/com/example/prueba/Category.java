package com.example.prueba;

import java.util.List;

public class Category {
    private String title;
    private List<Movie> movies;

    public Category(String title, List<Movie> movies) {
        this.title = title;
        this.movies = movies;
    }

    public String getTitle() { return title; }
    public List<Movie> getMovies() { return movies; }
}
