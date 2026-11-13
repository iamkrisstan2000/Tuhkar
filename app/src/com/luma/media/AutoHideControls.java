/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/** Observes interaction without consuming child buttons, seek drags or long presses. */
final class AutoHideControls extends LinearLayout {
    private final Runnable touched;
    boolean touching;
    AutoHideControls(Context context,Runnable touched){super(context);this.touched=touched;setOrientation(VERTICAL);setClickable(true);}
    @Override public boolean dispatchTouchEvent(MotionEvent event){int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN)touching=true;if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)touching=false;touched.run();return super.dispatchTouchEvent(event);}
}
