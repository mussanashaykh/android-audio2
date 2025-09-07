package com.nurulquran.audio.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.nurulquran.audio.R;
import com.nurulquran.audio.config.GlobalValue;
import com.nurulquran.audio.database.DatabaseUtility;
import com.nurulquran.audio.database.MySharePreferences;
import com.nurulquran.audio.fragment.PlayerListPlayingFragment;
import com.nurulquran.audio.fragment.PlayerThumbFragment;
import com.nurulquran.audio.interfaces.LayoutFooter;
import com.nurulquran.audio.modelmanager.CommonParser;
import com.nurulquran.audio.modelmanager.ModelManager;
import com.nurulquran.audio.modelmanager.ModelManagerListener;
import com.nurulquran.audio.network.ControllerRequest;
import com.nurulquran.audio.object.Playlist;
import com.nurulquran.audio.object.Song;
import com.nurulquran.audio.service.MusicService;
import com.nurulquran.audio.service.PlayerListener;
import com.nurulquran.audio.util.NetworkUtil;
import com.nurulquran.audio.util.ShareUtility;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.ACTION_VIEW;

public class PlayerActivity extends BaseActivity implements View.OnClickListener {

    public static final int LIST_PLAYING = 0;
    public static final int THUMB_PLAYING = 1;
    public static final int PERMISSION_WRITE_STORAGE_Player = 30;
    public static final int PERMISSION_WRITE_STORAGE_Download = 33;

    private ImageView btnBackward, btnForward;
    public static ImageView btnPlay;
    private ImageView btnShuffle, btnRepeat;
    private ViewPager viewPager;
    private SeekBar seekBarLength;
    private TextView lblTimeCurrent, lblTimeLength;
    private View viewIndicatorList, viewIndicatorThumb;
    public static TextView lblTopHeader;
    private View btnAction;
    private static final long PERIOD_BANNER = 30000;
    private int pos = 0;
    private String rootFolder, ringtoneFolder, alarmFolder, notifyFolder;

    public PlayerListPlayingFragment playerListPlayingFragment;
    public PlayerThumbFragment playerThumbFragment;
    private List<Playlist> listPlaylists;
    String[] arrayPlaylistName;
    public MusicService mService;
    private Intent intentService;
    public static final int NOTIFICATION_ID = 231109;
    public static final int PICK_CONTACT_REQUEST = 120;
    public DatabaseUtility databaseUtility;
    private static LayoutFooter layoutFooter;
    public static final int RESULT_CODE = 123;
    private NetworkImageView mIVBanner;
    private ImageLoader imageLoader;
    private Timer timer;
    private boolean isCheckShuffle = false;
    private boolean isCheckRepeat = false;
    private LayoutInflater layoutInflater;
    private ServiceConnection mConnection;
    private boolean playmp3 = true;
    private RelativeLayout relativecontroller;
    private LinearLayout linearseekbar;

    private BroadcastReceiver downloadCompleteReceiver; // Field for DownloadManager receiver

    private final BroadcastReceiver playStateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (MusicService.ACTION_PLAYSTATE_CHANGED.equals(intent.getAction())) {
                boolean isPlaying = intent.getBooleanExtra(MusicService.EXTRA_IS_PLAYING, false);
                if (btnPlay != null) {
                    btnPlay.setBackgroundResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
                }
            }
        }
    };

    private void safeUnregister(BroadcastReceiver r) {
        try {
            if (r != null) { // Add null check for safety
                unregisterReceiver(r);
            }
        } catch (IllegalArgumentException ignore) {}
    }

    public void initconnection() {
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MusicService.ServiceBinder binder = (MusicService.ServiceBinder) service;
                if (getIntent().getBooleanExtra("notification_status", false)) {
                    MainActivity.toMusicPlayer = MainActivity.FROM_OTHER;
                }
                mService = binder.getService();
                mService.setListSongs(GlobalValue.listSongPlay);
                mService.updateSeekProgress();
                pauseRadio();
                setUpFunctionShuffle();
                setUpFunctionRepeat();

                mService.setListener(new PlayerListener() {
                    @Override
                    public void onSeekChanged(int maxProgress, String lengthTime, String currentTime, int progress) {
                        PlayerActivity.this.seekChanged(maxProgress, lengthTime, currentTime, progress);
                    }

                    @Override
                    public void onChangeSong(int indexSong) {
                        PlayerActivity.this.changeSong(indexSong);
                    }

                    @Override
                    public void OnMusicPrepared() {
                        runOnUiThread(() -> setButtonPlay()); // ✅ Explicitly sync icon on prep complete
                    }
                });

                GlobalValue.currentMusicService = mService;

                if (playmp3) playMusic();
                if (GlobalValue.getCurrentSong() != null) {
                    setHeaderTitle(GlobalValue.getCurrentSong().getName());
                }
                setButtonPlay();
            }

            @Override public void onServiceDisconnected(ComponentName name) { }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inFlaterLayOut(R.layout.activity_player);
        databaseUtility = new DatabaseUtility(this);

        initUI();
        initControl();
        initPlayList();
        if (GlobalValue.getCurrentSong() != null) {
            playSong();
            setHeaderTitle(GlobalValue.getCurrentSong().getName());
        } else {
            // Handle case where no song is selected, maybe close activity or show message
            Toast.makeText(this, "No song selected to play.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void hidemp3view() {
        if (playerThumbFragment != null) playerThumbFragment.hidemp3view();
        if (relativecontroller != null) relativecontroller.setVisibility(View.GONE);
        if (linearseekbar != null) linearseekbar.setVisibility(View.GONE);
    }

    private void showmp3view() {
        if (playerThumbFragment != null) playerThumbFragment.showmp3view();
        if (relativecontroller != null) relativecontroller.setVisibility(View.VISIBLE);
        if (linearseekbar != null) linearseekbar.setVisibility(View.VISIBLE);
    }

    private void playvideo(Song currentSong) {
        if (playerThumbFragment != null) {
            playerThumbFragment.hidemp3view();
            playerThumbFragment.playvideo(currentSong);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(playStateReceiver, new IntentFilter(MusicService.ACTION_PLAYSTATE_CHANGED), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(playStateReceiver, new IntentFilter(MusicService.ACTION_PLAYSTATE_CHANGED));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregistering playStateReceiver in onStop as per standard practice
    }

    @Override
    protected void onStop() {
        safeUnregister(playStateReceiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            if (playerThumbFragment != null) playerThumbFragment.releasePlayer();
            if (mService != null) {
                // Consider whether you really want to stopSelf here, or just unbind.
                // If the service is meant to continue playing in background, don't stopSelf.
                // mService.stopSelf(); // Commented out for now
            }
            if (mConnection != null) {
                try {
                    unbindService(mConnection);
                } catch (IllegalArgumentException e) {
                     Log.w("PlayerActivity", "Service not registered or already unregistered in onDestroy for mConnection.");
                }
            }

            if (downloadCompleteReceiver != null) {
                try {
                    unregisterReceiver(downloadCompleteReceiver);
                } catch (IllegalArgumentException e) {
                    Log.w("PlayerActivity", "downloadCompleteReceiver not registered or already unregistered in onDestroy.");
                }
                downloadCompleteReceiver = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            cancelNotification();
        }
        super.onDestroy();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mService != null) {
            setButtonPlay();
        } else {
            if (playmp3 && intentService != null && mConnection != null) {
                try { // Add try-catch for bindService
                    bindService(intentService, mConnection, Context.BIND_AUTO_CREATE);
                } catch (Exception e) {
                    Log.e("PlayerActivity", "Error binding service in onResume", e);
                }
            }
        }
    }

    public void playSong() {
        Song currentSong = GlobalValue.getCurrentSong();
        if (currentSong == null) {
            Toast.makeText(this, "Error: Current song is not available.", Toast.LENGTH_SHORT).show();
            finish(); // Or handle appropriately
            return;
        }

        if (checkmp3(currentSong.getUrl())) {
            initconnection(); // This sets up mConnection
            playmp3 = true;
            initService(); // This starts and binds the service
            showmp3view();
        } else {
            hidemp3view();
            playmp3 = false;
            playvideo(currentSong);
            if (viewPager != null) viewPager.setCurrentItem(THUMB_PLAYING);
        }

        if (MainActivity.isPlaylist) {
            if (checkUrl(currentSong.getUrl())) {
                if (checkmp3(currentSong.getUrl())) {
                    File file = new File(rootFolder, currentSong.getName() + ".mp3");
                    if (!file.exists()) downloadSong(currentSong, true);
                } else {
                    File file = new File(rootFolder, currentSong.getName() + ".mp4");
                    if (!file.exists()) downloadSong(currentSong, false);
                }
            } else {
                Toast.makeText(getApplicationContext(), currentSong.getName() + " " + getString(R.string.downloaded), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Potentially re-process intent if activity is re-launched while on top
    }

    private void initService() {
        intentService = new Intent(this, MusicService.class);
        try {
            ContextCompat.startForegroundService(this, intentService); // Ensures service starts correctly on O+
            bindService(intentService, mConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e("PlayerActivity", "Error starting or binding service", e);
            Toast.makeText(this, "Error initializing player service.", Toast.LENGTH_SHORT).show();
        }
    }

    private void playMusic() {
        if (GlobalValue.getCurrentSong() == null) return;

        switch (MainActivity.toMusicPlayer) {
            case MainActivity.FROM_LIST_SONG:
            case MainActivity.FROM_SEARCH:
                if (!MainActivity.isTapOnFooter) setCurrentSong(GlobalValue.currentSongPlay);
                if (playerListPlayingFragment != null) playerListPlayingFragment.refreshListPlaying();
                if (playerThumbFragment != null) playerThumbFragment.refreshData();
                setSelectTab(THUMB_PLAYING);
                if (viewPager != null) viewPager.setCurrentItem(THUMB_PLAYING);
                break;
            case MainActivity.FROM_NOTICATION:
                try {
                    if (playerListPlayingFragment != null) playerListPlayingFragment.refreshListPlaying();
                    if (playerThumbFragment != null) playerThumbFragment.refreshData();
                    setSelectTab(THUMB_PLAYING);
                    if (viewPager != null) viewPager.setCurrentItem(THUMB_PLAYING);
                    setCurrentSong(GlobalValue.currentSongPlay);
                } catch (Exception e) {
                    cancelNotification();
                }
                break;
            case MainActivity.FROM_OTHER:
            default:
                if (playerListPlayingFragment != null) playerListPlayingFragment.refreshListPlaying();
                if (playerThumbFragment != null) playerThumbFragment.refreshData();
                setSelectTab(THUMB_PLAYING);
                if (viewPager != null) viewPager.setCurrentItem(THUMB_PLAYING);
                break;
        }
    }

    public void cancelNotification() {
        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nMgr != null) {
            nMgr.cancel(NOTIFICATION_ID);
        }
    }

    private void initUI() {
        btnAction = findViewById(R.id.btnRightButton);
        relativecontroller = findViewById(R.id.relative_controller);
        linearseekbar = findViewById(R.id.linear_seekbar);
        if (btnAction != null) {
            btnAction.setBackgroundResource(R.drawable.ic_action);
            btnAction.setVisibility(View.VISIBLE);
        }
        lblTopHeader = findViewById(R.id.lblHeader);
        if (lblTopHeader != null) lblTopHeader.setSelected(true);

        btnBackward = findViewById(R.id.btnBackward);
        btnPlay = findViewById(R.id.btnPlayMusic);
        btnForward = findViewById(R.id.btnForward);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        seekBarLength = findViewById(R.id.seekBarLength);
        if (seekBarLength != null) seekBarLength.setMax(100);
        lblTimeCurrent = findViewById(R.id.lblTimeCurrent);
        lblTimeLength = findViewById(R.id.lblTimeLength);
        viewPager = findViewById(R.id.viewPager);
        viewIndicatorList = findViewById(R.id.viewIndicatorList);
        viewIndicatorThumb = findViewById(R.id.viewIndicatorThumb);
        mIVBanner = findViewById(R.id.ivBanner);
        imageLoader = ControllerRequest.getInstance().getImageLoader();
        layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        loadImageBanner();
    }

    private void initControl() {
        setButtonBack();
        // folders
        rootFolder = getApplicationContext().getExternalFilesDir(null) + "/" + getString(R.string.app_name) + "/";
        File folder = new File(rootFolder); if (!folder.exists()) folder.mkdirs();

        ringtoneFolder = rootFolder + "ringtone/"; File rf = new File(ringtoneFolder); if (!rf.exists()) rf.mkdirs();
        alarmFolder = rootFolder + "alarm/"; File af = new File(alarmFolder); if (!af.exists()) af.mkdirs();
        notifyFolder = rootFolder + "notify/"; File nf = new File(notifyFolder); if (!nf.exists()) nf.mkdirs();

        if (btnAction != null) btnAction.setOnClickListener(this);
        if (btnShuffle != null) btnShuffle.setOnClickListener(this);
        if (btnBackward != null) btnBackward.setOnClickListener(this);
        if (btnPlay != null) btnPlay.setOnClickListener(this);
        if (btnForward != null) btnForward.setOnClickListener(this);
        if (btnRepeat != null) btnRepeat.setOnClickListener(this);
        if (mIVBanner != null) mIVBanner.setOnClickListener(v -> gotoPageBanner());

        if (seekBarLength != null) {
            seekBarLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    if (mService != null) mService.seekTo(seekBar.getProgress());
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }
            });
        }

        playerListPlayingFragment = new PlayerListPlayingFragment();
        playerThumbFragment = new PlayerThumbFragment();
        playerThumbFragment.setListener(new PlayerListener() {
            @Override public void onSeekChanged(int maxProgress, String lengthTime, String currentTime, int progress) { }
            @Override public void onChangeSong(int indexSong) { nextsong(indexSong); } // 'nextsong' might be a typo for 'nextSong'
            @Override public void OnMusicPrepared() { }
        });

        if (viewPager != null) {
            viewPager.setAdapter(new MyFragmentPagerAdapter(getSupportFragmentManager()));
            viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override public void onPageSelected(int position) { setSelectTab(position); }
                @Override public void onPageScrolled(int arg0, float arg1, int arg2) { }
                @Override public void onPageScrollStateChanged(int arg0) { }
            });
        }
    }

    private void nextsong(int newPosition) { // Assuming this was meant to be nextSong, or ensure it calls changeSong correctly
        if (GlobalValue.listSongPlay == null || GlobalValue.listSongPlay.isEmpty()) return;
        if (GlobalValue.currentSongPlay < GlobalValue.listSongPlay.size() - 1) {
            newPosition = GlobalValue.currentSongPlay + 1;
        } else {
            newPosition = 0;
        }
        changeSong(newPosition); // This will then call startMusic
    }

    private void loadImageBanner() {
        if (getApplicationContext() == null) return; // Guard against null context
        ModelManager.loadBanner(getApplicationContext(), new ModelManagerListener() {
            @Override public void onError(VolleyError error) {
                Log.e("PlayerActivity", "Error loading banner", error);
            }
            @Override public void onSuccess(String json) {
                CommonParser.parseBanner(json);
                if (GlobalValue.mListBanner == null || GlobalValue.mListBanner.isEmpty()) return;
                Collections.shuffle(GlobalValue.mListBanner);
                if (timer != null) {
                    timer.cancel(); // Cancel previous timer if exists
                }
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override public void run() {
                        runOnUiThread(() -> {
                            if (GlobalValue.mListBanner == null || GlobalValue.mListBanner.isEmpty()) {
                                if (timer != null) timer.cancel();
                                return;
                            }
                            if (pos >= GlobalValue.mListBanner.size()) pos = 0; // Check bounds
                            if (mIVBanner != null && pos < GlobalValue.mListBanner.size()) {
                                // mIVBanner.setImageUrl(GlobalValue.mListBanner.get(pos).getImage(), imageLoader);
                                pos++;
                            }
                        });
                    }
                }, 0, PERIOD_BANNER);
            }
        });
    }

    private void gotoPageBanner() {
        if (GlobalValue.mListBanner == null || GlobalValue.mListBanner.isEmpty()) return;
        if ((pos - 1) >= 0 && (pos -1) < GlobalValue.mListBanner.size()) {
            String url = GlobalValue.mListBanner.get(pos - 1).getUrl();
            if (url != null && !url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://" + url;
                }
                try {
                    Intent intent = new Intent(ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("PlayerActivity", "Error opening banner URL", e);
                }
            }
        }
    }

    private void setSelectTab(int tab) {
        if (tab == LIST_PLAYING) {
            if (viewIndicatorList != null) viewIndicatorList.setBackgroundResource(R.color.black_4);
            if (viewIndicatorThumb != null) viewIndicatorThumb.setBackgroundResource(R.color.color_selector);
        } else {
            if (viewIndicatorList != null) viewIndicatorList.setBackgroundResource(R.color.color_selector);
            if (viewIndicatorThumb != null) viewIndicatorThumb.setBackgroundResource(R.color.black_4);
        }
    }

    private void setCurrentSong(int position) {
        if (GlobalValue.listSongPlay == null || position >= GlobalValue.listSongPlay.size() || position < 0) {
            return;
        }
        GlobalValue.currentSongPlay = position; // Ensure currentSongPlay is set before use
        if (playerListPlayingFragment != null) playerListPlayingFragment.refreshListPlaying();
        if (playerThumbFragment != null) playerThumbFragment.refreshData();

        Song songToPlay = GlobalValue.listSongPlay.get(position);

        if (checkmp3(songToPlay.getUrl())) {
            playmp3 = true; // Set before initService if mService is null
            if (mService != null) {
                mService.startMusic(position);
            } else {
                initconnection(); // Sets up mConnection
                initService();    // Starts and binds service, which will eventually call playMusic
            }
            showmp3view();
        } else {
            hidemp3view();
            playmp3 = false;
            if (mConnection != null) {
                try {
                    unbindService(mConnection);
                } catch (IllegalArgumentException e) {
                     Log.w("PlayerActivity", "Service not registered or already unregistered for mConnection in setCurrentSong.");
                }
                mConnection = null; // Nullify after unbind
                mService = null;    // Nullify after unbind
            }
            playerThumbFragment.playvideo(songToPlay);
            if (viewPager != null) viewPager.setCurrentItem(THUMB_PLAYING);
        }
    }


    public void seekChanged(int maxprogress, String lengthTime, String currentTime, int progress) {
        if (lblTimeLength != null) lblTimeLength.setText(lengthTime);
        if (lblTimeCurrent != null) lblTimeCurrent.setText(currentTime);
        if (seekBarLength != null) {
            seekBarLength.setMax(maxprogress);
            seekBarLength.setProgress(progress);
        }
    }

    public void changeSong(int indexSong) {
        try {
            startMusic(indexSong);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setButtonPlay() {
        if (btnPlay == null || mService == null) return;

        if (mService.isPlay()) {
            btnPlay.setBackgroundResource(R.drawable.ic_pause);
        } else {
            btnPlay.setBackgroundResource(R.drawable.ic_play);
        }
    }


    private void showMenuAction(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals(getString(R.string.download))) {
                onClickDownload();
            } else if (title.equals(getString(R.string.Share))) {
                onClickShare();
            } else if (title.equals(getString(R.string.add_to_playlist))) {
                showPlayList();
            }
            return false;
        });
        popupMenu.inflate(R.menu.popup_audio_action);
        popupMenu.show();
    }

    private void onClickShuffle() {
        if (mService == null) return;
        isCheckShuffle = !isCheckShuffle;
        mService.setShuffle(isCheckShuffle);
        MySharePreferences.getInstance().saveValueShuffle(getApplicationContext(), isCheckShuffle);
        if (isCheckShuffle) {
            showToast(R.string.enable_shuffle);
            if (btnShuffle != null) btnShuffle.setImageResource(R.drawable.ic_shuffle_white);
        } else {
            showToast(R.string.off_shuffle);
            if (btnShuffle != null) btnShuffle.setImageResource(R.drawable.ic_shuffle);
        }
    }

    private void setUpFunctionShuffle() {
        if (mService == null) return;
        isCheckShuffle = MySharePreferences.getInstance().getValueShuffle(getApplicationContext());
        mService.setShuffle(isCheckShuffle);
        if (mService.isShuffle()) {
            if (btnShuffle != null) btnShuffle.setImageResource(R.drawable.ic_shuffle_white);
        } else {
            if (btnShuffle != null) btnShuffle.setImageResource(R.drawable.ic_shuffle);
        }
    }

    private void onClickBackward() {
        if (mService != null) mService.backSongByOnClick();
    }

    private void onClickPlay() {
        if (mService != null) {
            mService.playOrPauseMusic();
            setButtonPlay();
        }
    }


    private void onClickForward() {
        if (mService != null) mService.nextSongByOnClick();
    }

    private void setUpFunctionRepeat() {
        if (mService == null) return;
        isCheckRepeat = MySharePreferences.getInstance().getValueRepeat(getApplicationContext());
        mService.setRepeat(isCheckRepeat);
        if (mService.isRepeat()) {
            if (btnRepeat != null) btnRepeat.setImageResource(R.drawable.ic_repeat_white);
        } else {
            if (btnRepeat != null) btnRepeat.setImageResource(R.drawable.ic_repeat);
        }
    }

    private void onClickRepeat() {
        if (mService == null) return;
        isCheckRepeat = !isCheckRepeat;
        mService.setRepeat(isCheckRepeat);
        MySharePreferences.getInstance().saveValueRepeat(getApplicationContext(), isCheckRepeat);
        if (mService.isRepeat()) { showToast(R.string.enableRepeat); }
        else { showToast(R.string.offRepeat); }

        if (btnRepeat != null) {
            if (isCheckRepeat) btnRepeat.setImageResource(R.drawable.ic_repeat_white);
            else btnRepeat.setImageResource(R.drawable.ic_repeat);
        }
    }

    private void onClickDownload() {
        Song currentSong = GlobalValue.getCurrentSong();
        if (currentSong == null) {
            Toast.makeText(this, "No song selected to download.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkUrl(currentSong.getUrl())) {
            if (checkmp3(currentSong.getUrl())) {
                File file = new File(rootFolder, currentSong.getName() + ".mp3");
                if (file.exists()) { confirmdownload(currentSong, true); }
                else { downloadSong(currentSong, true); }
            } else {
                File file = new File(rootFolder, currentSong.getName() + ".mp4");
                if (file.exists()) { confirmdownload(currentSong, false); }
                else { downloadSong(currentSong, false); }
            }
        } else {
            Toast.makeText(getApplicationContext(), currentSong.getName() + " " + getString(R.string.downloaded), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmdownload(final Song currentSong, boolean mp3) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(currentSong.getName());
        builder.setNegativeButton(R.string.undegree, (dialog, which) -> dialog.dismiss());
        if (layoutInflater == null) layoutInflater = LayoutInflater.from(this); // Ensure inflater exists
        View view = layoutInflater.inflate(R.layout.confirm_download_dialog, null);
        builder.setView(view);
        TextView tvTitle = view.findViewById(R.id.tv_title);
        if (tvTitle != null) {
            tvTitle.setText(currentSong.getName() + " " + getString(R.string.exist_download));
            tvTitle.setSelected(true);
        }
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Call super
        switch (requestCode) {
            case PERMISSION_WRITE_STORAGE_Player: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // Check grant result
                    if (playerThumbFragment != null) playerThumbFragment.playvideo();
                } else {
                    Toast.makeText(this, "Storage permission is required to play videos.", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case PERMISSION_WRITE_STORAGE_Download: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                     // Permission granted, re-attempt download if a song was pending
                     // You might need to store the song info temporarily to re-trigger download
                    Toast.makeText(this, "Storage permission granted. Please try downloading again.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Storage permission is required to download songs.", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void downloadSong(Song currentSong, boolean mp3) {
        // Use getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) for private app storage
        // or ensure MANAGE_EXTERNAL_STORAGE for broader access on Android 11+ (with careful consideration)
        if (!checkPermission()) { // This checks for WRITE_EXTERNAL_STORAGE on older APIs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { // Request for API 23-29
                 ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE },
                        PERMISSION_WRITE_STORAGE_Download);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11+ (API 30+), direct file path downloads to arbitrary locations
                // like Environment.getExternalStorageDirectory() are restricted.
                // You should use MediaStore or have MANAGE_EXTERNAL_STORAGE (not recommended for most apps).
                // For simplicity here, assuming download to app-specific directory which doesn't need this perm.
                download(currentSong, mp3); // Proceed if permission not needed for app-specific dir
            } else {
                 // Below API 23, permission is granted at install time.
                 download(currentSong, mp3);
            }
        } else {
            download(currentSong, mp3);
        }
    }

    private boolean checkPermission() {
        // For API 30+, WRITE_EXTERNAL_STORAGE has less effect for general directories.
        // Reading/writing to app-specific directories (getExternalFilesDir) doesn't need explicit permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // If targeting general directories, you'd need MANAGE_EXTERNAL_STORAGE checks here.
            // For app-specific directories, no runtime permission is needed.
            return true; // Assuming download to app-specific directory for simplicity
        } else { // For API < 30 (Android 10 and below)
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void download(Song currentSong, boolean mp3) {
        if (NetworkUtil.checkNetworkAvailable(PlayerActivity.this)) {
            // For Android 10+ (API 29+), use relative path with MediaStore for public collections like DIRECTORY_DOWNLOADS
            // For app-specific files, use getExternalFilesDir()
            String fileName = mp3 ? currentSong.getName() + ".mp3" : currentSong.getName() + ".mp4";
            File appSpecificDownloadDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (appSpecificDownloadDir == null) {
                Toast.makeText(getBaseContext(), "Cannot access storage for downloads.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!appSpecificDownloadDir.exists()) {
                appSpecificDownloadDir.mkdirs();
            }
            String localLink = new File(appSpecificDownloadDir, fileName).getAbsolutePath();

            Toast.makeText(getBaseContext(), getString(R.string.downloadStarted) + ": " + fileName, Toast.LENGTH_SHORT).show();

            // String result = localLink.substring(localLink.indexOf("/storage")); // This logic might be problematic
            GlobalValue.getCurrentSong().addMoreDownload();
            Song songObj = new Song();
            // ... (populate songObj as before, but ensure 'result' for setUrl is a valid accessible path or identifier)
            songObj.setId(GlobalValue.getCurrentSong().getId());
            songObj.setName(GlobalValue.getCurrentSong().getName());
            songObj.setmTypePathFile(Song.PATH_FILE_DOWNLOAD);
            songObj.setUrl(localLink); // Store the actual file path
            songObj.setImage(GlobalValue.getCurrentSong().getImage());
            songObj.setDescription(GlobalValue.getCurrentSong().getDescription());
            // ... (rest of songObj setup)

            DatabaseUtility databaseUtility = new DatabaseUtility(getApplicationContext());
            if (!databaseUtility.checkFavourite(getBaseContext(), GlobalValue.getCurrentSong().getId())) {
                databaseUtility.insertFavorite(songObj);
            }

            if (downloadCompleteReceiver != null) {
                try {
                    unregisterReceiver(downloadCompleteReceiver);
                } catch (IllegalArgumentException e) {
                    Log.w("PlayerActivity", "Previous downloadCompleteReceiver not registered.");
                }
            }
            downloadCompleteReceiver = new BroadcastReceiver() {
                public void onReceive(Context ctxt, Intent intent) {
                    Toast.makeText(getBaseContext(), getString(R.string.downloadComplete) + ": " + fileName, Toast.LENGTH_SHORT).show();
                     // Optional: Unregister here if this specific download is complete and receiver is no longer needed
                     // safeUnregister(downloadCompleteReceiver);
                     // downloadCompleteReceiver = null;
                }
            };

            DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(currentSong.getUrl());
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(fileName);
            request.setDescription("Downloading");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // Scoped storage considerations:
            // For API 29+: Download to a public directory like Environment.DIRECTORY_DOWNLOADS.
            // The system handles the actual file path.
            // request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, getString(R.string.app_name) + "/" + fileName);
            // Or, for app-specific storage visible only to your app:
            request.setDestinationUri(Uri.fromFile(new File(localLink))); // For older APIs or if writing to app-specific dir that you manage URI for

            downloadmanager.enqueue(request);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(downloadCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(downloadCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
        } else {
             Toast.makeText(this, "Network not available for download.", Toast.LENGTH_SHORT).show();
        }
    }


    private void onClickShare() {
        Song currentSong = GlobalValue.getCurrentSong();
        if (currentSong == null) {
            Toast.makeText(this, "No song to share.", Toast.LENGTH_SHORT).show();
            return;
        }
        String shareBody = currentSong.getUrl(); // This is likely a streaming URL

        // Determine the actual file path if downloaded
        String fileExtension = checkmp3(currentSong.getUrl()) ? ".mp3" : ".mp4";
        File appSpecificDownloadDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File localFile = null;
        if (appSpecificDownloadDir != null) {
             localFile = new File(appSpecificDownloadDir, currentSong.getName() + fileExtension);
        }


        Intent intentShare = new Intent(Intent.ACTION_SEND);

        if (localFile != null && localFile.exists()) {
            // Share the downloaded file
            Uri fileUri = FileProvider.getUriForFile(PlayerActivity.this, "com.nurulquran.audio.provider", localFile);
            intentShare.setType(checkmp3(currentSong.getUrl()) ? "audio/mpeg" : "video/mp4"); // Correct MIME type
            intentShare.putExtra(Intent.EXTRA_STREAM, fileUri);
            intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentShare.putExtra(Intent.EXTRA_SUBJECT, currentSong.getName());
            intentShare.putExtra(Intent.EXTRA_TEXT, "Shared from NurulQuran Audio: Get.NurulQuran.com"); // More descriptive text
            startActivity(Intent.createChooser(intentShare, "Share File"));
        } else {
            // Share the streaming link
            intentShare.setType("text/plain");
            intentShare.putExtra(Intent.EXTRA_SUBJECT, currentSong.getName() + " - " + currentSong.getArtist());
            intentShare.putExtra(Intent.EXTRA_TEXT, "Listen to " + currentSong.getName() + " on NurulQuran Audio: " + shareBody + " (Get.NurulQuran.com)");
            startActivity(Intent.createChooser(intentShare, getResources().getString(R.string.share)));
        }
    }


    private void initPlayList() {
        if (databaseUtility != null) {
            listPlaylists = databaseUtility.getAllPlaylist();
        } else {
            listPlaylists = Collections.emptyList();
        }
    }

    private void showPlayList() {
        if (listPlaylists == null || listPlaylists.isEmpty()) {
             Toast.makeText(getApplicationContext(), "Please add a new playlist first!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (GlobalValue.getCurrentSong() == null) {
            Toast.makeText(getApplicationContext(), "No song is currently playing to add to a playlist.", Toast.LENGTH_SHORT).show();
            return;
        }

        arrayPlaylistName = new String[listPlaylists.size()];
        for (int i = 0; i < arrayPlaylistName.length; i++) {
            arrayPlaylistName[i] = listPlaylists.get(i).getName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
        builder.setTitle(R.string.choosePlaylist).setSingleChoiceItems(arrayPlaylistName, 0,
                (dialog, which) -> {
                    dialog.dismiss();
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    Playlist playlist = listPlaylists.get(selectedPosition);
                    boolean isExisted = false;
                    for (Song song : playlist.getListSongs()) {
                        if (song.getId().equals(GlobalValue.getCurrentSong().getId())) {
                            isExisted = true; break;
                        }
                    }
                    if (isExisted) {
                        Toast.makeText(getApplicationContext(), "This song already exists in " + playlist.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        playlist.addSong(GlobalValue.getCurrentSong());
                        if (databaseUtility.updatePlaylist(playlist)) {
                            Toast.makeText(getApplicationContext(), "Added to " + playlist.getName() + " successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                             Toast.makeText(getApplicationContext(), "Failed to add to playlist.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        builder.create().show();
    }

    @Override public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnShuffle) {
            onClickShuffle();
        } else if (id == R.id.btnBackward) {
            onClickBackward();
        } else if (id == R.id.btnPlayMusic) {
            onClickPlay();
        } else if (id == R.id.btnForward) {
            onClickForward();
        } else if (id == R.id.btnRepeat) {
            onClickRepeat();
        } else if (id == R.id.btnRightButton) {
            showMenuAction(btnAction);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    Uri contactData = data.getData();
                    if (contactData == null) return;

                    // String[] PROJECTION = new String[]{ // This projection wasn't used
                    //         ContactsContract.Data.CONTACT_ID,
                    //         ContactsContract.Contacts.DISPLAY_NAME,
                    //         ContactsContract.Contacts.HAS_PHONE_NUMBER,};
                    Cursor localCursor = getContentResolver().query(contactData, null, null, null, null);
                    if (localCursor == null) return;

                    if (localCursor.moveToFirst()) {
                        @SuppressLint("Range") String contactID = localCursor.getString(localCursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                        @SuppressLint("Range") String contactDisplayName = localCursor.getString(localCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                        // Ensure rootFolder is initialized
                        if (rootFolder == null) {
                             rootFolder = getApplicationContext().getExternalFilesDir(null) + "/" + getString(R.string.app_name) + "/";
                        }
                        String localRingtoneFolder = rootFolder + "ringtone/";
                        new File(localRingtoneFolder).mkdirs(); // Ensure directory exists

                        Toast.makeText(this, "Setting ringtone for contact is processing...", Toast.LENGTH_LONG).show();
                        if (GlobalValue.getCurrentSong() != null) {
                            ShareUtility.setRingtoneContact(PlayerActivity.this, contactID, GlobalValue.getCurrentSong().getUrl(), localRingtoneFolder, contactData);
                            Toast.makeText(this, "Ringtone assigned to: " + contactDisplayName, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "No song selected to set as ringtone.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    localCursor.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.makeText(this, "Error assigning contact ringtone. Please try again.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void startMusic(int position) {
        if (GlobalValue.listSongPlay == null || position >= GlobalValue.listSongPlay.size() || position < 0) {
             Toast.makeText(this, "Invalid song position.", Toast.LENGTH_SHORT).show();
            return;
        }
        GlobalValue.currentSongPlay = position;
        Song songToPlay = GlobalValue.listSongPlay.get(position);

        if (playerThumbFragment != null) playerThumbFragment.refreshData();
        if (playerListPlayingFragment != null) playerListPlayingFragment.refreshListPlaying();
        if (lblTopHeader != null) setHeaderTitle(songToPlay.getName());

        if (checkmp3(songToPlay.getUrl())) {
            playmp3 = true;
            if (mService != null) {
                mService.startMusic(position);
                showmp3view(); // Show controls if service is already there
            } else {
                initconnection(); // Sets up mConnection
                initService();    // Starts and binds service. onServiceConnected will handle playMusic and showmp3view
            }
        } else { // Video
            hidemp3view();
            playmp3 = false;
            if (mConnection != null) { // If it's a video, we might not need the MusicService bound
                try {
                    unbindService(mConnection);
                } catch (IllegalArgumentException e) {
                     Log.w("PlayerActivity", "Service not registered or already unregistered for mConnection in startMusic (video).");
                }
                mConnection = null;
                mService = null;
            }
            if (playerThumbFragment != null) playerThumbFragment.playvideo(songToPlay);
            if (viewPager != null) viewPager.setCurrentItem(THUMB_PLAYING);
        }
    }

    private class MyFragmentPagerAdapter extends FragmentPagerAdapter {
        public MyFragmentPagerAdapter(FragmentManager fm) { super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT); } // Added behavior
        @Override public Fragment getItem(int position) {
            return position == 0 ? playerListPlayingFragment : playerThumbFragment;
        }
        @Override public int getCount() { return 2; }
    }

    @SuppressLint("DefaultLocale")
    private String getTime(int millis) {
        long second = (millis / 1000) % 60;
        long minute = millis / (1000 * 60);
        return String.format("%02d:%02d", minute, second);
    }

    public void chooseContacts() {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE); // More specific type
        try {
            startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open contacts. Please check permissions.", Toast.LENGTH_SHORT).show();
            Log.e("PlayerActivity", "Error starting contact picker", e);
        }
    }

    private void pauseRadio() {
        if (GlobalValue.currentMenu == MainActivity.RADIO) {
            if (mService != null && mService.mediaNotification != null) { // Check mService for null
                mService.mediaNotification.onClickMediaNotification();
            }
        }
    }

    protected boolean checkUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }
    protected boolean checkmp3(String url) {
        return url != null && url.toLowerCase().contains(".mp3"); // Use toLowerCase for case-insensitive check
    }

    // Helper to show toast messages
    protected void showToast(int messageResId) {
        // Your existing method implementation
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    // Helper to set header title safely
    protected void setHeaderTitle(String title) {
        if (lblTopHeader != null) {
            lblTopHeader.setText(title);
        }
    }
}
