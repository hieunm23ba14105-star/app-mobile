package com.chess.stats;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.chess.stats.data.ChessComRepository;
import com.chess.stats.data.LookupResult;
import com.chess.stats.data.PlayerProfile;
import com.chess.stats.ui.StatsViews;
import com.chess.stats.ui.Translations;

public class MainActivity extends Activity {
    private static final String LANDING_VIDEO = "hero_1780586045036.webm";
    private ChessComRepository repository;
    private FrameLayout root;
    private FrameLayout landingLayer;
    private FrameLayout heroSurface;
    private ScrollView resultsScroll;
    private LinearLayout resultsContent;
    private LinearLayout profileCard;
    private LinearLayout statusRow;
    private EditText usernameInput;
    private Button lookupButton;
    private ProgressBar loadingIndicator;
    private TextView statusMessage;
    private TextView playerName;
    private TextView playerSubtitle;
    private ImageView playerAvatar;
    private ImageButton changeUserButton;
    private TextureView videoView;
    private View heroScrim;
    private MediaPlayer mediaPlayer;
    private Surface videoSurface;
    private boolean videoPrepared;
    private LookupResult currentResult;
    private int selectedMode;
    private boolean isCollapsed;
    private boolean isLoading;
    private boolean videoAllowed = true;
    private android.animation.ValueAnimator layoutAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        repository = new ChessComRepository();
        bindViews();
        setupActions();
        setupVideo();
        resultsScroll.setOnScrollChangeListener((View view, int x, int y, int oldX, int oldY) ->
                getWindow().setStatusBarColor(y < dp(100)
                        ? getColor(R.color.hero_background) : getColor(R.color.canvas)));
        setSystemBarColors(true);
    }

    private void bindViews() {
        root = findViewById(R.id.root);
        landingLayer = findViewById(R.id.landing_layer);
        heroSurface = findViewById(R.id.hero_surface);
        resultsScroll = findViewById(R.id.results_scroll);
        resultsContent = findViewById(R.id.results_content);
        profileCard = findViewById(R.id.profile_card);
        statusRow = findViewById(R.id.status_row);
        usernameInput = findViewById(R.id.username_input);
        lookupButton = findViewById(R.id.lookup_button);
        loadingIndicator = findViewById(R.id.loading_indicator);
        statusMessage = findViewById(R.id.status_message);
        playerName = findViewById(R.id.player_name);
        playerSubtitle = findViewById(R.id.player_subtitle);
        playerAvatar = findViewById(R.id.player_avatar);
        changeUserButton = findViewById(R.id.change_user_button);
        videoView = findViewById(R.id.landing_video);
        heroScrim = findViewById(R.id.hero_scrim);
        playerAvatar.setClipToOutline(true);
        lookupButton.setBackgroundTintList(null);
        changeUserButton.setBackgroundTintList(null);
    }

    private void setupActions() {
        lookupButton.setOnClickListener(v -> submitLookup());
        usernameInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                submitLookup();
                return true;
            }
            return false;
        });
        changeUserButton.setOnClickListener(v -> returnToLookup());
    }

    private void setupVideo() {
        videoView.setAlpha(1f);
        videoView.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                updateVideoTransform(right - left, bottom - top);
            }
        });
        videoView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                startBackgroundVideo(texture);
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                updateVideoTransform(width, height);
            }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                releaseBackgroundVideo();
                return true;
            }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture texture) { }
        });
    }

    private void startBackgroundVideo(SurfaceTexture texture) {
        releaseBackgroundVideo();
        try {
            videoSurface = new Surface(texture);
            MediaPlayer player = new MediaPlayer();
            mediaPlayer = player;
            player.setSurface(videoSurface);
            player.setVolume(0f, 0f);
            player.setLooping(true);
            player.setOnVideoSizeChangedListener((source, videoWidth, videoHeight) ->
                    updateVideoTransform(videoView.getWidth(), videoView.getHeight()));
            player.setOnPreparedListener(source -> {
                if (mediaPlayer != source) return;
                videoPrepared = true;
                updateVideoTransform(videoView.getWidth(), videoView.getHeight());
                videoView.setAlpha(1f);
                if (videoAllowed && landingLayer.getVisibility() == View.VISIBLE) source.start();
            });
            player.setOnErrorListener((source, what, extra) -> {
                videoView.setAlpha(0f);
                releaseMediaPlayerOnly();
                return true;
            });
            try (AssetFileDescriptor videoAsset = getAssets().openFd(LANDING_VIDEO)) {
                player.setDataSource(videoAsset.getFileDescriptor(), videoAsset.getStartOffset(), videoAsset.getLength());
            }
            player.prepareAsync();
        } catch (Exception ignored) {
            videoView.setAlpha(0f);
            releaseBackgroundVideo();
        }
    }

    private void updateVideoTransform(int viewWidth, int viewHeight) {
        if (mediaPlayer == null || viewWidth <= 0 || viewHeight <= 0) return;
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();
        if (videoWidth <= 0 || videoHeight <= 0) return;
        // Preserve the whole video frame and its aspect ratio inside the full-screen blue hero.
        float videoAspect = (float) videoWidth / videoHeight;
        float viewAspect = (float) viewWidth / viewHeight;
        float scaleX = videoAspect < viewAspect ? videoAspect / viewAspect : 1f;
        float scaleY = videoAspect > viewAspect ? viewAspect / videoAspect : 1f;
        Matrix transform = new Matrix();
        transform.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f);
        videoView.setTransform(transform);
    }

    private void releaseMediaPlayerOnly() {
        if (mediaPlayer != null) {
            try { mediaPlayer.setOnPreparedListener(null); mediaPlayer.setOnErrorListener(null); mediaPlayer.stop(); } catch (Exception ignored) { }
            try { mediaPlayer.release(); } catch (Exception ignored) { }
            mediaPlayer = null;
        }
        videoPrepared = false;
    }

    private void releaseBackgroundVideo() {
        releaseMediaPlayerOnly();
        if (videoSurface != null) {
            try { videoSurface.release(); } catch (Exception ignored) { }
            videoSurface = null;
        }
    }

    private void submitLookup() {
        String username = ChessComRepository.normalize(usernameInput.getText().toString());
        if (username.isEmpty()) {
            showError(LookupResult.Error.INVALID_DATA, 0);
            usernameInput.requestFocus();
            return;
        }
        usernameInput.setText(username);
        setLookupLoading(true);
        statusMessage.setText(Translations.text("loading"));
        statusMessage.setTextColor(getColor(R.color.muted));
        statusRow.setVisibility(View.VISIBLE);
        hideKeyboard();
        profileCard.post(() -> {
            if (isLoading && !isCollapsed) collapseLanding();
        });
        repository.lookup(username, result -> {
            setLookupLoading(false);
            if (result.isSuccess()) {
                currentResult = result;
                selectedMode = 0;
                renderResults();
                showResultsPage();
            } else {
                currentResult = null;
                showError(result.error, result.httpCode);
                showLandingPage();
            }
        });
    }

    private void showError(LookupResult.Error error, int httpCode) {
        String message;
        if (error == LookupResult.Error.NOT_FOUND) message = Translations.text("not_found");
        else if (error == LookupResult.Error.RATE_LIMITED) message = Translations.text("rate_limited");
        else if (error == LookupResult.Error.HTTP) message = String.format(Translations.text("http_error"), httpCode);
        else if (error == LookupResult.Error.CONNECTION) message = Translations.text("connection");
        else if (error == LookupResult.Error.INVALID_DATA
                && !ChessComRepository.normalize(usernameInput.getText().toString()).isEmpty()) {
            message = Translations.text("invalid_data");
        } else message = Translations.text("empty");
        statusMessage.setText(message);
        statusMessage.setTextColor(getColor(R.color.error));
        loadingIndicator.setVisibility(View.GONE);
        statusRow.setVisibility(View.VISIBLE);
    }

    private void setLookupLoading(boolean loading) {
        isLoading = loading;
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        lookupButton.setEnabled(!loading);
        lookupButton.setAlpha(loading ? 0.7f : 1f);
    }

    private void renderResults() {
        if (currentResult == null) return;
        PlayerProfile profile = currentResult.profile;
        String name = profile == null ? currentResult.username : profile.name;
        if (name == null || name.trim().isEmpty()) name = currentResult.username;
        playerName.setText(name);
        updatePlayerSubtitle();
        Bitmap avatar = profile == null ? null : profile.avatar;
        if (avatar != null) {
            playerAvatar.setPadding(0, 0, 0, 0);
            playerAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            playerAvatar.setImageBitmap(avatar);
        } else {
            playerAvatar.setPadding(dp(10), dp(10), dp(10), dp(10));
            playerAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            playerAvatar.setImageResource(R.drawable.ic_person);
        }
        playerAvatar.setContentDescription(name);
        StatsViews.render(this, resultsContent, currentResult.gameModes, selectedMode, index -> {
            selectedMode = index;
            renderResults();
        });
    }

    private void updatePlayerSubtitle() {
        if (currentResult == null) return;
        String username = currentResult.profile != null && currentResult.profile.username != null
                ? currentResult.profile.username : currentResult.username;
        playerSubtitle.setText(Translations.playerSubtitle(username, currentResult.gameModes.size()));
    }

    private void showResultsPage() {
        cancelLayoutAnimator();
        root.setBackgroundColor(getColor(R.color.canvas));
        setSystemBarColors(false);
        resultsScroll.scrollTo(0, 0);
        resultsScroll.setVisibility(View.VISIBLE);
        resultsScroll.setAlpha(0f);
        landingLayer.animate().cancel();
        landingLayer.setVisibility(View.GONE);
        resultsScroll.animate().alpha(1f).setDuration(260)
                .setInterpolator(new PathInterpolator(0.2f, 0.75f, 0.25f, 1f)).start();
        videoAllowed = false;
        releaseBackgroundVideo();
    }

    private void collapseLanding() {
        if (isCollapsed) return;
        isCollapsed = true;
        videoAllowed = false;
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
        root.setBackgroundColor(getColor(R.color.canvas));
        landingLayer.setBackgroundColor(getColor(R.color.canvas));
        setSystemBarColors(false);
        animateLandingLayout(true);
    }

    private void returnToLookup() {
        currentResult = null;
        selectedMode = 0;
        setLookupLoading(false);
        usernameInput.setText("");
        statusRow.setVisibility(View.GONE);
        statusMessage.setText("");
        resultsScroll.animate().cancel();
        resultsScroll.setVisibility(View.GONE);
        resultsScroll.setAlpha(1f);
        showLandingPage();
        usernameInput.requestFocus();
    }

    private void showLandingPage() {
        resultsScroll.setVisibility(View.GONE);
        landingLayer.setVisibility(View.VISIBLE);
        if (!isCollapsed) {
            videoAllowed = true;
            landingLayer.setBackgroundColor(getColor(R.color.hero_background));
            root.setBackgroundColor(getColor(R.color.hero_background));
            setSystemBarColors(true);
            videoView.post(this::ensureBackgroundVideo);
            return;
        }
        isCollapsed = false;
        videoAllowed = true;
        landingLayer.setBackgroundColor(getColor(R.color.hero_background));
        heroSurface.setBackgroundColor(getColor(R.color.hero_background));
        heroSurface.setBackgroundResource(android.R.color.transparent);
        videoView.setAlpha(1f);
        videoView.setVisibility(View.VISIBLE);
        heroScrim.setVisibility(View.VISIBLE);
        videoView.post(this::ensureBackgroundVideo);
        root.setBackgroundColor(getColor(R.color.hero_background));
        setSystemBarColors(true);
        animateLandingLayout(false);
    }

    private void animateLandingLayout(boolean collapse) {
        cancelLayoutAnimator();
        landingLayer.setVisibility(View.VISIBLE);
        landingLayer.bringToFront();

        int fullHeight = root.getHeight();
        if (fullHeight <= 0) fullHeight = getResources().getDisplayMetrics().heightPixels;
        int parentStart = landingLayer.getHeight() > 0 ? landingLayer.getHeight() : fullHeight;
        int heroStart = heroSurface.getHeight() > 0 ? heroSurface.getHeight() : fullHeight;
        int marginStart = ((FrameLayout.LayoutParams) profileCard.getLayoutParams()).bottomMargin;
        int cardHeight = profileCard.getHeight();
        if (cardHeight <= 0) {
            profileCard.measure(View.MeasureSpec.makeMeasureSpec(root.getWidth(), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            cardHeight = profileCard.getMeasuredHeight();
        }

        int parentEnd = collapse ? dp(102) + cardHeight : fullHeight;
        int heroEnd = collapse ? dp(112) : fullHeight;
        int marginEnd = collapse ? dp(2) : dp(20);
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        layoutAnimator = animator;
        animator.setDuration(420);
        animator.setInterpolator(new PathInterpolator(0.2f, 0.75f, 0.25f, 1f));
        animator.addUpdateListener(value -> {
            float fraction = (float) value.getAnimatedValue();
            setLayoutHeight(landingLayer, lerp(parentStart, parentEnd, fraction));
            setLayoutHeight(heroSurface, lerp(heroStart, heroEnd, fraction));
            FrameLayout.LayoutParams cardParams = (FrameLayout.LayoutParams) profileCard.getLayoutParams();
            cardParams.bottomMargin = lerp(marginStart, marginEnd, fraction);
            profileCard.setLayoutParams(cardParams);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(android.animation.Animator animation) { cancelled = true; }
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (layoutAnimator == animator) layoutAnimator = null;
                if (cancelled) return;
                FrameLayout.LayoutParams cardParams = (FrameLayout.LayoutParams) profileCard.getLayoutParams();
                cardParams.bottomMargin = marginEnd;
                profileCard.setLayoutParams(cardParams);
                if (collapse) {
                    videoView.setVisibility(View.GONE);
                    heroScrim.setVisibility(View.GONE);
                    heroSurface.setBackgroundResource(R.drawable.bg_hero_compact);
                } else {
                    landingLayer.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                    landingLayer.requestLayout();
                    heroSurface.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                    heroSurface.requestLayout();
                    resultsScroll.setVisibility(View.GONE);
                    if (videoView.getVisibility() == View.VISIBLE) videoView.setAlpha(1f);
                    updateVideoTransform(videoView.getWidth(), videoView.getHeight());
                    ensureBackgroundVideo();
                }
            }
        });
        animator.start();
    }

    private void cancelLayoutAnimator() {
        if (layoutAnimator != null) {
            layoutAnimator.cancel();
            layoutAnimator = null;
        }
    }

    private static void setLayoutHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params.height == height) return;
        params.height = height;
        view.setLayoutParams(params);
    }

    private static int lerp(int start, int end, float fraction) {
        return Math.round(start + (end - start) * fraction);
    }

    private void ensureBackgroundVideo() {
        if (videoView == null || videoView.getVisibility() != View.VISIBLE || !videoView.isAvailable()) return;
        int width = videoView.getWidth();
        int height = videoView.getHeight();
        if (width <= 0 || height <= 0) return;
        if (mediaPlayer == null) {
            startBackgroundVideo(videoView.getSurfaceTexture());
            return;
        }
        updateVideoTransform(width, height);
        if (videoPrepared && videoAllowed && landingLayer.getVisibility() == View.VISIBLE && !mediaPlayer.isPlaying()) {
            try { mediaPlayer.start(); } catch (IllegalStateException ignored) { }
        }
    }

    private void setSystemBarColors(boolean landing) {
        Window window = getWindow();
        window.setStatusBarColor(getColor(R.color.hero_background));
        window.setNavigationBarColor(getColor(landing ? R.color.hero_background : R.color.canvas));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decor = window.getDecorView();
            int flags = decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (landing) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            else flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decor.setSystemUiVisibility(flags);
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(usernameInput.getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (videoAllowed && landingLayer != null && landingLayer.getVisibility() == View.VISIBLE
                && mediaPlayer != null && !mediaPlayer.isPlaying()) {
            try { mediaPlayer.start(); } catch (IllegalStateException ignored) { }
        }
    }

    @Override protected void onDestroy() {
        cancelLayoutAnimator();
        releaseBackgroundVideo();
        super.onDestroy();
    }

}
