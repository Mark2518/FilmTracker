package com.example.prueba;

import java.io.Serializable;

public class Movie implements Serializable {
    private String title;
    private String posterUrl;
    private String backdropUrl;
    private float rating;
    private String description;
    private String year;
    private String director;
    private String duration;
    private String cast;
    private java.util.List<String> genres;


    private long id;
    private boolean isSeries;
    private boolean isWatched;
    private boolean isInWatchlist;
    private boolean isLoading = false;

    public Movie(long id, String title, String posterUrl, String backdropUrl, float rating, String description, String year, String director, String duration, String cast, java.util.List<String> genres, boolean isSeries) {
        this.id = id;
        this.title = title;
        this.posterUrl = posterUrl;
        this.backdropUrl = backdropUrl;
        this.rating = rating;
        this.description = description;
        this.year = year;
        this.director = director;
        this.duration = duration;
        this.cast = cast;
        this.genres = genres;
        this.isSeries = isSeries;
        this.isWatched = false;
        this.isInWatchlist = false;
        this.isLoading = false;
    }

    // Static factory for loading placeholder
    public static Movie createLoadingMovie() {
        Movie m = new Movie(0, "", "", "", 0, "", "", "", "", "", new java.util.ArrayList<>(), false);
        m.isLoading = true;
        // Use random negative ID to avoid collision
        m.id = -(long)(Math.random() * 1000000);
        return m;
    }

    public boolean isLoading() { return isLoading; }

    // Constructor for backward compatibility (defaults to Movie, random ID if needed but better to enforce ID)
    // We will use hash of title if no ID provided in legacy calls, or 0.
    public Movie(String title, String posterUrl, String backdropUrl, float rating, String description, String year, String director, String duration, String cast) {
        this(title.hashCode(), title, posterUrl, backdropUrl, rating, description, year, director, duration, cast, new java.util.ArrayList<>(), false);
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getPosterUrl() { return posterUrl; }
    public String getBackdropUrl() { return backdropUrl; }
    public float getRating() { return rating; }
    public String getDescription() { return description; }
    public String getYear() { return year; }
    public String getDirector() { return director; }
    public String getDuration() { return duration; }
    public String getCast() { return cast; }
    public java.util.List<String> getGenres() { return genres; }
    
    public boolean isSeries() { return isSeries; }
    public boolean isWatched() { return isWatched; }
    public void setWatched(boolean watched) { isWatched = watched; }
    public boolean isInWatchlist() { return isInWatchlist; }
    public void setInWatchlist(boolean inWatchlist) { isInWatchlist = inWatchlist; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return id == movie.id;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}
