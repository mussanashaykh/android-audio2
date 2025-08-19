package com.nurulquran.audio.util;

import android.os.Build;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.S)
public abstract class CustomTelephonyCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener{
    @Override
    public void onCallStateChanged(int state) {

        if (state == TelephonyManager.CALL_STATE_RINGING) {
            ringing();
        } else if (state == TelephonyManager.CALL_STATE_IDLE) {
            idle();
        } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            offHook();
        }
    }

    protected abstract void ringing();
    protected abstract void idle();
    protected abstract void offHook();
}
