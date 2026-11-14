package com.amr.plugins.gesture;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.lytefast.flexinput.fragment.FlexInputFragment;
import com.lytefast.flexinput.widget.FlexEditText;
import de.robv.android.xposed.XC_MethodHook;
import rx.functions.Action1;

@AliucordPlugin
public class Plugin extends com.aliucord.entities.Plugin {

  private static final float SWIPE_THRESHOLD = 100f;
  private static final float SWIPE_VELOCITY_THRESHOLD = 100f;

  @Override
  public void start(Context ctx) throws NoSuchMethodException {
    final ClipboardManager clipboard = ctx.getSystemService(ClipboardManager.class);

    patcher.patch(
        FlexInputFragment.class.getDeclaredMethod("onViewCreated", View.class, Bundle.class),
        new Action1<XC_MethodHook.MethodHookParam>() {
          @Override
          public void call(XC_MethodHook.MethodHookParam cf) {
            final FlexInputFragment input = (FlexInputFragment) cf.thisObject;
            final FlexEditText textInput = input.getView().findViewById(Utils.getResId("text_input", "id"));

            final GestureDetector gestureDetector = new GestureDetector(
                ctx,
                new GestureDetector.SimpleOnGestureListener() {
                  @Override
                  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false;

                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();

                    if (Math.abs(diffX) > Math.abs(diffY)
                        && Math.abs(diffX) > SWIPE_THRESHOLD
                        && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                      if (diffX > 0) {
                        if (clipboard != null && clipboard.hasPrimaryClip()) {
                          ClipDescription desc = clipboard.getPrimaryClipDescription();
                          if (desc != null && desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                            ClipData clipData = clipboard.getPrimaryClip();
                            if (clipData != null && clipData.getItemCount() > 0) {
                              CharSequence text = clipData.getItemAt(0).coerceToText(ctx);
                              if (text != null) textInput.append(text.toString());
                            }
                          }
                        }
                      } else {
                        textInput.setText("");
                      }
                      return true;
                    }
                    return false;
                  }
                });

            textInput.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
          }
        });
  }

  @Override
  public void stop(Context ctx) {
    patcher.unpatchAll();
  }
}
