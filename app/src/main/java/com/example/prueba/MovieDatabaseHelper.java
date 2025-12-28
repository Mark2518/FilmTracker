package com.example.prueba;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MovieDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "movies.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_MOVIES = "movies";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_RUNTIME = "runtime";
    public static final String COLUMN_OVERVIEW = "overview";
    public static final String COLUMN_POSTER_PATH = "poster_path";

    // Extra columns to match Movie class functionality
    public static final String COLUMN_BACKDROP_URL = "backdrop_url";
    public static final String COLUMN_RATING = "rating";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_DIRECTOR = "director";
    public static final String COLUMN_CAST = "cast";
    public static final String COLUMN_IS_SERIES = "is_series";


    private Context context;

    public MovieDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MOVIES_TABLE = "CREATE TABLE " + TABLE_MOVIES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_RUNTIME + " INTEGER,"
                + COLUMN_OVERVIEW + " TEXT,"
                + COLUMN_POSTER_PATH + " TEXT,"
                + COLUMN_BACKDROP_URL + " TEXT,"
                + COLUMN_RATING + " REAL,"
                + COLUMN_YEAR + " TEXT,"
                + COLUMN_DIRECTOR + " TEXT,"
                + COLUMN_CAST + " TEXT,"
                + COLUMN_IS_SERIES + " INTEGER"
                + ")";
        db.execSQL(CREATE_MOVIES_TABLE);
        // Data will be loaded from external source (Turso) or remain empty initially
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MOVIES);
        onCreate(db);
    }

    
    public List<Movie> getAllMovies() {
        List<Movie> movies = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MOVIES, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                int runtime = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RUNTIME));
                String overview = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OVERVIEW));
                String posterPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POSTER_PATH));
                
                // Construct full URLs if needed, or leave as path. 
                // The current app seems to use full URLs.
                String fullPosterUrl = "https://image.tmdb.org/t/p/w500" + posterPath;
                String fullBackdropUrl = "https://image.tmdb.org/t/p/w1280" + posterPath; // Fallback to poster if no backdrop

                // New Movie fields map
                // Duration is int in CSV (minutes), String "XH Ym" in Movie class
                int hours = runtime / 60;
                int minutes = runtime % 60;
                String durationStr = hours + "h " + minutes + "m";

                Movie movie = new Movie(
                        id,
                        title,
                        fullPosterUrl,
                        fullBackdropUrl,
                        0.0f, // Rating default
                        overview,
                        "", // Year default
                        "", // Director default
                        durationStr,
                        "", // Cast default
                        false // isSeries default
                );
                movies.add(movie);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return movies;
    }
}
