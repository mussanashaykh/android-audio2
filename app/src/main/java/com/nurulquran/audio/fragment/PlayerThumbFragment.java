package com.nurulquran.audio.fragment;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.VolleyError;
import com.androidquery.AQuery;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
// Add this import
import com.google.android.exoplayer2.ui.PlayerNotificationManager.Builder;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;
import com.nurulquran.audio.BaseFragment;
import com.nurulquran.audio.R;
import com.nurulquran.audio.activity.ExoFullScreen;
import com.nurulquran.audio.activity.MainActivity;
import com.nurulquran.audio.activity.PlayerActivity;
import com.nurulquran.audio.config.GlobalValue;
import com.nurulquran.audio.config.WebserviceApi;
import com.nurulquran.audio.modelmanager.ModelManager;
import com.nurulquran.audio.modelmanager.ModelManagerListener;
import com.nurulquran.audio.object.Song;
import com.nurulquran.audio.service.MusicService; // Added import for MusicService
import com.nurulquran.audio.service.PlayerListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.nurulquran.audio.activity.PlayerActivity.PERMISSION_WRITE_STORAGE_Player;

@SuppressWarnings({"deprecation"}) // Silence Exo 2.19 deprecation nudges (Media3 migration)
public class PlayerThumbFragment extends BaseFragment {

    private static final String CHANNEL_ID = "nur_quran";

    private TextView lblNameSong, lblArtist;
    public static TextView lblNumberListen, lblNumberDownload;
    private View btnDownload, btnShare;
    private ImageView imgSong;
    private AQuery aq = null;

    private ExoPlayer player;
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
        View view = inflater.inflate(R.layout.fragment_player_thumb, container, false);
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
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        relativedownload = view.findViewById(R.id.relative_download);
        relativeimage = view.findViewById(R.id.relative_image_view);
        imgSong = view.findViewById(R.id.imgSong);
        lblNumberDownload = view.findViewById(R.id.lblNumberDownload);
        lblNumberListen = view.findViewById(R.id.lblNumberListen);
        lblNameSong = view.findViewById(R.id.lblNameSong);
        lblArtist = view.findViewById(R.id.lblArtist);
        btnShare = view.findViewById(R.id.btnShare);
        btnDownload = view.findViewById(R.id.btnDownload);
        lblNameSong.setSelected(true);
        lblArtist.setSelected(true);
        playerView = view.findViewById(R.id.video_view);
        addFullscreen();
    }

    private void addFullscreen() {
        tvSpeed = playerView.findViewById(R.id.togle_speed);
        tvSpeed.setOnClickListener(v -> {
            if (player == null) return;
            String s = tvSpeed.getText().toString();
            float next =
                    "0.5".equals(s) ? 0.75f :
                            "0.75".equals(s) ? 1.0f :
                                    "1".equals(s) ? 1.25f :
                                            "1.25".equals(s) ? 1.5f :
                                                    "1.5".equals(s) ? 1.75f : 0.5f;
            tvSpeed.setText(String.valueOf(next));
            player.setPlaybackSpeed(next);
        });

        fullscreenButton = playerView.findViewById(R.id.exo_fullscreen_icon);
        fullscreenButton.setOnClickListener(v -> {
            if (videosong == null || player == null) return;
            Intent intent = new Intent(getActivity(), ExoFullScreen.class);
            intent.putExtra("video_url", videosong.getUrl());
            intent.putExtra("video_name", videosong.getName());
            intent.putExtra("video_position", player.getCurrentPosition());
            startActivity(intent);
            player.setPlayWhenReady(false);
        });
    }

    @Override public void onStart() {
        super.onStart();
        if (!playmp3) playvideo(); else showmp3view();
    }

    @Override public void onResume() {
        super.onResume();
        if (player != null) player.setPlayWhenReady(true);
    }

    @Override public void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) { /* keep instance; Exo handles lifecycle */ }
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
    }

    @Override public void onDestroy() {
        super.onDestroy();
    }

    public void releasePlayer() {
        if (player != null) {
            alreadayPlaying = false;
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentMediaItemIndex();
            player.removeListener(playbackStateListener);
            if (playerNotificationManager != null) playerNotificationManager.setPlayer(null);
            player.release();
            player = null;
        }
    }

    private void initializenotification() {
        PlayerNotificationManager.Builder builder = new PlayerNotificationManager.Builder(
                requireContext(),
                MainActivity.NOTIFICATION_ID,
                CHANNEL_ID
        );

        builder.setChannelNameResourceId(R.string.exo_download_notification_channel_name)
                .setChannelDescriptionResourceId(R.string.exo_download_notification_channel_name)
                .setMediaDescriptionAdapter(new DescriptionAdapter())
                .setNotificationListener(new PlayerNotificationManager.NotificationListener() {
                    @Override
                    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
                        PlayerNotificationManager.NotificationListener.super.onNotificationPosted(notificationId, notification, ongoing);
                        if (ongoing) {
                            ContextCompat.startForegroundService(requireContext(), new Intent(requireContext(), MusicService.class)); // Changed to MusicService
                        }
                    }

                    @Override
                    public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                        PlayerNotificationManager.NotificationListener.super.onNotificationCancelled(notificationId, dismissedByUser);
                        // Stop foreground service and remove the notification, as necessary
                        // This might involve calling stopSelf() from within your service
                    }
                })
                .setSmallIconResourceId(R.drawable.notify_icon);

        playerNotificationManager = builder.build();

        playerNotificationManager.setPlayer(player);
        playerNotificationManager.setColor(Color.BLACK);
        playerNotificationManager.setColorized(true);
        // These are now set in the builder, so they can be removed from here
        // playerNotificationManager.setUseChronometer(true);
        // playerNotificationManager.setUseStopAction(true);
        // playerNotificationManager.setSmallIcon(R.drawable.notify_icon);
        playerNotificationManager.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);
        // playerNotificationManager.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        // playerNotificationManager.setUseNavigationActions(false);
        // playerNotificationManager.setUsePlayPauseActions(true);
    }

    private void initializePlayer(String url) {
        if (player == null) {
            player = new ExoPlayer.Builder(requireContext()).build();
            initializenotification();
            alreadayPlaying = true;
            if (tvSpeed != null) tvSpeed.setText("1");
        }

        playerView.setPlayer(player);
        player.setPlayWhenReady(playWhenReady);
        player.addListener(playbackStateListener);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.seekTo(currentWindow, playbackPosition);
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
            aq.id(imgSong).image(GlobalValue.getCurrentSong().getImage(), true, false, 0, R.drawable.music_defaut);
        } else {
            new Handler().postDelayed(this::refreshData, 500);
        }
    }

    private void getCountDownLoadAndCountDown() {
        ModelManager.getCountDownAndCountListen(getActivity(), GlobalValue.getCurrentSong().getId(), new ModelManagerListener() {
            @Override public void onError(VolleyError error) { error.printStackTrace(); }
            @Override public void onSuccess(String json) {
                try {
                    JSONObject object = new JSONObject(json);
                    JSONArray jsonArray = object.getJSONArray(WebserviceApi.KEY_DATA);
                    JSONObject obj = jsonArray.getJSONObject(0);
                    lblNumberDownload.setText(obj.getString("download"));
                    lblNumberListen.setText(obj.getString("listen"));
                } catch (JSONException ignored) { }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_WRITE_STORAGE_Player && grantResults.length > 0) {
            if (!playmp3) {
                releasePlayer();
                initializePlayer(videosong.getUrl());
            }
        }
    }

    public void playvideo() {
        hidemp3view();
        if (!checkPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!alreadayPlaying) {
                    releasePlayer();
                    initializePlayer(videosong.getUrl());
                }
            } else {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE },
                        PERMISSION_WRITE_STORAGE_Player);
            }
        } else {
            if (!alreadayPlaying) {
                releasePlayer();
                initializePlayer(videosong.getUrl());
            }
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return true;
        int r = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        int w = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return r == PackageManager.PERMISSION_GRANTED && w == PackageManager.PERMISSION_GRANTED;
    }

    public void playvideo(Song currentSong) {
        playmp3 = false;
        videosong = currentSong;
        alreadayPlaying = false;
        if (relativeimage != null) playvideo();
    }

    public void setListener(PlayerListener listener) { this.listener = listener; }

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

    private class PlaybackStateListener implements Player.Listener {
        @Override public void onPlaybackStateChanged(int state) {
            switch (state) {
                case Player.STATE_ENDED:
                    alreadayPlaying = false;
                    if (listener != null) listener.onChangeSong(GlobalValue.currentSongPlay + 1);
                    break;
                default:
                    break;
            }
        }
    }

    class DescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {
        @Override public CharSequence getCurrentContentTitle(Player player) {
            if (GlobalValue.getCurrentSong() != null) {
                return GlobalValue.getCurrentSong().getName();
            }
            return "Unknown Title";
        }

        @Nullable @Override public CharSequence getCurrentContentText(Player player) {
            if (GlobalValue.getCurrentSong() != null && GlobalValue.getCurrentSong().getDescription() != null) {
                return Html.fromHtml(GlobalValue.getCurrentSong().getDescription());
            }
            return "Unknown Artist";
        }

        @Nullable @Override public android.graphics.Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback cb) {
            // This needs proper asynchronous loading if fetching from a URL.
            // For now, using a placeholder.
             if (GlobalValue.getCurrentSong() != null && GlobalValue.getCurrentSong().getImage() != null) {
                // TODO: Implement proper image loading with Glide or Picasso and use the callback.
                // Example:
                // Glide.with(requireContext())
                // .asBitmap()
                // .load(GlobalValue.getCurrentSong().getImage())
                // .into(new CustomTarget<Bitmap>() {
                // @Override
                // public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                // callback.onBitmap(resource);
                // }
                // @Override
                // public void onLoadCleared(@Nullable Drawable placeholder) { }
                // });
                // return null; // Return null now, callback will provide the bitmap
                return BitmapFactory.decodeResource(requireContext().getResources(), R.drawable.music_defaut); // Placeholder
            }
            return BitmapFactory.decodeResource(requireContext().getResources(), R.drawable.music_defaut); // Placeholder
        }

        @Nullable @Override public PendingIntent createCurrentContentIntent(Player player) {
            Intent intent = new Intent(getContext(), PlayerActivity.class);
            intent.putExtra("notification_status", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            return PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        }
    }
}
