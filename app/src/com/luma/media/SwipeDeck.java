/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

/** Two-card, dependency-free horizontal swipe deck with explicit accessibility actions. */
final class SwipeDeck extends FrameLayout {
    interface Factory {View create(int index);void activate(int index);void save(int index);void swipe(int index);}
    private Factory factory;
    private int count,index,version;
    private View card;
    private float downX,downY;
    private boolean animating,longPressed;
    private final Runnable hold=()->{longPressed=true;performLongClick();};
    SwipeDeck(Context context){super(context);setClickable(true);setLongClickable(true);setFocusable(true);setClipChildren(true);}
    int index(){return index;}
    void bind(int count,Factory factory){version++;if(card!=null)card.animate().cancel();this.count=count;this.factory=factory;index=Math.max(0,Math.min(index,count-1));animating=false;removeAllViews();if(count>0){card=factory.create(index);addView(card,new LayoutParams(-1,-1));}setContentDescription("Swipe media. Item "+(count==0?0:index+1)+" of "+count+". Swipe left or right, or tap to play.");}
    void align(int target){if(factory==null||animating||target<0||target>=count||target==index)return;index=target;removeAllViews();card=factory.create(index);addView(card,new LayoutParams(-1,-1));}
    void move(int direction){if(animating||count<2)return;int next=(index+direction+count)%count;
        animating=true;final int token=version;View old=card;View incoming=factory.create(next);incoming.setTranslationX(direction*getWidth());incoming.setAlpha(.5f);incoming.setRotation(direction*10);addView(incoming,new LayoutParams(-1,-1));old.animate().translationX(-direction*getWidth()).rotation(-direction*10).alpha(.3f).setDuration(240).start();incoming.animate().translationX(0).rotation(0).alpha(1).setDuration(240).withEndAction(()->{if(token!=version)return;removeView(old);card=incoming;index=next;animating=false;setContentDescription("Swipe media. Item "+(index+1)+" of "+count+". Swipe left for next, right for previous.");factory.swipe(index);}).start();
    }
    @Override public boolean onTouchEvent(MotionEvent event){int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();longPressed=false;postDelayed(hold,ViewConfiguration.getLongPressTimeout());return true;}if(action==MotionEvent.ACTION_MOVE){if(Math.abs(event.getX()-downX)>getResources().getDisplayMetrics().density*12||Math.abs(event.getY()-downY)>getResources().getDisplayMetrics().density*12)removeCallbacks(hold);return true;}if(action==MotionEvent.ACTION_CANCEL){removeCallbacks(hold);return true;}if(action==MotionEvent.ACTION_UP){removeCallbacks(hold);if(longPressed)return true;float dx=event.getX()-downX,dy=event.getY()-downY;if(Math.abs(dx)>getResources().getDisplayMetrics().density*36&&Math.abs(dx)>Math.abs(dy)){move(dx<0?1:-1);}else if(Math.abs(dx)<getResources().getDisplayMetrics().density*12&&Math.abs(dy)<getResources().getDisplayMetrics().density*12)performClick();return true;}return true;}
    @Override public boolean performLongClick(){if(factory!=null&&count>0&&!animating){factory.save(index);performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);return true;}return false;}
    @Override protected void onDetachedFromWindow(){removeCallbacks(hold);super.onDetachedFromWindow();}
    @Override public boolean performClick(){super.performClick();if(factory!=null&&count>0&&!animating)factory.activate(index);return true;}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setScrollable(true);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD){move(1);return true;}if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){move(-1);return true;}return super.performAccessibilityAction(action,args);}
}
