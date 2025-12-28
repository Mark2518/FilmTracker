package com.example.prueba;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.LinkedHashSet;
import java.util.Set;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TursoClient {
    private static final String TURSO_URL = BuildConfig.TURSO_URL + "/v2/pipeline";
    private static final String TURSO_TOKEN = "Bearer " + BuildConfig.TURSO_TOKEN;

    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public TursoClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface MovieCallback {
        void onSuccess(Set<Movie> movies);
        void onError(Exception e);
    }

    // --- MÉTODOS DE BÚSQUEDA ---
    public void fetchMovies(int limit, int offset, MovieCallback callback) {
        String sql = "SELECT rowid, * FROM peliculas LIMIT " + limit + " OFFSET " + offset;
        executeSql(sql, callback);
    }

    public void fetchRandomMovies(MovieCallback callback) {
        String sql = "SELECT rowid, * FROM peliculas ORDER BY RANDOM() LIMIT 20";
        executeSql(sql, callback);
    }

    public void fetchMoviesByGenre(String genre, int limit, MovieCallback callback) {
        String safeGenre = genre.replace("'", "''");
        String sql = "SELECT rowid, * FROM peliculas WHERE genres LIKE '%" + safeGenre + "%' ORDER BY RANDOM() LIMIT " + limit;
        executeSql(sql, callback);
    }

    public void searchMovies(String query, int limit, int offset, MovieCallback callback) {
        String safeQuery = query.trim().replace("'", "''");
        if (safeQuery.isEmpty()) {
            mainHandler.post(() -> callback.onSuccess(new LinkedHashSet<>()));
            return;
        }

        String[] terms = safeQuery.split("\\s+");
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT rowid, * FROM peliculas WHERE ");

        for (int i = 0; i < terms.length; i++) {
            if (i > 0) sqlBuilder.append(" AND ");
            String termClean = terms[i].toLowerCase().replace("-", "").replace(":", "");
            sqlBuilder.append("(LOWER(REPLACE(REPLACE(title, '-', ''), ' ', '')) LIKE '%")
                    .append(termClean).append("%')");
        }
        sqlBuilder.append(" LIMIT ").append(limit).append(" OFFSET ").append(offset);
        executeSql(sqlBuilder.toString(), callback);
    }

    // (Opcional) fetchRecommendations simplificado
    public void fetchRecommendations(List<String> genres, List<String> titleKeywords, MovieCallback callback) {
        if (genres == null || genres.isEmpty()) {
            fetchRandomMovies(callback);
            return;
        }
        List<String> topGenres = (genres.size() > 3) ? genres.subList(0, 3) : genres;

        StringBuilder sb = new StringBuilder("SELECT rowid, * FROM peliculas WHERE ");
        sb.append("(");
        for (int i = 0; i < topGenres.size(); i++) {
            String g = topGenres.get(i).replace("'", "''").trim();
            sb.append("genres LIKE '%").append(g).append("%'");
            if (i < topGenres.size() - 1) sb.append(" OR ");
        }
        sb.append(") ORDER BY RANDOM() LIMIT 20");
        executeSql(sb.toString(), callback);
    }

    // --- PARSEO Y EJECUCIÓN ---
    private void executeSql(String sql, MovieCallback callback) {
        executor.execute(() -> {
            try {
                JsonObject stmt = new JsonObject();
                stmt.addProperty("sql", sql);
                JsonObject executeRequest = new JsonObject();
                executeRequest.addProperty("type", "execute");
                executeRequest.add("stmt", stmt);
                JsonArray requests = new JsonArray();
                requests.add(executeRequest);
                JsonObject root = new JsonObject();
                root.add("requests", requests);

                RequestBody body = RequestBody.create(root.toString(), MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(TURSO_URL).addHeader("Authorization", TURSO_TOKEN).post(body).build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Error HTTP: " + response);
                    Set<Movie> movies = parseTursoResponse(response.body().string());
                    mainHandler.post(() -> callback.onSuccess(movies));
                }
            } catch (Exception e) {
                Log.e("TursoClient", "Error", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private Set<Movie> parseTursoResponse(String jsonResponse) {
        Set<Movie> movies = new LinkedHashSet<>();
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray results = root.getAsJsonArray("results");
            if (results.size() > 0) {
                JsonObject resultItem = results.get(0).getAsJsonObject();
                if (resultItem.has("error")) return movies;

                JsonObject responseObj = resultItem.getAsJsonObject("response");
                if (responseObj == null) return movies;
                JsonObject innerResult = responseObj.getAsJsonObject("result");
                JsonArray cols = innerResult.getAsJsonArray("cols");
                JsonArray rows = innerResult.getAsJsonArray("rows");

                int colId = -1, colTitle = -1, colRuntime = -1, colOverview = -1, colPoster = -1, colGenre = -1;
                for (int i = 0; i < cols.size(); i++) {
                    String name = cols.get(i).getAsJsonObject().get("name").getAsString();
                    switch (name) {
                        case "rowid": colId = i; break;
                        case "title": colTitle = i; break;
                        case "runtime": colRuntime = i; break;
                        case "overview": colOverview = i; break;
                        case "poster_path": colPoster = i; break;
                        case "genres": colGenre = i; break;
                    }
                }

                for (JsonElement rowElem : rows) {
                    JsonArray row = rowElem.getAsJsonArray();
                    try {
                        String title = getString(row, colTitle);
                        long id = (colId != -1) ? getLong(row, colId) : title.hashCode();
                        int runtime = getInt(row, colRuntime);
                        String overview = getString(row, colOverview);
                        String rawPoster = getString(row, colPoster);

                        // Parseo de géneros
                        String genreStr = getString(row, colGenre);
                        List<String> genreList = new ArrayList<>();
                        if (!genreStr.isEmpty()) {
                            String cleanGenres = genreStr.replace("[", "").replace("]", "").replace("'", "");
                            for (String g : cleanGenres.split(",")) genreList.add(g.trim());
                        }




                        int hours = runtime / 60;
                        int minutes = runtime % 60;
                        String durationStr = (hours > 0 ? hours + "h " : "") + minutes + "m";

                        // AQUÍ: Usamos el nuevo constructor limpio
                        movies.add(new Movie(id, title, rawPoster, overview, durationStr, genreList));

                    } catch (Exception e) {
                        Log.e("TursoParsing", "Skip row", e);
                    }
                }
            }
        } catch (Exception e) { Log.e("TursoParsing", "Error JSON", e); }
        return movies;
    }

    private String getString(JsonArray row, int index) {
        if (index == -1 || index >= row.size() || row.get(index).isJsonNull()) return "";
        JsonElement el = row.get(index);
        if (el.isJsonObject() && el.getAsJsonObject().has("value")) return el.getAsJsonObject().get("value").getAsString();
        return el.getAsString();
    }
    private long getLong(JsonArray row, int index) {
        if (index == -1 || index >= row.size() || row.get(index).isJsonNull()) return 0;
        JsonElement el = row.get(index);
        String val = (el.isJsonObject() && el.getAsJsonObject().has("value")) ? el.getAsJsonObject().get("value").getAsString() : el.getAsString();
        try { return Long.parseLong(val); } catch (Exception e) { return 0; }
    }
    private int getInt(JsonArray row, int index) {
        if (index == -1 || index >= row.size() || row.get(index).isJsonNull()) return 0;
        JsonElement el = row.get(index);
        String val = (el.isJsonObject() && el.getAsJsonObject().has("value")) ? el.getAsJsonObject().get("value").getAsString() : el.getAsString();
        try { return Integer.parseInt(val); } catch (Exception e) { return 0; }
    }
}