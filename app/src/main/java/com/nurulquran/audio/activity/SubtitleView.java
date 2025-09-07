package com.nurulquran.audio.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.accessibility.CaptioningManager;

import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.CueGroup; // Added import
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.ui.CaptionStyleCompat;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;

public class SubtitleView extends View implements TextOutput {
    private boolean applyEmbeddedFontSizes;
    private boolean applyEmbeddedStyles;
    private float bottomPaddingFraction;
    private List<Cue> cues;
    private final List<SubtitlePainter> painters;
    private CaptionStyleCompat style;
    private float textSize;
    private int textSizeType;

    public SubtitleView(Context context) {
        this(context, null);
    }

    public SubtitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.painters = new ArrayList<>();
        this.textSizeType = 0;
        this.textSize = 0.0533f;
        this.applyEmbeddedStyles = true;
        this.applyEmbeddedFontSizes = true;
        this.style = CaptionStyleCompat.DEFAULT;
        this.bottomPaddingFraction = 0.08f;
    }

    // Updated onCues method signature
    @Override
    public void onCues(CueGroup cueGroup) {
        setCues(cueGroup.cues); // Access the list of cues from CueGroup
    }

    public void setCues(List<Cue> cues) {
        if (this.cues != cues) {
            this.cues = cues;
            int cueCount = cues == null ? 0 : cues.size();
            while (this.painters.size() < cueCount) {
                this.painters.add(new SubtitlePainter(getContext()));
            }
            invalidate();
        }
    }

    public void setFixedTextSize(int unit, float size) {
        Resources resources = getContext() == null ? Resources.getSystem() : getContext().getResources();
        setTextSize(2, TypedValue.applyDimension(unit, size, resources.getDisplayMetrics()));
    }

    public void setUserDefaultTextSize() {
        float fontScale = (Util.SDK_INT < 19 || isInEditMode()) ? 1.0f : getUserCaptionFontScaleV19();
        setFractionalTextSize(0.0533f * fontScale);
    }

    public void setFractionalTextSize(float fractionOfHeight) {
        setFractionalTextSize(fractionOfHeight, false);
    }

    public void setFractionalTextSize(float fractionOfHeight, boolean ignorePadding) {
        setTextSize(ignorePadding ? 1 : 0, fractionOfHeight);
    }

    private void setTextSize(int textSizeType, float textSize) {
        if (this.textSizeType != textSizeType || this.textSize != textSize) {
            this.textSizeType = textSizeType;
            this.textSize = textSize;
            invalidate();
        }
    }

    public void setApplyEmbeddedStyles(boolean applyEmbeddedStyles) {
        if (this.applyEmbeddedStyles != applyEmbeddedStyles || this.applyEmbeddedFontSizes != applyEmbeddedStyles) {
            this.applyEmbeddedStyles = applyEmbeddedStyles;
            this.applyEmbeddedFontSizes = applyEmbeddedStyles;
            invalidate();
        }
    }

    public void setApplyEmbeddedFontSizes(boolean applyEmbeddedFontSizes) {
        if (this.applyEmbeddedFontSizes != applyEmbeddedFontSizes) {
            this.applyEmbeddedFontSizes = applyEmbeddedFontSizes;
            invalidate();
        }
    }

    public void setUserDefaultStyle() {
        CaptionStyleCompat user = (Util.SDK_INT < 19 || isInEditMode()) ? CaptionStyleCompat.DEFAULT : getUserCaptionStyleV19();
        setStyle(user);
    }

    public void setStyle(CaptionStyleCompat style) {
        if (this.style != style) {
            this.style = style;
            invalidate();
        }
    }

    public void setBottomPaddingFraction(float bottomPaddingFraction) {
        if (this.bottomPaddingFraction != bottomPaddingFraction) {
            this.bottomPaddingFraction = bottomPaddingFraction;
            invalidate();
        }
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        int cueCount = (this.cues == null) ? 0 : this.cues.size();
        int rawTop = getTop();
        int rawBottom = getBottom();
        int left = getLeft() + getPaddingLeft();
        int top = rawTop + getPaddingTop();
        int right = getRight() + getPaddingRight();
        int bottom = rawBottom - getPaddingBottom();
        if (bottom > top && right > left) {
            float textSizePx;
            if (this.textSizeType == 2) {
                textSizePx = this.textSize;
            } else {
                textSizePx = this.textSize * ((float) (this.textSizeType == 0 ? bottom - top : rawBottom - rawTop));
            }
            if (textSizePx > 0.0f) {
                for (int i = 0; i < cueCount; i++) {
                    this.painters.get(i).draw(
                            this.cues.get(i),
                            this.applyEmbeddedStyles,
                            this.applyEmbeddedFontSizes,
                            this.style,
                            textSizePx,
                            this.bottomPaddingFraction,
                            canvas, left, top, right, bottom
                    );
                }
            }
        }
    }

    @TargetApi(19)
    private float getUserCaptionFontScaleV19() {
        CaptioningManager cm = (CaptioningManager) getContext().getSystemService(Context.CAPTIONING_SERVICE);
        if (cm == null) return 1.0f;
        return cm.getFontScale();
    }

    @TargetApi(19)
    private CaptionStyleCompat getUserCaptionStyleV19() {
        CaptioningManager cm = (CaptioningManager) getContext().getSystemService(Context.CAPTIONING_SERVICE);
        if (cm == null) return CaptionStyleCompat.DEFAULT;
        CaptioningManager.CaptionStyle cs = cm.getUserStyle();
        // ExoPlayer 2.19.1 ui.CaptionStyleCompat provides fromCaptionStyle(..)
        try {
            // Try the public API if present
            return (CaptionStyleCompat) CaptionStyleCompat.class
                    .getMethod("fromCaptionStyle", CaptioningManager.CaptionStyle.class)
                    .invoke(null, cs);
        } catch (Throwable ignore) {
            // Fallback to DEFAULT if method name changes across minor versions
            return CaptionStyleCompat.DEFAULT;
        }
    }
}
