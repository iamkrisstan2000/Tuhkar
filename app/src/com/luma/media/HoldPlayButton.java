/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

/** Short click = play/pause. A continuous three-second press = Stop, with a progress ring. */
class HoldPlayButton extends Button {
    private final HoldToStopState state=new HoldToStopState();
    private final Paint ring=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ringBounds=new RectF();
    private final Runnable stopAction,holdChanged;
    private final float density;
    private final int slop;
    private final Runnable deadline=new Runnable(){public void run(){
        if(!state.holding())return;
        if(!isAttachedToWindow()||!hasWindowFocus()){cancelHold();return;}
        if(state.fireIfReady(SystemClock.uptimeMillis())){performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);stopAction.run();invalidate();}
        else if(!state.stopped())postDelayed(this,state.remaining(SystemClock.uptimeMillis()));
    }};
    HoldPlayButton(Context context,int ringColor,Runnable tapAction,Runnable stopAction,Runnable holdChanged){
        super(context);this.stopAction=stopAction;this.holdChanged=holdChanged;density=getResources().getDisplayMetrics().density;slop=ViewConfiguration.get(context).getScaledTouchSlop();ring.setColor(ringColor);ring.setStyle(Paint.Style.STROKE);ring.setStrokeWidth(2*density);ring.setStrokeCap(Paint.Cap.ROUND);setLongClickable(false);setOnClickListener(v->tapAction.run());setContentDescription("Play or pause. Hold for three seconds to stop playback.");setTooltipText("Tap to play/pause · hold 3 seconds to stop");setLongClickable(false);
    }
    void accentInk(int color){ring.setColor(color);invalidate();}
    boolean isHolding(){return state.holding();}
    private void begin(){cancelHold();state.begin(SystemClock.uptimeMillis());if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);setPressed(true);postDelayed(deadline,HoldToStopState.STOP_AFTER_MS);if(holdChanged!=null)holdChanged.run();invalidate();}
    void cancelHold(){if(deadline!=null)removeCallbacks(deadline);boolean active=state!=null&&state.holding();if(state!=null)state.cancel();if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);setPressed(false);if(active&&holdChanged!=null)holdChanged.run();invalidate();}
    private void finish(){boolean tap=state.finishAsTap(SystemClock.uptimeMillis(),ViewConfiguration.getLongPressTimeout());removeCallbacks(deadline);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);setPressed(false);if(holdChanged!=null)holdChanged.run();invalidate();if(tap)performClick();}
    private boolean inside(MotionEvent e){return e.getX()>=-slop&&e.getY()>=-slop&&e.getX()<=getWidth()+slop&&e.getY()<=getHeight()+slop;}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled())return false;
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:begin();return true;
            case MotionEvent.ACTION_POINTER_DOWN:case MotionEvent.ACTION_CANCEL:cancelHold();return true;
            case MotionEvent.ACTION_MOVE:if(!inside(event))cancelHold();return true;
            case MotionEvent.ACTION_UP:if(inside(event))finish();else cancelHold();return true;
            default:return true;
        }
    }
    @Override public boolean onKeyDown(int code,KeyEvent event){if(!isEnabled())return false;if(code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER||code==KeyEvent.KEYCODE_SPACE){if(event.getRepeatCount()==0)begin();return true;}return super.onKeyDown(code,event);}
    @Override public boolean onKeyUp(int code,KeyEvent event){if(code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER||code==KeyEvent.KEYCODE_SPACE){if(event.isCanceled())cancelHold();else finish();return true;}return super.onKeyUp(code,event);}
    protected float holdRingInset(){return 4*density;}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(state.holding()){float inset=holdRingInset();ringBounds.set(inset,inset,getWidth()-inset,getHeight()-inset);canvas.drawArc(ringBounds,-90,360*state.progress(SystemClock.uptimeMillis()),false,ring);if(!state.stopped())postInvalidateOnAnimation();}}
    @Override protected void onDetachedFromWindow(){cancelHold();super.onDetachedFromWindow();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(!focus)cancelHold();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(visibility!=VISIBLE)cancelHold();}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_stop_playback,"Stop playback"));}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==R.id.action_stop_playback){if(!isEnabled())return false;cancelHold();stopAction.run();return true;}return super.performAccessibilityAction(action,args);}
}
