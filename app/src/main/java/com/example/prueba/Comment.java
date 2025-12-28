package com.example.prueba;

import java.io.Serializable;

public class Comment implements Serializable {
    private String username;
    private String content;
    private float rating;
    private String avatarUrl;

    public Comment(String username, String content, float rating, String avatarUrl) {
        this.username = username;
        this.content = content;
        this.rating = rating;
        this.avatarUrl = avatarUrl;
    }

    public String getUsername() { return username; }
    public String getContent() { return content; }
    public float getRating() { return rating; }
    public String getAvatarUrl() { return avatarUrl; }
}
