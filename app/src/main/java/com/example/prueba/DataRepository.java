package com.example.prueba;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DataRepository {
    private static DataRepository instance;
    private User currentUser;
    private Set<Movie> cachedMovies;
    private TursoClient tursoClient;

    public void cacheMovie(Movie movie) {
        if (!cachedMovies.contains(movie)) {
            cachedMovies.add(movie);
        }
    }

    private DataRepository() {
        currentUser = new User("Guest User");
        cachedMovies = new LinkedHashSet<>();
        tursoClient = new TursoClient();
    }

    public static synchronized DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    public void init(android.content.Context context) {
        // Context not strictly needed for Turso HTTP client unless using ConnectivityManager
    }
    
    public interface DataCallback {
        void onDataLoaded();
        void onError(String error);
    }

    private int currentOffset = 0;
    private static final int PAGE_SIZE = 10;
    private boolean isLoading = false;

    public void refreshMovies(DataCallback callback) {
        if (isLoading) return;
        isLoading = true;
        currentOffset = 0; // Reset offset on refresh
        
        tursoClient.fetchMovies(PAGE_SIZE, currentOffset, new TursoClient.MovieCallback() {
            @Override
            public void onSuccess(Set<Movie> movies) {
                cachedMovies.clear();
                cachedMovies.addAll(movies);
                
                // Merge user specific movies back into cache to persist their state in UI
                for (Movie m : currentUser.getWatchlist()) {
                    if (!cachedMovies.contains(m)) cachedMovies.add(m);
                }
                for (Movie m : currentUser.getSeenList()) {
                    if (!cachedMovies.contains(m)) cachedMovies.add(m);
                }
                for (Movie m : currentUser.getResumeMovies()) {
                    if (!cachedMovies.contains(m)) cachedMovies.add(m);
                }

                currentOffset += movies.size();
                isLoading = false;
                if (callback != null) callback.onDataLoaded();
            }

            @Override
            public void onError(Exception e) {
                isLoading = false;
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void loadMoreMovies(DataCallback callback) {
        if (isLoading) return;
        isLoading = true;

        tursoClient.fetchMovies(PAGE_SIZE, currentOffset, new TursoClient.MovieCallback() {
            @Override
            public void onSuccess(Set<Movie> movies) {
                if (!movies.isEmpty()) {
                    for (Movie m : movies) {
                        if (!cachedMovies.contains(m)) {
                            cachedMovies.add(m);
                        }
                    }
                    currentOffset += movies.size();
                }
                isLoading = false;
                if (callback != null) callback.onDataLoaded();
            }

            @Override
            public void onError(Exception e) {
                isLoading = false;
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Set<Movie> getAllMovies() {
        return new LinkedHashSet<>(cachedMovies);
    }

    public Set<Movie> getMovies() {
        Set<Movie> movies = new LinkedHashSet<>();
        for (Movie m : cachedMovies) {
            if (!m.isSeries()) movies.add(m);
        }
        return movies;
    }

    public Set<Movie> getSeries() {
        Set<Movie> series = new LinkedHashSet<>();
        for (Movie m : cachedMovies) {
            if (m.isSeries()) series.add(m);
        }
        return series;
    }

    public Set<Movie> getActionMovies() {
        Set<Movie> action = new LinkedHashSet<>();
        for (Movie m : cachedMovies) {
            if (hasGenre(m, "action") || hasGenre(m, "adventure") || hasGenre(m, "aventura") || hasGenre(m, "accion") || hasGenre(m, "acción")) {
                action.add(m);
            }
        }
        return action;
    }

    public Set<Movie> getSciFiMovies() {
        Set<Movie> scifi = new LinkedHashSet<>();
        for (Movie m : cachedMovies) {
            if (hasGenre(m, "sci") || hasGenre(m, "future") || hasGenre(m, "ciencia") || hasGenre(m, "space")) {
                scifi.add(m);
            }
        }
        return scifi;
    }

    public Set<Movie> getCrimeMovies() {
        Set<Movie> crime = new LinkedHashSet<>();
        for (Movie m : cachedMovies) {
            if (hasGenre(m, "crime") || hasGenre(m, "drama") || hasGenre(m, "policial") || hasGenre(m, "thriller")) {
                crime.add(m);
            }
        }
        return crime;
    }

    public Set<Movie> search(String query) {
        // Local filter as fallback or for instant results
        Set<Movie> results = new LinkedHashSet<>();
        for (Movie m : cachedMovies) {
            if (m.getTitle().toLowerCase().contains(query.toLowerCase())) {
                results.add(m);
            }
        }
        return results;
    }

    public interface SearchCallback {
        void onResults(Set<Movie> movies);
        void onError(String error);
    }

    public void searchMovies(String query, int limit, int offset, SearchCallback callback) {
        tursoClient.searchMovies(query, limit, offset, new TursoClient.MovieCallback() {
            @Override
            public void onSuccess(Set<Movie> movies) {
                if (callback != null) callback.onResults(movies);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public Set<Movie> getContinueWatchingMovies() {
        Set<Movie> inProgress = new LinkedHashSet<>();
        for (Movie m : cachedMovies) {
            if (currentUser.getResumePosition(m) > 0) {
                inProgress.add(m);
            }
        }
        return inProgress;
    }

    private Set<Movie> recommendedMovies = new LinkedHashSet<>();

    public Set<Movie> getRecommendedMovies() {
        return new LinkedHashSet<>(recommendedMovies);
    }

    private boolean recommendationsDirty = true;

    public void addToWatchlist(Movie movie) {
        if (!currentUser.isInWatchlist(movie)) {
            currentUser.addToWatchlist(movie);
            recommendationsDirty = true;
        }
    }

    public void addToSeen(Movie movie) {
        if (!currentUser.isSeen(movie)) {
            currentUser.addToSeen(movie);
            recommendationsDirty = true;
        }
    }

    public void removeFromWatchlist(Movie movie) {
        if (currentUser.isInWatchlist(movie)) {
            currentUser.removeFromWatchlist(movie);
            recommendationsDirty = true;
        }
    }

    public void removeFromSeen(Movie movie) {
        if (currentUser.isSeen(movie)) {
            currentUser.removeFromSeen(movie);
            recommendationsDirty = true;
        }
    }

    public void refreshRecommendations(DataCallback callback) {
        // Force refresh
        recommendationsDirty = true;
        loadRecommendations(callback);
    }

    public void loadRecommendations(DataCallback callback) {
        if (!recommendationsDirty && !recommendedMovies.isEmpty()) {
            if (callback != null) callback.onDataLoaded();
            return;
        }

        // Logic relies on local cache for "optimization" as per request to avoid API spam if possible,
        // but since we want "20 random movies" initially or "based on genres", we can synthesize this.
        // Actually, the request says "random 20 initially" or "same genres as seen/watchlist".

        Set<Movie> candidates = new LinkedHashSet<>();
        
        Set<String> targetGenres = new LinkedHashSet<>();
        java.util.Map<String, Integer> genreCounts = new java.util.HashMap<>();
        addGenresToCount(currentUser.getSeenList(), genreCounts);
        addGenresToCount(currentUser.getWatchlist(), genreCounts);

        if (genreCounts.isEmpty()) {
            // Random 20 from cached movies (assuming cachedMovies has enough diversity or is the full DB)
            // If cachedMovies is small, this might just return what we have.
            candidates.addAll(cachedMovies);
        } else {
            // Filter by genre
            for (String genre : genreCounts.keySet()) {
                targetGenres.add(genre.toLowerCase());
            }

            for (Movie m : cachedMovies) {
                // Don't recommend what is already seen or in watchlist
                if (currentUser.isSeen(m) || currentUser.isInWatchlist(m)) continue;
                
                if (hasAnyGenre(m, targetGenres)) {
                    if (!candidates.contains(m)) candidates.add(m);
                }
            }
        }

        // If candidates are still empty (e.g. strict filtering), fallback to all cached
        if (candidates.isEmpty()) {
            for (Movie m : cachedMovies) {
                if (!currentUser.isSeen(m) && !currentUser.isInWatchlist(m)) {
                    candidates.add(m);
                }
            }
        }

        // Pick 20 random
        List<Movie> candidateList = new ArrayList<>(candidates);
        java.util.Collections.shuffle(candidateList);
        recommendedMovies.clear();
        for (int i = 0; i < Math.min(candidateList.size(), 20); i++) {
            recommendedMovies.add(candidateList.get(i));
        }
        
        recommendationsDirty = false;
        if (callback != null) callback.onDataLoaded();
    }
    
    private void addGenresToCount(java.util.Collection<Movie> movies, java.util.Map<String, Integer> counts) {
        for (Movie m : movies) {
            if (m.getGenres() != null && !m.getGenres().isEmpty()) {
                for (String g : m.getGenres()) {
                    if (g != null && !g.isEmpty()) {
                        counts.put(g, counts.getOrDefault(g, 0) + 1);
                    }
                }
            }
        }
    }

    private void addTitlesToCount(java.util.Collection<Movie> movies, java.util.Map<String, Integer> counts) {
        for (Movie m : movies) {
            if (m.getTitle() != null) {
                String[] words = m.getTitle().split("\\s+");
                for (String w : words) {
                    w = w.replaceAll("[^a-zA-Z0-9]", "");
                    if (w.length() > 3) {
                        counts.put(w, counts.getOrDefault(w, 0) + 1);
                    }
                }
            }
        }
    }

    private boolean hasAnyGenre(Movie m, java.util.Collection<String> targetGenres) {
        if (m.getGenres() == null) return false;
        for (String g : m.getGenres()) {
            if (targetGenres.contains(g.toLowerCase())) return true;
        }
        return false;
    }

    // Genre Sections Logic
    public List<String> getSignificantGenres() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        addGenresToCount(cachedMovies, counts);
        
        List<String> sorted = new ArrayList<>(counts.keySet());
        sorted.sort((a, b) -> counts.get(b) - counts.get(a));
        
        // Return top 5 or so
        if (sorted.size() > 5) return sorted.subList(0, 5);
        return sorted;
    }

    public Set<Movie> getMoviesForGenre(String genre) {
        List<Movie> matches = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (hasGenre(m, genre)) {
                matches.add(m);
            }
        }
        // Limit 10, random
        java.util.Collections.shuffle(matches);
        if (matches.size() > 10) return new LinkedHashSet<>(matches.subList(0, 10));
        return new LinkedHashSet<>(matches);
    }
    
    // Helper for manual refresh of a genre section - just calls getMoviesForGenre again
    // since it shuffles internally.

    

    private boolean hasGenre(Movie m, String keyword) {
        if (m.getGenres() == null || m.getGenres().isEmpty()) return false;
        for (String g : m.getGenres()) {
            if (g.toLowerCase().contains(keyword.toLowerCase())) return true;
        }
        return false;
    }
}
