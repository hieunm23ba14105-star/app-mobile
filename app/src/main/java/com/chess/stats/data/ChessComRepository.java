package com.chess.stats.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChessComRepository {
    public interface Callback { void onComplete(LookupResult result); }

    private static final String API = "https://api.chess.com/pub/player/";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void lookup(String rawUsername, Callback callback) {
        final String username = normalize(rawUsername);
        executor.execute(() -> {
            LookupResult result;
            try {
                HttpResult statsResponse = get(API + encodePath(username) + "/stats", false);
                if (statsResponse.code == 404) {
                    result = failure(username, LookupResult.Error.NOT_FOUND, 404);
                } else if (statsResponse.code == 429) {
                    result = failure(username, LookupResult.Error.RATE_LIMITED, 429);
                } else if (statsResponse.code < 200 || statsResponse.code >= 300) {
                    result = failure(username, LookupResult.Error.HTTP, statsResponse.code);
                } else {
                    JSONObject stats = new JSONObject(statsResponse.body);
                    List<GameModeStats> modes = parseModes(stats);
                    if (modes.isEmpty()) {
                        result = failure(username, LookupResult.Error.INVALID_DATA, 0);
                    } else {
                        PlayerProfile profile = fetchProfile(username);
                        result = new LookupResult(username, profile, modes, LookupResult.Error.NONE, 200);
                    }
                }
            } catch (HttpFailure e) {
                result = failure(username, e.code == 404 ? LookupResult.Error.NOT_FOUND : e.code == 429 ? LookupResult.Error.RATE_LIMITED : LookupResult.Error.HTTP, e.code);
            } catch (JSONException | IllegalArgumentException e) {
                result = failure(username, LookupResult.Error.INVALID_DATA, 0);
            } catch (Exception e) {
                result = failure(username, LookupResult.Error.CONNECTION, 0);
            }
            LookupResult completed = result;
            mainHandler.post(() -> callback.onComplete(completed));
        });
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        while (value.startsWith("@")) value = value.substring(1);
        return value.trim();
    }

    private PlayerProfile fetchProfile(String username) {
        try {
            HttpResult response = get(API + encodePath(username), true);
            if (response.code < 200 || response.code >= 300) return new PlayerProfile(username, username, null);
            JSONObject json = new JSONObject(response.body);
            String name = json.optString("name", username);
            if (name.trim().isEmpty() || "null".equals(name)) name = username;
            String avatarUrl = json.optString("avatar", null);
            if (avatarUrl != null && (avatarUrl.isEmpty() || "null".equals(avatarUrl))) avatarUrl = null;
            Bitmap bitmap = avatarUrl == null ? null : fetchAvatar(avatarUrl);
            return new PlayerProfile(username, name, bitmap);
        } catch (Exception ignored) {
            return new PlayerProfile(username, username, null);
        }
    }

    private Bitmap fetchAvatar(String address) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setRequestProperty("User-Agent", "ChessStatsAndroid/1.0");
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return null;
            try (InputStream input = connection.getInputStream()) {
                byte[] bytes = readAll(input, 2 * 1024 * 1024);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private HttpResult get(String address, boolean optionalProfile) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "ChessStatsAndroid/1.0 (public Chess.com stats viewer)");
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 400 ? connection.getInputStream() : connection.getErrorStream();
            String body = stream == null ? "" : new String(readAll(stream, 2 * 1024 * 1024), java.nio.charset.StandardCharsets.UTF_8);
            if (!optionalProfile && (code == 404 || code == 429 || code < 200 || code >= 300)) throw new HttpFailure(code);
            return new HttpResult(code, body);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static byte[] readAll(InputStream stream, int maxBytes) throws Exception {
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > maxBytes) throw new IllegalArgumentException("Response too large");
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private List<GameModeStats> parseModes(JSONObject root) {
        List<GameModeStats> modes = new ArrayList<>();
        java.util.Iterator<String> keys = root.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!key.startsWith("chess_") && !key.startsWith("chess960_") && !key.startsWith("bughouse_")
                    && !key.startsWith("kingofthehill_") && !key.startsWith("threecheck_") && !key.startsWith("crazyhouse_")) continue;
            JSONObject game = root.optJSONObject(key);
            if (game == null) continue;
            String[] parts = key.split("_");
            if (parts.length < 2) continue;
            String speed = parts[parts.length - 1];
            String rule = key.substring(0, key.length() - speed.length() - 1);
            JSONObject last = game.optJSONObject("last");
            JSONObject best = game.optJSONObject("best");
            JSONObject record = game.optJSONObject("record");
            JSONObject tournament = game.optJSONObject("tournament");
            modes.add(new GameModeStats(rule, speed,
                    nullableInt(last, "rating"), nullableLong(last, "date"),
                    nullableInt(best, "rating"), nullableLong(best, "date"),
                    nullableString(best, "game"), nullableInt(record, "win"),
                    nullableInt(record, "draw"), nullableInt(record, "loss"),
                    nullableInt(record, "time_per_move"), nullableDouble(record, "timeout_percent"),
                    nullableInt(tournament, "count"), nullableInt(tournament, "points"), nullableInt(tournament, "highest_finish")));
        }
        modes.sort(Comparator.comparingInt(ChessComRepository::orderOf));
        return modes;
    }

    private static Integer nullableInt(JSONObject object, String key) {
        return object == null || object.isNull(key) || !object.has(key) ? null : object.optInt(key);
    }
    private static Long nullableLong(JSONObject object, String key) {
        return object == null || object.isNull(key) || !object.has(key) ? null : object.optLong(key);
    }
    private static Double nullableDouble(JSONObject object, String key) {
        return object == null || object.isNull(key) || !object.has(key) ? null : object.optDouble(key);
    }
    private static String nullableString(JSONObject object, String key) {
        if (object == null || object.isNull(key)) return null;
        String value = object.optString(key, null);
        return value == null || value.isEmpty() || "null".equals(value) ? null : value;
    }

    private static int orderOf(GameModeStats mode) {
        String rule = mode.rule.toLowerCase(Locale.ROOT);
        String speed = mode.speed.toLowerCase(Locale.ROOT);
        int ruleOrder = rule.equals("chess") ? 0 : rule.equals("chess960") ? 1 : rule.equals("bughouse") ? 2 : rule.equals("kingofthehill") ? 3 : rule.equals("threecheck") ? 4 : 5;
        int speedOrder = speed.equals("rapid") ? 0 : speed.equals("blitz") ? 1 : speed.equals("bullet") ? 2 : speed.equals("daily") ? 3 : 4;
        return ruleOrder * 10 + speedOrder;
    }

    private static String encodePath(String value) throws Exception { return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20"); }
    private static LookupResult failure(String username, LookupResult.Error error, int code) { return new LookupResult(username, null, null, error, code); }
    private static final class HttpResult { final int code; final String body; HttpResult(int code, String body) { this.code = code; this.body = body; } }
    private static final class HttpFailure extends Exception { final int code; HttpFailure(int code) { this.code = code; } }
}
