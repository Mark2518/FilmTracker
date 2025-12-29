package com.example.prueba;

import android.content.Context;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class DataRepository {
    private static DataRepository instance;
    private User currentUser;
    private Set<Movie> cachedMovies;
    private TursoClient tursoClient;

    // Referencia a la Base de Datos Local
    private MovieDatabaseHelper dbHelper;

    // Caché en memoria de los IDs guardados (para acceso rápido)
    private Set<Long> localWatchlistIds;
    private Set<Long> localSeenIds;
    private Map<Long, Integer> localResumeMap;

    public void cacheMovie(Movie movie) {
        if (!cachedMovies.contains(movie)) {
            cachedMovies.add(movie);
        }
    }

    private DataRepository() {
        currentUser = new User();
        cachedMovies = new LinkedHashSet<>();
        tursoClient = new TursoClient();
    }

    public static synchronized DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    // --- NUEVO: Inicialización de BD ---
    public void init(Context context) {
        dbHelper = new MovieDatabaseHelper(context);
        // Cargamos lo que hay guardado en el móvil
        localWatchlistIds = dbHelper.getWatchlistIds();
        localSeenIds = dbHelper.getSeenIds();
        localResumeMap = dbHelper.getResumePositions();
    }

    public void clearCache() {
        if (cachedMovies != null) {
            cachedMovies.clear();
        }
    }

    // --- MÉTODOS DE PERSISTENCIA ---
    public void addToWatchlist(Movie movie) {
        if (!currentUser.isInWatchlist(movie)) {
            currentUser.addToWatchlist(movie);
            if (dbHelper != null) {
                dbHelper.addToWatchlist(movie.getId()); // Guardar en BD
                localWatchlistIds.add(movie.getId());
            }
        }
    }

    public void removeFromWatchlist(Movie movie) {
        if (currentUser.isInWatchlist(movie)) {
            currentUser.removeFromWatchlist(movie);
            if (dbHelper != null) {
                dbHelper.removeFromWatchlist(movie.getId()); // Borrar de BD
                localWatchlistIds.remove(movie.getId());
            }
        }
    }

    public void addToSeen(Movie movie) {
        if (!currentUser.isSeen(movie)) {
            currentUser.addToSeen(movie);
            if (dbHelper != null) {
                dbHelper.addToSeen(movie.getId()); // Guardar en BD
                localSeenIds.add(movie.getId());
            }
        }
    }

    public void removeFromSeen(Movie movie) {
        if (currentUser.isSeen(movie)) {
            currentUser.removeFromSeen(movie);
            if (dbHelper != null) {
                dbHelper.removeFromSeen(movie.getId()); // Borrar de BD
                localSeenIds.remove(movie.getId());
            }
        }
    }

    public void saveProgress(Movie movie, int minutes) {
        currentUser.setResumePosition(movie, minutes);
        if (dbHelper != null) {
            dbHelper.saveProgress(movie.getId(), minutes); // Guardar en BD
            localResumeMap.put(movie.getId(), minutes);
        }
    }

    // Sincroniza las películas que llegan de Internet con lo que tenemos guardado
    private void syncWithLocalData(Set<Movie> movies) {
        if (localWatchlistIds == null) return;

        for (Movie m : movies) {
            if (localWatchlistIds.contains(m.getId())) {
                m.setInWatchlist(true);
                currentUser.addToWatchlist(m);
            }
            if (localSeenIds.contains(m.getId())) {
                m.setWatched(true);
                currentUser.addToSeen(m);
            }
            if (localResumeMap.containsKey(m.getId())) {
                int min = localResumeMap.get(m.getId());
                currentUser.setResumePosition(m, min);
            }
        }
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
        currentOffset = 0;
        cachedMovies.clear();

        // Reiniciamos usuario en memoria para reconstruirlo desde BD + Internet
        currentUser = new User();

        tursoClient.fetchMovies(50, 0, new TursoClient.MovieCallback() {
            @Override
            public void onSuccess(Set<Movie> movies) {
                // Aquí cruzamos los datos nuevos con la BD local
                syncWithLocalData(movies);

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
                        syncWithLocalData(movies); // Sincronizar
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



    public User getCurrentUser() { return currentUser; }
    public Set<Movie> getAllMovies() { return new LinkedHashSet<>(cachedMovies); }


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
    // Métodos delegados actualizados (ya implementados arriba)

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