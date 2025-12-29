package com.example.prueba;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class MovieDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "movies.db";
    private static final int DATABASE_VERSION = 4;

    public static final String TABLE_MOVIES = "movies";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_RUNTIME = "runtime";
    public static final String COLUMN_OVERVIEW = "overview";
    public static final String COLUMN_POSTER_PATH = "poster_path";
    public static final String COLUMN_GENRES = "genres";

    // --- NUEVAS TABLAS DE USUARIO ---
    private static final String TABLE_WATCHLIST = "watchlist";
    private static final String TABLE_SEEN = "seen";
    private static final String TABLE_RESUME = "resume";

    private static final String COL_MOVIE_ID = "movie_id";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_POSITION = "position";

    public MovieDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
            //No hace falta
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //No hace falta
    }

    public void addToWatchlist(long movieId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MOVIE_ID, movieId);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_WATCHLIST, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removeFromWatchlist(long movieId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WATCHLIST, COL_MOVIE_ID + " = ?", new String[]{String.valueOf(movieId)});
    }

    public Set<Long> getWatchlistIds() {
        Set<Long> ids = new HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_MOVIE_ID + " FROM " + TABLE_WATCHLIST, null);
        if (cursor.moveToFirst()) {
            do {
                ids.add(cursor.getLong(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return ids;
    }


    public void addToSeen(long movieId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MOVIE_ID, movieId);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_SEEN, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removeFromSeen(long movieId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SEEN, COL_MOVIE_ID + " = ?", new String[]{String.valueOf(movieId)});
    }

    public Set<Long> getSeenIds() {
        Set<Long> ids = new HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_MOVIE_ID + " FROM " + TABLE_SEEN, null);
        if (cursor.moveToFirst()) {
            do {
                ids.add(cursor.getLong(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return ids;
    }


    public void saveProgress(long movieId, int position) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MOVIE_ID, movieId);
        values.put(COL_POSITION, position);
        db.insertWithOnConflict(TABLE_RESUME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Map<Long, Integer> getResumePositions() {
        Map<Long, Integer> map = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_MOVIE_ID + ", " + COL_POSITION + " FROM " + TABLE_RESUME, null);
        if (cursor.moveToFirst()) {
            do {
                map.put(cursor.getLong(0), cursor.getInt(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return map;
    }


}