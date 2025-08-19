package com.nurulquran.audio.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.util.Log;

import com.nurulquran.audio.util.Disk;
import com.nurulquran.audio.util.Framme;

import java.util.ArrayList;

public class Utility_Constants {

    public static ArrayList<Disk> FOLDER_LIST = new ArrayList();
    public static final String INT_VIDEO_POSITION = "INT_VIDEO_POSITION";
    public static ArrayList<Framme> MEDIA_LIST = new ArrayList();
    public static String PRFS_ADS1 = "ads1";
    public static String PRFS_ADS2 = "ads2";
    public static String PRFS_ADS3 = "ads3";
    public static String PRFS_ADS4 = "ads4";
    public static String PRFS_NAME = "MyAppPrfs";
    public static ArrayList<Framme> VIDEO_LIST = new ArrayList();


    public static void dismissWithCheck(Dialog dialog) {
        Log.d("DIALOG", "Dismiss Dialog With Check");
        if (dialog != null) {
            if (dialog.isShowing()) {
                Context context = ((ContextWrapper) dialog.getContext()).getBaseContext();
                if (!(context instanceof Activity)) {
                    dismissWithTryCatch(dialog);
                } else if (Build.VERSION.SDK_INT >= 17) {
                    if (!(((Activity) context).isFinishing() || ((Activity) context).isDestroyed())) {
                        dismissWithTryCatch(dialog);
                    }
                } else if (!((Activity) context).isFinishing()) {
                    dismissWithTryCatch(dialog);
                }
            }
        }
    }

    public static void dismissWithTryCatch(Dialog dialog) {
        try {
            dialog.dismiss();
        } catch (IllegalArgumentException e) {
            Log.d("DIALOG", "Dismiss Dialog With Try Catch : " + e.getMessage());
        } catch (Exception e2) {
            Log.d("DIALOG", "Dismiss Dialog With Try Catch : " + e2.getMessage());
        } finally {
            Log.d("DIALOG", "Dismiss Dialog With Try Catch : finally ");
        }
    }
}
