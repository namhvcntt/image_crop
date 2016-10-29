package com.namhv.image_crop.image_crop.animation;

public interface SimpleValueAnimator {
    void startAnimation(long duration);
    void cancelAnimation();
    boolean isAnimationStarted();
    void addAnimatorListener(SimpleValueAnimatorListener animatorListener);
}
