package com.nurulquran.audio.fragment;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.VolleyError;
import com.androidquery.AQuery;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.nurulquran.audio.BaseFragment;
import com.nurulquran.audio.R;
import com.nurulquran.audio.activity.ExoFullScreen;
//import com.nurulquran.audio.activity.FullScreenVideoActivity;
import com.nurulquran.audio.activity.MainActivity;
import com.nurulquran.audio.activity.PlayerActivity;
import com.nurulquran.audio.config.GlobalValue;
import com.nurulquran.audio.config.WebserviceApi;
import com.nurulquran.audio.modelmanager.ModelManager;
import com.nurulquran.audio.modelmanager.ModelManagerListener;
import com.nurulquran.audio.object.Song;
import com.nurulquran.audio.service.PlayerListener;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Objects;

import static com.nurulquran.audio.activity.PlayerActivity.NOTIFICATION_ID;
import static com.nurulquran.audio.activity.PlayerActivity.PERMISSION_WRITE_STORAGE_Player;

public class PlayerThumbFragment extends BaseFragment {

    private static final String CHANNEL_ID = "nur_quran";
    private TextView lblNameSong, lblArtist;
    public static TextView lblNumberListen, lblNumberDownload;
    private View btnDownload, btnShare;
    private ImageView imgSong;
    private AQuery aq = null;
    private SimpleExoPlayer player;
    private PlayerView playerView;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private PlaybackStateListener playbackStateListener;
    private RelativeLayout relativedownload;
    private RelativeLayout relativeimage;
    private boolean playmp3 = true;
    private Song videosong;
    private ImageView fullscreenButton;
    private PlayerListener listener;
    private PlayerNotificationManager playerNotificationManager;
    private boolean alreadayPlaying = false;
    private TextView tvSpeed;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player_thumb, container,
                false);
        aq = new AQuery(getActivity());
        initUI(view);
        playbackStateListener = new PlaybackStateListener();
        if (!playmp3) {
            playvideo();
        } else {

            showmp3view();
        }

        return view;
    }

    private void initUI(View view) {
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        relativedownload = (RelativeLayout) view.findViewById(R.id.relative_download);
        relativeimage = (RelativeLayout) view.findViewById(R.id.relative_image_view);
        imgSong = (ImageView) view.findViewById(R.id.imgSong);
        lblNumberDownload = (TextView) view
                .findViewById(R.id.lblNumberDownload);
        lblNumberListen = (TextView) view.findViewById(R.id.lblNumberListen);
        lblNameSong = (TextView) view.findViewById(R.id.lblNameSong);
        lblArtist = (TextView) view.findViewById(R.id.lblArtist);
        btnShare = view.findViewById(R.id.btnShare);
        btnDownload = view.findViewById(R.id.btnDownload);
        lblNameSong.setSelected(true);
        lblArtist.setSelected(true);
        playerView = view.findViewById(R.id.video_view);
        addFullscreen();
    }

    boolean fullscreen = false;

    public void addFullscreen() {
         tvSpeed =(TextView) playerView.findViewById(R.id.togle_speed);
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
        fullscreenButton = playerView.findViewById(R.id.exo_fullscreen_icon);
        fullscreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                playWhenReady = false; // pause current video if it's playing
//                startActivity(
//                        FullScreenVideoActivity.newIntent(
//                                getActivity(),
//                                videosong.getUrl(),
//                                player.getCurrentPosition()
//                        )
//                );
                Intent intent = new Intent(getActivity(), ExoFullScreen.class);
                intent.setData(Uri.fromFile(new File(videosong.getUrl())));
                intent.putExtra("video_url",videosong.getUrl());
                intent.putExtra("video_name",videosong.getName());
                intent.putExtra("video_position",player.getCurrentPosition());
                startActivity(intent);
               player.setPlayWhenReady(false);

//                releasePlayer();
//                if (fullscreen) {
//                    fullscreenButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_baseline_aspect_ratio_24));
//                    getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
//
//                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) playerView.getLayoutParams();
//                    params.width = params.MATCH_PARENT;
//                    params.height = (int) (200 * getActivity().getApplicationContext().getResources().getDisplayMetrics().density);
//                    playerView.setLayoutParams(params);
//                    fullscreen = false;
//                } else {
//                    fullscreenButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_baseline_aspect_ratio_24));
//                    getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
////                    if(getSupportActionBar() != null){
////                        getSupportActionBar().hide();
////                    }
//                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) playerView.getLayoutParams();
//                    params.width = params.MATCH_PARENT;
//                    params.height = params.MATCH_PARENT;
//                    playerView.setLayoutParams(params);
//                    fullscreen = true;
//                }
            }
        });
    }

    @Override
    public void onDestroy() {

//            releasePlayer();

        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
//        releasePlayer();
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!playmp3) {
            playvideo();
        } else {
            showmp3view();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
//            releasePlayer();
        }
    }

    public void releasePlayer() {
        if (player != null) {
            alreadayPlaying = false;
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            player.removeListener(playbackStateListener);
            playerNotificationManager.setPlayer(null);
            player.release();
            player = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
            if(player != null)
            player.setPlayWhenReady(true);

    }

    private void initializenotification() {
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(getContext(),
                CHANNEL_ID,
                R.string.exo_download_notification_channel_name,
                R.string.exo_download_notification_channel_name,
                MainActivity.NOTIFICATION_ID, new DescriptionAdapter(), new PlayerNotificationManager.NotificationListener() {
            @Override
            public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {

            }

            @Override
            public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
            }
        });

        playerNotificationManager.setPlayer(player);
        playerNotificationManager.setColor(Color.BLACK);
        playerNotificationManager.setColorized(true);
        playerNotificationManager.setUseChronometer(true);
        playerNotificationManager.setUseStopAction(true);
        playerNotificationManager.setSmallIcon(R.drawable.notify_icon);
        playerNotificationManager.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);
        playerNotificationManager.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
// omit skip previous and next actions
        playerNotificationManager.setUseNavigationActions(false);
// omit fast forward action by setting the increment to zero
        playerNotificationManager.setFastForwardIncrementMs(10000);
// omit rewind action by setting the increment to zero
        playerNotificationManager.setRewindIncrementMs(10000);
// omit the stop action
        playerNotificationManager.setUsePlayPauseActions(true);
    }

    private void initializePlayer(String url) {

        if (player == null) {
//            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(getContext().getApplicationContext());

            DefaultLoadControl loadControl = new DefaultLoadControl();
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(getActivity());
//            trackSelector.setParameters(
//                    trackSelector.buildUponParameters().setMaxVideoSizeSd());
            player = new SimpleExoPlayer.Builder(getActivity())
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadControl)
                    .build();

            initializenotification();
            alreadayPlaying = true;
            if(tvSpeed != null)
            tvSpeed.setText("1");
        }

//            player = new SimpleExoPlayer.Builder(getActivity()).build();
        playerView.setPlayer(player);

        MediaSource mediaSource = buildMediaSource(Uri.parse(url));
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        player.addListener(playbackStateListener);
        player.prepare(mediaSource, true, false);
    }

    private MediaSource buildMediaSource(Uri uri) {


        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(getActivity(), "exoplayer-nurulquran");

        CacheDataSourceFactory cacheDataSourceFactory =
                new CacheDataSourceFactory(getCache(getContext()), dataSourceFactory);

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
            File cachedir = new File(rootFolder, ".nqc1");
            if (!cachedir.exists()) {
                cachedir.mkdirs();
            }
            cache = new SimpleCache(cachedir, new NoOpCacheEvictor(), new ExoDatabaseProvider(context));
        }
        return cache;

    }

    public void refreshData() {
        if (lblNameSong != null && lblArtist != null) {
            lblNameSong.setText(GlobalValue.getCurrentSong().getName());
            if (GlobalValue.getCurrentSong().getDescription() != null) {
                lblArtist.setText(Html.fromHtml(GlobalValue.getCurrentSong().getDescription()));
            } else {
                lblArtist.setText("");
            }
            getCountDownLoadAndCountDown();
            aq.id(imgSong).image(GlobalValue.getCurrentSong().getImage(), true,
                    false, 0, R.drawable.music_defaut);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshData();
                }
            }, 500);
        }
    }

    private void getCountDownLoadAndCountDown() {
        ModelManager.getCountDownAndCountListen(getActivity(), GlobalValue.getCurrentSong().getId(), new ModelManagerListener() {
            @Override
            public void onError(VolleyError error) {
                error.printStackTrace();
            }

            @Override
            public void onSuccess(String json) {
                try {
                    JSONObject object = new JSONObject(json);
                    JSONArray jsonArray = object.getJSONArray(WebserviceApi.KEY_DATA);
                    JSONObject obj = jsonArray.getJSONObject(0);
                    lblNumberDownload.setText(obj.getString("download"));
                    lblNumberListen.setText(obj.getString("listen"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_STORAGE_Player: {
                if (grantResults.length > 0) {
                    if (!playmp3) {
                        releasePlayer();
                        initializePlayer(videosong.getUrl());
                    }
                }
            }
        }
    }

    public void playvideo() {
        hidemp3view();

        if (!checkPermission()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if(!alreadayPlaying) {
                    releasePlayer();
                    initializePlayer(videosong.getUrl());
                }

//                try {
//                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//                    intent.addCategory("android.intent.category.DEFAULT");
//                    intent.setData(Uri.parse(String.format("package:%s",requireActivity().getApplicationContext().getPackageName())));
//                    startActivityForResult(intent, 2296);
//                } catch (Exception e) {
//                    Intent intent = new Intent();
//                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                    startActivityForResult(intent, 2296);
//                }
            }else {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        }, PERMISSION_WRITE_STORAGE_Player);
            }
        }  else {
            if(!alreadayPlaying) {
                releasePlayer();
                initializePlayer(videosong.getUrl());
            }
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return true;
        } else {
            int result = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }
    public void playvideo(Song currentSong) {
        playmp3 = false;
        videosong = currentSong;
        alreadayPlaying = false;
        if (relativeimage != null) {
            playvideo();
        }
    }

    public void setListener(PlayerListener listener) {
        this.listener = listener;
    }

    public void hidemp3view() {
        if (relativeimage != null) {
            relativedownload.setVisibility(View.GONE);
            relativeimage.setVisibility(View.GONE);
            playerView.setVisibility(View.VISIBLE);
        }
    }

    public void showmp3view() {
        if (relativeimage != null) {
            relativedownload.setVisibility(View.VISIBLE);
            relativeimage.setVisibility(View.VISIBLE);
            playerView.setVisibility(View.GONE);
            releasePlayer();
        }
    }


    private class PlaybackStateListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady,
                                         int playbackState) {
            String stateString;
            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                    stateString = "ExoPlayer.STATE_IDLE      -";
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    break;
                case ExoPlayer.STATE_READY:
                    stateString = "ExoPlayer.STATE_READY     -";
                    break;
                case ExoPlayer.STATE_ENDED:
                    alreadayPlaying = false;
                    listener.onChangeSong(GlobalValue.currentSongPlay + 1);
                    break;
                default:
                    stateString = "UNKNOWN_STATE             -";
                    break;
            }

        }
    }

    class DescriptionAdapter implements
            PlayerNotificationManager.MediaDescriptionAdapter {

        @Override
        public String getCurrentContentTitle(Player player) {
//            int window = player.getCurrentWindowIndex();
            return GlobalValue.getCurrentSong().getName();
        }

        @Nullable
        @Override
        public String getCurrentContentText(Player player) {
//            int window = player.getCurrentWindowIndex();
            return Html.fromHtml(GlobalValue.getCurrentSong().getDescription()).toString();
        }

        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player,
                                          PlayerNotificationManager.BitmapCallback callback) {
//            int window = player.getCurrentWindowIndex();
            return BitmapFactory.decodeResource(getContext().getResources(),
                    R.drawable.notify_icon);
        }

        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
            Intent intent = new Intent(getContext(), PlayerActivity.class);
            intent.putExtra("notification_status", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
            );
            return PendingIntent.getActivity(getContext(), 0,
                    intent, PendingIntent.FLAG_NO_CREATE);
        }

    }


}
