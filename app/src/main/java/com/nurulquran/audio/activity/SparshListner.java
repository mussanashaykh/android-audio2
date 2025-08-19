package com.nurulquran.audio.activity;

import android.view.MotionEvent;
import android.view.View;

 abstract class SparshListner implements View.OnTouchListener {

    private float decidedX;
    private float decidedY;
    private int initialGesture;
    protected float initialX;
    protected float initialY;

    enum Direction {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    public abstract void onAfterMove();

    public abstract void onBeforeMove(Direction direction);

    public abstract void onClick();

    public abstract void onMove(Direction direction, float f);

    SparshListner() {
    }

    public boolean onTouch(View v, MotionEvent event) {
        float x;
        float y;
        switch (event.getActionMasked()) {
            case 0:
                this.initialX = event.getX();
                this.initialY = event.getY();
                this.initialGesture = 0;
                break;
            case 1:
                x = event.getX() - this.initialX;
                y = event.getY() - this.initialY;
                if (this.initialGesture != 0) {
                    onAfterMove();
                    this.initialGesture = 0;
                    break;
                }
                onClick();
                break;
            case 2:
                if (this.initialGesture == 0) {
                    x = event.getX() - this.initialX;
                    y = event.getY() - this.initialY;
                } else {
                    x = event.getX() - this.decidedX;
                    y = event.getY() - this.decidedY;
                }
                if (this.initialGesture == 0 && Math.abs(x) > 100.0f) {
                    this.initialGesture = 1;
                    this.decidedX = event.getX();
                    this.decidedY = event.getY();
                    if (x > 0.0f) {
                        onBeforeMove(Direction.RIGHT);
                    } else {
                        onBeforeMove(Direction.LEFT);
                    }
                } else if (this.initialGesture == 0 && Math.abs(y) > 100.0f) {
                    this.initialGesture = 2;
                    this.decidedX = event.getX();
                    this.decidedY = event.getY();
                    if (y > 0.0f) {
                        onBeforeMove(Direction.DOWN);
                    } else {
                        onBeforeMove(Direction.UP);
                    }
                }
                if (this.initialGesture != 1) {
                    if (this.initialGesture == 2) {
                        if (y <= 0.0f) {
                            onMove(Direction.UP, -y);
                            break;
                        }
                        onMove(Direction.DOWN, y);
                        break;
                    }
                } else if (x <= 0.0f) {
                    onMove(Direction.LEFT, -x);
                    break;
                } else {
                    onMove(Direction.RIGHT, x);
                    break;
                }
                break;
        }
        return true;
    }
}
