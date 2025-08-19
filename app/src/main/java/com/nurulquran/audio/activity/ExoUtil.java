package com.nurulquran.audio.activity;

import android.text.TextUtils;

import androidx.core.os.EnvironmentCompat;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.util.MimeTypes;

import java.util.Locale;

public class ExoUtil {
    private ExoUtil() {
    }

    public static String buildTrackName(Format format) {
        String trackName;
        if (MimeTypes.isVideo(format.sampleMimeType)) {
            trackName = joinWithSeparator(joinWithSeparator(joinWithSeparator(buildResolutionString(format), buildBitrateString(format)), buildTrackIdString(format)), buildSampleMimeTypeString(format));
        } else if (MimeTypes.isAudio(format.sampleMimeType)) {
            trackName = buildLanguageString(format);
        } else {
            trackName = buildLanguageString(format);
        }
        return trackName.length() == 0 ? EnvironmentCompat.MEDIA_UNKNOWN : trackName;
    }

    private static String buildResolutionString(Format format) {
        return (format.width == -1 || format.height == -1) ? "" : format.width + "x" + format.height;
    }

    private static String buildAudioPropertyString(Format format) {
        return (format.channelCount == -1 || format.sampleRate == -1) ? "" : format.channelCount + "ch, " + format.sampleRate + "Hz";
    }

    private static String buildLanguageString(Format format) {
        StringBuilder stringBuilder = new StringBuilder();
        String fullLanguage = (TextUtils.isEmpty(format.language) || "und".equals(format.language)) ? "Track" : getFullLanguage(format.language);
        return stringBuilder.append(fullLanguage).append(format.id == null ? "0" : " #" + format.id).toString();
    }

    private static String buildBitrateString(Format format) {
        if (format.bitrate == -1) {
            return "";
        }
        return String.format(Locale.US, "%.2fMbit", new Object[]{Float.valueOf(((float) format.bitrate) / 1000000.0f)});
    }

    private static String joinWithSeparator(String first, String second) {
        if (first.length() == 0) {
            return second;
        }
        return second.length() == 0 ? first : first + ", " + second;
    }

    private static String buildTrackIdString(Format format) {
        return format.id == null ? "" : "id:" + format.id;
    }

    private static String buildSampleMimeTypeString(Format format) {
        return format.sampleMimeType == null ? "" : format.sampleMimeType;
    }

    private static String getFullLanguage(String language) {
        Object obj = -1;
        switch (language.hashCode()) {
            case 96848:
                if (language.equals("ara")) {
                    obj = 4;
                    break;
                }
                break;
            case 97419:
                if (language.equals("ben")) {
                    obj = 6;
                    break;
                }
                break;
            case 98468:
                if (language.equals("chi")) {
                    obj = 2;
                    break;
                }
                break;
            case 100574:
                if (language.equals("eng")) {
                    obj = null;
                    break;
                }
                break;
            case 103309:
                if (language.equals("hin")) {
                    obj = 1;
                    break;
                }
                break;
            case 105435:
                if (language.equals("jpa")) {
                    obj = 8;
                    break;
                }
                break;
            case 110749:
                if (language.equals("pan")) {
                    obj = 9;
                    break;
                }
                break;
            case 111187:
                if (language.equals("por")) {
                    obj = 5;
                    break;
                }
                break;
            case 113296:
                if (language.equals("rus")) {
                    obj = 7;
                    break;
                }
                break;
            case 114084:
                if (language.equals("spa")) {
                    obj = 3;
                    break;
                }
                break;
        }
        {
            return (String) obj;
        }
    }
}