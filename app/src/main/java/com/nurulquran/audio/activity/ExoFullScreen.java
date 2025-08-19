package com.nurulquran.audio.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
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
import androidx.vectordrawable.graphics.drawable.PathInterpolatorCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.nurulquran.audio.R;
import com.nurulquran.audio.util.Framme;

import java.io.File;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

public class ExoFullScreen  extends AppCompatActivity implements View.OnClickListener, AudioManager.OnAudioFocusChangeListener, TextOutput, SimpleExoPlayer.VideoListener, TimeBar.OnScrubListener {
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final ControlDispatcher DEFAULT_CONTROL_DISPATCHER = new ControlDispatcher() {
        public boolean dispatchSetPlayWhenReady(Player player, boolean playWhenReady) {
            player.setPlayWhenReady(playWhenReady);
            return true;
        }

        public boolean dispatchSeekTo(Player player, int windowIndex, long positionMs) {
            player.seekTo(windowIndex, positionMs);
            return true;
        }

        public boolean dispatchSetRepeatMode(Player player, int repeatMode) {
            player.setRepeatMode(repeatMode);
            return true;
        }
    };
    private Activity activity = this;
    private AudioManager.OnAudioFocusChangeListener afChangeListener;
    private AudioManager audioManager;
    private int audioManagerResult;
    private View audioTrackView;
    private View backView;
    private AspectRatioFrameLayout contentFrame;
    private ControlDispatcher controlDispatcher;
    private TextView durationView;
    SharedPreferences.Editor edit;
    private StringBuilder formatBuilder;
    private Formatter formatter;
    private Runnable hideAction = new Runnable() {
        public void run() {
            ExoFullScreen.this.hideControl();
        }
    };
    private long hideAtMs;
    private ImageView imgIndicator;
    private boolean inErrorState;
    private Drawable indBrightnessDrawable;
    private Drawable indVolumeDrawable;
    private boolean isLock = false;
    private TrackGroupArray lastSeenTrackGroupArray;
    private LinearLayout llControlRoot;
    private View lockVIew;
    long AdLoadTime;
//    private InterstitialAd Facebook_Ads_Inter;
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
    SharedPreferences pref;
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
    private int showTimeoutMs = PathInterpolatorCompat.MAX_NUM_POINTS;
    private LinearLayout shutterView;
    private View subtitleTrackButton;
    private SubtitleView subtitleView;
    private SurfaceView surfaceView;
    private TimeBar timeBar;
    private TextView titleView;
    private TrackSelectionHelper trackSelectionHelper;
    private DefaultTrackSelector trackSelector;
    private View unLockView;
    private Runnable updateProgressAction = new Runnable() {
        public void run() {
            ExoFullScreen.this.updateProgress();
        }
    };
//    private File videoSourceFile;
    private Timeline.Window window;
    private String Url;
    private String VideoName;
    private TextView tvSpeed;


    public interface ControlDispatcher {
        boolean dispatchSeekTo(Player player, int i, long j);

        boolean dispatchSetPlayWhenReady(Player player, boolean z);

        boolean dispatchSetRepeatMode(Player player, int i);
    }
    private MediaSource buildMediaSource(Uri uri) {


        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(this, "exoplayer-nurulquran");

        CacheDataSourceFactory cacheDataSourceFactory =
                new CacheDataSourceFactory(getCache(this), dataSourceFactory);

        return new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(uri);
//        return new ExtractorMediaSource(uri,
//                new CacheDataSourceFactory(getContext(), 100 * 1024 * 1024, 5 * 1024 * 1024), new DefaultExtractorsFactory(), null, null);
    }


    static Cache cache;

    public Cache getCache(Context context) {
        if (cache == null) {
            String rootFolder = Environment.getExternalStorageDirectory() + "/"
                    + getString(R.string.app_name) + "/";
            File cachedir = new File(rootFolder, ".nqc2");
            if (!cachedir.exists()) {
                cachedir.mkdirs();
            }
            cache = new SimpleCache(cachedir, new NoOpCacheEvictor(), new ExoDatabaseProvider(context));
        }
        return cache;

    }
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        int requestAudioFocus;
        super.onCreate(savedInstanceState);
        this.shouldAutoPlay = true;
        clearResumePosition();
        this.mediaDataSourceFactory = buildDataSourceFactory(true);
        setContentView((int) R.layout.exo_full_screen);
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.activity.getWindow();
            window.addFlags(Integer.MIN_VALUE);
            window.clearFlags(67108864);
            window.setStatusBarColor(ContextCompat.getColor(this.activity, 17170444));
        }
//        ApplicationUtilitys.LOG("onCreate", new Object[0]);
        getWindow().addFlags(128);
//        try {
//            this.position = getIntent().getExtras().getInt(Utility_Constants.INT_VIDEO_POSITION, 0);
//        } catch (Exception e) {
            try {
//                e.printStackTrace();
                Intent intent = getIntent();
//                ApplicationUtilitys.LOG(intent.toString(), new Object[0]);
                String filePath = FileUriUtils.getFilePathFromUri(this.activity, intent.getData());
               Url = intent.getStringExtra("video_url");
               VideoName = intent.getStringExtra("video_name");
               VideoPosition = intent.getLongExtra("video_position",0);

//                ApplicationUtilitys.LOG(filePath, new Object[0]);
                this.position = 0;
                Utility_Constants.VIDEO_LIST.clear();
                Utility_Constants.VIDEO_LIST.add(new Framme(filePath));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
//        }
        this.Window_Play = getWindow();
        this.audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (this.audioManager != null) {
            requestAudioFocus = this.audioManager.requestAudioFocus(this.afChangeListener, 3, 1);
        } else {
            requestAudioFocus = 0;
        }
        this.audioManagerResult = requestAudioFocus;
        WindowManager.LayoutParams layout = this.Window_Play.getAttributes();
//        layout.screenBrightness = Float.valueOf(Utility_SharedPref.getSharedPrefData(this.activity, Utility_SharedPref.playerBrightness, "10")).floatValue() / 100.0f;
        this.Window_Play.setAttributes(layout);
        bindControl();
        bindPlayerControl();
        this.InitialViewWidth = getResources().getDisplayMetrics().widthPixels;
        this.InitialViewHeight = getResources().getDisplayMetrics().heightPixels;

        this.overlayFrameLayout.setOnTouchListener(new SparshListner() {
            float diffTime = -1.0f;

            float finalTime = -1.0f;
            int maxBrightness;
            int maxVolume;
            int startBrightness;
            int startVolume;

            @Override
            public void onAfterMove() {
                if (!ExoFullScreen.this.isLock) {
                    if (this.finalTime >= 0.0f) {
                        ExoFullScreen.this.seekToTimeBarPosition((long) this.finalTime);
                    }
                    if (ExoFullScreen.this.imgIndicator.getDrawable() != null) {
                        ExoFullScreen.this.imgIndicator.setImageDrawable(null);
                    }
                    ExoFullScreen.this.imgIndicator.setVisibility(View.GONE);
                    ExoFullScreen.this.progressTextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onBeforeMove(Direction dir) {
                if (!ExoFullScreen.this.isLock) {
                    if (dir == Direction.LEFT || dir == Direction.RIGHT) {
                        ExoFullScreen.this.progressTextView.setVisibility(View.VISIBLE);
                        return;
                    }
                    this.maxBrightness = 100;
                    if (ExoFullScreen.this.Window_Play != null) {
                        this.startBrightness = (int) (ExoFullScreen.this.Window_Play.getAttributes().screenBrightness * 100.0f);
                    }
                    this.maxVolume = ExoFullScreen.this.audioManager.getStreamMaxVolume(3);
                    this.startVolume = ExoFullScreen.this.audioManager.getStreamVolume(3);
                    ExoFullScreen.this.imgIndicator.setVisibility(View.VISIBLE);
                    ExoFullScreen.this.progressTextView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onClick() {
                if (ExoFullScreen.this.playerControlVisible()) {
                    ExoFullScreen.this.removeCallbacks(ExoFullScreen.this.updateProgressAction);
                    ExoFullScreen.this.removeCallbacks(ExoFullScreen.this.hideAction);
                    ExoFullScreen.this.hideControl();
                    return;
                }
                if (ExoFullScreen.this.hideAtMs != C.TIME_UNSET) {
                    long delayMs = ExoFullScreen.this.hideAtMs - SystemClock.uptimeMillis();
                    if (delayMs <= 0) {
                        ExoFullScreen.this.hideControl();
                    } else {
                        ExoFullScreen.this.llControlRoot.postDelayed(ExoFullScreen.this.hideAction, delayMs);
                    }
                }
                ExoFullScreen.this.updateAll();
                ExoFullScreen.this.showControl();
            }

            @Override
            public void onMove(Direction dir, float diff) {
                if (!ExoFullScreen.this.isLock) {
                    if (dir == Direction.LEFT || dir == Direction.RIGHT) {
                        if (ExoFullScreen.this.player.getDuration() <= 60) {
                            this.diffTime = (((float) ExoFullScreen.this.player.getDuration()) * diff) / ((float) ExoFullScreen.this.InitialViewWidth);
                        } else {
                            this.diffTime = (60000.0f * diff) / ((float) ExoFullScreen.this.InitialViewWidth);
                        }
                        if (dir == Direction.LEFT) {
                            this.diffTime *= -1.0f;
                        }
                        this.finalTime = ((float) ExoFullScreen.this.player.getCurrentPosition()) + this.diffTime;
                        if (this.finalTime < 0.0f) {
                            this.finalTime = 0.0f;
                        } else if (this.finalTime > ((float) ExoFullScreen.this.player.getDuration())) {
                            this.finalTime = (float) ExoFullScreen.this.player.getDuration();
                        }
                        this.diffTime = this.finalTime - ((float) ExoFullScreen.this.player.getCurrentPosition());
//                        ExoFullScreen.this.progressTextView.setText(ApplicationUtilitys.getDurationString((long) this.finalTime, false) + " [" + (dir == Direction.LEFT ? "-" : "+") + ApplicationUtilitys.getDurationString((long) Math.abs(this.diffTime), false) + "]");
                        return;
                    }
                    this.finalTime = -1.0f;
                    String progressText;
                    if (this.initialX >= ((float) (ExoFullScreen.this.InitialViewWidth / 2)) || ExoFullScreen.this.Window_Play == null) {
                        float diffVolume = (((float) this.maxVolume) * diff) / (((float) ExoFullScreen.this.InitialViewHeight) / 2.0f);
                        if (dir == Direction.DOWN) {
                            diffVolume = -diffVolume;
                        }
                        int finalVolume = this.startVolume + ((int) diffVolume);
                        if (finalVolume < 0) {
                            finalVolume = 0;
                        } else if (finalVolume > this.maxVolume) {
                            finalVolume = this.maxVolume;
                        }
                        progressText = "\t" + finalVolume;
                        if (ExoFullScreen.this.imgIndicator.getDrawable() == null) {
                            ExoFullScreen.this.imgIndicator.setImageDrawable(ExoFullScreen.this.indVolumeDrawable);
                        }
                        ExoFullScreen.this.progressTextView.setText(progressText);
                        ExoFullScreen.this.audioManager.setStreamVolume(3, finalVolume, 0);
                    } else if (this.initialX < ((float) (ExoFullScreen.this.InitialViewWidth / 2))) {
                        float diffBrightness = (((float) this.maxBrightness) * diff) / (((float) ExoFullScreen.this.InitialViewHeight) / 2.0f);
                        if (dir == Direction.DOWN) {
                            diffBrightness = -diffBrightness;
                        }
                        int finalBrightness = this.startBrightness + ((int) diffBrightness);
                        if (finalBrightness < 0) {
                            finalBrightness = 0;
                        } else if (finalBrightness > this.maxBrightness) {
                            finalBrightness = this.maxBrightness;
                        }
                        progressText = "\t" + finalBrightness;
                        if (ExoFullScreen.this.imgIndicator.getDrawable() == null) {
                            ExoFullScreen.this.imgIndicator.setImageDrawable(ExoFullScreen.this.indBrightnessDrawable);
                        }
                        ExoFullScreen.this.progressTextView.setText(progressText);
                        WindowManager.LayoutParams layout = ExoFullScreen.this.Window_Play.getAttributes();
                        layout.screenBrightness = ((float) finalBrightness) / 100.0f;
                        ExoFullScreen.this.Window_Play.setAttributes(layout);
//                        Utility_SharedPref.setSharedPrefData(ExoFullScreen.this.activity, Utility_SharedPref.playerBrightness, String.valueOf(finalBrightness));
                    }
                }
            }
        });

        showControl();

//        this.pref = getSharedPreferences(Utility_Constants.PRFS_NAME, 0);
//        this.edit = this.pref.edit();
//        if (this.pref.getString(Utility_Constants.PRFS_ADS1, "").equals(Utility_Constants.PRFS_ADS1)) {
//            loadInterstitialAd();
//        }
    }

//    private void loadInterstitialAd() {
//        Log.d("load", "function");
//        try {
//
//            this.Facebook_Ads_Inter = new InterstitialAd(this,ADS_Manager.FB_Intertitial_ID);
//            this.Facebook_Ads_Inter.setAdListener(new InterstitialAdListener() {
//                public void onError(Ad ad, AdError adError) {
//                    Log.d("Erro load", adError.getErrorMessage());
//                    Log.d("Erro Code", adError.getErrorCode() + "");
//                }
//
//                public void onAdLoaded(Ad ad) {
//                    ExoFullScreen.this.AdLoadTime = System.currentTimeMillis();
//                    Log.d("call post", "method");
//                }
//
//                public void onAdClicked(Ad ad) {
////                    ExoFullScreen.this.Facebook_Ads_Inter.show();
//                }
//
//                public void onLoggingImpression(Ad ad) {
//                }
//
//                public void onInterstitialDisplayed(Ad ad) {
//                }
//
//                public void onInterstitialDismissed(Ad ad) {
//                }
//            });
////            this.Facebook_Ads_Inter.loadAd();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    public void displayIntertatialAds() {
//        if (this.Facebook_Ads_Inter != null && this.Facebook_Ads_Inter.isAdLoaded()) {
//            final ProgressDialog pd = new ProgressDialog(this);
//            pd.getWindow().setFlags(1024, 1024);
//            pd.setMessage("Loading Ads");
//            pd.setCanceledOnTouchOutside(false);
//            pd.setCancelable(false);
//            pd.show();
//            new Handler().postDelayed(new Runnable() {
//                public void run() {
//                    Utility_Constants.dismissWithCheck(pd);
//                    try {
//                        if (ExoFullScreen.this.Facebook_Ads_Inter != null) {
//                            ExoFullScreen.this.Facebook_Ads_Inter.show();
//                            ExoFullScreen.this.pref = ExoFullScreen.this.getSharedPreferences(Utility_Constants.PRFS_NAME, 0);
//                            ExoFullScreen.this.edit = ExoFullScreen.this.pref.edit();
//                            ExoFullScreen.this.edit.putString(Utility_Constants.PRFS_ADS1, "ads11");
//                            ExoFullScreen.this.edit.commit();
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }, 1000);
//        }
//    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.InitialViewWidth = getResources().getDisplayMetrics().widthPixels;
        this.InitialViewHeight = getResources().getDisplayMetrics().heightPixels;
    }

    public void onNewIntent(Intent intent) {
        releasePlayer();
        this.shouldAutoPlay = true;
        clearResumePosition();
        setIntent(intent);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length <= 0 || grantResults[0] != 0) {
            showToast((int) R.string.storage_permission_denied);
            finish();
            return;
        }
        initializePlayer();
    }

    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
//            initializePlayer();
//            ApplicationUtilitys.LOG("onStart", new Object[0]);
        }
    }

    public void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || this.player == null) {
            initializePlayer();
//            ApplicationUtilitys.LOG("onResume", new Object[0]);
        }
    }

    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
//            ApplicationUtilitys.LOG("onPause", new Object[0]);
        }
    }

    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
//            ApplicationUtilitys.LOG("onStop", new Object[0]);
        }
    }

    public void onBackPressed() {
        super.onBackPressed();
        releasePlayer();
            finish();

    }

    private void addSpeedToggle()
    {
        tvSpeed =(TextView) findViewById(R.id.togle_speed);
        tvSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(tvSpeed.getText().toString().equals("0.5")) {
                    PlaybackParameters param = new PlaybackParameters(0.75f);
                    tvSpeed.setText("0.75");
                    if (player != null)
                        player.setPlaybackParameters(param);
                }else if(tvSpeed.getText().toString().equals("0.75"))
                {
                    PlaybackParameters param = new PlaybackParameters(1f);
                    tvSpeed.setText("1");
                    if (player != null)
                        player.setPlaybackParameters(param);
                }

                else if(tvSpeed.getText().toString().equals("1"))
                {
                    PlaybackParameters param = new PlaybackParameters(1.25f);
                    tvSpeed.setText("1.25");
                    if (player != null)
                        player.setPlaybackParameters(param);
                }else if(tvSpeed.getText().toString().equals("1.25"))
                {
                    PlaybackParameters param = new PlaybackParameters(1.5f);
                    tvSpeed.setText("1.5");
                    if (player != null)
                        player.setPlaybackParameters(param);
                }
                else if(tvSpeed.getText().toString().equals("1.5"))
                {
                    PlaybackParameters param = new PlaybackParameters(1.75f);
                    tvSpeed.setText("1.75");
                    if (player != null)
                        player.setPlaybackParameters(param);
                }else if(tvSpeed.getText().toString().equals("1.75"))
                {
                    PlaybackParameters param = new PlaybackParameters(0.5f);
                    tvSpeed.setText("0.5");
                    if (player != null)
                        player.setPlaybackParameters(param);
                }
            }
        });
    }
    private void bindControl() {
        addSpeedToggle();
        this.contentFrame = (AspectRatioFrameLayout) findViewById(R.id.exo_content_frame);
        this.surfaceView = (SurfaceView) findViewById(R.id.exo_surfaceView);
        this.shutterView = (LinearLayout) findViewById(R.id.exo_shutter);
        this.subtitleView = (SubtitleView) findViewById(R.id.exo_subtitles);
        if (this.subtitleView != null) {
            this.subtitleView.setUserDefaultStyle();
//            this.subtitleView.setStyle(new CaptionStyleCompat(-1, 0, 0, 0, -1, ApplicationUtilitys.getTextTypeFace(this.activity)));
            this.subtitleView.setUserDefaultTextSize();
        }
        this.overlayFrameLayout = (FrameLayout) findViewById(R.id.exo_overlay);
        this.imgIndicator = (ImageView) findViewById(R.id.imgIndicator);
        this.progressTextView = (TextView) findViewById(R.id.exo_progress_text_view);
        setResizeModeRaw(this.contentFrame, 0);
    }

    private void bindPlayerControl() {
        this.llControlRoot = (LinearLayout) findViewById(R.id.llControlRoot);
        this.positionView = (TextView) findViewById(R.id.exo_position);
        this.durationView = (TextView) findViewById(R.id.exo_duration);
        this.controlDispatcher = DEFAULT_CONTROL_DISPATCHER;
        this.window = new Timeline.Window();
        this.formatBuilder = new StringBuilder();
        this.formatter = new Formatter(this.formatBuilder, Locale.getDefault());
        this.timeBar = (TimeBar) findViewById(R.id.exo_progress);
        if (this.timeBar != null) {
            this.timeBar.setListener(this);
        }
        this.backView = findViewById(R.id.exo_back);
        if (this.backView != null) {
            this.backView.setOnClickListener(this);
        }
        this.titleView = (TextView) findViewById(R.id.exo_title);
        this.audioTrackView = findViewById(R.id.exo_audio_track);
        if (this.audioTrackView != null) {
            this.audioTrackView.setOnClickListener(this);
        }
        this.subtitleTrackButton = findViewById(R.id.exo_subtitle_track);
        if (this.subtitleTrackButton != null) {
            this.subtitleTrackButton.setOnClickListener(this);
        }
        this.unLockView = findViewById(R.id.exo_unlock);
        if (this.unLockView != null) {
            this.unLockView.setOnClickListener(this);
        }
        this.lockVIew = findViewById(R.id.exo_lock);
        if (this.lockVIew != null) {
            this.lockVIew.setOnClickListener(this);
        }
        this.rotateButton = findViewById(R.id.exo_rotate);
        if (this.rotateButton != null) {
            this.rotateButton.setOnClickListener(this);
        }
        this.playView = findViewById(R.id.exo_play);
        if (this.playView != null) {
            this.playView.setOnClickListener(this);
        }
        this.pauseView = findViewById(R.id.exo_pause);
        if (this.pauseView != null) {
            this.pauseView.setOnClickListener(this);
        }
        this.previousButton = findViewById(R.id.exo_prev);
        if (this.previousButton != null) {
            this.previousButton.setOnClickListener(this);
        }
        this.nextView = findViewById(R.id.exo_next);
        if (this.nextView != null) {
            this.nextView.setOnClickListener(this);
        }
        this.scaleButton = (ImageView) findViewById(R.id.exo_scale_toggle);
        if (this.scaleButton != null) {
            this.scaleButton.setOnClickListener(this);
        }
        this.scaleModeTextView = (TextView) findViewById(R.id.exo_scale_toggle_text);
        this.scaleModeTextView.setVisibility(View.GONE);
        Resources resources = this.activity.getResources();
        this.scaleFitButtonDrawable = resources.getDrawable(R.drawable.simple_scale_fit);
        this.scaleWidthButtonDrawable = resources.getDrawable(R.drawable.simple_scale_width);
        this.scaleHeightButtonDrawable = resources.getDrawable(R.drawable.simple_scale_height);
        this.scaleFillButtonDrawable = resources.getDrawable(R.drawable.simple_scale_fill);
        this.scaleZoomButtonDrawable = resources.getDrawable(R.drawable.simple_scale_zoom);
        this.indVolumeDrawable = resources.getDrawable(R.drawable.simple_volume);
        this.indBrightnessDrawable = resources.getDrawable(R.drawable.simple_brightness);
        this.scaleFitContentDescription = resources.getString(R.string.exo_controls_scale_fit_description);
        this.scaleWidthContentDescription = resources.getString(R.string.exo_controls_scale_width_description);
        this.scaleHeightContentDescription = resources.getString(R.string.exo_controls_scale_height_description);
        this.scaleFillContentDescription = resources.getString(R.string.exo_controls_scale_fill_description);
        this.scaleZoomContentDescription = resources.getString(R.string.exo_controls_scale_zoom_description);
    }

    private void initializePlayer() {
        if (this.audioManagerResult == 1) {
            if (this.player == null) {
                TrackSelection.Factory adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
                this.trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
                this.trackSelectionHelper = new TrackSelectionHelper(this.trackSelector, adaptiveTrackSelectionFactory);
                this.lastSeenTrackGroupArray = null;
                this.player = ExoPlayerFactory.newSimpleInstance(this.activity, this.trackSelector);
                setPlayer(this.player);
                this.player.setPlayWhenReady(this.shouldAutoPlay);
            }
//            this.videoSourceFile = new File(((Framme) Utility_Constants.VIDEO_LIST.get(this.position)).getData());
//            if (!Util.maybeRequestReadExternalStoragePermission(this, Uri.parse(Url))) {
                boolean haveResumePosition;
                boolean z;
                this.titleView.setText(VideoName);
//                Utility_SharedPref.setSharedPrefData(this.activity, Utility_SharedPref.lastPlayed, this.videoSourceFile.getAbsolutePath());
                MediaSource mediaSource = buildMediaSource(Uri.parse(Url));
                if (this.resumeWindow != -1) {
                    haveResumePosition = true;
                } else {
                    haveResumePosition = false;
                }
                if (haveResumePosition) {
                    this.player.seekTo(this.resumeWindow, this.resumePosition);
                }
                SimpleExoPlayer simpleExoPlayer = this.player;
                if (haveResumePosition) {
                    z = false;
                } else {
                    z = true;
                }
                simpleExoPlayer.prepare(mediaSource, z, false);
                seekToTimeBarPosition(VideoPosition);
                this.inErrorState = false;
                updateButtonVisibilities();
//            }
        }
    }
    //  player.addListener(new EventListener() {
//                @Override
//                public void onTimelineChanged(Timeline timeline, Object manifest) {
//                    updateNavigation();
//                    updateProgress();
//                }
//
//                @Override
//                public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
//                    updateButtonVisibilities();
//                    if (trackGroups != lastSeenTrackGroupArray) {
//                        MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
//                        if (mappedTrackInfo != null) {
//                            if (mappedTrackInfo.getTrackTypeRendererSupport(2) == 1) {
//                                showToast((int) R.string.error_unsupported_video);
//                            }
//                            if (mappedTrackInfo.getTrackTypeRendererSupport(1) == 1) {
//                                showToast((int) R.string.error_unsupported_audio);
//                            }
//                        }
//                        lastSeenTrackGroupArray = trackGroups;
//                    }
//                }
//
//                @Override
//                public void onLoadingChanged(boolean isLoading) {
//
//                }
//
//                @Override
//                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//                    updatePlayPauseButton();
//                    updateProgress();
//                    if (playbackState == 4) {
//                        nextMedia();
//                    }
//                    updateButtonVisibilities();
//                }
//
//                @Override
//                public void onRepeatModeChanged(int repeatMode) {
//
//                }
//
//                @Override
//                public void onPlayerError(ExoPlaybackException e) {
//                    String errorString = null;
//                    if (e.type == 1) {
//                        Exception cause = e.getRendererException();
//                        if (cause instanceof DecoderInitializationException) {
//                            DecoderInitializationException decoderInitializationException = (DecoderInitializationException) cause;
//                            if (decoderInitializationException.decoderName != null) {
//                                errorString = getString(R.string.error_instantiating_decoder, new Object[]{decoderInitializationException.decoderName});
//                            } else if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
//                                errorString = getString(R.string.error_querying_decoders);
//                            } else if (decoderInitializationException.secureDecoderRequired) {
//                                errorString = getString(R.string.error_no_secure_decoder, new Object[]{decoderInitializationException.mimeType});
//                            } else {
//                                errorString = getString(R.string.error_no_decoder, new Object[]{decoderInitializationException.mimeType});
//                            }
//                        }
//                    }
//                    if (errorString != null) {
//                        final Dialog dialog = new Dialog(ui);
//                        dialog.setContentView(R.layout.dialog_error);
//                        ((TextView) dialog.findViewById(R.id.txtError)).setText(errorString);
//                        dialog.findViewById(R.id.txtDialogConfirm).setOnClickListener(new OnClickListener() {
//                            public void onClick(View view) {
//                                Utility_DialogDismiss.dismissWithCheck(dialog);
//                                ExoFullScreen.this.onBackPressed();
//                            }
//                        });
//                        dialog.show();
//                    }
//                    inErrorState = true;
//                    if (isBehindLiveWindow(e)) {
//                        clearResumePosition();
//                        initializePlayer();
//                        return;
//                    }
//                    updateResumePosition();
//                    updateButtonVisibilities();
//                    showControl();
//                }
//
//                @Override
//                public void onPositionDiscontinuity() {
//                    updateNavigation();
//                    updateProgress();
//                    if (inErrorState) {
//                        updateResumePosition();
//                    }
//                }
//
//                @Override
//                public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
//
//                }
//            });
    private void setPlayer(SimpleExoPlayer player) {
        if (player != null) {
            player.setVideoSurfaceView(this.surfaceView);
            player.addTextOutput(this);
            player.addVideoListener(this);
            updateAll();
            return;
        }
        hideControl();
    }

    private void releasePlayer() {
        if (this.player != null) {

//            if (this.position + 1 < Utility_Constants.VIDEO_LIST.size()) {
//                this.position++;
//                this.videoSourceFile = new File(((Framme) Utility_Constants.VIDEO_LIST.get(this.position)).getData());
//                this.titleView.setText(this.videoSourceFile.getName());
//                Utility_SharedPref.setSharedPrefData(this.ui, Utility_SharedPref.lastPlayed, this.videoSourceFile.getAbsolutePath());
//                MediaSource mediaSource = new ExtractorMediaSource(Uri.fromFile(this.videoSourceFile), this.mediaDataSourceFactory, new DefaultExtractorsFactory(), null, null);
//                this.player.stop();
//                this.player.seekTo(0);
//                this.player.prepare(mediaSource);
//            } else if (this.player.getPlaybackState() == 4) {
//                finish();
//            }

            this.player.stop();
            this.audioManager.abandonAudioFocus(this.afChangeListener);
            this.shouldAutoPlay = this.player.getPlayWhenReady();
            updateResumePosition();
            this.player.release();
            this.player = null;
            this.trackSelector = null;
            this.trackSelectionHelper = null;
        }
    }

    private void updateResumePosition() {
        this.resumeWindow = this.player.getCurrentWindowIndex();
        this.resumePosition = Math.max(0, this.player.getContentPosition());
    }

    private void clearResumePosition() {
        this.resumeWindow = -1;
        this.resumePosition = C.TIME_UNSET;
    }

    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return (buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null));
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return (buildHttpDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null));
    }
    public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory((Context) this, (TransferListener) bandwidthMeter, buildHttpDataSourceFactory(bandwidthMeter));
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory("nurulquran", bandwidthMeter);
    }
    private void updateButtonVisibilities() {
        if (this.player != null) {
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = this.trackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                for (int i = 0; i < mappedTrackInfo.length; i++) {
                    if (mappedTrackInfo.getTrackGroups(i).length != 0) {
                        switch (this.player.getRendererType(i)) {
                            case 1:
                                this.audioTrackView.setVisibility(View.VISIBLE);
                                this.audioTrackView.setTag(Integer.valueOf(i));
                                break;
                            case 3:
                                this.subtitleTrackButton.setVisibility(View.VISIBLE);
                                this.subtitleTrackButton.setTag(Integer.valueOf(i));
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }

    private void setResizeModeRaw(AspectRatioFrameLayout aspectRatioFrame, int resizeMode) {
        aspectRatioFrame.setResizeMode(resizeMode);
    }

    private void nextMedia() {
        hideControl();

        if (this.player == null) {
            return;
        }
        long p = this.player.getCurrentPosition();
        p += 30000; // for rewind use -5000
        player.seekTo(p);
//        if (this.position + 1 < Utility_Constants.VIDEO_LIST.size()) {
//            this.position++;
////            this.videoSourceFile = new File(((Framme) Utility_Constants.VIDEO_LIST.get(this.position)).getData());
////            this.titleView.setText(this.videoSourceFile.getName());
////            Utility_SharedPref.setSharedPrefData(this.activity, Utility_SharedPref.lastPlayed, this.videoSourceFile.getAbsolutePath());
//            MediaSource mediaSource = buildMediaSource(Uri.parse(Url));
//            this.player.stop();
//            this.player.seekTo(0);
//            this.player.prepare(mediaSource);
//        } else if (this.player.getPlaybackState() == 4) {
//            finish();
//        }
    }

    private void previousMedia() {
        hideControl();

        if (this.player == null) {
            return;
        }
        long p = this.player.getCurrentPosition();
        p -= 10000; // for rewind use -5000
        player.seekTo(p);
//        if (this.position - 1 >= 0) {
//            this.position--;
////            this.videoSourceFile = new File(((Framme) Utility_Constants.VIDEO_LIST.get(this.position)).getData());
////            this.titleView.setText(this.videoSourceFile.getName());
////            Utility_SharedPref.setSharedPrefData(this.activity, Utility_SharedPref.lastPlayed, this.videoSourceFile.getAbsolutePath());
//            MediaSource mediaSource = buildMediaSource(Uri.parse(Url));
//            this.player.stop();
//            this.player.seekTo(0);
//            this.player.prepare(mediaSource);
//            return;
//        }
//        this.player.seekToDefaultPosition();
    }

    private void hideControl() {
        this.llControlRoot.setVisibility(View.GONE);
        removeCallbacks(this.updateProgressAction);
        removeCallbacks(this.hideAction);
        this.hideAtMs = C.TIME_UNSET;
        setFullScreen(true);
    }

    private void showControl() {
        if (!playerControlVisible()) {
            this.llControlRoot.setVisibility(View.VISIBLE);
            updateAll();
            requestPlayPauseFocus();
        }
        setFullScreen(false);
        hideAfterTimeout();
    }

    private void hideAfterTimeout() {
        removeCallbacks(this.hideAction);
        if (this.showTimeoutMs > 0) {
            this.hideAtMs = SystemClock.uptimeMillis() + ((long) this.showTimeoutMs);
            this.llControlRoot.postDelayed(this.hideAction, (long) this.showTimeoutMs);
            return;
        }
        this.hideAtMs = C.TIME_UNSET;
    }

    private void setFullScreen(boolean fullScreen) {
        int flags;
        boolean z = true;
        if (fullScreen) {
            flags = 1;
        } else {
            flags = 0;
        }
        View view = this.llControlRoot;
        if (fullScreen) {
            z = false;
        }
        ViewCompat.setFitsSystemWindows(view, z);
        if (Build.VERSION.SDK_INT >= 19) {
            flags |= 1792;
            if (fullScreen || this.isLock) {
                flags |= 4102;
            }
        }
        this.contentFrame.setSystemUiVisibility(flags);
    }

    private boolean playerControlVisible() {
        return this.llControlRoot.getVisibility() == 0;
    }

    private void updateAll() {
        updatePlayPauseButton();
        updateNavigation();
        updateScaleMode();
        updateProgress();
    }

    private void updatePlayPauseButton() {
        int i = View.GONE;
        int i2 = 1;
        if (playerControlVisible()) {
            boolean playing;
            boolean requestPlayPauseFocus = true;
            if (this.player == null || !this.player.getPlayWhenReady()) {
                playing = false;
            } else {
                playing = true;
            }
            if (this.playView != null) {
                int i3;
                if (playing && this.playView.isFocused()) {
                    i3 = 1;

                } else {
                    i3 = 0;

                }

                requestPlayPauseFocus = true;
                View view = this.playView;
                if (playing) {
                    i3 = 8;
                } else {
                    i3 = 0;
                }
                view.setVisibility(i3);
            }
            if (this.pauseView != null) {
                if (playing || !this.pauseView.isFocused()) {
                    i2 = 0;

                }

                requestPlayPauseFocus = false;
                View view2 = this.pauseView;
                if (playing) {
                    i = 0;
                }
                view2.setVisibility(i);
            }

            if (requestPlayPauseFocus) {
                requestPlayPauseFocus();

            }
        }
    }

    private void updateScaleMode() {
        if (playerControlVisible() && this.player != null) {
            int scaleMode = this.contentFrame.getResizeMode();
            if (scaleMode == 0) {
                this.scaleButton.setImageDrawable(this.scaleFitButtonDrawable);
                this.scaleButton.setContentDescription(this.scaleFitContentDescription);
            } else if (scaleMode == 1) {
                this.scaleButton.setImageDrawable(this.scaleWidthButtonDrawable);
                this.scaleButton.setContentDescription(this.scaleWidthContentDescription);
            } else if (scaleMode == 2) {
                this.scaleButton.setImageDrawable(this.scaleHeightButtonDrawable);
                this.scaleButton.setContentDescription(this.scaleHeightContentDescription);
            } else if (scaleMode == 3) {
                this.scaleButton.setImageDrawable(this.scaleFillButtonDrawable);
                this.scaleButton.setContentDescription(this.scaleFillContentDescription);
            } else if (scaleMode == 4) {
                this.scaleButton.setImageDrawable(this.scaleZoomButtonDrawable);
                this.scaleButton.setContentDescription(this.scaleZoomContentDescription);
            }
        }
    }

    private void updateNavigation() {
        if (playerControlVisible()) {
            Timeline timeline = this.player != null ? this.player.getCurrentTimeline() : null;
            boolean haveNonEmptyTimeline = (timeline == null || timeline.isEmpty()) ? false : true;
            boolean isSeekable = false;
            if (haveNonEmptyTimeline) {
                timeline.getWindow(this.player.getCurrentWindowIndex(), this.window);
                isSeekable = this.window.isSeekable;
            }
            setButtonEnabled(true, this.previousButton);
            setButtonEnabled(true, this.nextView);
            if (this.timeBar != null) {
                this.timeBar.setEnabled(isSeekable);
            }
        }
    }

    private void updateProgress() {
        if (playerControlVisible()) {
            long position = 0;
            long bufferedPosition = 0;
            long duration = 0;
            if (this.player != null) {
                long currentWindowTimeBarOffsetUs = 0;
                long durationUs = 0;
                Timeline timeline = this.player.getCurrentTimeline();
                if (!timeline.isEmpty()) {
                    int firstWindowIndex;
                    int lastWindowIndex;
                    int currentWindowIndex = this.player.getCurrentWindowIndex();
                    if (this.multiWindowTimeBar) {
                        firstWindowIndex = 0;
                    } else {
                        firstWindowIndex = currentWindowIndex;
                    }
                    if (this.multiWindowTimeBar) {
                        lastWindowIndex = timeline.getWindowCount() - 1;
                    } else {
                        lastWindowIndex = currentWindowIndex;
                    }
                    int i = firstWindowIndex;
                    while (i <= lastWindowIndex) {
                        if (i == currentWindowIndex) {
                            currentWindowTimeBarOffsetUs = durationUs;
                        }
                        timeline.getWindow(i, this.window);
                        if (this.window.durationUs == C.TIME_UNSET) {
                            Assertions.checkState(!this.multiWindowTimeBar);
                        } else {
                            durationUs += this.window.durationUs;
                            i++;
                        }
                    }
                }
                duration = C.usToMs(durationUs);
                position = C.usToMs(currentWindowTimeBarOffsetUs);
                bufferedPosition = position;
                position += this.player.getCurrentPosition();
                bufferedPosition += this.player.getBufferedPosition();
            }
            if (this.durationView != null) {
                this.durationView.setText(Util.getStringForTime(this.formatBuilder, this.formatter, duration));
            }
            if (!(this.positionView == null || this.scrubbing)) {
                this.positionView.setText(Util.getStringForTime(this.formatBuilder, this.formatter, position));
            }
            if (this.timeBar != null) {
                this.timeBar.setPosition(position);
                this.timeBar.setBufferedPosition(bufferedPosition);
                this.timeBar.setDuration(duration);
            }
            removeCallbacks(this.updateProgressAction);
            int playbackState = this.player == null ? 1 : this.player.getPlaybackState();
            if (playbackState != 1 && playbackState != 4) {
                long delayMs;
                if (this.player.getPlayWhenReady() && playbackState == 3) {
                    delayMs = 10 - (position % 10);
                    if (delayMs < 2) {
                        delayMs += 10;
                    }
                } else {
                    delayMs = 10;
                }
                if (this.player.getDuration() >= 120000) {
                    delayMs *= 10;
                }
                this.llControlRoot.postDelayed(this.updateProgressAction, delayMs);
            }
        }
    }

    private void setLock(boolean lock) {
        int i;
        int i2 = 0;
        this.isLock = lock;
        findViewById(R.id.llControlTop).setVisibility(lock ? View.GONE : View.VISIBLE);
        View findViewById = findViewById(R.id.llControlBottom);
        if (lock) {
            i = 8;
        } else {
            i = 0;
        }
        findViewById.setVisibility(i);
        findViewById = this.rotateButton;
        if (lock) {
            i = 8;
        } else {
            i = 0;
        }
        findViewById.setVisibility(i);
        View view = this.unLockView;
        if (!lock) {
            i2 = 8;
        }
        view.setVisibility(i2);
        if (lock) {
            hideControl();
        } else {
            showControl();
        }
    }

    private void requestPlayPauseFocus() {
        boolean playing = this.player != null && this.player.getPlayWhenReady();
        if (!playing && this.playView != null) {
            this.playView.requestFocus();

        } else if (playing && this.pauseView != null) {
            this.pauseView.requestFocus();

        }
    }

    private void setButtonEnabled(boolean enabled, View view) {
        if (view != null) {
            view.setEnabled(enabled);
            view.setAlpha(enabled ? 1.0f : 0.3f);
            view.setVisibility(View.VISIBLE);
        }
    }

    private void seekToTimeBarPosition(long positionMs) {
        int windowIndex;
        Timeline timeline = this.player.getCurrentTimeline();
        if (this.multiWindowTimeBar && !timeline.isEmpty()) {
            int windowCount = timeline.getWindowCount();
            windowIndex = 0;
            while (true) {
                long windowDurationMs = timeline.getWindow(windowIndex, this.window).getDurationMs();
                if (positionMs < windowDurationMs) {
                    break;
                } else if (windowIndex == windowCount - 1) {
                    positionMs = windowDurationMs;
                    break;
                } else {
                    positionMs -= windowDurationMs;
                    windowIndex++;
                }
            }
        } else {
            windowIndex = this.player.getCurrentWindowIndex();
        }
        seekTo(windowIndex, positionMs);
    }

    private void seekTo(int windowIndex, long positionMs) {
        if (!this.controlDispatcher.dispatchSeekTo(this.player, windowIndex, positionMs)) {
            updateProgress();
        }
    }

    public void removeCallbacks(Runnable action) {
        if (action != null) {
            this.llControlRoot.removeCallbacks(action);
        }
    }

    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, 1).show();
    }

    public void onCues(List<Cue> cues) {
        if (this.subtitleView != null) {
            this.subtitleView.onCues(cues);
        }
    }

    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        if (this.contentFrame != null) {
            this.contentFrame.setAspectRatio(height == 0 ? 1.0f : (((float) width) * pixelWidthHeightRatio) / ((float) height));
        }
    }

    public void onRenderedFirstFrame() {
        if (this.shutterView != null) {
            this.shutterView.setVisibility(View.GONE);
        }
    }


    public void onScrubStart(TimeBar timeBar, long position) {
        removeCallbacks(this.hideAction);
        this.scrubbing = true;
    }

    public void onScrubMove(TimeBar timeBar, long position) {
        if (this.positionView != null) {
            this.positionView.setText(Util.getStringForTime(this.formatBuilder, this.formatter, position));
        }
    }

    public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
        this.scrubbing = false;
        if (!(canceled || this.player == null)) {
            seekToTimeBarPosition(position);
        }
        hideAfterTimeout();
    }

    public void onClick(View view) {
        boolean z = true;
        if (this.player != null) {
            if (this.audioTrackView == view) {
                if (this.trackSelector.getCurrentMappedTrackInfo() != null) {
                    this.trackSelectionHelper.showSelectionDialog(this, view.getContentDescription(), this.trackSelector.getCurrentMappedTrackInfo(), (Integer) view.getTag());
                }
            } else if (this.subtitleTrackButton == view) {
                if (this.trackSelector.getCurrentMappedTrackInfo() != null) {
                    this.trackSelectionHelper.showSelectionDialog(this, view.getContentDescription(), this.trackSelector.getCurrentMappedTrackInfo(), (Integer) view.getTag());
                }
            } else if (this.lockVIew == view) {
                setLock(true);
            } else if (this.unLockView == view) {
                setLock(false);
            } else if (this.rotateButton == view) {
                if (getResources().getConfiguration().orientation == 1) {
                    setRequestedOrientation(6);
                    this.rotateButton.setActivated(false);
                } else {
                    setRequestedOrientation(1);
                    this.rotateButton.setActivated(true);
                }
            } else if (this.backView == view) {
                onBackPressed();
            } else if (this.nextView == view) {
                nextMedia();
            } else if (this.previousButton == view) {
                previousMedia();
            } else if (this.playView == view) {
                this.controlDispatcher.dispatchSetPlayWhenReady(this.player, true);
                getWindow().addFlags(128);
            } else if (this.pauseView == view) {
//                if (System.currentTimeMillis() - this.AdLoadTime >= 3000000) {
//                    loadInterstitialAd();
//                } else if (this.Facebook_Ads_Inter != null) {
//                    displayIntertatialAds();
//                }
                this.controlDispatcher.dispatchSetPlayWhenReady(this.player, false);
                getWindow().clearFlags(128);
            } else if (this.scaleButton == view) {
                if (this.contentFrame == null) {
                    z = false;
                }
                Assertions.checkState(z);
                this.contentFrame.setResizeMode(this.contentFrame.getNextResizeMode());
                updateScaleMode();
                if (this.scaleModeTextView.getVisibility() == View.GONE) {
                    this.scaleModeTextView.setVisibility(View.VISIBLE);
                }
                this.scaleModeTextView.setText(this.scaleButton.getContentDescription().toString());
                this.scaleModeTextView.postDelayed(new Runnable() {
                    public void run() {
                        ExoFullScreen.this.scaleModeTextView.setVisibility(View.GONE);
                    }
                }, 3000);
            }
        }
        updateAll();
        hideAfterTimeout();
    }

    public void onAudioFocusChange(int focusChange) {
        if (focusChange == -1) {
            releasePlayer();
        } else if (focusChange == -2) {
            this.controlDispatcher.dispatchSetPlayWhenReady(this.player, false);
        } else if (focusChange == -3) {
            this.controlDispatcher.dispatchSetPlayWhenReady(this.player, false);
        } else if (focusChange != 1) {
        } else {
            if (this.player != null) {
                this.controlDispatcher.dispatchSetPlayWhenReady(this.player, true);
            } else {
                initializePlayer();
            }
        }
    }
}
