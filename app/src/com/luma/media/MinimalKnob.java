/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

/** Small native rotary-style control: drag up/right, tap, keyboard or accessibility adjust. */
final class MinimalKnob extends View {
    interface Listener {void changed(int value);}
    final Paint paint=new Paint(3);
    final int max,step,accent,ink,muted;
    final boolean seconds;
    int value;
    float downX,downY;
    int startValue;
    boolean moved;
    final String label;
    Listener listener;
    MinimalKnob(Context context,String label,int max,int step,int value,boolean seconds,int accent,int ink,int muted,Typeface font){
        super(context);this.label=label;this.max=max;this.step=step;this.seconds=seconds;this.accent=accent;this.ink=ink;this.muted=muted;this.value=Math.max(0,Math.min(max,value));paint.setTypeface(font);setFocusable(true);setClickable(true);describe();
    }
    private String display(){return value==0?"Off":seconds?String.format(Locale.ROOT,"%.1f s",value/1000f):value+"%";}
    private void describe(){setContentDescription(label+", "+display()+". Drag up or right to increase. Tap to increase one step.");}
    void change(int next){int bounded=Math.max(0,Math.min(max,Math.round(next/(float)step)*step));if(value==bounded)return;value=bounded;describe();invalidate();if(listener!=null)listener.changed(value);}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.max(1,Math.min(getWidth(),getHeight())*.34f);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(dp(4));paint.setColor(muted);paint.setAlpha(55);c.drawArc(cx-r,cy-r,cx+r,cy+r,135,270,false,paint);
        paint.setColor(accent);paint.setAlpha(255);c.drawArc(cx-r,cy-r,cx+r,cy+r,135,270f*value/max,false,paint);
        double angle=Math.toRadians(135+270*value/(float)max);paint.setStyle(Paint.Style.FILL);c.drawCircle(cx+(float)Math.cos(angle)*r,cy+(float)Math.sin(angle)*r,dp(4),paint);
        paint.setColor(ink);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(17));c.drawText(display(),cx,cy-(paint.ascent()+paint.descent())/2,paint);
        if(isFocused()){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(accent);c.drawCircle(cx,cy,r+dp(8),paint);}
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();startValue=value;moved=false;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);setPressed(true);return true;
            case MotionEvent.ACTION_MOVE:float delta=(e.getX()-downX)+(downY-e.getY());if(Math.abs(delta)>dp(5)){moved=true;change(startValue+Math.round(delta/dp(160)*max));}return true;
            case MotionEvent.ACTION_UP:setPressed(false);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);if(!moved)performClick();return true;
            case MotionEvent.ACTION_CANCEL:setPressed(false);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();change(value+step);return true;}
    @Override public boolean onKeyDown(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_RIGHT){change(value+step);return true;}if(key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT){change(value-step);return true;}return super.onKeyDown(key,event);}
    @Override public CharSequence getAccessibilityClassName(){return "android.widget.SeekBar";}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT,0,max,value));info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD){change(value+step);return true;}if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){change(value-step);return true;}if(action==AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()&&args!=null){change(Math.round(args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,value)));return true;}return super.performAccessibilityAction(action,args);}
}
