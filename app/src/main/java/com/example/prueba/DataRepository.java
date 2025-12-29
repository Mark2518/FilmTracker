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


    public void clearCache() {
        if (cachedMovies != null) {
            cachedMovies.clear();
        }
    }
    // -------------------------------

    public void init(android.content.Context context) { }

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
        currentOffset = 0;
        cachedMovies.clear();

        tursoClient.fetchMovies(50, 0, new TursoClient.MovieCallback() {
            @Override
            public void onSuccess(Set<Movie> movies) {
                synchronized (cachedMovies) {
                    cachedMovies.addAll(movies);
                }
                mergeUserData();
                if (callback != null) callback.onDataLoaded();
                ensureGenreCoverage(callback);
            }

            @Override
            public void onError(Exception e) {
                isLoading = false;
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    private void ensureGenreCoverage(DataCallback callback) {
        String[] targetGenres = {"Action", "Drama", "Comedy", "Romance", "Documentary", "Adventure"};
        final java.util.concurrent.atomic.AtomicInteger pendingRequests = new java.util.concurrent.atomic.AtomicInteger(0);

        for (String genre : targetGenres) {
            int count = 0;
            for (Movie m : cachedMovies) {
                if (hasGenre(m, genre)) count++;
            }

            if (count < 10) {
                int needed = 10 - count;
                pendingRequests.incrementAndGet();
                tursoClient.fetchMoviesByGenre(genre, needed, new TursoClient.MovieCallback() {
                    @Override
                    public void onSuccess(Set<Movie> movies) {
                        synchronized (cachedMovies) {
                            cachedMovies.addAll(movies);
                        }
                        if (pendingRequests.decrementAndGet() == 0) finishRefresh(callback);
                    }
                    @Override
                    public void onError(Exception e) {
                        if (pendingRequests.decrementAndGet() == 0) finishRefresh(callback);
                    }
                });
            }
        }
        if (pendingRequests.get() == 0) finishRefresh(callback);
    }

    private void mergeUserData() {
        synchronized (cachedMovies) {
            for (Movie m : currentUser.getWatchlist()) if (!cachedMovies.contains(m)) cachedMovies.add(m);
            for (Movie m : currentUser.getSeenList()) if (!cachedMovies.contains(m)) cachedMovies.add(m);
            for (Movie m : currentUser.getResumeMovies()) if (!cachedMovies.contains(m)) cachedMovies.add(m);
        }
    }

    private void finishRefresh(DataCallback callback) {
        mergeUserData();
        isLoading = false;
        if (callback != null) callback.onDataLoaded();
    }

    public void loadMoreMovies(DataCallback callback) {
        if (isLoading) return;
        isLoading = true;
        tursoClient.fetchMovies(PAGE_SIZE, currentOffset, new TursoClient.MovieCallback() {
            @Override
            public void onSuccess(Set<Movie> movies) {
                if (!movies.isEmpty()) {
                    for (Movie m : movies) if (!cachedMovies.contains(m)) cachedMovies.add(m);
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

    public User getCurrentUser() { return currentUser; }
    public Set<Movie> getAllMovies() { return new LinkedHashSet<>(cachedMovies); }
    public Set<Movie> getMovies() {
        Set<Movie> movies = new LinkedHashSet<>();
        for (Movie m : cachedMovies) if (!m.isSeries()) movies.add(m);
        return movies;
    }
    public Set<Movie> getSeries() {
        Set<Movie> series = new LinkedHashSet<>();
        for (Movie m : cachedMovies) if (m.isSeries()) series.add(m);
        return series;
    }
    public Set<Movie> getActionMovies() {
        Set<Movie> action = new LinkedHashSet<>();
        for (Movie m : cachedMovies) if (hasGenre(m, "action") || hasGenre(m, "adventure") || hasGenre(m, "aventura")) action.add(m);
        return action;
    }
    public Set<Movie> getSciFiMovies() {
        Set<Movie> scifi = new LinkedHashSet<>();
        for (Movie m : cachedMovies) if (hasGenre(m, "sci") || hasGenre(m, "future") || hasGenre(m, "ciencia")) scifi.add(m);
        return scifi;
    }
    public Set<Movie> getCrimeMovies() {
        Set<Movie> crime = new LinkedHashSet<>();
        for (Movie m : cachedMovies) if (hasGenre(m, "crime") || hasGenre(m, "drama") || hasGenre(m, "thriller")) crime.add(m);
        return crime;
    }

    public Set<Movie> search(String query) {
        String normalizedQuery = normalizeForSearch(query);
        Set<Movie> results = new LinkedHashSet<>();
        for (Movie m : cachedMovies) {
            String normalizedTitle = normalizeForSearch(m.getTitle());
            if (normalizedTitle.contains(normalizedQuery)) results.add(m);
        }
        return results;
    }
    private String normalizeForSearch(String s) {
        return s.toLowerCase().replace("-", "").replace(" ", "").replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u");
    }

    public interface SearchCallback { void onResults(Set<Movie> movies); void onError(String error); }
    public void searchMovies(String query, int limit, int offset, SearchCallback callback) {
        tursoClient.searchMovies(query, limit, offset, new TursoClient.MovieCallback() {
            @Override public void onSuccess(Set<Movie> movies) { if (callback != null) callback.onResults(movies); }
            @Override public void onError(Exception e) { if (callback != null) callback.onError(e.getMessage()); }
        });
    }
    public Set<Movie> getContinueWatchingMovies() {
        Set<Movie> inProgress = new LinkedHashSet<>();
        for (Movie m : cachedMovies) if (currentUser.getResumePosition(m) > 0) inProgress.add(m);
        return inProgress;
    }
    private Set<Movie> recommendedMovies = new LinkedHashSet<>();
    public Set<Movie> getRecommendedMovies() { return new LinkedHashSet<>(recommendedMovies); }
    private boolean recommendationsDirty = true;
    public void addToWatchlist(Movie movie) { if (!currentUser.isInWatchlist(movie)) { currentUser.addToWatchlist(movie); recommendationsDirty = true; } }
    public void addToSeen(Movie movie) { if (!currentUser.isSeen(movie)) { currentUser.addToSeen(movie); recommendationsDirty = true; } }
    public void removeFromWatchlist(Movie movie) { if (currentUser.isInWatchlist(movie)) { currentUser.removeFromWatchlist(movie); recommendationsDirty = true; } }
    public void removeFromSeen(Movie movie) { if (currentUser.isSeen(movie)) { currentUser.removeFromSeen(movie); recommendationsDirty = true; } }
    public void refreshRecommendations(DataCallback callback) { recommendationsDirty = true; loadRecommendations(callback); }
    public void loadRecommendations(DataCallback callback) {
        if (!recommendationsDirty && !recommendedMovies.isEmpty()) { if (callback != null) callback.onDataLoaded(); return; }
        Set<Movie> candidates = new LinkedHashSet<>();
        if (cachedMovies.isEmpty()) { recommendationsDirty = false; if (callback != null) callback.onDataLoaded(); return; }
        List<Movie> candidateList = new ArrayList<>(cachedMovies);
        java.util.Collections.shuffle(candidateList);
        recommendedMovies.clear();
        for (int i = 0; i < Math.min(candidateList.size(), 20); i++) recommendedMovies.add(candidateList.get(i));
        recommendationsDirty = false;
        if (callback != null) callback.onDataLoaded();
    }
    private void addGenresToCount(java.util.Collection<Movie> movies, java.util.Map<String, Integer> counts) {
        for (Movie m : movies) if (m.getGenres() != null) for (String g : m.getGenres()) counts.put(g, counts.getOrDefault(g, 0) + 1);
    }
    private boolean hasAnyGenre(Movie m, java.util.Collection<String> targetGenres) {
        if (m.getGenres() == null) return false;
        for (String g : m.getGenres()) if (targetGenres.contains(g.toLowerCase())) return true;
        return false;
    }
    public List<String> getSignificantGenres() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        addGenresToCount(cachedMovies, counts);
        List<String> sorted = new ArrayList<>(counts.keySet());
        sorted.sort((a, b) -> counts.get(b) - counts.get(a));
        if (sorted.size() > 5) return sorted.subList(0, 5);
        return sorted;
    }
    public Set<Movie> getMoviesForGenre(String genre) {
        List<Movie> matches = new ArrayList<>();
        for (Movie m : cachedMovies) if (hasGenre(m, genre)) matches.add(m);
        java.util.Collections.shuffle(matches);
        if (matches.size() > 10) return new LinkedHashSet<>(matches.subList(0, 10));
        return new LinkedHashSet<>(matches);
    }
    private boolean hasGenre(Movie m, String keyword) {
        if (m.getGenres() == null || m.getGenres().isEmpty()) return false;
        for (String g : m.getGenres()) if (g.toLowerCase().contains(keyword.toLowerCase())) return true;
        return false;
    }
}