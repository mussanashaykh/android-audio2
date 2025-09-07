package com.nurulquran.audio.util;

import android.content.Context;

import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import java.io.File;

@SuppressWarnings({"deprecation"}) // keep all Exo 2.19.1 deprecations quiet here
public class CacheDataSourceFactory implements DataSource.Factory {
    private final Context context;
    private final DefaultDataSource.Factory defaultDataSourceFactory;
    private final long maxFileSize;
    private final long maxCacheSize;

    private static volatile Cache sCache;

    public CacheDataSourceFactory(Context context, long maxCacheSize, long maxFileSize) {
        this.context = context.getApplicationContext();
        this.maxCacheSize = maxCacheSize;
        this.maxFileSize = maxFileSize;

        DefaultHttpDataSource.Factory httpFactory =
                new DefaultHttpDataSource.Factory()
                        .setUserAgent(context.getPackageName())
                        .setAllowCrossProtocolRedirects(true);

        this.defaultDataSourceFactory = new DefaultDataSource.Factory(this.context, httpFactory);
    }

    @Override
    public DataSource createDataSource() {
        Cache cache = getOrCreateCache();

        CacheDataSource.Factory cacheFactory = new CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                .setCacheReadDataSourceFactory(new FileDataSource.Factory())
                .setCacheWriteDataSinkFactory(
                        new CacheDataSink.Factory().setCache(cache).setFragmentSize(maxFileSize)
                )
                .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        return cacheFactory.createDataSource();
    }

    @SuppressWarnings("deprecation")
    private Cache getOrCreateCache() {
        if (sCache == null) {
            synchronized (CacheDataSourceFactory.class) {
                if (sCache == null) {
                    File cacheDir = new File(context.getCacheDir(), "media");
                    if (!cacheDir.exists()) cacheDir.mkdirs();
                    DatabaseProvider db = new StandaloneDatabaseProvider(context);
                    sCache = new SimpleCache(
                            cacheDir,
                            new LeastRecentlyUsedCacheEvictor(maxCacheSize),
                            db
                    );
                }
            }
        }
        return sCache;
    }
}
