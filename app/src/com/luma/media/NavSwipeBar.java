/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.HorizontalScrollView;

/** Intercepts horizontal drags only in the bottom bar; child taps remain native clicks. */
final class NavSwipeBar extends HorizontalScrollView {
    interface Listener {void swipe(int direction);}
    private final Listener listener;
    private final float slop,threshold;
    private float downX,downY;
    private boolean intercepted,cancelled;
    NavSwipeBar(Context context,Listener listener){
        super(context);this.listener=listener;
        slop=ViewConfiguration.get(context).getScaledTouchSlop();
        threshold=Math.max(slop*2,48*getResources().getDisplayMetrics().density);
        setTooltipText("Swipe left or right to change sections");
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();intercepted=false;cancelled=false;return false;}
        if(action==MotionEvent.ACTION_POINTER_DOWN){cancelled=true;}
        if(action==MotionEvent.ACTION_MOVE&&!cancelled){
            if(NavSwipeMath.horizontal(event.getX()-downX,event.getY()-downY,slop)){intercepted=true;return true;}
            if(Math.abs(event.getY()-downY)>slop)cancelled=true;
        }
        return intercepted;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();intercepted=false;cancelled=false;return true;}
        if(action==MotionEvent.ACTION_POINTER_DOWN)cancelled=true;
        if(action==MotionEvent.ACTION_UP){
            int direction=cancelled?0:NavSwipeMath.direction(event.getX()-downX,event.getY()-downY,threshold);
            intercepted=false;cancelled=false;if(direction!=0)listener.swipe(direction);return true;
        }
        if(action==MotionEvent.ACTION_CANCEL){intercepted=false;cancelled=true;return true;}
        return true;
    }
}
