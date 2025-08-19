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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

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

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.nurulquran.audio.BuildConfig;
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

/**
 * Created by phamtuan on 03/05/2016.
 */
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
                setButtonPlay();
                mService.setListener(new PlayerListener() {
                    @Override
                    public void onSeekChanged(int maxProgress, String lengthTime,
                                              String currentTime, int progress) {
                        PlayerActivity.this.seekChanged(maxProgress, lengthTime, currentTime,
                                progress);
                    }

                    @Override
                    public void onChangeSong(int indexSong) {

                        PlayerActivity.this.changeSong(indexSong);
                    }

                    @Override
                    public void OnMusicPrepared() {

                    }
                });
                GlobalValue.currentMusicService = mService;

                if (playmp3)
                    playMusic();
                setHeaderTitle(GlobalValue.getCurrentSong().getName());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
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
        playSong();
        setHeaderTitle(GlobalValue.getCurrentSong().getName());
    }

    private void hidemp3view() {
        playerThumbFragment.hidemp3view();
        relativecontroller.setVisibility(View.GONE);
        linearseekbar.setVisibility(View.GONE);
    }

    private void showmp3view() {
        playerThumbFragment.showmp3view();
        relativecontroller.setVisibility(View.VISIBLE);
        linearseekbar.setVisibility(View.VISIBLE);
    }

    private void playvideo(Song currentSong) {
        playerThumbFragment.hidemp3view();
        playerThumbFragment.playvideo(currentSong);
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();
//        try {
//            unbindService(mConnection);
//        } catch (Exception e) {
//            e.printStackTrace();
//            cancelNotification();
//        }
    }

    @Override
    protected void onDestroy() {
        try {
            playerThumbFragment.releasePlayer();
            mService.stopSelf();
            unbindService(mConnection);

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
            if (playmp3 && intentService != null && mConnection != null)
                bindService(intentService, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void playSong() {
        Song currentSong = GlobalValue.getCurrentSong();
        if (checkmp3(currentSong.getUrl())) {
            initconnection();
            playmp3 = true;
            initService();
            showmp3view();
        } else {
            hidemp3view();
            playmp3 = false;
            playvideo(currentSong);
            viewPager.setCurrentItem(THUMB_PLAYING);
        }
        if (MainActivity.isPlaylist) {

            if (checkUrl(currentSong.getUrl())) {
                if (checkmp3(currentSong.getUrl())) {
                    File file = new File(rootFolder, currentSong.getName() + ".mp3");
                    if (file.exists()) {
                        //do nothing
                    } else {
                        downloadSong(currentSong, true);
                    }
                } else {
                    File file = new File(rootFolder, currentSong.getName() + ".mp4");
                    if (file.exists()) {
                        //do nothing
                    } else {
                        downloadSong(currentSong, false);
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), currentSong.getName() + " " + getString(R.string.downloaded), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);


    }

    private void initService() {
        intentService = new Intent(this, MusicService.class);
        startService(intentService);
        bindService(intentService, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void playMusic() {
        if (true) {

            switch (MainActivity.toMusicPlayer) {
                case MainActivity.FROM_LIST_SONG:
                case MainActivity.FROM_SEARCH:
                    if (MainActivity.isTapOnFooter == false)// re-set song only when not from footer
                    {
                        setCurrentSong(GlobalValue.currentSongPlay);
                    }
                    playerListPlayingFragment.refreshListPlaying();
                    playerThumbFragment.refreshData();
                    setSelectTab(THUMB_PLAYING);
                    viewPager.setCurrentItem(THUMB_PLAYING);
                    break;

                case MainActivity.FROM_NOTICATION:
                    try {
                        playerListPlayingFragment.refreshListPlaying();
                        playerThumbFragment.refreshData();
                        setSelectTab(THUMB_PLAYING);
                        viewPager.setCurrentItem(THUMB_PLAYING);
                        setCurrentSong(GlobalValue.currentSongPlay);
                    } catch (Exception e) {
                        cancelNotification();
                    }
                    break;

                case MainActivity.FROM_OTHER:
                    playerListPlayingFragment.refreshListPlaying();
                    playerThumbFragment.refreshData();
                    setSelectTab(THUMB_PLAYING);
                    viewPager.setCurrentItem(THUMB_PLAYING);
                    break;
                default:
                    playerListPlayingFragment.refreshListPlaying();
                    playerThumbFragment.refreshData();
                    setSelectTab(THUMB_PLAYING);
                    viewPager.setCurrentItem(THUMB_PLAYING);
                    break;
            }
        }
    }

    public void cancelNotification() {
        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(NOTIFICATION_ID);
    }

    private void initUI() {
        btnAction = findViewById(R.id.btnRightButton);
        relativecontroller = findViewById(R.id.relative_controller);
        linearseekbar = findViewById(R.id.linear_seekbar);
        btnAction.setBackgroundResource(R.drawable.ic_action);
        btnAction.setVisibility(View.VISIBLE);
        lblTopHeader = (TextView) findViewById(R.id.lblHeader);
        lblTopHeader.setSelected(true);
        btnBackward = (ImageView) findViewById(R.id.btnBackward);
        btnPlay = (ImageView) findViewById(R.id.btnPlayMusic);
        btnForward = (ImageView) findViewById(R.id.btnForward);
        btnShuffle = (ImageView) findViewById(R.id.btnShuffle);
        btnRepeat = (ImageView) findViewById(R.id.btnRepeat);
        seekBarLength = (SeekBar) findViewById(R.id.seekBarLength);
        seekBarLength.setMax(100);
        lblTimeCurrent = (TextView) findViewById(R.id.lblTimeCurrent);
        lblTimeLength = (TextView) findViewById(R.id.lblTimeLength);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewIndicatorList = findViewById(R.id.viewIndicatorList);
        viewIndicatorThumb = findViewById(R.id.viewIndicatorThumb);
        mIVBanner = (NetworkImageView) findViewById(R.id.ivBanner);
        imageLoader = ControllerRequest.getInstance().getImageLoader();
        layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        loadImageBanner();
    }

    private void initControl() {
        setButtonBack();
        // make root folder
        rootFolder = Environment.getExternalStorageDirectory() + "/"
                + getString(R.string.app_name) + "/";
        File folder = new File(rootFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // make folder ringtone
        ringtoneFolder = rootFolder + "ringtone/";
        File ringtonefolder = new File(ringtoneFolder);
        if (!ringtonefolder.exists()) {
            ringtonefolder.mkdirs();
        }

        alarmFolder = rootFolder + "alarm/";
        File alarmfolder = new File(alarmFolder);
        if (!alarmfolder.exists()) {
            alarmfolder.mkdirs();
        }

        notifyFolder = rootFolder + "notify/";
        File notifyfolder = new File(notifyFolder);
        if (!notifyfolder.exists()) {
            notifyfolder.mkdirs();
        }

        btnAction.setOnClickListener(this);
        btnShuffle.setOnClickListener(this);
        btnBackward.setOnClickListener(this);
        btnPlay.setOnClickListener(this);
        btnForward.setOnClickListener(this);
        btnRepeat.setOnClickListener(this);
        mIVBanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoPageBanner();
            }
        });
        seekBarLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mService.seekTo(seekBar.getProgress());

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
            }
        });

        playerListPlayingFragment = new PlayerListPlayingFragment();
        playerThumbFragment = new PlayerThumbFragment();
        playerThumbFragment.setListener(new PlayerListener() {
            @Override
            public void onSeekChanged(int maxProgress, String lengthTime, String currentTime, int progress) {

            }

            @Override
            public void onChangeSong(int indexSong) {
                nextsong(indexSong);
            }

            @Override
            public void OnMusicPrepared() {

            }
        });
        viewPager.setAdapter(new MyFragmentPagerAdapter(getSupportFragmentManager()));
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setSelectTab(position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
    }

    private void nextsong(int newPosition) {
//        int newPosition;
//        if (isShuffle) {
//            newPosition = new Random().nextInt(listSongs.size());
//        } else {
//            if (isRepeat)
//                newPosition = GlobalValue.currentSongPlay;
//            else
        if (GlobalValue.currentSongPlay < GlobalValue.listSongPlay.size() - 1) {
            newPosition = GlobalValue.currentSongPlay + 1;
        } else {
            newPosition = 0;

        }

        changeSong(newPosition);
//        }
//        }
    }

    /**
     * load image for banner with period 30s will change image different
     */
    private void loadImageBanner() {
        ModelManager.loadBanner(getApplicationContext(), new ModelManagerListener() {
            @Override
            public void onError(VolleyError error) {

            }

            @Override
            public void onSuccess(String json) {
                CommonParser.parseBanner(json);

                Collections.shuffle(GlobalValue.mListBanner);
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (pos == GlobalValue.mListBanner.size()) {
                                    pos = 0;
                                }
                                if (pos < GlobalValue.mListBanner.size()) {
//                                    mIVBanner.setImageUrl(GlobalValue.mListBanner.get(pos).getImage(), imageLoader);
                                    pos++;

                                }

                            }
                        });

                    }
                }, 0, PERIOD_BANNER);

            }
        });

    }

    /****
     * go to page banner
     */
    private void gotoPageBanner() {
        if ((pos - 1) >= 0) {
            Intent intent = new Intent(ACTION_VIEW, Uri.parse(GlobalValue.mListBanner.get((pos - 1)).getUrl()));
            startActivity(intent);
        }


    }

    private void setSelectTab(int tab) {
        if (tab == LIST_PLAYING) {
            viewIndicatorList.setBackgroundResource(R.color.black_4);
            viewIndicatorThumb
                    .setBackgroundResource(R.color.color_selector);
        } else {
            viewIndicatorList.setBackgroundResource(R.color.color_selector);
            viewIndicatorThumb.setBackgroundResource(R.color.black_4);
        }
    }

    private void setCurrentSong(int position) {
        playerListPlayingFragment.refreshListPlaying();
        playerThumbFragment.refreshData();
        if (checkmp3(GlobalValue.listSongPlay.get(position).getUrl())) {
            if (mService != null)
                mService.startMusic(position);
            else {
                initService();
                playmp3 = true;
            }
        } else {
            if (mConnection != null)
                unbindService(mConnection);
            playmp3 = false;
            playerThumbFragment.playvideo(GlobalValue.listSongPlay.get(position));
        }
    }

    public void seekChanged(int maxprogress, String lengthTime,
                            String currentTime, int progress) {
        lblTimeLength.setText(lengthTime);
        lblTimeCurrent.setText(currentTime);
        seekBarLength.setMax(maxprogress);
        seekBarLength.setProgress(progress);
    }

    public void changeSong(int indexSong) {
        try {
//            lblTimeCurrent.setText(getString(R.string.timeStart));
//            lblTimeLength.setText(mService.getLengSong());
//            playerListPlayingFragment.refreshListPlaying();
//            playerThumbFragment.refreshData();
            startMusic(indexSong);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setButtonPlay() {
        if (mService.isPause()) {
            btnPlay.setBackgroundResource(R.drawable.ic_play);
        } else {
            btnPlay.setBackgroundResource(R.drawable.ic_pause);
        }
    }

    private void showMenuAction(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getTitle().equals(
                        getString(R.string.download))) {
                    onClickDownload();
                } else if (item.getTitle().equals(
                        getString(R.string.Share))) {
                    onClickShare();
//                } else if (item.getTitle().equals(
//                        getString(R.string.set_as_ringtone))) {
////                    Toast.makeText(getApplicationContext(),
////                            "Set as ringtone is processing ... !", 3000).show();
//                    ShareUtility.setRingtone(PlayerActivity.this, GlobalValue
//                            .getCurrentSong().getUrl(), ringtoneFolder);
//                } else if (item.getTitle().equals(
//                        getString(R.string.set_as_alarm))) {
////                    Toast.makeText(getApplicationContext(),
////                            "Set as Alarm is processing ... !", 3000).show();
//                    ShareUtility.setAlarm(PlayerActivity.this, GlobalValue
//                            .getCurrentSong().getUrl(), alarmFolder);
//                } else if (item.getTitle().equals(
//                        getString(
//                                R.string.set_as_notification))) {
////                    Toast.makeText(getApplicationContext(),
////                            "Set as Notification is processing ... !", 3000)
////                            .show();
//                    ShareUtility.setNotification(PlayerActivity.this, GlobalValue
//                            .getCurrentSong().getUrl(), notifyFolder);
//                } else if (item.getTitle().equals(
//                        getString(
//                                R.string.set_as_contact_ringtone))) {
//                    chooseContacts();
                } else if (item.getTitle().equals(
                        getString(R.string.add_to_playlist))) {
                    showPlayList();
                }
                return false;
            }
        });
        popupMenu.inflate(R.menu.popup_audio_action);
        popupMenu.show();
    }

    private void onClickShuffle() {
        isCheckShuffle = !isCheckShuffle;
        mService.setShuffle(isCheckShuffle);
        MySharePreferences.getInstance().saveValueShuffle(getApplicationContext(), isCheckShuffle);
        if (isCheckShuffle) {
            showToast(R.string.enable_shuffle);
            btnShuffle.setImageResource(R.drawable.ic_shuffle_white);
        } else {
            showToast(R.string.off_shuffle);
            btnShuffle.setImageResource(R.drawable.ic_shuffle);
        }
    }

    private void setUpFunctionShuffle() {
        isCheckShuffle = MySharePreferences.getInstance().getValueShuffle(getApplicationContext());
        mService.setShuffle(isCheckShuffle);
        if (mService.isShuffle()) {
            btnShuffle.setImageResource(R.drawable.ic_shuffle_white);
        } else {
            btnShuffle.setImageResource(R.drawable.ic_shuffle);
        }
    }

    private void onClickBackward() {
        mService.backSongByOnClick();
    }

    private void onClickPlay() {
        mService.playOrPauseMusic();
        setButtonPlay();
    }

    private void onClickForward() {
        mService.nextSongByOnClick();
    }

    private void setUpFunctionRepeat() {
        isCheckRepeat = MySharePreferences.getInstance().getValueRepeat(getApplicationContext());
        mService.setRepeat(isCheckRepeat);
        if (mService.isRepeat()) {
            btnRepeat.setImageResource(R.drawable.ic_repeat_white);
        } else {
            btnRepeat.setImageResource(R.drawable.ic_repeat);
        }
    }

    private void onClickRepeat() {
        isCheckRepeat = !isCheckRepeat;
        mService.setRepeat(isCheckRepeat);
        MySharePreferences.getInstance().saveValueRepeat(getApplicationContext(), isCheckRepeat);
        if (mService.isRepeat()) {
            showToast(R.string.enableRepeat);
        } else {
            showToast(R.string.offRepeat);
        }
        if (isCheckRepeat) {
            btnRepeat.setImageResource(R.drawable.ic_repeat_white);
        } else {
            btnRepeat.setImageResource(R.drawable.ic_repeat);
        }
    }

    private void onClickDownload() {
        Song currentSong = GlobalValue.getCurrentSong();
        if (checkUrl(currentSong.getUrl())) {
            if (checkmp3(currentSong.getUrl())) {
                File file = new File(rootFolder, currentSong.getName() + ".mp3");
                if (file.exists()) {
                    confirmdownload(currentSong, true);
                } else {
                    downloadSong(currentSong, true);
                }
            } else {
                File file = new File(rootFolder, currentSong.getName() + ".mp4");
                if (file.exists()) {
                    confirmdownload(currentSong, false);
                } else {
                    downloadSong(currentSong, false);
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), currentSong.getName() + " " + getString(R.string.downloaded), Toast.LENGTH_SHORT).show();
        }

    }

    private void confirmdownload(final Song currentSong, boolean mp3) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(currentSong.getName());
//        builder.setPositiveButton(R.string.degree, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                downloadSong(currentSong, mp3);
//                dialog.dismiss();
//            }
//        });
        builder.setNegativeButton(R.string.undegree, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        View view = layoutInflater.inflate(R.layout.confirm_download_dialog, null);
        builder.setView(view);
        TextView tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvTitle.setText(currentSong.getName() + " " + getString(R.string.exist_download));
        tvTitle.setSelected(true);
        builder.create().show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_STORAGE_Player: {
                if (grantResults.length > 0) {
                    playerThumbFragment.playvideo();
                }
                break;
            }
            case PERMISSION_WRITE_STORAGE_Download: {
                if (grantResults.length > 0) {

                }
                break;
            }
        }
    }


    private void downloadSong(Song currentSong, boolean mp3) {
        if (!checkPermission()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                download(currentSong, mp3);
//                try {
//                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//                    intent.addCategory("android.intent.category.DEFAULT");
//                    intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
//                    startActivityForResult(intent, 2296);
//                } catch (Exception e) {
//                    Intent intent = new Intent();
//                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                    startActivityForResult(intent, 2296);
//                }
            }else {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        }, PERMISSION_WRITE_STORAGE_Download);
            }
        } else {
            download(currentSong, mp3);
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return true;
        } else {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void download(Song currentSong, boolean mp3) {
        if (NetworkUtil.checkNetworkAvailable(PlayerActivity.this)) {
            String rootFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                    + getString(R.string.app_name) + "/";
            String file;
            if (mp3)
                file = currentSong.getName() + ".mp3";
            else
                file = currentSong.getName() + ".mp4";

            File folder = new File(rootFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String localLink = rootFolder + file;
//                        Intent intent = new Intent(getApplicationContext(),
//                    DownloadUpdateActivity.class);
//            intent.putExtra("url_song", currentSong.getUrl());
//            if (mp3)
//                intent.putExtra("file_name", currentSong.getName() + ".mp3");
//            else
//                intent.putExtra("file_name", currentSong.getName() + ".mp4");
//            intent.putExtra("show_progress", MainActivity.isPlaylist);
//            startActivity(intent);
            Toast.makeText(getBaseContext(), R.string.downloadStarted,
                    Toast.LENGTH_SHORT).show();
//                    addNewDownload();
            String result = localLink.toString();
            result = result.substring(result.indexOf("/storage"), result.length());
            GlobalValue.getCurrentSong().addMoreDownload();
            Song songObj = new Song();
            songObj.setId(GlobalValue.getCurrentSong().getId());
            songObj.setName(GlobalValue.getCurrentSong().getName());
            songObj.setmTypePathFile(Song.PATH_FILE_DOWNLOAD);
            songObj.setUrl(result);
            songObj.setImage(GlobalValue.getCurrentSong().getImage());
            songObj.setDescription(GlobalValue.getCurrentSong().getDescription());
            songObj.setPosition(GlobalValue.getCurrentSong().getPosition());
            songObj.setShareLink(GlobalValue.getCurrentSong().getShareLink());
            songObj.setListenCount(GlobalValue.getCurrentSong().getListenCount());
            songObj.setDownloadCount(GlobalValue.getCurrentSong().getDownloadCount());
            DatabaseUtility databaseUtility = new DatabaseUtility(getApplicationContext());
            if (databaseUtility.checkFavourite(getBaseContext(), GlobalValue.getCurrentSong().getId())) {
                Log.e("Download", "Song exsit");
            } else {
                databaseUtility.insertFavorite(songObj);
            }
            BroadcastReceiver onComplete=new BroadcastReceiver() {
                public void onReceive(Context ctxt, Intent intent) {
                    Toast.makeText(getBaseContext(), R.string.downloadComplete,
                            Toast.LENGTH_SHORT).show();
//                    finish();
                }
            };
            DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(currentSong.getUrl());
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(file);
            request.setDescription("Downloading");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, getString(R.string.app_name) +"/"+ file);
            }else{
                request.setDestinationUri(Uri.parse("file://" + localLink));
            }

            downloadmanager.enqueue(request);
            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));


        }



    }

    private void onClickShare() {
        Song currentSong = GlobalValue.getCurrentSong();

        String shareBody = currentSong.getUrl();
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
//        File fileWithinMyDir = new File(shareBody);
//        if(checkUrl(shareBody))
//        {
        if (checkmp3(currentSong.getUrl())) {
            File file = new File(rootFolder, currentSong.getName() + ".mp3");
            if (file.exists()) {
//                confirmdownload(currentSong, true);
                intentShareFile.setType("application/pdf");
                Uri imageUri = FileProvider.getUriForFile(
                        PlayerActivity.this,
                        "com.nurulquran.audio.provider", //(use your app signature + ".provider" )
                        file);

                intentShareFile.putExtra(Intent.EXTRA_STREAM, imageUri);

                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                        currentSong.getName());
                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Get.NurulQuran.com");

                startActivity(Intent.createChooser(intentShareFile, "Share File"));
            } else {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        currentSong.getName() + " - " + currentSong.getArtist());
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, getResources()
                        .getString(R.string.share)));
            }
        } else {
            File file = new File(rootFolder, currentSong.getName() + ".mp4");
            if (file.exists()) {
                intentShareFile.setType("application/pdf");
                Uri imageUri = FileProvider.getUriForFile(
                        PlayerActivity.this,
                        "com.nurulquran.audio.provider", //(use your app signature + ".provider" )
                        file);

                intentShareFile.putExtra(Intent.EXTRA_STREAM, imageUri);

                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                        currentSong.getName());
                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Get.NurulQuran.com");

                startActivity(Intent.createChooser(intentShareFile, "Share File"));

            } else {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        currentSong.getName() + " - " + currentSong.getArtist());
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, getResources()
                        .getString(R.string.share)));
            }

        }

//        }

    }

    private void initPlayList() {
        listPlaylists = databaseUtility.getAllPlaylist();

    }

    private void showPlayList() {

        if (listPlaylists.size() > 0) {
            arrayPlaylistName = new String[listPlaylists.size()];
            for (int i = 0; i < arrayPlaylistName.length; i++) {
                arrayPlaylistName[i] = listPlaylists.get(i).getName();
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
            builder.setTitle(R.string.choosePlaylist).setSingleChoiceItems(
                    arrayPlaylistName, 0,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            int selectedPosition = ((AlertDialog) dialog)
                                    .getListView().getCheckedItemPosition();
                            // Do something useful withe the position of the

                            Playlist playlist = listPlaylists
                                    .get(selectedPosition);

                            // check existed file music on this play list
                            boolean isExisited = false;
                            for (Song song : playlist.getListSongs()) {
                                if (song.getId().equals(
                                        GlobalValue.getCurrentSong().getId())) {
                                    isExisited = true;
                                    break;
                                }
                            }
                            if (isExisited) {
                                Toast.makeText(
                                        getApplicationContext(),
                                        "This song is existed on "
                                                + playlist.getName(),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                playlist.addSong(GlobalValue.getCurrentSong());
                                // update play list
                                if (databaseUtility
                                        .updatePlaylist(playlist)) {
                                    Toast.makeText(
                                            getApplicationContext(),
                                            "Add to " + playlist.getName()
                                                    + " successfully!",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

            builder.create().show();
        } else {
            Toast.makeText(getApplicationContext(), "Please add a new playlist!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnShuffle:
                onClickShuffle();
                break;

            case R.id.btnBackward:
                onClickBackward();
                break;

            case R.id.btnPlayMusic:
                onClickPlay();
                break;

            case R.id.btnForward:
                onClickForward();
                break;

            case R.id.btnRepeat:
                onClickRepeat();
                break;
            case R.id.btnRightButton:
                showMenuAction(btnAction);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (PICK_CONTACT_REQUEST):
                if (resultCode == Activity.RESULT_OK) {

                    try {
                        Uri contactData = data.getData();
                        // String contactId = contactData.getLastPathSegment();
                        String[] PROJECTION = new String[]{
                                ContactsContract.Data.CONTACT_ID,
                                ContactsContract.Contacts.DISPLAY_NAME,
                                ContactsContract.Contacts.HAS_PHONE_NUMBER,};
                        Cursor localCursor = getContentResolver().query(
                                contactData, null, null, null, null);
                        localCursor.moveToFirst();

                        for (int i = 0; i < localCursor.getColumnCount(); i++) {
                            String value = localCursor.getString(i);
                            Log.e("ShareUtility",
                                    "value " + localCursor.getColumnName(i) + ": "
                                            + localCursor.getString(i));
                        }

                        String contactID = localCursor
                                .getString(localCursor
                                        .getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID));
                        String contactDisplayName = localCursor
                                .getString(localCursor
                                        .getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));

                        String localFolder = Environment
                                .getExternalStorageDirectory()
                                + "/"
                                + getString(R.string.app_name) + "/ringtone/";

                        // String localFolder = Environment
                        // .getExternalStorageDirectory()
                        // + "/Ringtones/";
                        Toast.makeText(this,
                                "Set as contact ringtone is processing ... !", Toast.LENGTH_LONG)
                                .show();
                        ShareUtility.setRingtoneContact(PlayerActivity.this,
                                contactID, GlobalValue.getCurrentSong().getUrl(),
                                localFolder, contactData);
                        Toast.makeText(this,
                                "Ringtone assigned to: " + contactDisplayName,
                                Toast.LENGTH_LONG).show();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Toast.makeText(
                                this,
                                "Have error when Add contact ringtone. Try again please!",
                                Toast.LENGTH_LONG).show();
                    }
                }

                break;
        }
    }

    public void startMusic(int position) {
        GlobalValue.currentSongPlay = position;
        playerThumbFragment.refreshData();
        playerListPlayingFragment.refreshListPlaying();
        setHeaderTitle(GlobalValue.getCurrentSong().getName());
        if (checkmp3(GlobalValue.listSongPlay.get(position).getUrl())) {
            playmp3 = true;
            if (mService != null)
                mService.startMusic(position);
            else {
                initconnection();
                initService();
                showmp3view();

            }

        } else {
            hidemp3view();
            playmp3 = false;
            if (mConnection != null) {
                unbindService(mConnection);
                mConnection = null;
                mService = null;
            }
            playerThumbFragment.playvideo(GlobalValue.listSongPlay.get(position));
            viewPager.setCurrentItem(THUMB_PLAYING);
        }
    }

    private class MyFragmentPagerAdapter extends FragmentPagerAdapter {
        public MyFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return playerListPlayingFragment;
            }
            return playerThumbFragment;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @SuppressLint("DefaultLocale")
    private String getTime(int millis) {
        long second = (millis / 1000) % 60;
        long minute = millis / (1000 * 60);
        return String.format("%02d:%02d", minute, second);
    }

    public void chooseContacts() {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        pickContactIntent
                .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }

    private void pauseRadio() {
        if (GlobalValue.currentMenu == MainActivity.RADIO) {
            if (mService.mediaNotification != null) {
                mService.mediaNotification.onClickMediaNotification();
            }
        }
    }

    protected boolean checkUrl(String url) {

        return url.startsWith("http://") || url.startsWith("https://");
    }

    protected boolean checkmp3(String url) {
        if (url.contains(".mp3"))
            return true;
        else
            return false;
    }
}
