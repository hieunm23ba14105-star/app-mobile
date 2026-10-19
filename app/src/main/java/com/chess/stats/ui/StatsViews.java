package com.chess.stats.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chess.stats.R;
import com.chess.stats.data.GameModeStats;

import java.util.List;
import java.util.Locale;

public final class StatsViews {
    public interface OnModeSelected { void onSelected(int index); }
    private static final int INK = R.color.ink;
    private static final int MUTED = R.color.muted;
    private static final int BLUE = R.color.primary;
    private static final int PALE = R.color.tile_surface;
    private static final int BORDER = R.color.border;
    private static final int GREEN = R.color.win;
    private static final int DRAW = R.color.draw;
    private static final int RED = R.color.loss;

    private StatsViews() {}

    public static void render(Activity activity, LinearLayout target, List<GameModeStats> modes,
                              int selectedIndex, OnModeSelected listener) {
        target.removeAllViews();
        if (modes == null || modes.isEmpty()) return;
        int selected = Math.max(0, Math.min(selectedIndex, modes.size() - 1));
        target.addView(buildGamesCard(activity, modes, selected, listener));
        addGap(activity, target, 22);
        target.addView(buildSummaryCard(activity, modes.get(selected)));
    }

    private static View buildGamesCard(Activity activity, List<GameModeStats> modes, int selected,
                                       OnModeSelected listener) {
        LinearLayout card = card(activity);
        LinearLayout heading = new LinearLayout(activity);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(activity, Translations.text("games"), 17, INK, true);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView chevron = new ImageView(activity);
        chevron.setImageResource(R.drawable.ic_chevron_down);
        chevron.setColorFilter(activity.getColor(MUTED));
        chevron.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        heading.addView(chevron, new LinearLayout.LayoutParams(dp(activity, 28), dp(activity, 28)));
        card.addView(heading);

        android.widget.GridLayout grid = new android.widget.GridLayout(activity);
        grid.setColumnCount(4);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(-1, -2);
        gridParams.topMargin = dp(activity, 14);
        card.addView(grid, gridParams);
        for (int i = 0; i < modes.size(); i++) {
            GameModeStats mode = modes.get(i);
            LinearLayout tile = makeTile(activity, mode, i == selected);
            tile.setOnClickListener(v -> listener.onSelected(modes.indexOf(mode)));
            android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(activity, 146);
            params.columnSpec = android.widget.GridLayout.spec(i % 4, 1f);
            params.setMargins(dp(activity, 2), dp(activity, 2), dp(activity, 2), dp(activity, 4));
            grid.addView(tile, params);
        }
        return card;
    }

    private static LinearLayout makeTile(Activity activity, GameModeStats mode, boolean selected) {
        LinearLayout tile = new LinearLayout(activity);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(activity, 3), dp(activity, 4), dp(activity, 3), dp(activity, 4));
        tile.setBackgroundColor(Color.TRANSPARENT);

        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconFor(mode));
        icon.setColorFilter(activity.getColor(BLUE));
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));
        icon.setBackground(rounded(activity, selected ? R.color.tile_selected : PALE,
                selected ? R.color.tile_selected_border : PALE, 100, selected ? 2 : 0));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(activity, 60), dp(activity, 60));
        tile.addView(icon, iconParams);

        TextView label = text(activity, modeName(mode), 12, INK, false);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setMinHeight(dp(activity, 34));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(activity, 6);
        tile.addView(label, labelParams);

        TextView rating = text(activity, Translations.rating(mode.currentRating), 11, BLUE, true);
        rating.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ratingParams = new LinearLayout.LayoutParams(-1, -2);
        ratingParams.topMargin = dp(activity, 3);
        tile.addView(rating, ratingParams);
        TextView caption = text(activity, Translations.text("rating"), 9, MUTED, false);
        caption.setGravity(Gravity.CENTER);
        tile.addView(caption);
        return tile;
    }

    private static View buildSummaryCard(Activity activity, GameModeStats mode) {
        LinearLayout card = card(activity);
        LinearLayout heading = new LinearLayout(activity);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        String titleText = modeName(mode) + " · " + Translations.text("overview");
        heading.addView(text(activity, titleText, 17, INK, false), new LinearLayout.LayoutParams(0, -2, 1));
        ImageView chart = new ImageView(activity);
        chart.setImageResource(R.drawable.ic_chart);
        chart.setColorFilter(activity.getColor(INK));
        chart.setPadding(dp(activity, 9), dp(activity, 9), dp(activity, 9), dp(activity, 9));
        chart.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        chart.setBackground(rounded(activity, PALE, PALE, 14, 0));
        heading.addView(chart, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 42)));
        card.addView(heading);

        LinearLayout ratings = new LinearLayout(activity);
        ratings.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout current = new LinearLayout(activity);
        current.setOrientation(LinearLayout.VERTICAL);
        current.addView(text(activity, Translations.text("current"), 11, MUTED, false));
        TextView currentValue = text(activity, Translations.rating(mode.currentRating), 38, INK, true);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-1, -2);
        valueParams.topMargin = dp(activity, 2);
        current.addView(currentValue, valueParams);
        current.addView(text(activity, Translations.text("updated") + " " + Translations.date(mode.lastDate), 11, MUTED, false));
        ratings.addView(current, new LinearLayout.LayoutParams(0, -2, 1));

        View divider = new View(activity);
        divider.setBackgroundColor(activity.getColor(BORDER));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(dp(activity, 1), dp(activity, 88));
        dividerParams.setMargins(dp(activity, 10), 0, dp(activity, 10), 0);
        ratings.addView(divider, dividerParams);

        LinearLayout peak = new LinearLayout(activity);
        peak.setOrientation(LinearLayout.VERTICAL);
        peak.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        peak.addView(text(activity, Translations.text("peak"), 11, MUTED, false));
        TextView peakValue = text(activity, Translations.rating(mode.peakRating), 23, INK, true);
        peak.addView(peakValue);
        peak.addView(text(activity, Translations.date(mode.peakDate), 10, MUTED, false));
        if (mode.peakGameUrl != null) {
            TextView link = text(activity, Translations.text("peak_game"), 10, BLUE, true);
            link.setClickable(true);
            link.setOnClickListener(v -> {
                try { activity.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mode.peakGameUrl))); }
                catch (Exception ignored) { }
            });
            LinearLayout.LayoutParams linkParams = new LinearLayout.LayoutParams(-2, -2);
            linkParams.topMargin = dp(activity, 6);
            peak.addView(link, linkParams);
        }
        ratings.addView(peak, new LinearLayout.LayoutParams(dp(activity, 110), -2));
        LinearLayout.LayoutParams ratingParams = new LinearLayout.LayoutParams(-1, -2);
        ratingParams.topMargin = dp(activity, 16);
        card.addView(ratings, ratingParams);

        if (mode.wins != null || mode.draws != null || mode.losses != null) {
            View line = new View(activity);
            line.setBackgroundColor(activity.getColor(BORDER));
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(-1, dp(activity, 1));
            lineParams.topMargin = dp(activity, 14);
            lineParams.bottomMargin = dp(activity, 12);
            card.addView(line, lineParams);
            LinearLayout resultHeading = new LinearLayout(activity);
            resultHeading.setGravity(Gravity.CENTER_VERTICAL);
            resultHeading.addView(text(activity, Translations.text("results"), 12, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
            resultHeading.addView(text(activity, mode.getGameCount() + " " + Translations.text("games_count"), 10, MUTED, false));
            card.addView(resultHeading);
            card.addView(recordBar(activity, mode));
            LinearLayout counts = new LinearLayout(activity);
            counts.setGravity(Gravity.CENTER_VERTICAL);
            addCount(activity, counts, "wins", mode.wins);
            addCount(activity, counts, "draws", mode.draws);
            addCount(activity, counts, "losses", mode.losses);
            LinearLayout.LayoutParams countsParams = new LinearLayout.LayoutParams(-1, -2);
            countsParams.topMargin = dp(activity, 9);
            card.addView(counts, countsParams);
        }
        addExtras(activity, card, mode);
        return card;
    }

    private static View recordBar(Activity activity, GameModeStats mode) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackground(rounded(activity, BORDER, BORDER, 10, 0));
        bar.setClipToOutline(true);
        Integer[] values = {mode.wins, mode.draws, mode.losses};
        int[] colors = {GREEN, DRAW, RED};
        for (int i = 0; i < values.length; i++) {
            int count = values[i] == null ? 0 : Math.max(0, values[i]);
            if (count == 0) continue;
            View segment = new View(activity);
            segment.setBackgroundColor(activity.getColor(colors[i]));
            bar.addView(segment, new LinearLayout.LayoutParams(0, dp(activity, 8), count));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(activity, 8));
        params.topMargin = dp(activity, 8);
        bar.setLayoutParams(params);
        return bar;
    }

    private static void addCount(Activity activity, LinearLayout row, String key, Integer value) {
        LinearLayout item = new LinearLayout(activity);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.addView(text(activity, Translations.text(key) + " ", 11, MUTED, false));
        item.addView(text(activity, String.valueOf(value == null ? 0 : value), 13, INK, true));
        row.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private static void addExtras(Activity activity, LinearLayout card, GameModeStats mode) {
        List<String> labels = new java.util.ArrayList<>();
        List<String> values = new java.util.ArrayList<>();
        if (mode.timePerMove != null) { labels.add(Translations.text("time_per_move")); values.add(formatDuration(mode.timePerMove)); }
        if (mode.timeoutPercent != null) { labels.add(Translations.text("timeout")); values.add(String.format(Locale.getDefault(), "%.0f%%", mode.timeoutPercent)); }
        if (mode.tournamentCount != null) { labels.add(Translations.text("tournaments")); values.add(String.valueOf(mode.tournamentCount)); }
        if (mode.tournamentPoints != null) { labels.add(Translations.text("points")); values.add(String.valueOf(mode.tournamentPoints)); }
        if (mode.highestFinish != null) { labels.add(Translations.text("highest_finish")); values.add(String.valueOf(mode.highestFinish)); }
        if (labels.isEmpty()) return;
        View line = new View(activity);
        line.setBackgroundColor(activity.getColor(BORDER));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(-1, dp(activity, 1));
        lineParams.topMargin = dp(activity, 14);
        card.addView(line, lineParams);
        for (int i = 0; i < labels.size(); i++) {
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(text(activity, labels.get(i), 11, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(text(activity, values.get(i), 13, INK, true));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.topMargin = dp(activity, 8);
            card.addView(row, rowParams);
        }
    }

    private static String formatDuration(int seconds) {
        if (seconds <= 0) return "—";
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        if (hours > 0) return hours + " giờ " + minutes + " phút";
        return minutes + " phút";
    }

    private static String modeName(GameModeStats mode) {
        String speed = Translations.text(mode.speed);
        return "chess".equals(mode.rule) ? speed : Translations.text(mode.rule) + " · " + speed;
    }
    private static int iconFor(GameModeStats mode) {
        if ("rapid".equals(mode.speed)) return R.drawable.ic_rapid;
        if ("blitz".equals(mode.speed)) return R.drawable.ic_blitz;
        if ("bullet".equals(mode.speed)) return R.drawable.ic_bullet;
        return R.drawable.ic_calendar;
    }
    private static LinearLayout card(Activity activity) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(activity, 16), dp(activity, 17), dp(activity, 16), dp(activity, 15));
        layout.setBackground(rounded(activity, R.color.white, R.color.white, 20, 0));
        return layout;
    }
    private static TextView text(Activity activity, String value, int sp, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(activity.getColor(color));
        view.setTextSize(sp);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private static GradientDrawable rounded(Activity activity, int fill, int stroke, int radiusDp, int strokeDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(activity.getColor(fill));
        shape.setCornerRadius(dp(activity, radiusDp));
        if (strokeDp > 0) shape.setStroke(dp(activity, strokeDp), activity.getColor(stroke));
        return shape;
    }
    private static void addGap(Activity activity, LinearLayout parent, int height) {
        View gap = new View(activity);
        parent.addView(gap, new LinearLayout.LayoutParams(1, dp(activity, height)));
    }
    private static int dp(Activity activity, int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
}
