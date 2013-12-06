package com.thehayro.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class CustomScroller extends Scroller {

    private int mAnimationDuration = 200;

    public CustomScroller(Context context) {
        super(context);
    }

    public CustomScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
    }

    @SuppressLint("NewApi")
    public CustomScroller(Context context, Interpolator interpolator, boolean flywheel) {
        super(context, interpolator, flywheel);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        super.startScroll(startX, startY, dx, dy, mAnimationDuration);
    }

    public void setAnimationDuration(int animationDuration) {
        mAnimationDuration = animationDuration;
    }
}