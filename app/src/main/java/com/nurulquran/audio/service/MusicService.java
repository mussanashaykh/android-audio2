package com.nurulquran.audio.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.android.volley.VolleyError;
import com.nurulquran.audio.R;
import com.nurulquran.audio.activity.MainActivity;
import com.nurulquran.audio.activity.PlayerActivity;
import com.nurulquran.audio.config.GlobalValue;
import com.nurulquran.audio.config.WebserviceApi;
import com.nurulquran.audio.interfaces.MediaNotification;
import com.nurulquran.audio.modelmanager.ModelManager;
import com.nurulquran.audio.modelmanager.ModelManagerListener;
import com.nurulquran.audio.object.Song;
import com.nurulquran.audio.util.CustomTelephonyCallback;
import com.nurulquran.audio.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicService extends Service {

    // ---- Play/pause broadcast so the Activity updates its own UI safely
    public static final String ACTION_PLAYSTATE_CHANGED = "com.nurulquran.audio.PLAYSTATE_CHANGED";
    public static final String EXTRA_IS_PLAYING = "extra_is_playing";
    private static final String TAG = "MusicService_Lifecycle";

    private void notifyPlayState(boolean isPlaying) {
        Intent i = new Intent(ACTION_PLAYSTATE_CHANGED);
        i.putExtra(EXTRA_IS_PLAYING, isPlaying);
        sendBroadcast(i); // Activity registers/unregisters a BroadcastReceiver
    }
    // ---------------------------------------------------------------------

    //private static final String TAG = "MusicService";

    private final IBinder mBinder = new ServiceBinder();
    private List<Song> listSongs;
    private MediaPlayer mPlayer;
    private int length;
    private int lengthSong;
    private PlayerListener listener;
    public boolean isPause;
    private boolean isPreparing;
    private boolean isUpdatingSeek;
    private boolean isShuffle;
    private boolean isRepeat;
    private Handler mHandler;
    private boolean ringPhone = false;

    private Handler handler = new Handler();
    private static final int DELAY = 1000;
    private CustomTelephonyCallback customTelephonyCallback;

    private final Runnable r = new Runnable() {
        @Override public void run() { updateSeekProgress(); }
    };

    private NotificationManager notificationManager;

    public class ServiceBinder extends Binder {
        public MusicService getService() { return MusicService.this; }
    }

    @Override public IBinder onBind(Intent arg0) { return mBinder; }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate: Method CALLED. Attempting to start foreground service.");

        final String CHANNEL_ID = "music_service_channel_id_manual"; // Unique Channel ID
        final String CHANNEL_NAME = "Music Service Channel Manual";    // Channel Name for system settings
        final int FOREGROUND_NOTIFICATION_ID = 123456; // Unique ID for this notification

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW); // Use LOW to be less intrusive
                channel.setDescription("Channel for immediate foreground service notification."); // Optional description
                notificationManager.createNotificationChannel(channel);
                Log.e(TAG, "onCreate: Notification channel CREATED: " + CHANNEL_ID);
            } else {
                Log.e(TAG, "onCreate: Notification channel ALREADY EXISTS: " + CHANNEL_ID);
            }
        }

        // Intent to launch when notification is clicked (optional, but good practice)
        // Change PlayerActivity.class to your app's main activity if more appropriate for an initial notification
        Intent notificationIntent = new Intent(this, PlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notify_icon) // MAKE SURE this drawable exists in res/drawable
                .setContentTitle("Audio Service Active")      // Clear title
                .setContentText("Initializing audio playback...") // Clear message
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent) // Makes the notification clickable
                .setOngoing(true); // Indicates it's for a foreground service

        try {
            Log.e(TAG, "onCreate: PREPARING TO CALL startForeground() with Notification ID: " + FOREGROUND_NOTIFICATION_ID);
            startForeground(FOREGROUND_NOTIFICATION_ID, notificationBuilder.build());
            Log.e(TAG, "onCreate: SUCCESSFULLY CALLED startForeground().");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: EXCEPTION while calling startForeground(): " + e.getMessage(), e);
        }
    }


    @Override public void onTaskRemoved(Intent rootIntent) { cancelNotification(); }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler = new Handler();
        return START_STICKY;
    }

    private void showSmallNotification(Song song, PendingIntent resultPendingIntent) {
        final String CHANNEL_ID = "nurulquran";
        final String CHANNEL_NAME = "Notification-nurulquran";

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLightColor(Color.BLUE);
            channel.enableLights(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setSound(null, null);
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentTitle(song.getName())
                        .setSmallIcon(R.drawable.notify_icon)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.notify_icon));

        RemoteViews rmView = new RemoteViews(getPackageName(), R.layout.layout_custom_notification);
        notificationBuilder.setContent(rmView);

        rmView.setTextViewText(R.id.lbl_song_name, song.getName());
        rmView.setTextViewText(R.id.lbl_singer, song.getArtist());

        rmView.setOnClickPendingIntent(R.id.btnBackward,
                createReceiverIntent(this, GlobalValue.BACK_ACTION_ID));
        rmView.setOnClickPendingIntent(R.id.btnForward,
                createReceiverIntent(this, GlobalValue.NEXT_ACTION_ID));
        rmView.setOnClickPendingIntent(R.id.btnPlay,
                createReceiverIntent(this, GlobalValue.PLAY_OR_ACTION_ID));

        // Use the service field to avoid NPEs
        rmView.setInt(R.id.btnPlay, "setBackgroundResource",
                isPause ? R.drawable.ic_play : R.drawable.ic_pause);

        notificationBuilder.setDeleteIntent(createReceiverIntent(this, GlobalValue.PAUSE_ACTION_ID));
        notificationBuilder.setChannelId(CHANNEL_ID);

        // Tap -> open PlayerActivity
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("notification_status", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, piFlags);
        notificationBuilder.setContentIntent(contentIntent);

        // Foreground on all versions
        startForeground(MainActivity.NOTIFICATION_ID, notificationBuilder.build());
    }


    @Override public boolean onUnbind(Intent intent) {
        stopSelf();
        stopForeground(true);
        if (notificationManager != null) notificationManager.cancelAll();
        return super.onUnbind(intent);
    }

    public void updateSeekProgress() {
        try {
            listener.onSeekChanged(lengthSong, getLengSong(),
                    getTime(mPlayer.getCurrentPosition()),
                    mPlayer.getCurrentPosition());
            handler.postDelayed(r, DELAY);
        } catch (Exception e) {
            handler.postDelayed(r, DELAY);
        }
    }

    private void updateSeekProgressWithPlayingCheck() {
        try {
            listener.onSeekChanged(lengthSong, getLengSong(),
                    getTime(mPlayer.getCurrentPosition()),
                    mPlayer.getCurrentPosition());
            if (isPlay()) handler.postDelayed(r, DELAY);
        } catch (Exception e) {
            if (isPlay()) handler.postDelayed(r, DELAY);
        }
    }

    public boolean isPause() { return isPause; }
    public boolean isPreparing() { return isPreparing; }
    public void setPause(boolean isPause) { this.isPause = isPause; }
    public void changeStatePause() { isPause = !isPause; }

    public boolean isPlay() {
        try { return mPlayer.isPlaying(); }
        catch (Exception e) { return false; }
    }

    public boolean isRepeat() { return isRepeat; }
    public void setRepeat(boolean isRepeat) { this.isRepeat = isRepeat; }
    public boolean isShuffle() { return isShuffle; }
    public void setShuffle(boolean isShuffle) { this.isShuffle = isShuffle; }
    public void setListener(PlayerListener listener) { this.listener = listener; }

    public void setListSongs(List<Song> listSongs) {
        if (this.listSongs == null) this.listSongs = new ArrayList<>();
        this.listSongs.clear();
        this.listSongs.addAll(listSongs);
    }

    public List<Song> getListSongs() { return listSongs; }

    private void plusNewListen() {
        String getUrl = WebserviceApi.getAddNewView(getBaseContext()) + "?id=" + GlobalValue.getCurrentSong().getId();
        ModelManager.sendGetRequest(getApplicationContext(), getUrl, null, false, new ModelManagerListener() {
            @Override public void onError(VolleyError error) { }
            @Override public void onSuccess(String json) { }
        });
    }

    private void setNewPlayer() {
        mPlayer = new MediaPlayer();
        mPlayer.setVolume(100, 100);
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        checkCall();

        mPlayer.setOnErrorListener((mp, what, extra) -> {
            isPreparing = false;
            int newPosition;
            if (isShuffle) {
                newPosition = new Random().nextInt(listSongs.size());
            } else {
                if (isRepeat) newPosition = GlobalValue.currentSongPlay;
                else if (GlobalValue.currentSongPlay < listSongs.size() - 1) {
                    newPosition = GlobalValue.currentSongPlay + 1;
                } else {
                    newPosition = 0;
                }
            }
            mHandler.post(new ToastRunnable(getString(R.string.song_error) + " " + listSongs.get(newPosition).getName() + " will be play in 3s"));
            new Handler().postDelayed(this::nextSong, 3000);
            return true;
        });

        mPlayer.setOnCompletionListener(mp -> {
            Log.e("musicService", "progress : completed");
            addListen();
            nextSong();
        });

        mPlayer.setOnPreparedListener(mp -> {
            lengthSong = mPlayer.getDuration();
            isPreparing = false;
            mPlayer.start();
            isPause = false;
            if (listener != null) listener.OnMusicPrepared();
            if (!isUpdatingSeek) {
                isUpdatingSeek = true;
                updateSeekProgress();
            }
            notifyPlayState(true);
        });

    }

    public void startMusic(int index) {
        Log.e("MusicService", "Start");
        isPause = false;
        GlobalValue.currentSongPlay = index;

        try { mPlayer.reset(); }
        catch (Exception e) { setNewPlayer(); }

        try {
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(listSongs.get(index).getUrl());
            mPlayer.prepareAsync();
            showSmallNotification(GlobalValue.getCurrentSong(), createReceiverIntent(this, MainActivity.NOTIFICATION_ID));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playOrPauseMusic() {
        if (isPause) resumeMusic();
        else pauseMusic();
    }

    public void pauseMusic() { pauseMusic(true); }

    public void pauseMusic(boolean doCancelNotification) {
        if (mPlayer.isPlaying()) {
            handler.removeCallbacks(r);
            isPause = true;
            length = mPlayer.getCurrentPosition();
            mPlayer.pause();
            isUpdatingSeek = false;
            notifyPlayState(false);
        }

        // Always update the notification to refresh the play/pause icon
        showSmallNotification(GlobalValue.getCurrentSong(), createReceiverIntent(this, MainActivity.NOTIFICATION_ID));


        if (doCancelNotification) {
            cancelNotification();
        }
    }


    public void resumeMusic() {
        if (isPause) {
            handler.postDelayed(r, DELAY);
            mPlayer.seekTo(length);
            mPlayer.start();
            isPause = false;
            showSmallNotification(GlobalValue.getCurrentSong(), createReceiverIntent(this, MainActivity.NOTIFICATION_ID));
            if (!isUpdatingSeek) {
                isUpdatingSeek = true;
                updateSeekProgress();
            }
            notifyPlayState(true); // <— tell UI we’re playing
        }
    }

    public void seekTo(int progress) {
        mPlayer.seekTo(progress);
        length = progress;
        updateSeekProgressWithPlayingCheck();
        if (progress >= lengthSong) nextSongByOnClick();
    }

    public void backSong() {
        int newPosition;
        if (isShuffle) newPosition = new Random().nextInt(listSongs.size());
        else {
            if (isRepeat) newPosition = GlobalValue.currentSongPlay;
            else if (GlobalValue.currentSongPlay > 0) newPosition = GlobalValue.currentSongPlay - 1;
            else newPosition = listSongs.size() - 1;
        }
        startMusic(newPosition);
        if (listener != null) listener.onChangeSong(newPosition);
    }

    public void backSongByOnClick() { backSong(); }

    public void nextSong() {
        int newPosition;
        if (isShuffle) newPosition = new Random().nextInt(listSongs.size());
        else {
            if (isRepeat) newPosition = GlobalValue.currentSongPlay;
            else if (GlobalValue.currentSongPlay < listSongs.size() - 1) newPosition = GlobalValue.currentSongPlay + 1;
            else newPosition = 0;
        }
        startMusic(newPosition);
        if (listener != null) listener.onChangeSong(newPosition);
    }

    public void nextSongByOnClick() { nextSong(); }

    public String getLengSong() { return getTime(lengthSong); }

    @SuppressLint("DefaultLocale")
    public String getTime(int millis) {
        long second = (millis / 1000) % 60;
        long minute = millis / (1000 * 60);
        return String.format("%02d:%02d", minute, second);
    }

    private void checkCall() {
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    if (listSongs != null && mPlayer != null && mPlayer.isPlaying()) pauseMusic();
                    ringPhone = true;
                } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                    if (ringPhone) {
                        if (listSongs != null) {
                            if (GlobalValue.currentMenu != MainActivity.RADIO) {
                                if (mPlayer != null && !mPlayer.isPlaying()) resumeMusic();
                            } else {
                                pauseMusic();
                            }
                        }
                        ringPhone = false;
                    }
                } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    if (listSongs != null && mPlayer != null && mPlayer.isPlaying()) pauseMusic();
                    ringPhone = true;
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };

        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                TelephonyManager telephony = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
                customTelephonyCallback = new CustomTelephonyCallback() {
                    @Override protected void ringing() {
                        if (listSongs != null && mPlayer != null && mPlayer.isPlaying()) pauseMusic();
                        ringPhone = true;
                    }
                    @Override protected void idle() {
                        if (ringPhone) {
                            if (listSongs != null) {
                                if (GlobalValue.currentMenu != MainActivity.RADIO) {
                                    if (mPlayer != null && !mPlayer.isPlaying()) resumeMusic();
                                } else {
                                    pauseMusic();
                                }
                            }
                            ringPhone = false;
                        }
                    }
                    @Override protected void offHook() {
                        if (listSongs != null && mPlayer != null && mPlayer.isPlaying()) pauseMusic();
                        ringPhone = true;
                    }
                };
                telephony.registerTelephonyCallback(getApplicationContext().getMainExecutor(), customTelephonyCallback);
            } else {
                TelephonyManager mgr = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
                if (mgr != null) mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
    }

    private void sendNotification() {
        Song song = GlobalValue.getCurrentSong();
        final String CHANNEL_ID = "nurulquran";
        final String CHANNEL_NAME = "Notification-nurulquran";

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLightColor(Color.BLUE);
            channel.enableLights(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(true);
            mNotificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notify_icon)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        RemoteViews rmView = new RemoteViews(getPackageName(), R.layout.layout_custom_notification);
        mBuilder.setContent(rmView);

        rmView.setTextViewText(R.id.lbl_song_name, song.getName());
        rmView.setTextViewText(R.id.lbl_singer, song.getArtist());
        rmView.setOnClickPendingIntent(R.id.btnBackward, createReceiverIntent(this, GlobalValue.BACK_ACTION_ID));
        rmView.setOnClickPendingIntent(R.id.btnForward, createReceiverIntent(this, GlobalValue.NEXT_ACTION_ID));
        rmView.setOnClickPendingIntent(R.id.btnPlay, createReceiverIntent(this, GlobalValue.PLAY_OR_ACTION_ID));
        rmView.setInt(R.id.btnPlay, "setBackgroundResource", isPause ? R.drawable.ic_play : R.drawable.ic_pause);

        mBuilder.setDeleteIntent(createReceiverIntent(this, GlobalValue.PAUSE_ACTION_ID));

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("notification_status", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, piFlags);
        mBuilder.setContentIntent(contentIntent);

        startForeground(MainActivity.NOTIFICATION_ID, mBuilder.build());
    }


    private PendingIntent createReceiverIntent(Context context, int notificationId) {
        Intent intent = new Intent(context, NotificationDismissedReceiver.class);
        intent.putExtra("notificationId", notificationId);

        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE; // required on API 23+, enforced on 31+
        }
        return PendingIntent.getBroadcast(context.getApplicationContext(), notificationId, intent, flags);
    }


    private void cancelNotification() { stopForeground(true); }

    @Override public void onDestroy() {
        super.onDestroy();
        Logger.e(TAG, "onDestroy");
        if (mPlayer != null) {
            try {
                mPlayer.stop();
                mPlayer.release();
            } finally {
                mPlayer = null;
            }
        }
    }

    private class ToastRunnable implements Runnable {
        String mText;
        ToastRunnable(String text) { mText = text; }
        @Override public void run() {
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_LONG).show();
        }
    }

    private void addListen() {
        String getUrl = WebserviceApi.getAddNewView(getBaseContext()) + "?id=" + GlobalValue.getCurrentSong().getId();
        ModelManager.sendGetRequest(getApplicationContext(), getUrl, null, false, new ModelManagerListener() {
            @Override public void onError(VolleyError error) { }
            @Override public void onSuccess(String json) { }
        });
    }

    public static MediaNotification mediaNotification;
    public static MediaNotification listenerClickMediaNotification(MediaNotification mMediaNotification) {
        mediaNotification = mMediaNotification;
        return mediaNotification;
    }
}
