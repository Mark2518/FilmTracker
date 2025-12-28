package com.example.prueba;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TursoClient {
    private static final String TURSO_URL = BuildConfig.TURSO_URL + "/v2/pipeline";
    private static final String TURSO_TOKEN = "Bearer " + BuildConfig.TURSO_TOKEN;

    private final OkHttpClient client;
    private final Gson gson;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public TursoClient() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface MovieCallback {
        void onSuccess(List<Movie> movies);
        void onError(Exception e);
    }

    public void fetchMovies(int limit, int offset, MovieCallback callback) {
        String sql = "SELECT * FROM movies LIMIT " + limit + " OFFSET " + offset;
        executeSql(sql, callback);
    }

    public void searchMovies(String query, int limit, int offset, MovieCallback callback) {
        // Basic sanitization
        String safeQuery = query.replace("'", "''");
        String sql = "SELECT * FROM movies WHERE title LIKE '%" + safeQuery + "%' LIMIT " + limit + " OFFSET " + offset;
        executeSql(sql, callback);
    }

    public void fetchRandomMovies(MovieCallback callback) {
        String sql = "SELECT * FROM movies ORDER BY RANDOM() LIMIT 20";
        executeSql(sql, callback);
    }

    public void fetchRecommendations(List<String> genres, List<String> titleKeywords, MovieCallback callback) {
        if ((genres == null || genres.isEmpty()) && (titleKeywords == null || titleKeywords.isEmpty())) {
            fetchRandomMovies(callback);
            return;
        }
        
        // Take top 3 of each to avoid huge query
        List<String> topGenres = (genres != null && genres.size() > 3) ? genres.subList(0, 3) : (genres != null ? genres : new ArrayList<>());
        List<String> topKeywords = (titleKeywords != null && titleKeywords.size() > 3) ? titleKeywords.subList(0, 3) : (titleKeywords != null ? titleKeywords : new ArrayList<>());

        StringBuilder sb = new StringBuilder("SELECT * FROM movies WHERE ");
        boolean hasConditions = false;

        if (!topGenres.isEmpty()) {
            sb.append("(");
            for (int i = 0; i < topGenres.size(); i++) {
                String g = topGenres.get(i).replace("'", "''").trim();
                sb.append("genre LIKE '%").append(g).append("%'");
                if (i < topGenres.size() - 1) {
                    sb.append(" OR ");
                }
            }
            sb.append(")");
            hasConditions = true;
        }

        if (!topKeywords.isEmpty()) {
            if (hasConditions) sb.append(" OR ");
            sb.append("(");
            for (int i = 0; i < topKeywords.size(); i++) {
                String k = topKeywords.get(i).replace("'", "''").trim();
                sb.append("title LIKE '%").append(k).append("%'");
                if (i < topKeywords.size() - 1) {
                    sb.append(" OR ");
                }
            }
            sb.append(")");
        }

        sb.append(" ORDER BY RANDOM() LIMIT 20");
        executeSql(sb.toString(), callback);
    }

    private void executeSql(String sql, MovieCallback callback) {
        executor.execute(() -> {
            try {
                // Construct JSON Body for Turso
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
                        .url(TURSO_URL)
                        .addHeader("Authorization", TURSO_TOKEN)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    
                    String responseBody = response.body().string();
                    Log.d("TursoClient", "Response: " + responseBody);
                    
                    List<Movie> movies = parseTursoResponse(responseBody);
                    mainHandler.post(() -> callback.onSuccess(movies));
                }
            } catch (Exception e) {
                Log.e("TursoClient", "Error executing SQL: " + sql, e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private List<Movie> parseTursoResponse(String jsonResponse) {
        List<Movie> movies = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray results = root.getAsJsonArray("results");
            
            if (results.size() > 0) {
                JsonObject result = results.get(0).getAsJsonObject();
                
                // Check if there is an error in the response item
                if (result.has("error")) {
                    Log.e("TursoClient", "SQL Error: " + result.get("error").toString());
                    return movies;
                }
                
                JsonObject responseObj = result.getAsJsonObject("response");
                if (responseObj == null) {
                    // Sometimes direct result if not batched exactly same way? 
                    // Turso v2 pipeline structure:
                    // { "results": [ { "type": "ok", "response": { "cols": [...], "rows": [...] } } ] }
                    return movies;
                }
                
                // Correctly extracting the result object

                JsonObject innerResult = responseObj.getAsJsonObject("result");
                JsonArray cols = innerResult.getAsJsonArray("cols");
                JsonArray rows = innerResult.getAsJsonArray("rows");

                // Map column names to indices
                int colId = -1, colTitle = -1, colRuntime = -1, colOverview = -1, colPoster = -1, colBackdrop = -1, colRating = -1, colYear = -1, colDirector = -1, colCast = -1, colIsSeries = -1, colGenre = -1;
                
                for (int i = 0; i < cols.size(); i++) {
                    JsonObject col = cols.get(i).getAsJsonObject();
                    String name = col.get("name").getAsString();
                    switch (name) {
                        case "id": colId = i; break;
                        case "title": colTitle = i; break;
                        case "runtime": colRuntime = i; break;
                        case "overview": colOverview = i; break;
                        case "poster_path": colPoster = i; break;
                        case "backdrop_url": colBackdrop = i; break;
                        case "rating": colRating = i; break;
                        case "year": colYear = i; break;
                        case "director": colDirector = i; break;
                        case "cast": colCast = i; break;
                        case "is_series": colIsSeries = i; break;
                        case "genre": 
                        case "genres": colGenre = i; break;
                    }
                }

                for (JsonElement rowElem : rows) {
                    JsonArray row = rowElem.getAsJsonArray();
                    // Extract values based on indices. CAREFUL: value types (type: "integer", value: "1") or just raw values?
                    // LibSQL usually returns raw values in rows array: [ 1, "Inception", ... ]
                    
                    try {
                        long id = getLong(row, colId);
                        String title = getString(row, colTitle);
                        int runtime = getInt(row, colRuntime);
                        String overview = getString(row, colOverview);
                        String posterPath = getString(row, colPoster);
                        String genreStr = getString(row, colGenre);
                        List<String> genreList = new ArrayList<>();
                        if (genreStr != null && !genreStr.isEmpty()) {
                            for (String g : genreStr.split(",")) {
                                genreList.add(g.trim());
                            }
                        }
                        
                        // Construct URLs
                        String fullPosterUrl = posterPath.startsWith("http") ? posterPath : "https://image.tmdb.org/t/p/w500" + posterPath;
                        String fullBackdropUrl = "https://image.tmdb.org/t/p/w1280" + posterPath; // Mocking backdrop if empty
                        
                        // Parse duration
                        int hours = runtime / 60;
                        int minutes = runtime % 60;
                        String durationStr = hours + "h " + minutes + "m";
                        
                        Movie m = new Movie(
                            id,
                            title,
                            fullPosterUrl,
                            fullBackdropUrl,
                            0.0f, // Default rating if DB is null, or extract from db if present
                            overview,
                            "", // Year
                            "", // Director
                            durationStr,
                            "", // Cast
                            genreList, // Genre List
                            false // isSeries check if column exists
                        );
                        movies.add(m);
                    } catch (Exception e) {
                        Log.e("TursoParsing", "Error parsing row", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("TursoParsing", "JSON structure mismatch", e);
        }
        return movies;
    }
    
    private String getString(JsonArray row, int index) {
        if (index == -1 || index >= row.size() || row.get(index).isJsonNull()) return "";
        JsonElement el = row.get(index);
        if (el.isJsonObject() && el.getAsJsonObject().has("value")) return el.getAsJsonObject().get("value").getAsString(); // Sometimes enclosed
        return el.getAsString();
    }

    private long getLong(JsonArray row, int index) {
        if (index == -1 || index >= row.size() || row.get(index).isJsonNull()) return 0;
        JsonElement el = row.get(index);
        if (el.isJsonObject() && el.getAsJsonObject().has("value")) return el.getAsJsonObject().get("value").getAsLong();
        return el.getAsLong();
    }
    
    private int getInt(JsonArray row, int index) {
        if (index == -1 || index >= row.size() || row.get(index).isJsonNull()) return 0;
        JsonElement el = row.get(index);
        if (el.isJsonObject() && el.getAsJsonObject().has("value")) return el.getAsJsonObject().get("value").getAsInt();
        return el.getAsInt();
    }
}
