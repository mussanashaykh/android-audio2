package com.nurulquran.audio.activity;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import androidx.core.view.ViewCompat;

import com.google.android.exoplayer2.ui.CaptionStyleCompat;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.util.Util;

public class SubtitlePainter {
    private static final float INNER_PADDING_RATIO = 0.125f;
    private static final String TAG = "SubtitlePainter";

    private boolean applyEmbeddedFontSizes;
    private boolean applyEmbeddedStyles;
    private int backgroundColor;
    private Rect bitmapRect;
    private float bottomPaddingFraction;
    private final float cornerRadius;
    private Bitmap cueBitmap;
    private float cueBitmapHeight;
    private float cueLine;
    private int cueLineAnchor;
    private int cueLineType;
    private float cuePosition;
    private int cuePositionAnchor;
    private float cueSize;
    private CharSequence cueText;
    private Layout.Alignment cueTextAlignment;
    private int edgeColor;
    private int edgeType;
    private int foregroundColor;
    private final RectF lineBounds = new RectF();
    private final float outlineWidth;
    private final Paint paint;
    private int parentBottom;
    private int parentLeft;
    private int parentRight;
    private int parentTop;
    private final float shadowOffset;
    private final float shadowRadius;
    private final float spacingAdd;
    private final float spacingMult;
    private StaticLayout textLayout;
    private int textLeft;
    private int textPaddingX;
    private final TextPaint textPaint;
    private float textSizePx;
    private int textTop;
    private int windowColor;

    public SubtitlePainter(Context context) {
        TypedArray styledAttributes = context.obtainStyledAttributes(
                null, new int[]{android.R.attr.lineSpacingExtra, android.R.attr.lineSpacingMultiplier}, 0, 0);
        this.spacingAdd = styledAttributes.getDimensionPixelSize(0, 0);
        this.spacingMult = styledAttributes.getFloat(1, 1.0f);
        styledAttributes.recycle();

        int twoDpInPx = Math.round(2.0f * (context.getResources().getDisplayMetrics().densityDpi / 160f));
        this.cornerRadius = twoDpInPx;
        this.outlineWidth = twoDpInPx;
        this.shadowRadius = twoDpInPx;
        this.shadowOffset = twoDpInPx;

        this.textPaint = new TextPaint();
        this.textPaint.setAntiAlias(true);
        this.textPaint.setSubpixelText(true);

        this.paint = new Paint();
        this.paint.setAntiAlias(true);
        this.paint.setStyle(Paint.Style.FILL);
    }

    private static boolean areCharSequencesEqual(CharSequence first, CharSequence second) {
        return first == second || (first != null && first.equals(second));
    }

    public void draw(
            Cue cue,
            boolean applyEmbeddedStyles,
            boolean applyEmbeddedFontSizes,
            CaptionStyleCompat style,
            float textSizePx,
            float bottomPaddingFraction,
            Canvas canvas,
            int cueBoxLeft,
            int cueBoxTop,
            int cueBoxRight,
            int cueBoxBottom
    ) {
        boolean isTextCue = cue.bitmap == null;
        int windowColor = ViewCompat.MEASURED_STATE_MASK;

        if (isTextCue) {
            if (!TextUtils.isEmpty(cue.text)) {
                windowColor = (cue.windowColorSet && applyEmbeddedStyles) ? cue.windowColor : style.windowColor;
            } else {
                return;
            }
        }

        if (areCharSequencesEqual(this.cueText, cue.text)
                && Util.areEqual(this.cueTextAlignment, cue.textAlignment)
                && this.cueBitmap == cue.bitmap
                && this.cueLine == cue.line
                && this.cueLineType == cue.lineType
                && Util.areEqual(this.cueLineAnchor, cue.lineAnchor)
                && this.cuePosition == cue.position
                && Util.areEqual(this.cuePositionAnchor, cue.positionAnchor)
                && this.cueSize == cue.size
                && this.cueBitmapHeight == cue.bitmapHeight
                && this.applyEmbeddedStyles == applyEmbeddedStyles
                && this.applyEmbeddedFontSizes == applyEmbeddedFontSizes
                && this.foregroundColor == style.foregroundColor
                && this.backgroundColor == style.backgroundColor
                && this.windowColor == windowColor
                && this.edgeType == style.edgeType
                && this.edgeColor == style.edgeColor
                && Util.areEqual(this.textPaint.getTypeface(), style.typeface)
                && this.textSizePx == textSizePx
                && this.bottomPaddingFraction == bottomPaddingFraction
                && this.parentLeft == cueBoxLeft
                && this.parentTop == cueBoxTop
                && this.parentRight == cueBoxRight
                && this.parentBottom == cueBoxBottom) {
            drawLayout(canvas, isTextCue);
            return;
        }

        this.cueText = cue.text;
        this.cueTextAlignment = cue.textAlignment;
        this.cueBitmap = cue.bitmap;
        this.cueLine = cue.line;
        this.cueLineType = cue.lineType;
        this.cueLineAnchor = cue.lineAnchor;
        this.cuePosition = cue.position;
        this.cuePositionAnchor = cue.positionAnchor;
        this.cueSize = cue.size;
        this.cueBitmapHeight = cue.bitmapHeight;
        this.applyEmbeddedStyles = applyEmbeddedStyles;
        this.applyEmbeddedFontSizes = applyEmbeddedFontSizes;
        this.foregroundColor = style.foregroundColor;
        this.backgroundColor = style.backgroundColor;
        this.windowColor = windowColor;
        this.edgeType = style.edgeType;
        this.edgeColor = style.edgeColor;
        this.textPaint.setTypeface(style.typeface);
        this.textSizePx = textSizePx;
        this.bottomPaddingFraction = bottomPaddingFraction;
        this.parentLeft = cueBoxLeft;
        this.parentTop = cueBoxTop;
        this.parentRight = cueBoxRight;
        this.parentBottom = cueBoxBottom;

        if (isTextCue) {
            setupTextLayout();
        } else {
            setupBitmapLayout();
        }
        drawLayout(canvas, isTextCue);
    }

    private void setupTextLayout() {
        int parentWidth = this.parentRight - this.parentLeft;
        int parentHeight = this.parentBottom - this.parentTop;

        this.textPaint.setTextSize(this.textSizePx);
        int textPaddingX = (int) (this.textSizePx * INNER_PADDING_RATIO + 0.5f);
        int availableWidth = parentWidth - (textPaddingX * 2);
        if (this.cueSize != Float.MIN_VALUE) {
            availableWidth = (int) (availableWidth * this.cueSize);
        }
        if (availableWidth <= 0) {
            Log.w(TAG, "Skipped drawing subtitle cue (insufficient space)");
            return;
        }

        CharSequence cueText;
        if (this.applyEmbeddedFontSizes && this.applyEmbeddedStyles) {
            cueText = this.cueText;
        } else if (this.applyEmbeddedStyles) {
            SpannableStringBuilder ssb = new SpannableStringBuilder(this.cueText);
            int cueLength = ssb.length();
            for (AbsoluteSizeSpan absSpan : ssb.getSpans(0, cueLength, AbsoluteSizeSpan.class)) {
                ssb.removeSpan(absSpan);
            }
            for (RelativeSizeSpan relSpan : ssb.getSpans(0, cueLength, RelativeSizeSpan.class)) {
                ssb.removeSpan(relSpan);
            }
            cueText = ssb; // <— ensure we keep styled text but strip font sizes
        } else {
            cueText = this.cueText.toString();
        }

        Layout.Alignment textAlignment =
                this.cueTextAlignment == null ? Layout.Alignment.ALIGN_CENTER : this.cueTextAlignment;

        this.textLayout = new StaticLayout(
                cueText, this.textPaint, availableWidth, textAlignment, this.spacingMult, this.spacingAdd, true);

        int textHeight = this.textLayout.getHeight();
        int textWidth = 0;
        int lineCount = this.textLayout.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            textWidth = Math.max((int) Math.ceil(this.textLayout.getLineWidth(i)), textWidth);
        }
        if (this.cueSize != Float.MIN_VALUE && textWidth < availableWidth) {
            textWidth = availableWidth;
        }
        textWidth += textPaddingX * 2;

        int textLeft;
        int textRight;
        if (this.cuePosition != Float.MIN_VALUE) {
            int anchorPosition = Math.round(parentWidth * this.cuePosition) + this.parentLeft;
            if (this.cuePositionAnchor == 2) {
                textLeft = anchorPosition - textWidth;
            } else if (this.cuePositionAnchor == 1) {
                textLeft = ((anchorPosition * 2) - textWidth) / 2;
            } else {
                textLeft = anchorPosition;
            }
            textLeft = Math.max(textLeft, this.parentLeft);
            textRight = Math.min(textLeft + textWidth, this.parentRight);
        } else {
            textLeft = (parentWidth - textWidth) / 2;
            textRight = textLeft + textWidth;
        }

        textWidth = textRight - textLeft;
        if (textWidth <= 0) {
            Log.w(TAG, "Skipped drawing subtitle cue (invalid horizontal positioning)");
            return;
        }

        int textTop;
        if (this.cueLine != Float.MIN_VALUE) {
            int anchorPosition;
            if (this.cueLineType == 0) {
                anchorPosition = Math.round(parentHeight * this.cueLine) + this.parentTop;
            } else {
                int firstLineHeight = this.textLayout.getLineBottom(0) - this.textLayout.getLineTop(0);
                if (this.cueLine >= 0.0f) {
                    anchorPosition = Math.round(this.cueLine * firstLineHeight) + this.parentTop;
                } else {
                    anchorPosition = Math.round((this.cueLine + 1.0f) * firstLineHeight) + this.parentBottom;
                }
            }
            if (this.cueLineAnchor == 2) {
                textTop = anchorPosition - textHeight;
            } else if (this.cueLineAnchor == 1) {
                textTop = ((anchorPosition * 2) - textHeight) / 2;
            } else {
                textTop = anchorPosition;
            }
            if (textTop + textHeight > this.parentBottom) {
                textTop = this.parentBottom - textHeight;
            } else if (textTop < this.parentTop) {
                textTop = this.parentTop;
            }
        } else {
            textTop = this.parentBottom - textHeight - (int) (parentHeight * this.bottomPaddingFraction);
        }

        this.textLayout = new StaticLayout(
                cueText, this.textPaint, textWidth, textAlignment, this.spacingMult, this.spacingAdd, true);

        this.textLeft = textLeft;
        this.textTop = textTop;
        this.textPaddingX = textPaddingX;
    }

    private void setupBitmapLayout() {
        int parentWidth = this.parentRight - this.parentLeft;
        int parentHeight = this.parentBottom - this.parentTop;

        float anchorX = this.parentLeft + parentWidth * this.cuePosition;
        float anchorY = this.parentTop + parentHeight * this.cueLine;

        int width = Math.round(parentWidth * this.cueSize);
        int height = (this.cueBitmapHeight != Float.MIN_VALUE)
                ? Math.round(parentHeight * this.cueBitmapHeight)
                : Math.round(width * (this.cueBitmap.getHeight() / (float) this.cueBitmap.getWidth()));

        if (this.cueLineAnchor == 2) {
            anchorX -= width;
        } else if (this.cueLineAnchor == 1) {
            anchorX -= (width / 2f);
        }
        int x = Math.round(anchorX);

        if (this.cuePositionAnchor == 2) {
            anchorY -= height;
        } else if (this.cuePositionAnchor == 1) {
            anchorY -= (height / 2f);
        }
        int y = Math.round(anchorY);

        this.bitmapRect = new Rect(x, y, x + width, y + height);
    }

    private void drawLayout(Canvas canvas, boolean isTextCue) {
        if (isTextCue) {
            drawTextLayout(canvas);
        } else {
            drawBitmapLayout(canvas);
        }
    }

    private void drawTextLayout(Canvas canvas) {
        StaticLayout layout = this.textLayout;
        if (layout == null) return;

        int save = canvas.save();
        canvas.translate(this.textLeft, this.textTop);

        if (Color.alpha(this.windowColor) > 0) {
            this.paint.setColor(this.windowColor);
            canvas.drawRect(-this.textPaddingX, 0, layout.getWidth() + this.textPaddingX, layout.getHeight(), this.paint);
        }

        if (Color.alpha(this.backgroundColor) > 0) {
            this.paint.setColor(this.backgroundColor);
            float previousBottom = layout.getLineTop(0);
            int lineCount = layout.getLineCount();
            for (int i = 0; i < lineCount; i++) {
                this.lineBounds.left = layout.getLineLeft(i) - this.textPaddingX;
                this.lineBounds.right = layout.getLineRight(i) + this.textPaddingX;
                this.lineBounds.top = previousBottom;
                this.lineBounds.bottom = layout.getLineBottom(i);
                previousBottom = this.lineBounds.bottom;
                canvas.drawRoundRect(this.lineBounds, this.cornerRadius, this.cornerRadius, this.paint);
            }
        }

        if (this.edgeType == 1) { // outline
            this.textPaint.setStrokeJoin(Paint.Join.ROUND);
            this.textPaint.setStrokeWidth(this.outlineWidth);
            this.textPaint.setColor(this.edgeColor);
            this.textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            layout.draw(canvas);
        } else if (this.edgeType == 2) { // drop shadow
            this.textPaint.setShadowLayer(this.shadowRadius, this.shadowOffset, this.shadowOffset, this.edgeColor);
        } else if (this.edgeType == 3 || this.edgeType == 4) { // raised/depressed
            boolean raised = this.edgeType == 3;
            int colorUp = raised ? Color.WHITE : this.edgeColor;
            int colorDown = raised ? this.edgeColor : Color.WHITE;
            float offset = this.shadowRadius / 2f;

            this.textPaint.setColor(this.foregroundColor);
            this.textPaint.setStyle(Paint.Style.FILL);
            this.textPaint.setShadowLayer(this.shadowRadius, -offset, -offset, colorUp);
            layout.draw(canvas);

            this.textPaint.setShadowLayer(this.shadowRadius, offset, offset, colorDown);
        }

        this.textPaint.setColor(this.foregroundColor);
        this.textPaint.setStyle(Paint.Style.FILL);
        layout.draw(canvas);

        this.textPaint.setShadowLayer(0f, 0f, 0f, 0);
        canvas.restoreToCount(save);
    }

    private void drawBitmapLayout(Canvas canvas) {
        canvas.drawBitmap(this.cueBitmap, null, this.bitmapRect, null);
    }
}
