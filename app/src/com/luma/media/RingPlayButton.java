/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;

/** 64dp composite: 48dp center for tap/hold, separate outer annulus for seeking. */
final class RingPlayButton extends HoldPlayButton {
    interface Seek {void to(long position);}
    private final Seek seek;
    private final Paint progressPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF progressBounds=new RectF();
    private final float density;
    private int progressColor;
    private final int slop;
    private long position,duration;
    private float preview,downX,downY;
    private boolean ringGesture,seeking,centerCancelled;
    private String mediaKey="";
    RingPlayButton(Context context,int centerInk,int color,Runnable tap,Runnable stop,Runnable changed,Seek seek){
        super(context,centerInk,tap,stop,changed);this.seek=seek;progressColor=color;density=getResources().getDisplayMetrics().density;slop=ViewConfiguration.get(context).getScaledTouchSlop();
        progressPaint.setStyle(Paint.Style.STROKE);progressPaint.setStrokeWidth(2*density);progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }
    void accentColor(int color){progressColor=color;invalidate();}
    void media(String key){if(!mediaKey.equals(key)){cancelHold();mediaKey=key;position=duration=0;invalidate();}}
    void progress(long position,long duration,boolean playing){
        this.position=Math.max(0,position);this.duration=Math.max(0,duration);
        if(this.duration==0&&ringGesture)cancelSeekGesture();
        setContentDescription((playing?"Pause":"Play")+". "+(duration>0?"Drag the outer ring to seek. "+(position/1000)+" of "+(duration/1000)+" seconds. ":"Seeking unavailable. ")+"Hold the center for three seconds to stop.");invalidate();
    }
    private float value(){return seeking?preview:duration>0?SeekRingMath.clamp((float)((double)position/duration)):0;}
    private void cancelSeekGesture(){seeking=false;ringGesture=false;setPressed(false);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);invalidate();}
    @Override void cancelHold(){cancelSeekGesture();super.cancelHold();}
    @Override protected float holdRingInset(){return 12*getResources().getDisplayMetrics().density;}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled())return false;
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            downX=event.getX();downY=event.getY();centerCancelled=false;
            if(Math.hypot(downX-getWidth()/2f,downY-getHeight()/2f)>=24*density){
                cancelHold();ringGesture=true;seeking=duration>0&&SeekRingMath.ring(downX,downY,getWidth(),getHeight(),24*density,slop);
                if(seeking)preview=SeekRingMath.fraction(downX,downY,getWidth(),getHeight());
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();return true;
            }
        }
        if(ringGesture){
            if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN){cancelSeekGesture();centerCancelled=true;return true;}
            double radius=Math.hypot(event.getX()-getWidth()/2f,event.getY()-getHeight()/2f);
            if(action==MotionEvent.ACTION_MOVE){
                if(radius>Math.min(getWidth(),getHeight())+slop){cancelSeekGesture();centerCancelled=true;return true;}
                if(seeking&&radius>=12*density)preview=SeekRingMath.drag(preview,SeekRingMath.fraction(event.getX(),event.getY(),getWidth(),getHeight()));
                invalidate();return true;
            }
            if(action==MotionEvent.ACTION_UP){
                boolean commit=seeking&&duration>0&&radius>=12*density&&radius<=Math.min(getWidth(),getHeight())+slop;
                long target=SeekRingMath.position(preview,duration);cancelSeekGesture();
                if(commit){position=target;seek.to(target);}return true;
            }
            return true;
        }
        if(action==MotionEvent.ACTION_MOVE&&Math.hypot(event.getX()-downX,event.getY()-downY)>slop){super.cancelHold();centerCancelled=true;}
        if(centerCancelled){if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_UP)super.cancelHold();return true;}
        return super.onTouchEvent(event);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float inset=2*density;progressBounds.set(inset,inset,getWidth()-inset,getHeight()-inset);
        progressPaint.setStyle(Paint.Style.STROKE);progressPaint.setColor(progressColor);progressPaint.setAlpha(65);canvas.drawArc(progressBounds,-90,360,false,progressPaint);
        if(duration>0){float value=value();progressPaint.setAlpha(255);canvas.drawArc(progressBounds,-90,value*360,false,progressPaint);
            double angle=Math.toRadians(value*360-90);float radius=progressBounds.width()/2f;progressPaint.setStyle(Paint.Style.FILL);canvas.drawCircle(getWidth()/2f+(float)Math.cos(angle)*radius,getHeight()/2f+(float)Math.sin(angle)*radius,2.5f*density,progressPaint);
        }
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);
        if(duration>0){info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,0,1000,value()*1000));info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}
    }
    @Override public boolean performAccessibilityAction(int action,Bundle args){
        if(isEnabled()&&duration>0){
            if(action==AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()&&args!=null){seek.to(SeekRingMath.position(args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE)/1000f,duration));return true;}
            if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){seek.to(Math.max(0,Math.min(duration,position+(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?10000:-10000))));return true;}
        }
        return super.performAccessibilityAction(action,args);
    }
    @Override public boolean onKeyDown(int code,KeyEvent event){if(isEnabled()&&duration>0&&(code==KeyEvent.KEYCODE_DPAD_LEFT||code==KeyEvent.KEYCODE_DPAD_RIGHT)){seek.to(Math.max(0,Math.min(duration,position+(code==KeyEvent.KEYCODE_DPAD_RIGHT?10000:-10000))));return true;}return super.onKeyDown(code,event);}
}
