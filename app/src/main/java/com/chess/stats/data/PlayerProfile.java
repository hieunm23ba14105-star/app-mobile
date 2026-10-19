package com.chess.stats.data;

import android.graphics.Bitmap;

public final class PlayerProfile {
    public final String username;
    public final String name;
    public final Bitmap avatar;

    public PlayerProfile(String username, String name, Bitmap avatar) {
        this.username = username;
        this.name = name;
        this.avatar = avatar;
    }
}
