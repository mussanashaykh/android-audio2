package com.nurulquran.audio.activity

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.MimeTypes.BASE_TYPE_AUDIO
import com.google.android.exoplayer2.util.Util
import com.nurulquran.audio.R
import java.io.File

private const val EXTRA_VIDEO_URL = "EXTRA_VIDEO_URL"
private const val EXTRA_PLAYBACK_POSITION_MS = "EXTRA_PLAYBACK_POSITION_MS"

private const val STATE_PLAYBACK_POSITION_MS = "STATE_PLAYBACK_POSITION_MS"
private var overlayFrameLayout: FrameLayout? = null
class FullScreenVideoActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun newIntent(packageContext: Context, videoUrl: String, playbackPositionMs: Long): Intent {
            val intent =
                    Intent(packageContext, FullScreenVideoActivity::class.java)
            intent.putExtra(EXTRA_VIDEO_URL, videoUrl)
            intent.putExtra(EXTRA_PLAYBACK_POSITION_MS, playbackPositionMs)
            return intent
        }

    }

    lateinit var  playerView: PlayerView
    var player: SimpleExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_video)

        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)

        var playbackPositionMs = intent.getLongExtra(EXTRA_PLAYBACK_POSITION_MS, 0)

        if (savedInstanceState != null) {
            // The user rotated the screen
            playbackPositionMs = savedInstanceState.getLong(STATE_PLAYBACK_POSITION_MS)
        }
// Hide the status bar.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
// Remember that you should never show the action bar if the
// status bar is hidden, so hide that too if necessary.
        actionBar?.hide()
        findViewById<View>(R.id.exo_fullscreen_button).setOnClickListener {
            finish()
        }
        overlayFrameLayout = findViewById<View>(R.id.exo_overlay) as FrameLayout
       playerView = findViewById(R.id.video_view)
        setTouchEventListner()
//            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(getContext().getApplicationContext());
            val loadControl = DefaultLoadControl()
            val trackSelector = DefaultTrackSelector(this)

            player = SimpleExoPlayer.Builder(this)
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadControl)
                    .build()

        Window_Play = window
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        InitialViewWidth = resources.displayMetrics.widthPixels
        InitialViewHeight = resources.displayMetrics.heightPixels

        val userAgent = Util.getUserAgent(this, getString(R.string.app_name))
//        val dataSourceFactory = DefaultDataSourceFactory(this, userAgent)
        val mediaSource: MediaSource? =buildMediaSource(Uri.parse(videoUrl))
//        val mediaSource: MediaSource =
//                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(videoUrl))

        mediaSource?.let { player!!.prepare(it) }
        player!!.seekTo(playbackPositionMs)
        player!!.playWhenReady = true
        playerView.player = player
    }
    var cache: Cache? = null
    fun releasePlayer() {
        if (player != null) {


            player!!.release()
            player = null
        }
    }
    private var Window_Play: Window? = null
    private var audioManager: AudioManager? = null
    private var InitialViewHeight = 0
    private var InitialViewWidth = 0
    fun setTouchEventListner()
    {

        playerView.setOnTouchListener(object : SparshListner() {
            var diffTime = -1.0f

            var finalTime = -1.0f
            var maxBrightness = 0
            var maxVolume = 0
            var startBrightness = 0
            var startVolume = 0


            override fun onAfterMove() {

            }

            override fun onBeforeMove(dir: Direction?) {
                if (dir === Direction.LEFT || dir === Direction.RIGHT) {
//                    this@FullScreenVideoActivity.progressTextView.setVisibility(View.VISIBLE)
                    return
                }
                maxBrightness = 100

                startBrightness = ((this@FullScreenVideoActivity.Window_Play!!.getAttributes().screenBrightness * 100.0f).toInt())

                maxVolume = this@FullScreenVideoActivity.audioManager?.getStreamMaxVolume(3)!!
                startVolume = this@FullScreenVideoActivity.audioManager?.getStreamVolume(3)!!
//                this@FullScreenVideoActivity.imgIndicator.setVisibility(View.VISIBLE)
//                this@FullScreenVideoActivity.progressTextView.setVisibility(View.VISIBLE)

            }

            override fun onClick() {

            }

            override fun onMove(dir: Direction?, diff: Float) {
                if (dir === Direction.LEFT || dir === Direction.RIGHT) {
                    if (this@FullScreenVideoActivity.player?.getDuration()!! <= 60) {
                        diffTime = this@FullScreenVideoActivity.player!!.getDuration() as Float * diff / this@FullScreenVideoActivity.InitialViewWidth as Float
                    } else {
                        diffTime = 60000.0f * diff / this@FullScreenVideoActivity.InitialViewWidth as Float
                    }
                    if (dir === Direction.LEFT) {
                        diffTime *= -1.0f
                    }
                    finalTime = this@FullScreenVideoActivity.player!!.getCurrentPosition() as Float + diffTime
                    if (finalTime < 0.0f) {
                        finalTime = 0.0f
                    } else if (finalTime > this@FullScreenVideoActivity.player!!.getDuration() as Float) {
                        finalTime = this@FullScreenVideoActivity.player!!.getDuration().toFloat()
                    }
                    diffTime = finalTime - this@FullScreenVideoActivity.player!!.getCurrentPosition() as Float
//                    this@FullScreenVideoActivity.progressTextView.setText(ApplicationUtilitys.getDurationString(finalTime.toLong(), false).toString() + " [" + (if (dir === Direction.LEFT) "-" else "+") + ApplicationUtilitys.getDurationString(Math.abs(diffTime).toLong(), false) + "]")
                    return
                }
                finalTime = -1.0f
                val progressText: String
//                if (initialX >= (this@FullScreenVideoActivity.InitialViewWidth / 2) as Float || this@FullScreenVideoActivity.Window_Play == null) {
                    var diffVolume: Float = maxVolume.toFloat() * diff / (this@FullScreenVideoActivity.InitialViewHeight as Float / 2.0f)
                    if (dir === Direction.DOWN) {
                        diffVolume = -diffVolume
                    }
                    var finalVolume = startVolume + diffVolume.toInt()
                    if (finalVolume < 0) {
                        finalVolume = 0
                    } else if (finalVolume > maxVolume) {
                        finalVolume = maxVolume
                    }
                    progressText = "\t" + finalVolume
//                    if (this@FullScreenVideoActivity.imgIndicator.getDrawable() == null) {
//                        this@FullScreenVideoActivity.imgIndicator.setImageDrawable(this@Simaple_Video_Activity.indVolumeDrawable)
//                    }
//                    this@FullScreenVideoActivity.progressTextView.setText(progressText)
                    this@FullScreenVideoActivity.audioManager?.setStreamVolume(3, finalVolume, 0)
//                }
//                else if (initialX < ((this@FullScreenVideoActivity.InitialViewWidth / 2) as Float)) {
//                    var diffBrightness: Float = maxBrightness.toFloat() * diff / (this@FullScreenVideoActivity.InitialViewHeight as Float / 2.0f)
//                    if (dir === Direction.DOWN) {
//                        diffBrightness = -diffBrightness
//                    }
//                    var finalBrightness = startBrightness + diffBrightness.toInt()
//                    if (finalBrightness < 0) {
//                        finalBrightness = 0
//                    } else if (finalBrightness > maxBrightness) {
//                        finalBrightness = maxBrightness
//                    }
//                    progressText = "\t" + finalBrightness
////                    if (this@FullScreenVideoActivity.imgIndicator.getDrawable() == null) {
////                        this@FullScreenVideoActivity.imgIndicator.setImageDrawable(this@Simaple_Video_Activity.indBrightnessDrawable)
////                    }
////                    this@FullScreenVideoActivity.progressTextView.setText(progressText)
//                    val layout: WindowManager.LayoutParams = this@FullScreenVideoActivity.Window_Play!!.getAttributes()
//                    layout.screenBrightness = finalBrightness.toFloat() / 100.0f
//                    this@FullScreenVideoActivity.Window_Play!!.setAttributes(layout)
////                    Utility_SharedPref.setSharedPrefData(this@Simaple_Video_Activity.activity, Utility_SharedPref.playerBrightness, finalBrightness.toString())
//                }
            }

        })
    }
    fun getCache(context: Context?): Cache? {
        if (cache == null) {
            val rootFolder = (Environment.getExternalStorageDirectory().toString() + "/"
                    + getString(R.string.app_name) + "/")
            val cachedir = File(rootFolder, ".nqc")
            if (!cachedir.exists()) {
                cachedir.mkdirs()
            }
            cache = SimpleCache(cachedir, NoOpCacheEvictor(), ExoDatabaseProvider(context!!))
        }
        return cache
    }
    private fun buildMediaSource(uri: Uri): MediaSource? {
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(this, "exoplayer-nurulquran")
//        val cacheDataSourceFactory = CacheDataSourceFactory(getCache(this), dataSourceFactory)
        return ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
//        return new ExtractorMediaSource(uri,
//                new CacheDataSourceFactory(getContext(), 100 * 1024 * 1024, 5 * 1024 * 1024), new DefaultExtractorsFactory(), null, null);
    }
    override fun onPause() {
        super.onPause()
//        player.playWhenReady = false
    }

    override fun onStop() {
        super.onStop()
        player?.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        player!!.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        player?.currentPosition?.let { outState.putLong(STATE_PLAYBACK_POSITION_MS, it) }
    }

}