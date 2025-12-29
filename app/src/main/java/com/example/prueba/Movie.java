package com.example.prueba;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Movie implements Serializable {

    private long id;
    private String title;

    private String posterUrl;
    private String description; // overview

    private String duration;
    private List<String> genres;

    private boolean isWatched = false;
    private boolean isInWatchlist = false;
    private boolean isLoading = false;

    public Movie(long id, String title, String posterUrl, String description, String duration, List<String> genres) {
        this.id = id;
        this.title = title;

        this.posterUrl = posterUrl;
        this.description = description;
        this.duration = duration;
        this.genres = genres != null ? genres : new ArrayList<>();
    }

    public static Movie createLoadingMovie() {
        long randomId = -(long)(Math.random() * 1000000);
        Movie m = new Movie(randomId, "", "", "", "", new ArrayList<>());
        m.isLoading = true;
        return m;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }


    public String getPosterUrl() { return posterUrl; }
    public String getDescription() { return description; }
    public String getDuration() { return duration; }
    public List<String> getGenres() { return genres; }


    public String getBackdropUrl() { return posterUrl; }



    public boolean isLoading() { return isLoading; }
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
        return Objects.hash(id);
    }
}