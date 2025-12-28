package com.example.prueba;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private List<Movie> watchlist;
    private List<Movie> seenList;
    private java.util.Map<Long, Integer> resumePositions;
    private List<Movie> resumeMovies;

    public User(String username) {
        this.username = username;
        this.watchlist = new ArrayList<>();
        this.seenList = new ArrayList<>();
        this.resumePositions = new java.util.HashMap<>();
        this.resumeMovies = new ArrayList<>();
    }

    public String getUsername() { return username; }
    
    public List<Movie> getWatchlist() { return watchlist; }
    public List<Movie> getSeenList() { return seenList; }
    public List<Movie> getResumeMovies() { return resumeMovies; }

    public void addToWatchlist(Movie movie) {
        if (!watchlist.contains(movie)) {
            watchlist.add(movie);
            movie.setInWatchlist(true);
        }
    }

    public void removeFromWatchlist(Movie movie) {
        watchlist.remove(movie);
        movie.setInWatchlist(false);
    }

    public void addToSeen(Movie movie) {
        if (!seenList.contains(movie)) {
            seenList.add(movie);
            movie.setWatched(true);
        }
    }

    public void removeFromSeen(Movie movie) {
        seenList.remove(movie);
        movie.setWatched(false);
    }
    
    public boolean isInWatchlist(Movie movie) {
        return watchlist.contains(movie);
    }

    public boolean isSeen(Movie movie) {
        return seenList.contains(movie);
    }

    public void setResumePosition(Movie movie, int minutes) {
        resumePositions.put(movie.getId(), minutes);
        if (!resumeMovies.contains(movie)) {
            resumeMovies.add(movie);
        }
    }

    public int getResumePosition(Movie movie) {
        if (resumePositions.containsKey(movie.getId())) {
            return resumePositions.get(movie.getId());
        }
        return 0; // Default to 0
    }
}
