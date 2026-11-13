package com.amr.plugins.gesture;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

class OnSwipeListener implements View.OnTouchListener {
  public static final float SWIPE_THRESHOLD = 100;
  public static final float SWIPE_VELOCITY_THRESHOLD = 100;

  private final GestureDetector gestureDetector;

  public OnSwipeListener(Context ctx) {
    gestureDetector =
        new GestureDetector(
            ctx,
            new SimpleOnGestureListener() {
              public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                  float diffY = e2.getY() - e1.getY();
                  float diffX = e2.getX() - e1.getX();
                  if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                      if (diffX > 0) onSwipeRight();
                      else onSwipeLeft();
                      result = true;
                    }
                  }
                } catch (Exception e) { e.printStackTrace(); }
                return result;
              }
            });
  }

  public boolean onTouch(View v, MotionEvent event) {
    return gestureDetector.onTouchEvent(event);
  }

  public void onSwipeRight() {}
  public void onSwipeLeft() {}
}
