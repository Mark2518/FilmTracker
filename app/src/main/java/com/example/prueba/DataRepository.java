package com.example.prueba;

import java.util.ArrayList;
import java.util.List;

public class DataRepository {
    private static DataRepository instance;
    private User currentUser;
    private List<Movie> cachedMovies;
    private TursoClient tursoClient;

    public void cacheMovie(Movie movie) {
        if (!cachedMovies.contains(movie)) {
            cachedMovies.add(movie);
        }
    }

    private DataRepository() {
        currentUser = new User("Guest User");
        cachedMovies = new ArrayList<>();
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
    private static final int PAGE_SIZE = 50;
    private boolean isLoading = false;

    public void refreshMovies(DataCallback callback) {
        if (isLoading) return;
        isLoading = true;
        currentOffset = 0; // Reset offset on refresh
        
        tursoClient.fetchMovies(PAGE_SIZE, currentOffset, new TursoClient.MovieCallback() {
            @Override
            public void onSuccess(List<Movie> movies) {
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
            public void onSuccess(List<Movie> movies) {
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

    public List<Movie> getAllMovies() {
        return new ArrayList<>(cachedMovies);
    }

    public List<Movie> getMovies() {
        List<Movie> movies = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (!m.isSeries()) movies.add(m);
        }
        return movies;
    }

    public List<Movie> getSeries() {
        List<Movie> series = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (m.isSeries()) series.add(m);
        }
        return series;
    }

    public List<Movie> getActionMovies() {
        List<Movie> action = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (hasGenre(m, "action") || hasGenre(m, "adventure") || hasGenre(m, "aventura") || hasGenre(m, "accion") || hasGenre(m, "acción")) {
                action.add(m);
            }
        }
        return action;
    }

    public List<Movie> getSciFiMovies() {
        List<Movie> scifi = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (hasGenre(m, "sci") || hasGenre(m, "future") || hasGenre(m, "ciencia") || hasGenre(m, "space")) {
                scifi.add(m);
            }
        }
        return scifi;
    }

    public List<Movie> getCrimeMovies() {
        List<Movie> crime = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (hasGenre(m, "crime") || hasGenre(m, "drama") || hasGenre(m, "policial") || hasGenre(m, "thriller")) {
                crime.add(m);
            }
        }
        return crime;
    }

    public List<Movie> search(String query) {
        // Local filter as fallback or for instant results
        List<Movie> results = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (m.getTitle().toLowerCase().contains(query.toLowerCase())) {
                results.add(m);
            }
        }
        return results;
    }

    public interface SearchCallback {
        void onResults(List<Movie> movies);
        void onError(String error);
    }

    public void searchMovies(String query, int limit, int offset, SearchCallback callback) {
        tursoClient.searchMovies(query, limit, offset, new TursoClient.MovieCallback() {
            @Override
            public void onSuccess(List<Movie> movies) {
                if (callback != null) callback.onResults(movies);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public List<Movie> getContinueWatchingMovies() {
        List<Movie> inProgress = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (currentUser.getResumePosition(m) > 0) {
                inProgress.add(m);
            }
        }
        return inProgress;
    }

    private List<Movie> recommendedMovies = new ArrayList<>();

    public List<Movie> getRecommendedMovies() {
        return new ArrayList<>(recommendedMovies);
    }

    public void loadRecommendations(DataCallback callback) {
        if (currentUser.getSeenList().isEmpty() && currentUser.getWatchlist().isEmpty() && currentUser.getResumeMovies().isEmpty()) {
            fetchRandomRecs(callback);
        } else {
            java.util.Map<String, Integer> genreCounts = new java.util.HashMap<>();
            addGenresToCount(currentUser.getSeenList(), genreCounts);
            addGenresToCount(currentUser.getWatchlist(), genreCounts);
            addGenresToCount(currentUser.getResumeMovies(), genreCounts);

            List<String> sortedGenres = new ArrayList<>(genreCounts.keySet());
            sortedGenres.sort((a, b) -> genreCounts.get(b) - genreCounts.get(a));
            
            java.util.Map<String, Integer> titleCounts = new java.util.HashMap<>();
            addTitlesToCount(currentUser.getSeenList(), titleCounts);
            addTitlesToCount(currentUser.getWatchlist(), titleCounts);
            addTitlesToCount(currentUser.getResumeMovies(), titleCounts);
            
            List<String> sortedKeywords = new ArrayList<>(titleCounts.keySet());
            sortedKeywords.sort((a, b) -> titleCounts.get(b) - titleCounts.get(a));

            if (sortedGenres.isEmpty() && sortedKeywords.isEmpty()) {
                fetchRandomRecs(callback);
                return;
            }

            tursoClient.fetchRecommendations(sortedGenres, sortedKeywords, new TursoClient.MovieCallback() {
                @Override
                public void onSuccess(List<Movie> movies) {
                    if (movies.isEmpty()) {
                        fetchRandomRecs(callback);
                    } else {
                        recommendedMovies.clear();
                        recommendedMovies.addAll(movies);
                        if (callback != null) callback.onDataLoaded();
                    }
                }

                @Override
                public void onError(Exception e) {
                    fetchRandomRecs(callback);
                }
            });
        }
    }

    private void fetchRandomRecs(DataCallback callback) {
        tursoClient.fetchRandomMovies(new TursoClient.MovieCallback() {
            @Override
            public void onSuccess(List<Movie> movies) {
                recommendedMovies.clear();
                recommendedMovies.addAll(movies);
                if (callback != null) callback.onDataLoaded();
            }
            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    private void addGenresToCount(List<Movie> movies, java.util.Map<String, Integer> counts) {
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

    private void addTitlesToCount(List<Movie> movies, java.util.Map<String, Integer> counts) {
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

    private boolean hasGenre(Movie m, String keyword) {
        if (m.getGenres() == null || m.getGenres().isEmpty()) return false;
        for (String g : m.getGenres()) {
            if (g.toLowerCase().contains(keyword.toLowerCase())) return true;
        }
        return false;
    }
}
