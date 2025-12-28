package com.example.prueba;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class MovieDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "movies.db";
    private static final int DATABASE_VERSION = 3; // Subimos versión para limpiar tablas viejas

    public static final String TABLE_MOVIES = "movies";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_RUNTIME = "runtime";
    public static final String COLUMN_OVERVIEW = "overview";
    public static final String COLUMN_POSTER_PATH = "poster_path";
    public static final String COLUMN_GENRES = "genres";

    public MovieDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MOVIES_TABLE = "CREATE TABLE " + TABLE_MOVIES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_RUNTIME + " INTEGER,"
                + COLUMN_OVERVIEW + " TEXT,"
                + COLUMN_POSTER_PATH + " TEXT,"
                + COLUMN_GENRES + " TEXT"
                + ")";
        db.execSQL(CREATE_MOVIES_TABLE);
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
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                int runtime = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RUNTIME));
                String overview = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OVERVIEW));
                String posterUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POSTER_PATH));
                String genresStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENRES));

                // Conversión de minutos a "1h 30m"
                int hours = runtime / 60;
                int minutes = runtime % 60;
                String durationStr = (hours > 0 ? hours + "h " : "") + minutes + "m";

                List<String> genreList = new ArrayList<>();
                if (genresStr != null && !genresStr.isEmpty()) {
                    for (String g : genresStr.split(",")) genreList.add(g.trim());
                }

                // Usamos el constructor limpio
                movies.add(new Movie(id, title, posterUrl, overview, durationStr, genreList));

            } while (cursor.moveToNext());
        }
        cursor.close();
        return movies;
    }
}