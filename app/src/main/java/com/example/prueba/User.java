package com.example.prueba;

import java.util.LinkedHashSet;
import java.util.Set;

public class User {
    private Set<Movie> watchlist;
    private Set<Movie> seenList;
    private java.util.Map<Long, Integer> resumePositions;
    private Set<Movie> resumeMovies;

    public User() {
        this.watchlist = new LinkedHashSet<>();
        this.seenList = new LinkedHashSet<>();
        this.resumePositions = new java.util.HashMap<>();
        this.resumeMovies = new LinkedHashSet<>();
    }


    public Set<Movie> getWatchlist() { return watchlist; }
    public Set<Movie> getSeenList() { return seenList; }
    public Set<Movie> getResumeMovies() { return resumeMovies; }

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
