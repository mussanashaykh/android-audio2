package com.nurulquran.audio.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.CaptionStyleCompat;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.CueGroup;
import java.util.List;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoSize;
import com.nurulquran.audio.R;
import com.nurulquran.audio.util.Framme;
import android.graphics.drawable.Drawable;
import android.content.Intent;
import java.io.File;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import android.content.pm.ActivityInfo;

/**
 * Minimal updates for ExoPlayer 2.19.1:
 * - SimpleExoPlayer.Builder instead of ExoPlayerFactory
 * - ProgressiveMediaSource instead of ExtractorMediaSource
 * - DefaultHttpDataSource.Factory & DefaultDataSource.Factory instead of *Factory classes removed
 * - CacheDataSource.Factory instead of CacheDataSourceFactory
 * - Player.Listener for callbacks
 * - VideoSize in onVideoSizeChanged
 * - CaptionStyleCompat from ui package
 */
public class ExoFullScreen extends AppCompatActivity
        implements View.OnClickListener,
        AudioManager.OnAudioFocusChangeListener,
        //com.google.android.exoplayer2.text.TextOutput,
        Player.Listener,
        TimeBar.OnScrubListener {

    // === Control dispatcher (unchanged behavior) ===
    private static final ControlDispatcher DEFAULT_CONTROL_DISPATCHER = new ControlDispatcher() {
        @Override public boolean dispatchSetPlayWhenReady(Player player, boolean playWhenReady) {
            player.setPlayWhenReady(playWhenReady);
            return true;
        }
        @Override public boolean dispatchSeekTo(Player player, int windowIndex, long positionMs) {
            player.seekTo(windowIndex, positionMs);
            return true;
        }
        @Override public boolean dispatchSetRepeatMode(Player player, int repeatMode) {
            player.setRepeatMode(repeatMode);
            return true;
        }
    };

    public interface ControlDispatcher {
        boolean dispatchSeekTo(Player player, int i, long j);
        boolean dispatchSetPlayWhenReady(Player player, boolean z);
        boolean dispatchSetRepeatMode(Player player, int i);
    }

    private Activity activity = this;

    private AudioManager.OnAudioFocusChangeListener afChangeListener;
    private AudioManager audioManager;
    private int audioManagerResult;

    private View audioTrackView;
    private View backView;
    private AspectRatioFrameLayout contentFrame;
    private ControlDispatcher controlDispatcher;
    private TextView durationView;

    private StringBuilder formatBuilder;
    private Formatter formatter;
    private final Runnable hideAction = this::hideControl;
    private long hideAtMs;

    private ImageView imgIndicator;
    private boolean inErrorState;

    private Drawable indBrightnessDrawable;
    private Drawable indVolumeDrawable;

    private boolean isLock = false;

    private LinearLayout llControlRoot;
    private View lockVIew;

    private int InitialViewHeight;
    private int InitialViewWidth;
    private Window Window_Play;

    private DataSource.Factory mediaDataSourceFactory;

    private boolean multiWindowTimeBar;

    private View nextView;
    private FrameLayout overlayFrameLayout;
    private View pauseView;
    private View playView;

    private SimpleExoPlayer player;

    public long VideoPosition;
    private int position;
    private TextView positionView;

    private View previousButton;
    private TextView progressTextView;

    private long resumePosition;
    private int resumeWindow;

    private View rotateButton;
    private ImageView scaleButton;

    private Drawable scaleFillButtonDrawable;
    private String scaleFillContentDescription;
    private Drawable scaleFitButtonDrawable;
    private String scaleFitContentDescription;
    private Drawable scaleHeightButtonDrawable;
    private String scaleHeightContentDescription;
    private TextView scaleModeTextView;
    private Drawable scaleWidthButtonDrawable;
    private String scaleWidthContentDescription;
    private Drawable scaleZoomButtonDrawable;
    private String scaleZoomContentDescription;

    private boolean scrubbing;
    private boolean shouldAutoPlay;

    private int showTimeoutMs = 5000;

    private LinearLayout shutterView;
    private View subtitleTrackButton;
    private SubtitleView subtitleView;
    private SurfaceView surfaceView;
    private TimeBar timeBar;
    private TextView titleView;

    private DefaultTrackSelector trackSelector;

    private View unLockView;

    private final Runnable updateProgressAction = this::updateProgress;

    private Timeline.Window window;

    private String Url;
    private String VideoName;

    private TextView tvSpeed;

    private static Cache cache;

    private static Cache getCache(Context context) {
        if (cache == null) {
            String rootFolder = Environment.getExternalStorageDirectory() + "/" + context.getString(R.string.app_name) + "/";
            File cachedir = new File(rootFolder, ".nqc2");
            if (!cachedir.exists()) cachedir.mkdirs();
            cache = new SimpleCache(cachedir, new NoOpCacheEvictor(), new ExoDatabaseProvider(context));
        }
        return cache;
    }

    private MediaSource buildMediaSource(Uri uri) {
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent("exoplayer-nurulquran");
        DataSource.Factory upstream = new DefaultDataSource.Factory(this, http);

        CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(getCache(this))
                .setUpstreamDataSourceFactory(upstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        return new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shouldAutoPlay = true;
        clearResumePosition();
        mediaDataSourceFactory = new DefaultDataSource.Factory(this,
                new DefaultHttpDataSource.Factory().setUserAgent("exoplayer-nurulquran"));

        setContentView(R.layout.exo_full_screen);

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = activity.getWindow();
            window.addFlags(Integer.MIN_VALUE);
            window.clearFlags(0x4000000);
            window.setStatusBarColor(ContextCompat.getColor(activity, android.R.color.transparent));
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        try {
            Intent intent = getIntent();
            String filePath = FileUriUtils.getFilePathFromUri(this, intent.getData());
            Url = intent.getStringExtra("video_url");
            VideoName = intent.getStringExtra("video_name");
            VideoPosition = intent.getLongExtra("video_position", 0);
            position = 0;
            Utility_Constants.VIDEO_LIST.clear();
            Utility_Constants.VIDEO_LIST.add(new Framme(filePath));
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        Window_Play = getWindow();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManagerResult = audioManager != null
                ? audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                : 0;

        WindowManager.LayoutParams layout = Window_Play.getAttributes();
        Window_Play.setAttributes(layout);

        bindControl();
        bindPlayerControl();

        InitialViewWidth = getResources().getDisplayMetrics().widthPixels;
        InitialViewHeight = getResources().getDisplayMetrics().heightPixels;

        overlayFrameLayout.setOnTouchListener(new SparshListner() {
            float finalTime = -1f;
            int startVolume;
            int maxVolume;
            int startBrightness;
            int maxBrightness;

            @Override public void onAfterMove() {
                if (!isLock) {
                    if (finalTime >= 0) seekToTimeBarPosition((long) finalTime);
                    imgIndicator.setVisibility(View.GONE);
                    progressTextView.setVisibility(View.GONE);
                }
            }

            @Override public void onBeforeMove(Direction dir) {
                if (isLock) return;
                if (dir == Direction.LEFT || dir == Direction.RIGHT) {
                    progressTextView.setVisibility(View.VISIBLE);
                    return;
                }
                maxBrightness = 100;
                if (Window_Play != null) {
                    startBrightness = (int) (Window_Play.getAttributes().screenBrightness * 100f);
                }
                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                imgIndicator.setVisibility(View.VISIBLE);
                progressTextView.setVisibility(View.VISIBLE);
            }

            @Override public void onClick() {
                if (playerControlVisible()) {
                    removeCallbacks(updateProgressAction);
                    removeCallbacks(hideAction);
                    hideControl();
                    return;
                }
                if (hideAtMs != C.TIME_UNSET) {
                    long delayMs = hideAtMs - SystemClock.uptimeMillis();
                    if (delayMs <= 0) hideControl();
                    else llControlRoot.postDelayed(hideAction, delayMs);
                }
                updateAll();
                showControl();
            }

            @Override public void onMove(Direction dir, float diff) {
                if (isLock) return;

                if (dir == Direction.LEFT || dir == Direction.RIGHT) {
                    float diffTime;
                    long duration = player != null ? player.getDuration() : 0L;
                    if (duration <= 60) diffTime = (duration * diff) / InitialViewWidth;
                    else diffTime = (60000f * diff) / InitialViewWidth;

                    if (dir == Direction.LEFT) diffTime *= -1f;

                    long current = player != null ? player.getCurrentPosition() : 0L;
                    finalTime = current + diffTime;
                    if (finalTime < 0) finalTime = 0;
                    if (finalTime > duration) finalTime = duration;
                    return;
                }

                finalTime = -1f;

                if (this.initialX >= (InitialViewWidth / 2f) || Window_Play == null) {
                    // Volume
                    float diffVolume = (maxVolume * diff) / (InitialViewHeight / 2f);
                    if (dir == Direction.DOWN) diffVolume = -diffVolume;
                    int finalVolume = startVolume + Math.round(diffVolume);
                    finalVolume = Math.max(0, Math.min(maxVolume, finalVolume));
                    if (imgIndicator.getDrawable() == null) imgIndicator.setImageDrawable(indVolumeDrawable);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, finalVolume, 0);
                } else {
                    // Brightness
                    float diffBrightness = (maxBrightness * diff) / (InitialViewHeight / 2f);
                    if (dir == Direction.DOWN) diffBrightness = -diffBrightness;
                    int finalBrightness = startBrightness + Math.round(diffBrightness);
                    finalBrightness = Math.max(0, Math.min(maxBrightness, finalBrightness));
                    if (imgIndicator.getDrawable() == null) imgIndicator.setImageDrawable(indBrightnessDrawable);
                    WindowManager.LayoutParams lp = Window_Play.getAttributes();
                    lp.screenBrightness = (finalBrightness / 100f);
                    Window_Play.setAttributes(lp);
                }
            }
        });

        showControl();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        InitialViewWidth = getResources().getDisplayMetrics().widthPixels;
        InitialViewHeight = getResources().getDisplayMetrics().heightPixels;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        releasePlayer();
        shouldAutoPlay = true;
        clearResumePosition();
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            // lazy init in onResume is fine
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        releasePlayer();
        finish();
    }

    private void addSpeedToggle() {
        tvSpeed = findViewById(R.id.togle_speed);
        tvSpeed.setOnClickListener(v -> {
            String cur = tvSpeed.getText().toString();
            float next;
            String label;
            switch (cur) {
                case "0.5": next = 0.75f; label = "0.75"; break;
                case "0.75": next = 1.0f; label = "1"; break;
                case "1": next = 1.25f; label = "1.25"; break;
                case "1.25": next = 1.5f; label = "1.5"; break;
                case "1.5": next = 1.75f; label = "1.75"; break;
                default: next = 0.5f; label = "0.5"; break;
            }
            tvSpeed.setText(label);
            if (player != null) player.setPlaybackParameters(new PlaybackParameters(next));
        });
    }

    private void bindControl() {
        addSpeedToggle();
        contentFrame = findViewById(R.id.exo_content_frame);
        surfaceView = findViewById(R.id.exo_surfaceView);
        shutterView = findViewById(R.id.exo_shutter);
        subtitleView = findViewById(R.id.exo_subtitles);
        if (subtitleView != null) {
            subtitleView.setUserDefaultStyle();
            subtitleView.setUserDefaultTextSize();
        }
        overlayFrameLayout = findViewById(R.id.exo_overlay);
        imgIndicator = findViewById(R.id.imgIndicator);
        progressTextView = findViewById(R.id.exo_progress_text_view);
        setResizeModeRaw(contentFrame, 0);
    }

    private void bindPlayerControl() {
        llControlRoot = findViewById(R.id.llControlRoot);
        positionView = findViewById(R.id.exo_position);
        durationView = findViewById(R.id.exo_duration);
        controlDispatcher = DEFAULT_CONTROL_DISPATCHER;
        window = new Timeline.Window();
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        timeBar = findViewById(R.id.exo_progress);
        if (timeBar != null) timeBar.setListener(this);

        backView = findViewById(R.id.exo_back);
        if (backView != null) backView.setOnClickListener(this);

        titleView = findViewById(R.id.exo_title);

        audioTrackView = findViewById(R.id.exo_audio_track);
        if (audioTrackView != null) audioTrackView.setOnClickListener(this);

        subtitleTrackButton = findViewById(R.id.exo_subtitle_track);
        if (subtitleTrackButton != null) subtitleTrackButton.setOnClickListener(this);

        unLockView = findViewById(R.id.exo_unlock);
        if (unLockView != null) unLockView.setOnClickListener(this);

        lockVIew = findViewById(R.id.exo_lock);
        if (lockVIew != null) lockVIew.setOnClickListener(this);

        rotateButton = findViewById(R.id.exo_rotate);
        if (rotateButton != null) rotateButton.setOnClickListener(this);

        playView = findViewById(R.id.exo_play);
        if (playView != null) playView.setOnClickListener(this);

        pauseView = findViewById(R.id.exo_pause);
        if (pauseView != null) pauseView.setOnClickListener(this);

        previousButton = findViewById(R.id.exo_prev);
        if (previousButton != null) previousButton.setOnClickListener(this);

        nextView = findViewById(R.id.exo_next);
        if (nextView != null) nextView.setOnClickListener(this);

        scaleButton = findViewById(R.id.exo_scale_toggle);
        if (scaleButton != null) scaleButton.setOnClickListener(this);

        scaleModeTextView = findViewById(R.id.exo_scale_toggle_text);
        scaleModeTextView.setVisibility(View.GONE);

        Resources res = activity.getResources();
        scaleFitButtonDrawable = res.getDrawable(R.drawable.simple_scale_fit);
        scaleWidthButtonDrawable = res.getDrawable(R.drawable.simple_scale_width);
        scaleHeightButtonDrawable = res.getDrawable(R.drawable.simple_scale_height);
        scaleFillButtonDrawable = res.getDrawable(R.drawable.simple_scale_fill);
        scaleZoomButtonDrawable = res.getDrawable(R.drawable.simple_scale_zoom);
        indVolumeDrawable = res.getDrawable(R.drawable.simple_volume);
        indBrightnessDrawable = res.getDrawable(R.drawable.simple_brightness);
        scaleFitContentDescription = res.getString(R.string.exo_controls_scale_fit_description);
        scaleWidthContentDescription = res.getString(R.string.exo_controls_scale_width_description);
        scaleHeightContentDescription = res.getString(R.string.exo_controls_scale_height_description);
        scaleFillContentDescription = res.getString(R.string.exo_controls_scale_fill_description);
        scaleZoomContentDescription = res.getString(R.string.exo_controls_scale_zoom_description);
    }

    private void initializePlayer() {
        if (audioManagerResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (player == null) {
                trackSelector = new DefaultTrackSelector(this);
                player = new SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build();
                player.addListener(this);
                player.setPlayWhenReady(shouldAutoPlay);
                // attach video surface and text output
                player.setVideoSurfaceView(surfaceView);
                //player.addTextOutput(this);
                updateAll();
            }

            titleView.setText(VideoName);
            MediaSource mediaSource = buildMediaSource(Uri.parse(Url));

            boolean haveResumePosition = (resumeWindow != -1);
            if (haveResumePosition) {
                player.seekTo(resumeWindow, resumePosition);
            }
            player.prepare(mediaSource, !haveResumePosition, false);
            if (VideoPosition > 0) seekToTimeBarPosition(VideoPosition);
            inErrorState = false;
            updateButtonVisibilities();
        }
    }

    private void setPlayer(SimpleExoPlayer player) {
        if (player != null) {
            player.setVideoSurfaceView(surfaceView);
            //player.addTextOutput(this);
            updateAll();
        } else {
            hideControl();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.stop();
            if (audioManager != null && afChangeListener != null) {
                audioManager.abandonAudioFocus(afChangeListener);
            }
            shouldAutoPlay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            player = null;
            trackSelector = null;
        }
    }

    private void updateResumePosition() {
        resumeWindow = player.getCurrentWindowIndex();
        resumePosition = Math.max(0, player.getContentPosition());
    }

    private void clearResumePosition() {
        resumeWindow = -1;
        resumePosition = C.TIME_UNSET;
    }

    private void updateButtonVisibilities() {
        if (player == null || trackSelector == null) return;
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        if (info == null) return;
        for (int i = 0; i < info.getRendererCount(); i++) {
            if (info.getTrackGroups(i).length == 0) continue;
            switch (player.getRendererType(i)) {
                case C.TRACK_TYPE_AUDIO:
                    if (audioTrackView != null) {
                        audioTrackView.setVisibility(View.VISIBLE);
                        audioTrackView.setTag(i);
                    }
                    break;
                case C.TRACK_TYPE_TEXT:
                    if (subtitleTrackButton != null) {
                        subtitleTrackButton.setVisibility(View.VISIBLE);
                        subtitleTrackButton.setTag(i);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void setResizeModeRaw(AspectRatioFrameLayout aspectRatioFrame, int resizeMode) {
        aspectRatioFrame.setResizeMode(resizeMode);
    }

    private void nextMedia() {
        hideControl();
        if (player == null) return;
        long p = player.getCurrentPosition();
        p += 30000; // +30s
        player.seekTo(p);
    }

    private void previousMedia() {
        hideControl();
        if (player == null) return;
        long p = player.getCurrentPosition();
        p -= 10000; // -10s
        player.seekTo(p);
    }

    private void hideControl() {
        llControlRoot.setVisibility(View.GONE);
        removeCallbacks(updateProgressAction);
        removeCallbacks(hideAction);
        hideAtMs = C.TIME_UNSET;
        setFullScreen(true);
    }

    private void showControl() {
        if (!playerControlVisible()) {
            llControlRoot.setVisibility(View.VISIBLE);
            updateAll();
            requestPlayPauseFocus();
        }
        setFullScreen(false);
        hideAfterTimeout();
    }

    private void hideAfterTimeout() {
        removeCallbacks(hideAction);
        if (showTimeoutMs > 0) {
            hideAtMs = SystemClock.uptimeMillis() + showTimeoutMs;
            llControlRoot.postDelayed(hideAction, showTimeoutMs);
        } else {
            hideAtMs = C.TIME_UNSET;
        }
    }

    private void setFullScreen(boolean fullScreen) {
        int flags = fullScreen ? View.SYSTEM_UI_FLAG_LOW_PROFILE : 0;
        ViewCompat.setFitsSystemWindows(llControlRoot, !fullScreen);
        if (Build.VERSION.SDK_INT >= 19) {
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            if (fullScreen || isLock) {
                flags |= View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
        }
        contentFrame.setSystemUiVisibility(flags);
    }

    private boolean playerControlVisible() {
        return llControlRoot.getVisibility() == View.VISIBLE;
    }

    private void updateAll() {
        updatePlayPauseButton();
        updateNavigation();
        updateScaleMode();
        updateProgress();
    }

    private void updatePlayPauseButton() {
        if (!playerControlVisible()) return;
        boolean playing = player != null && player.getPlayWhenReady();
        if (playView != null) playView.setVisibility(playing ? View.GONE : View.VISIBLE);
        if (pauseView != null) pauseView.setVisibility(playing ? View.VISIBLE : View.GONE);
        requestPlayPauseFocus();
    }

    private void updateScaleMode() {
        if (!playerControlVisible() || player == null || contentFrame == null) return;
        int scaleMode = contentFrame.getResizeMode();
        if (scaleMode == 0) {
            scaleButton.setImageDrawable(scaleFitButtonDrawable);
            scaleButton.setContentDescription(scaleFitContentDescription);
        } else if (scaleMode == 1) {
            scaleButton.setImageDrawable(scaleWidthButtonDrawable);
            scaleButton.setContentDescription(scaleWidthContentDescription);
        } else if (scaleMode == 2) {
            scaleButton.setImageDrawable(scaleHeightButtonDrawable);
            scaleButton.setContentDescription(scaleHeightContentDescription);
        } else if (scaleMode == 3) {
            scaleButton.setImageDrawable(scaleFillButtonDrawable);
            scaleButton.setContentDescription(scaleFillContentDescription);
        } else if (scaleMode == 4) {
            scaleButton.setImageDrawable(scaleZoomButtonDrawable);
            scaleButton.setContentDescription(scaleZoomContentDescription);
        }
    }

    private void updateNavigation() {
        if (!playerControlVisible()) return;
        Timeline timeline = player != null ? player.getCurrentTimeline() : null;
        boolean haveNonEmptyTimeline = timeline != null && !timeline.isEmpty();
        boolean isSeekable = false;
        if (haveNonEmptyTimeline) {
            timeline.getWindow(player.getCurrentWindowIndex(), window);
            isSeekable = window.isSeekable;
        }
        setButtonEnabled(true, previousButton);
        setButtonEnabled(true, nextView);
        if (timeBar != null) timeBar.setEnabled(isSeekable);
    }

    private void updateProgress() {
        if (!playerControlVisible()) return;

        long position = 0, bufferedPosition = 0, duration = 0;
        if (player != null) {
            long currentWindowTimeBarOffsetUs = 0;
            long durationUs = 0;
            Timeline timeline = player.getCurrentTimeline();
            if (!timeline.isEmpty()) {
                int currentWindowIndex = player.getCurrentWindowIndex();
                int firstWindowIndex = multiWindowTimeBar ? 0 : currentWindowIndex;
                int lastWindowIndex = multiWindowTimeBar ? (timeline.getWindowCount() - 1) : currentWindowIndex;

                for (int i = firstWindowIndex; i <= lastWindowIndex; i++) {
                    if (i == currentWindowIndex) currentWindowTimeBarOffsetUs = durationUs;
                    timeline.getWindow(i, window);
                    if (window.durationUs == C.TIME_UNSET) {
                        break;
                    } else {
                        durationUs += window.durationUs;
                    }
                }
            }
            duration = C.usToMs(durationUs);
            position = C.usToMs(currentWindowTimeBarOffsetUs);
            bufferedPosition = position;
            position += player.getCurrentPosition();
            bufferedPosition += player.getBufferedPosition();
        }

        if (durationView != null) {
            durationView.setText(Util.getStringForTime(formatBuilder, formatter, duration));
        }
        if (positionView != null && !scrubbing) {
            positionView.setText(Util.getStringForTime(formatBuilder, formatter, position));
        }
        if (timeBar != null) {
            timeBar.setPosition(position);
            timeBar.setBufferedPosition(bufferedPosition);
            timeBar.setDuration(duration);
        }

        removeCallbacks(updateProgressAction);
        int playbackState = player == null ? Player.STATE_IDLE : player.getPlaybackState();
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            long delayMs = (player.getPlayWhenReady() && playbackState == Player.STATE_READY)
                    ? 10 - (position % 10) : 10;
            if (player.getDuration() >= 120000) delayMs *= 10;
            llControlRoot.postDelayed(updateProgressAction, delayMs);
        }
    }

    private void setLock(boolean lock) {
        isLock = lock;
        findViewById(R.id.llControlTop).setVisibility(lock ? View.GONE : View.VISIBLE);
        findViewById(R.id.llControlBottom).setVisibility(lock ? View.GONE : View.VISIBLE);
        rotateButton.setVisibility(lock ? View.GONE : View.VISIBLE);
        unLockView.setVisibility(lock ? View.VISIBLE : View.GONE);
        if (lock) hideControl(); else showControl();
    }

    private void requestPlayPauseFocus() {
        boolean playing = player != null && player.getPlayWhenReady();
        if (!playing && playView != null) playView.requestFocus();
        else if (playing && pauseView != null) pauseView.requestFocus();
    }

    private void setButtonEnabled(boolean enabled, View view) {
        if (view != null) {
            view.setEnabled(enabled);
            view.setAlpha(enabled ? 1f : 0.3f);
            view.setVisibility(View.VISIBLE);
        }
    }

    private void seekToTimeBarPosition(long positionMs) {
        int windowIndex;
        Timeline timeline = player.getCurrentTimeline();
        if (multiWindowTimeBar && !timeline.isEmpty()) {
            int windowCount = timeline.getWindowCount();
            windowIndex = 0;
            while (true) {
                long windowDurationMs = timeline.getWindow(windowIndex, window).getDurationMs();
                if (positionMs < windowDurationMs) break;
                if (windowIndex == windowCount - 1) {
                    positionMs = windowDurationMs;
                    break;
                }
                positionMs -= windowDurationMs;
                windowIndex++;
            }
        } else {
            windowIndex = player.getCurrentWindowIndex();
        }
        seekTo(windowIndex, positionMs);
    }

    private void seekTo(int windowIndex, long positionMs) {
        if (!controlDispatcher.dispatchSeekTo(player, windowIndex, positionMs)) {
            updateProgress();
        }
    }

    private void removeCallbacks(Runnable action) {
        if (action != null) llControlRoot.removeCallbacks(action);
    }

    private void showToast(int messageId) { showToast(getString(messageId)); }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // === Player.Listener callbacks (2.19.1) ===
    @Override public void onPlaybackStateChanged(int state) {
        updatePlayPauseButton();
        updateProgress();
        if (state == Player.STATE_ENDED) nextMedia();
        updateButtonVisibilities();
    }

    @Override public void onIsPlayingChanged(boolean isPlaying) {
        updatePlayPauseButton();
        updateProgress();
    }

    @Override public void onTimelineChanged(Timeline timeline, int reason) {
        updateNavigation();
        updateProgress();
    }

    @Override public void onVideoSizeChanged(VideoSize videoSize) {
        if (contentFrame != null) {
            int width = videoSize.width;
            int height = videoSize.height;
            float ratio = (height == 0) ? 1f : (width * videoSize.pixelWidthHeightRatio) / (float) height;
            contentFrame.setAspectRatio(ratio);
        }
    }

    @Override public void onRenderedFirstFrame() {
        if (shutterView != null) shutterView.setVisibility(View.GONE);
    }

    // === TextOutput ===
    @Override public void onCues(@NonNull CueGroup cueGroup) {
        List<Cue> cues = cueGroup.cues;
        if (subtitleView != null) subtitleView.onCues(cues);
    }

    // === TimeBar.OnScrubListener ===
    @Override public void onScrubStart(TimeBar timeBar, long position) {
        removeCallbacks(hideAction);
        scrubbing = true;
    }

    @Override public void onScrubMove(TimeBar timeBar, long position) {
        if (positionView != null) {
            positionView.setText(Util.getStringForTime(formatBuilder, formatter, position));
        }
    }

    @Override public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
        scrubbing = false;
        if (!canceled && player != null) seekToTimeBarPosition(position);
        hideAfterTimeout();
    }

    @Override
    public void onClick(View view) {
        if (player == null) return;

        if (audioTrackView == view) {
            // (kept minimal) — no selection dialog provided to avoid large changes
        } else if (subtitleTrackButton == view) {
            // (kept minimal) — no selection dialog provided to avoid large changes
        } else if (lockVIew == view) {
            setLock(true);
        } else if (unLockView == view) {
            setLock(false);
        } else if (rotateButton == view) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                rotateButton.setActivated(false);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                rotateButton.setActivated(true);
            }
        } else if (backView == view) {
            onBackPressed();
        } else if (nextView == view) {
            nextMedia();
        } else if (previousButton == view) {
            previousMedia();
        } else if (playView == view) {
            controlDispatcher.dispatchSetPlayWhenReady(player, true);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else if (pauseView == view) {
            controlDispatcher.dispatchSetPlayWhenReady(player, false);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else if (scaleButton == view) {
            if (contentFrame == null) return;
            contentFrame.setResizeMode(contentFrame.getNextResizeMode());
            updateScaleMode();
            if (scaleModeTextView.getVisibility() == View.GONE) {
                scaleModeTextView.setVisibility(View.VISIBLE);
            }
            scaleModeTextView.setText(scaleButton.getContentDescription().toString());
            scaleModeTextView.postDelayed(() -> scaleModeTextView.setVisibility(View.GONE), 3000);
        }

        updateAll();
        hideAfterTimeout();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            releasePlayer();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            controlDispatcher.dispatchSetPlayWhenReady(player, false);
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            if (player != null) controlDispatcher.dispatchSetPlayWhenReady(player, true);
            else initializePlayer();
        }
    }
}
