package com.nurulquran.audio.activity;

import static android.content.Intent.ACTION_VIEW;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.nurulquran.audio.R;
import com.nurulquran.audio.config.GlobalValue;
import com.nurulquran.audio.database.DatabaseUtility;
import com.nurulquran.audio.fragment.AlbumFragment;
import com.nurulquran.audio.fragment.ListSongsFragment;
import com.nurulquran.audio.fragment.SearchFragment;
import com.nurulquran.audio.gcm.Args;
import com.nurulquran.audio.modelmanager.CommonParser;
import com.nurulquran.audio.modelmanager.ModelManager;
import com.nurulquran.audio.modelmanager.ModelManagerListener;
import com.nurulquran.audio.network.ControllerRequest;
import com.nurulquran.audio.object.Playlist;
import com.nurulquran.audio.object.Song;
import com.nurulquran.audio.service.MusicService;
import com.nurulquran.audio.service.MusicService.ServiceBinder;
import com.nurulquran.audio.service.PlayerListener;
import com.nurulquran.audio.slidingmenu.SlidingMenu;
import com.nurulquran.audio.util.Logger;
import com.nurulquran.audio.util.NetworkUtil;
import com.nurulquran.audio.widget.AutoBgButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends FragmentActivity implements OnClickListener {
    public static final int MOST_FAVOURITE = 0;
    public static final int LASTEST = 1;
    public static final int SERIES = 2;
    public static final int CATEGORY_MUSIC = 3;
    public static final int PLAYLIST = 4;
    public static final int SEARCH = 5;
    public static final int RADIO = 6;
    public static final int MULTIPLE_YOUR_REWARD = 7;
    public static final int MY_DOWNLOAD = 8;
    public static final int ABOUT = 9;
    public static final int EXIT_APP = 10;

    public static final int LIST_SONG_FRAGMENT = 0;
    public static final int CATEGORY_MUSIC_FRAGMENT = 1;
    public static final int PLAYLIST_FRAGMENT = 2;
    public static final int SEARCH_FRAGMENT = 3;
    public static final int RADIO_FRAGMENT = 4;
    public static final int SETTING_FRAGMENT = 5;
    public static final int MULTIPLE_YOUR_WARD_FRAGMENT = 6;
    public static final int MY_DOWNLOAD_FRAGMENT = 7;
    public static final int ABOUT_FRAGMENT = 8;
    public static final int ALBUM_FRAGMENT = 9;

    public static final int FROM_LIST_SONG = 0;
    public static final int FROM_NOTICATION = 10000;
    public static final int FROM_SEARCH = 2;
    public static final int FROM_OTHER = 3;

    public static final int NOTIFICATION_ID = 231109;
    public static final int PICK_CONTACT_REQUEST = 120;

    private FragmentManager fm;
    private Fragment[] arrayFragments;
    public SlidingMenu menu;
    private AutoBgButton btnPlayFooter; // Changed from static to private instance field
    private AutoBgButton btnPreviousFooter, btnNextFooter;
    private View layoutPlayerFooter;
    private TextView lblSongNameFooter, lblArtistFooter;

    private TextView lblTopChart, lblAllSongs, lblAlbum, lblCategoryMusic,
            lblPlaylist, lblSearch, lblMultipleYourReward, lblAbout, lblExitApp, lblRadio, llMyDownLoad, lblCleatCache;

    private LinearLayout llAdview;

    public int currentFragment;
    public static int toMusicPlayer;
    public static boolean isTapOnFooter;
    public Playlist currentPlaylist;

    public List<Song> listNominations;
    public List<Song> listTopWeek;

    public DatabaseUtility databaseUtility;
    public MusicService mService;
    private Intent intentService;
    private NetworkImageView mImageBanner;
    private ImageLoader imageLoader;
    private Timer mTimer;
    private static final long PERIOD_BANNER = 30000;
    private int pos = 0;
    private boolean mIsPause;

    public static boolean isPlaylist = false;

    // ---- Notification permission (Android 13+)
    private static final int REQ_POST_NOTIF = 2001;

    private void askPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIF
                );
            }
        }
    }

    // ---- Listen to play/pause changes coming from MusicService
    private final BroadcastReceiver playStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MusicService.ACTION_PLAYSTATE_CHANGED.equals(intent.getAction())) {
                boolean isPlaying = intent.getBooleanExtra(MusicService.EXTRA_IS_PLAYING, false);
                updateFooterPlayState(isPlaying);
            }
        }
    };

    private void updateFooterPlayState(boolean isPlaying) {
        mIsPause = !isPlaying;
        if (btnPlayFooter != null) {
            btnPlayFooter.setBackgroundResource(
                    isPlaying ? R.drawable.bg_btn_pause_small : R.drawable.bg_btn_play_small
            );
        }
    }
    // ------------------------------------------------------------

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceBinder binder = (ServiceBinder) service;
            mService = binder.getService();
            if (GlobalValue.currentMenu != RADIO) {
                setVisibilityFooter();
            }

            mService.setListener(new PlayerListener() {
                @Override
                public void onSeekChanged(int maxProgress, String lengthTime, String currentTime, int progress) {
                    // no-op here
                }

                @Override
                public void onChangeSong(int indexSong) {
                    lblSongNameFooter.setText(GlobalValue.getCurrentSong().getName());
                    if (GlobalValue.getCurrentSong().getDescription() != null) {
                        lblArtistFooter.setText(Html.fromHtml(GlobalValue.getCurrentSong().getDescription()));
                    } else {
                        lblArtistFooter.setText("");
                    }
                    // Update button based on actual service state via broadcast, or a direct check if needed after song change
                    if (mService != null) {
                        updateFooterPlayState(!mService.isPause());
                    }
                }

                @Override
                public void OnMusicPrepared() {
                    if (GlobalValue.currentMenu != RADIO) {
                        setVisibilityFooter();
                    }
                }
            });
            GlobalValue.currentMusicService = mService;

            // Sync footer button immediately when we connect
            updateFooterPlayState(mService != null && !mService.isPause());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // no-op
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NetworkUtil.enableStrictMode();
        initList();
        databaseUtility = new DatabaseUtility(this);
        setContentView(R.layout.activity_main);

        // Ask notification permission on Android 13+
        askPostNotificationsIfNeeded();

        initService();
        initMenu();
        initUI();
        initControl();
        initFragment();
        isTapOnFooter = false;
        setSelect(GlobalValue.currentMenu);
        toMusicPlayer = MainActivity.FROM_OTHER;
        ListSongsFragment.isShowing = false;

        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }
    }

    public void unbindservice() {
        new Handler().postDelayed(() -> {
            try {
                unbindService(mConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1000);
    }

    @Override
    public void onDestroy() {
        cancelNotification();
        try {
            unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
            cancelNotification();
        }
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        try { unregisterReceiver(playStateReceiver); } catch (Exception ignored) {}
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Intentionally keeping the service bound
    }

    @Override
    public void onResume() {
        super.onResume();
        // Listen for play/pause updates from the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(playStateReceiver, new IntentFilter(MusicService.ACTION_PLAYSTATE_CHANGED), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(playStateReceiver, new IntentFilter(MusicService.ACTION_PLAYSTATE_CHANGED));
        }

        if (mService != null) {
            if (GlobalValue.currentMenu != RADIO) {
                setVisibilityFooter();
            }
            // Sync button state on resume
            updateFooterPlayState(!mService.isPause());
        } else {
           // If service is not yet connected, hide footer or set to default play icon until connection and broadcast update it
            if (layoutPlayerFooter != null) layoutPlayerFooter.setVisibility(View.GONE); // Or set to default play icon
        }
        openPlayerFromNotification(getIntent());
        bindService(intentService, mConnection, Context.BIND_AUTO_CREATE); // Ensure service is bound
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        openPlayerFromNotification(intent);
    }

    private void openPlayerFromNotification(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            if (intent.hasExtra(Args.NOTIFICATION)) {
                if (bundle.getBoolean(Args.NOTIFICATION)) {
                    if (intent.hasExtra("Song")) {
                        if (GlobalValue.listSongPlay == null) {
                            GlobalValue.constructor(MainActivity.this);
                        }
                        Intent intentPlayer = new Intent(getApplicationContext(), PlayerActivity.class);
                        Song song = intent.getParcelableExtra("Song");
                        MainActivity.toMusicPlayer = MainActivity.FROM_NOTICATION;
                        GlobalValue.listSongPlay.clear();
                        GlobalValue.listSongPlay.add(song);
                        GlobalValue.currentSongPlay = 0;
                        intentPlayer.putExtra("Song", song);
                        startActivity(intentPlayer);
                    }
                    intent.putExtra(Args.NOTIFICATION, false);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            outState.putInt("currentSongPlay", GlobalValue.currentSongPlay);
            outState.putInt("currentMenu", GlobalValue.currentMenu);
            outState.putInt("currentCategoryId", GlobalValue.currentCategoryId);
            outState.putInt("currentParentCategoryId", GlobalValue.currentParentCategoryId);
            outState.putString("currentCategoryName", GlobalValue.currentCategoryName);
            outState.putInt("currentAlbumId", GlobalValue.currentAlbumId);
            outState.putString("currentAlbumName", GlobalValue.currentAlbumName);
            cancelNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            try {
                GlobalValue.listSongPlay = databaseUtility.getAllFavorite();
                GlobalValue.currentSongPlay = savedInstanceState.getInt("currentSongPlay");
                GlobalValue.currentMenu = savedInstanceState.getInt("currentMenu");
                GlobalValue.currentCategoryId = savedInstanceState.getInt("currentCategoryId");
                GlobalValue.currentParentCategoryId = savedInstanceState.getInt("currentParentCategoryId");
                GlobalValue.currentCategoryName = savedInstanceState.getString("currentCategoryName");
                GlobalValue.currentAlbumId = savedInstanceState.getInt("currentAlbumId");
                GlobalValue.currentAlbumName = savedInstanceState.getString("currentAlbumName");
                cancelNotification();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setVisibilityFooter() {
        try {
            if (mService != null && (mService.isPause() || mService.isPlay())) { // Added null check for mService
                layoutPlayerFooter.setVisibility(View.VISIBLE);
                lblSongNameFooter.setText(GlobalValue.getCurrentSong().getName());
                if (GlobalValue.getCurrentSong().getDescription() != null) {
                    lblArtistFooter.setText(Html.fromHtml(GlobalValue.getCurrentSong().getDescription()));
                } else {
                    lblArtistFooter.setText("");
                }
                updateFooterPlayState(!mService.isPause()); // Update based on actual service state
            } else {
                layoutPlayerFooter.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            if (layoutPlayerFooter != null) layoutPlayerFooter.setVisibility(View.GONE);
        }
    }

    public void hideMediaFooter() {
        if (layoutPlayerFooter != null) layoutPlayerFooter.setVisibility(View.GONE);
    }

    public void showMediaFooter() {
        if (mService != null && (mService.isPlay() || mService.isPause())) { // Added null check
             if (layoutPlayerFooter != null) layoutPlayerFooter.setVisibility(View.VISIBLE);
        }
    }

    private void initService() {
        intentService = new Intent(this, MusicService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intentService);
        } else {
            startService(intentService);
        }
        bindService(intentService, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void initUI() {
        btnPreviousFooter = findViewById(R.id.btnPreviousFooter);
        btnPlayFooter = findViewById(R.id.btnPlayFooter);
        btnNextFooter = findViewById(R.id.btnNextFooter);
        layoutPlayerFooter = findViewById(R.id.layoutPlayerFooterMain);
        lblSongNameFooter = findViewById(R.id.lblSongNameFooter);
        lblArtistFooter = findViewById(R.id.lblArtistFooter);
        lblTopChart = menu.findViewById(R.id.lblTopChart);
        lblAllSongs = menu.findViewById(R.id.lblAllSong);
        lblAlbum = menu.findViewById(R.id.lblNominations);
        lblCategoryMusic = menu.findViewById(R.id.lblCategoryMusic);
        lblPlaylist = menu.findViewById(R.id.lblPlaylist);
        lblSearch = menu.findViewById(R.id.lblSearch);
        lblMultipleYourReward = menu.findViewById(R.id.lblMultipleYourReward);
        lblAbout = menu.findViewById(R.id.lblAbout);
        llMyDownLoad = menu.findViewById(R.id.lblMyDownLoad);
        lblExitApp = menu.findViewById(R.id.lblExitApp);
        llAdview = findViewById(R.id.adMod);
        mImageBanner = findViewById(R.id.nwImageBanner);
        imageLoader = ControllerRequest.getInstance().getImageLoader();
        lblRadio = findViewById(R.id.lblRadio);
        lblCleatCache = menu.findViewById(R.id.lblClear);
        hideBannerAd();
    }

    private void initControl() {
        btnPreviousFooter.setOnClickListener(this);
        btnPlayFooter.setOnClickListener(this);
        btnNextFooter.setOnClickListener(this);
        layoutPlayerFooter.setOnClickListener(this);
        lblTopChart.setOnClickListener(this);
        lblAllSongs.setOnClickListener(this);
        lblAlbum.setOnClickListener(this);
        lblCategoryMusic.setOnClickListener(this);
        lblPlaylist.setOnClickListener(this);
        lblSearch.setOnClickListener(this);
        lblRadio.setOnClickListener(this);
        lblMultipleYourReward.setOnClickListener(this);
        llMyDownLoad.setOnClickListener(this);
        lblAbout.setOnClickListener(this);
        lblExitApp.setOnClickListener(this);
        lblSongNameFooter.setSelected(true);
        lblArtistFooter.setSelected(true);
        lblCleatCache.setOnClickListener(this);
        mImageBanner.setOnClickListener(v -> gotoPageBanner());
        loadImageBanner();
    }

    private void loadImageBanner() {
        ModelManager.loadBanner(getApplicationContext(), new ModelManagerListener() {
            @Override
            public void onError(VolleyError error) { }

            @Override
            public void onSuccess(String json) {
                CommonParser.parseBanner(json);
                Collections.shuffle(GlobalValue.mListBanner);
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            if (pos == GlobalValue.mListBanner.size()) {
                                pos = 0;
                            }
                            if (pos < GlobalValue.mListBanner.size()) {
                                mImageBanner.setImageUrl(GlobalValue.mListBanner.get(pos).getImage(), imageLoader);
                                pos++;
                            }
                        });
                    }
                }, 0, PERIOD_BANNER);
            }
        });
    }

    private void gotoPageBanner() {
        if ((pos - 1) >= 0 && GlobalValue.mListBanner != null && (pos -1) < GlobalValue.mListBanner.size() ) { // Added null and bounds check
            String url = GlobalValue.mListBanner.get(pos - 1).getUrl();
            if (url != null && !checkUrl(url)) { // Added null check for url
                url = "http://" + url;
            }
            if (url != null) { // Check url again before parsing
               Intent intent = new Intent(ACTION_VIEW, Uri.parse(url));
               startActivity(intent);
            }
        }
    }

    protected boolean checkUrl(String url) {
        return (url != null && (url.startsWith("http://") || url.startsWith("https://"))); // Added null check
    }

    private void initFragment() {
        fm = getSupportFragmentManager();
        arrayFragments = new Fragment[10];
        arrayFragments[LIST_SONG_FRAGMENT] = fm.findFragmentById(R.id.fragmentListSongs);
        arrayFragments[CATEGORY_MUSIC_FRAGMENT] = fm.findFragmentById(R.id.fragmentCategoryMusic);
        arrayFragments[PLAYLIST_FRAGMENT] = fm.findFragmentById(R.id.fragmentPlaylist);
        arrayFragments[SEARCH_FRAGMENT] = fm.findFragmentById(R.id.fragmentSearch);
        arrayFragments[RADIO_FRAGMENT] = fm.findFragmentById(R.id.frRadio);
        arrayFragments[SETTING_FRAGMENT] = fm.findFragmentById(R.id.fragmentSetting);
        arrayFragments[MULTIPLE_YOUR_WARD_FRAGMENT] = fm.findFragmentById(R.id.fragmentMultipleYourReward);
        arrayFragments[MY_DOWNLOAD_FRAGMENT] = fm.findFragmentById(R.id.fragmentMyDownLoad);
        arrayFragments[ABOUT_FRAGMENT] = fm.findFragmentById(R.id.fragmentAbout);
        arrayFragments[ALBUM_FRAGMENT] = fm.findFragmentById(R.id.fragmentAlbum);

        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment fragment : arrayFragments) {
            if (fragment != null) transaction.hide(fragment); // Added null check
        }
        transaction.commit();
    }

    private void showFragment(int fragmentIndex) {
        if (fragmentIndex < 0 || fragmentIndex >= arrayFragments.length || arrayFragments[fragmentIndex] == null) return; // Bounds and null check
        currentFragment = fragmentIndex;
        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment fragment : arrayFragments) {
            if (fragment != null) transaction.hide(fragment); // Added null check
        }
        transaction.show(arrayFragments[fragmentIndex]);
        transaction.commit();
        Logger.e(fragmentIndex);
    }

    private void initList() {
        listNominations = new ArrayList<>();
        listTopWeek = new ArrayList<>();
    }

    private void initMenu() {
        menu = new SlidingMenu(this);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        menu.setFadeDegree(0.35f);
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        menu.setMenu(R.layout.layout_menu);
        menu.setOnOpenListener(() -> {
            if (arrayFragments != null && SEARCH_FRAGMENT < arrayFragments.length && arrayFragments[SEARCH_FRAGMENT] instanceof SearchFragment) { // Check before cast
                SearchFragment searchFragment = (SearchFragment) arrayFragments[SEARCH_FRAGMENT];
                searchFragment.hideSoft();
            }
        });
    }

    public void hideBannerAd() {
        if (llAdview != null) llAdview.setVisibility(View.GONE);
    }

    public void showBannerAd() {
        if (llAdview != null) llAdview.post(() -> llAdview.setVisibility(View.VISIBLE));
    }

    public void gotoFragment(int fragment) {
        if (fragment < 0 || fragment >= arrayFragments.length || arrayFragments[fragment] == null || arrayFragments[currentFragment] == null) return; // Bounds and null checks
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left);
        transaction.show(arrayFragments[fragment]);
        transaction.hide(arrayFragments[currentFragment]);
        transaction.commit();
        currentFragment = fragment;
    }

    public void backFragment(int fragment) {
         if (fragment < 0 || fragment >= arrayFragments.length || arrayFragments[fragment] == null || arrayFragments[currentFragment] == null) return; // Bounds and null checks
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right);
        transaction.show(arrayFragments[fragment]);
        transaction.hide(arrayFragments[currentFragment]);
        transaction.commit();
        currentFragment = fragment;
    }

    private void setSelect(int select) {
        ListSongsFragment.isShowing = false;
        AlbumFragment.isShowing = false;
        GlobalValue.currentMenu = select;
        switch (select) {
            case MOST_FAVOURITE:
                lblTopChart.setBackgroundResource(R.drawable.bg_item_menu_select);
                lblAllSongs.setBackgroundColor(Color.TRANSPARENT);
                lblAlbum.setBackgroundColor(Color.TRANSPARENT);
                lblCategoryMusic.setBackgroundColor(Color.TRANSPARENT);
                lblPlaylist.setBackgroundColor(Color.TRANSPARENT);
                lblSearch.setBackgroundColor(Color.TRANSPARENT);
                lblRadio.setBackgroundColor(Color.TRANSPARENT);
                lblMultipleYourReward.setBackgroundColor(Color.TRANSPARENT);
                lblAbout.setBackgroundColor(Color.TRANSPARENT);
                lblExitApp.setBackgroundColor(Color.TRANSPARENT);
                llMyDownLoad.setBackgroundColor(Color.TRANSPARENT);
                showFragment(LIST_SONG_FRAGMENT);
                break;

            case LASTEST:
                lblTopChart.setBackgroundColor(Color.TRANSPARENT);
                lblAllSongs.setBackgroundResource(R.drawable.bg_item_menu_select);
                lblAlbum.setBackgroundColor(Color.TRANSPARENT);
                lblCategoryMusic.setBackgroundColor(Color.TRANSPARENT);
                lblPlaylist.setBackgroundColor(Color.TRANSPARENT);
                lblSearch.setBackgroundColor(Color.TRANSPARENT);
                lblRadio.setBackgroundColor(Color.TRANSPARENT);
                lblMultipleYourReward.setBackgroundColor(Color.TRANSPARENT);
                lblAbout.setBackgroundColor(Color.TRANSPARENT);
                lblExitApp.setBackgroundColor(Color.TRANSPARENT);
                llMyDownLoad.setBackgroundColor(Color.TRANSPARENT);
                showFragment(LIST_SONG_FRAGMENT);
                break;

            case SERIES:
                hideBannerAd();
                lblTopChart.setBackgroundColor(Color.TRANSPARENT);
                lblAllSongs.setBackgroundColor(Color.TRANSPARENT);
                lblAlbum.setBackgroundResource(R.drawable.bg_item_menu_select);
                lblCategoryMusic.setBackgroundColor(Color.TRANSPARENT);
                lblPlaylist.setBackgroundColor(Color.TRANSPARENT);
                lblSearch.setBackgroundColor(Color.TRANSPARENT);
                lblRadio.setBackgroundColor(Color.TRANSPARENT);
                lblMultipleYourReward.setBackgroundColor(Color.TRANSPARENT);
                lblAbout.setBackgroundColor(Color.TRANSPARENT);
                lblExitApp.setBackgroundColor(Color.TRANSPARENT);
                llMyDownLoad.setBackgroundColor(Color.TRANSPARENT);
                showFragment(ALBUM_FRAGMENT);
                break;

            case CATEGORY_MUSIC:
                hideBannerAd();
                lblTopChart.setBackgroundColor(Color.TRANSPARENT);
                lblAllSongs.setBackgroundColor(Color.TRANSPARENT);
                lblAlbum.setBackgroundColor(Color.TRANSPARENT);
                lblCategoryMusic.setBackgroundResource(R.drawable.bg_item_menu_select);
                lblPlaylist.setBackgroundColor(Color.TRANSPARENT);
                lblSearch.setBackgroundColor(Color.TRANSPARENT);
                lblRadio.setBackgroundColor(Color.TRANSPARENT);
                lblMultipleYourReward.setBackgroundColor(Color.TRANSPARENT);
                lblAbout.setBackgroundColor(Color.TRANSPARENT);
                lblExitApp.setBackgroundColor(Color.TRANSPARENT);
                llMyDownLoad.setBackgroundColor(Color.TRANSPARENT);
                showFragment(CATEGORY_MUSIC_FRAGMENT);
                break;

            case PLAYLIST:
                lblTopChart.setBackgroundColor(Color.TRANSPARENT);
                lblAllSongs.setBackgroundColor(Color.TRANSPARENT);
                lblAlbum.setBackgroundColor(Color.TRANSPARENT);
                lblCategoryMusic.setBackgroundColor(Color.TRANSPARENT);
                lblPlaylist.setBackgroundResource(R.drawable.bg_item_menu_select);
                lblSearch.setBackgroundColor(Color.TRANSPARENT);
                lblRadio.setBackgroundColor(Color.TRANSPARENT);
                lblMultipleYourReward.setBackgroundColor(Color.TRANSPARENT);
                lblAbout.setBackgroundColor(Color.TRANSPARENT);
                llMyDownLoad.setBackgroundColor(Color.TRANSPARENT);
                lblExitApp.setBackgroundColor(Color.TRANSPARENT);
                showFragment(PLAYLIST_FRAGMENT);
                break;

            case SEARCH:
                lblTopChart.setBackgroundColor(Color.TRANSPARENT);
                lblAllSongs.setBackgroundColor(Color.TRANSPARENT);
                lblAlbum.setBackgroundColor(Color.TRANSPARENT);
                lblCategoryMusic.setBackgroundColor(Color.TRANSPARENT);
                lblPlaylist.setBackgroundColor(Color.TRANSPARENT);
                lblSearch.setBackgroundResource(R.drawable.bg_item_menu_select);
                lblRadio.setBackgroundColor(Color.TRANSPARENT);
                lblMultipleYourReward.setBackgroundColor(Color.TRANSPARENT);
                lblAbout.setBackgroundColor(Color.TRANSPARENT);
                lblExitApp.setBackgroundColor(Color.TRANSPARENT);
                llMyDownLoad.setBackgroundColor(Color.TRANSPARENT);
                if (arrayFragments != null && SEARCH_FRAGMENT < arrayFragments.length && arrayFragments[SEARCH_FRAGMENT] instanceof SearchFragment) { // Check before cast
                     ((SearchFragment) arrayFragments[SEARCH_FRAGMENT]).keyword = "";
                }
                showFragment(SEARCH_FRAGMENT);
                break;

            case RADIO:
                hideBannerAd();
                lblTopChart.setBackgroundColor(Color.TRANSPARENT);
                lblAllSongs.setBackgroundColor(Color.TRANSPARENT);
                lblAlbum.setBackgroundColor(Color.TRANSPARENT);
                lblCategoryMusic.setBackgroundColor(Color.TRANSPARENT);
                lblPlaylist.setBackgroundColor(Color.TRANSPARENT);
                lblSearch.setBackgroundColor(Color.TRANSPARENT);
                lblRadio.setBackgroundResource(R.drawable.bg_item_menu_select);
                lblMultipleYourReward.setBackgroundColor(Color.TRANSPARENT);
                lblAbout.setBackgroundColor(Color.TRANSPARENT);
                lblExitApp.setBackgroundColor(Color.TRANSPARENT);
                llMyDownLoad.setBackgroundColor(Color.TRANSPARENT);
                showFragment(RADIO_FRAGMENT);
                break;

            case ABOUT:
                hideBannerAd();
                lblTopChart.setBackgroundColor(Color.TRANSPARENT);
                lblAllSongs.setBackgroundColor(Color.TRANSPARENT);
                lblAlbum.setBackgroundColor(Color.TRANSPARENT);
                lblCategoryMusic.setBackgroundColor(Color.TRANSPARENT);
                lblPlaylist.setBackgroundColor(Color.TRANSPARENT);
                lblSearch.setBackgroundColor(Color.TRANSPARENT);
                lblRadio.setBackgroundColor(Color.TRANSPARENT);
                lblMultipleYourReward.setBackgroundColor(Color.TRANSPARENT);
                lblAbout.setBackgroundResource(R.drawable.bg_item_menu_select);
                lblExitApp.setBackgroundColor(Color.TRANSPARENT);
                llMyDownLoad.setBackgroundColor(Color.TRANSPARENT);
                showFragment(ABOUT_FRAGMENT);
                break;

            case MULTIPLE_YOUR_REWARD:
                hideBannerAd();
                lblTopChart.setBackgroundColor(Color.TRANSPARENT);
                lblAllSongs.setBackgroundColor(Color.TRANSPARENT);
                lblAlbum.setBackgroundColor(Color.TRANSPARENT);
                lblCategoryMusic.setBackgroundColor(Color.TRANSPARENT);
                lblPlaylist.setBackgroundColor(Color.TRANSPARENT);
                lblSearch.setBackgroundColor(Color.TRANSPARENT);
                lblRadio.setBackgroundColor(Color.TRANSPARENT);
                lblMultipleYourReward.setBackgroundResource(R.drawable.bg_item_menu_select);
                lblAbout.setBackgroundColor(Color.TRANSPARENT);
                llMyDownLoad.setBackgroundColor(Color.TRANSPARENT);
                lblExitApp.setBackgroundColor(Color.TRANSPARENT);
                showFragment(MULTIPLE_YOUR_WARD_FRAGMENT);
                break;

            case MY_DOWNLOAD:
                hideBannerAd();
                lblTopChart.setBackgroundColor(Color.TRANSPARENT);
                lblAllSongs.setBackgroundColor(Color.TRANSPARENT);
                lblAlbum.setBackgroundColor(Color.TRANSPARENT);
                lblCategoryMusic.setBackgroundColor(Color.TRANSPARENT);
                lblPlaylist.setBackgroundColor(Color.TRANSPARENT);
                lblSearch.setBackgroundColor(Color.TRANSPARENT);
                lblRadio.setBackgroundColor(Color.TRANSPARENT);
                lblMultipleYourReward.setBackgroundColor(Color.TRANSPARENT);
                lblAbout.setBackgroundColor(Color.TRANSPARENT);
                lblExitApp.setBackgroundColor(Color.TRANSPARENT);
                llMyDownLoad.setBackgroundResource(R.drawable.bg_item_menu_select);
                showFragment(MY_DOWNLOAD_FRAGMENT);
                break;

            case EXIT_APP:
                return;
        }
        if (menu != null) menu.showContent(); // Added null check
    }

    // This method is now primarily updated by the playStateReceiver via updateFooterPlayState
    // It can be kept for initial setup or direct calls if state is guaranteed to be fresh.
    public void setButtonPlay() {
        if (mService != null) { // Null check for mService
            updateFooterPlayState(!mService.isPause());
        }
    }

    public void cancelNotification() {
        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nMgr != null) {
            nMgr.cancel(NOTIFICATION_ID);
            nMgr.cancelAll();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == null) return; // Null check for the clicked view
        int id = v.getId();
        if (id == R.id.btnPreviousFooter) {
            onClickPreviousFooter();
        } else if (id == R.id.btnPlayFooter) {
            onClickPlayFooter();
        } else if (id == R.id.btnNextFooter) {
            onClickNextFooter();
        } else if (id == R.id.layoutPlayerFooterMain) {
            onClickPlayerFooter();
        } else if (id == R.id.lblTopChart) {
            isPlaylist = false;
            onClickTopChart();
        } else if (id == R.id.lblAllSong) {
            isPlaylist = false;
            onClickAllSongs();
        } else if (id == R.id.lblNominations) {
            isPlaylist = false;
            onClickAlbum();
        } else if (id == R.id.lblCategoryMusic) {
            isPlaylist = false;
            onClickCategoryMusic();
        } else if (id == R.id.lblPlaylist) {
            isPlaylist = true;
            onClickPlaylist();
        } else if (id == R.id.lblSearch) {
            isPlaylist = false;
            onClickSearch();
        } else if (id == R.id.lblRadio) {
            isPlaylist = false;
            onClickRadio();
        } else if (id == R.id.lblMultipleYourReward) {
            isPlaylist = false;
            onClickGoodApp();
        } else if (id == R.id.lblMyDownLoad) {
            isPlaylist = false;
            onClickMyDownload();
        } else if (id == R.id.lblAbout) {
            isPlaylist = false;
            onClickAbout();
        } else if (id == R.id.lblExitApp) {
            isPlaylist = false;
            onClickExitApp();
        } else if (id == R.id.lblClear) {
            isPlaylist = false;
            onClickClearCache();
        }
    }

    private void onClickClearCache() {
        File fileOrDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                + getString(R.string.app_name));
        if (fileOrDirectory.exists()) {
            String deleteCmd = "rm -r " + fileOrDirectory.toString();
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
            } catch (IOException ignored) { }
        }
    }

    private void onClickPreviousFooter() {
        if (mService != null) mService.backSongByOnClick(); // Null check
    }

    private void onClickPlayFooter() {
        if (mService != null) mService.playOrPauseMusic(); // Null check. UI update will be handled by playStateReceiver.
        // setButtonPlay(); // REMOVED: Let playStateReceiver handle UI update
    }

    private void onClickNextFooter() {
        if (mService != null) mService.nextSongByOnClick(); // Null check
    }

    private void onClickPlayerFooter() {
        toMusicPlayer = currentFragment;
        isTapOnFooter = true;
        Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
        startActivityForResult(intent, PlayerActivity.REQUEST_CODE);
    }

    private void onClickTopChart() { setSelect(MOST_FAVOURITE); }
    private void onClickAllSongs() { setSelect(LASTEST); }
    private void onClickAlbum() { setSelect(SERIES); }
    private void onClickCategoryMusic() { setSelect(CATEGORY_MUSIC); }
    private void onClickPlaylist() { setSelect(PLAYLIST); }
    private void onClickSearch() { setSelect(SEARCH); }
    private void onClickRadio() { setSelect(RADIO); }
    private void onClickGoodApp() { setSelect(MULTIPLE_YOUR_REWARD); }
    private void onClickMyDownload() { setSelect(MY_DOWNLOAD); }
    private void onClickAbout() { setSelect(ABOUT); }

    private void onClickExitApp() { showQuitDialog(); }

    @Override
    public void onBackPressed() {
        if (menu != null && menu.isMenuShowing()) { // Null check
            menu.showContent();
        } else {
            switch (currentFragment) {
                case CATEGORY_MUSIC_FRAGMENT:
                    quitApp();
                    break;
                case LIST_SONG_FRAGMENT:
                    if (GlobalValue.currentMenu == CATEGORY_MUSIC) {
                        backFragment(CATEGORY_MUSIC_FRAGMENT);
                    } else if (GlobalValue.currentMenu == PLAYLIST) {
                        backFragment(PLAYLIST_FRAGMENT);
                    } else if (GlobalValue.currentMenu == SERIES) {
                        backFragment(ALBUM_FRAGMENT);
                    } else {
                        quitApp();
                    }
                    break;
                default:
                    quitApp();
                    break;
            }
        }
    }

    private void quitApp() {
        showQuitDialog();
    }

    private void showQuitDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.msgQuitApp)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                        if (intentService != null) stopService(intentService); // Null check
                        cancelNotification();
                        finish();
                        System.exit(0);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }
}
