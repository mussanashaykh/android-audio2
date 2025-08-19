package com.nurulquran.audio.activity;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.nurulquran.audio.R;

public class AspectRatioFrameLayout extends FrameLayout {
    private static final float MAX_ASPECT_RATIO_DEFORMATION_FRACTION = 0.01f;
    private int resizeMode;
    private float videoAspectRatio;

    public AspectRatioFrameLayout(Context context) {
        this(context, null);
    }

    public AspectRatioFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.resizeMode = 0;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AspectRatioFrameLayout, 0, 0);
            try {
                this.resizeMode = a.getInt(0, 0);
            } finally {
                a.recycle();
            }
        }
    }

    public void setAspectRatio(float widthHeightRatio) {
        if (this.videoAspectRatio != widthHeightRatio) {
            this.videoAspectRatio = widthHeightRatio;
            requestLayout();
        }
    }

    public int getResizeMode() {
        return this.resizeMode;
    }

    public void setResizeMode(int resizeMode) {
        if (this.resizeMode != resizeMode) {
            this.resizeMode = resizeMode;
            requestLayout();
        }
    }

    public int getNextResizeMode() {
        int resizeMode = getResizeMode();
        if (resizeMode == 0) {
            return 1;
        }
        if (resizeMode == 1) {
            return 2;
        }
        if (resizeMode == 2) {
            return 3;
        }
        if (resizeMode == 3) {
            return 4;
        }
        return 0;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.resizeMode != 3 && this.videoAspectRatio > 0.0f) {
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            float aspectDeformation = (this.videoAspectRatio / (((float) width) / ((float) height))) - 1.0f;
            if (Math.abs(aspectDeformation) > MAX_ASPECT_RATIO_DEFORMATION_FRACTION) {
                switch (this.resizeMode) {
                    case 1:
                        height = (int) (((float) width) / this.videoAspectRatio);
                        break;
                    case 2:
                        width = (int) (((float) height) * this.videoAspectRatio);
                        break;
                    case 4:
                        if (aspectDeformation <= 0.0f) {
                            height = (int) (((float) width) / this.videoAspectRatio);
                            break;
                        } else {
                            width = (int) (((float) height) * this.videoAspectRatio);
                            break;
                        }
                    default:
                        if (aspectDeformation <= 0.0f) {
                            width = (int) (((float) height) * this.videoAspectRatio);
                            break;
                        } else {
                            height = (int) (((float) width) / this.videoAspectRatio);
                            break;
                        }
                }
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, 1073741824), MeasureSpec.makeMeasureSpec(height, 1073741824));
            }
        }
    }
}