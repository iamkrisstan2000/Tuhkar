/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.animation.ValueAnimator;
import android.graphics.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

/** Whole-card progress/seek surface. FM glow uses only supplied playback-session energy. */
final class MiniPlayerCard extends FrameLayout {
    interface Seek {void to(float fraction);}
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip=new Path(),flow=new Path(),liquid=new Path();
    private final Rect bounds=new Rect();
    private final float density,slop;
    private final Seek listener;
    private final int base;
    private int accent;
    private long position,duration,updatedAt;
    private boolean playing,live,canSeek,dragging,excluded,cancelled;
    private float downX,downY,preview,targetEnergy,energy;
    private View[] controls=new View[0];
    MiniPlayerCard(Context context,int base,int accent,Seek listener){
        super(context);this.base=base;this.accent=accent;this.listener=listener;
        density=getResources().getDisplayMetrics().density;slop=ViewConfiguration.get(context).getScaledTouchSlop();
        setClickable(true);setFocusable(true);setWillNotDraw(false);
    }
    void controls(View... views){controls=views;}
    void accent(int value){accent=value;invalidate();}
    void media(long position,long duration,boolean playing,boolean live,boolean prepared){
        this.position=position;this.duration=duration;this.playing=playing;this.live=live;this.canSeek=prepared&&!live&&duration>0;updatedAt=SystemClock.uptimeMillis();
        if(!playing||!live)targetEnergy=0;invalidate();
    }
    void energy(float value){targetEnergy=playing&&live?AudioGlowMath.clamp(value):0;invalidate();}
    private float fraction(){return dragging?preview:WaveProgressMath.fraction(position,duration,playing?SystemClock.uptimeMillis()-updatedAt:0);}
    private boolean animating(){return playing&&ValueAnimator.areAnimatorsEnabled()&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE;}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);clip.reset();clip.addRoundRect(0,0,w,h,26*density,26*density,Path.Direction.CW);}
    @Override protected void dispatchDraw(Canvas canvas){
        int save=canvas.save();canvas.clipPath(clip);paint.setStyle(Paint.Style.FILL);paint.setColor(base);canvas.drawRect(0,0,getWidth(),getHeight(),paint);
        boolean animate=animating();
        if(live){
            energy=animate?energy+(targetEnergy-energy)*.22f:0;
            int alpha=playing?12+Math.round(energy*48):5;
            paint.setShader(new RadialGradient(getWidth()*.5f,getHeight()*.5f,Math.max(1,getWidth()*.72f),new int[]{Color.argb(alpha,Color.red(accent),Color.green(accent),Color.blue(accent)),Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,getWidth(),getHeight(),paint);paint.setShader(null);
        }else{
            float progress=fraction();float phase=animate?(SystemClock.uptimeMillis()%4200)/4200f*(float)(Math.PI*2):0;
            liquid.reset();flow.reset();liquid.moveTo(0,0);
            for(float y=0;y<=getHeight();y+=Math.max(1,density)){
                float x=LiquidFlowMath.edge(progress,y,getHeight(),getWidth(),density,phase);
                liquid.lineTo(x,y);if(y==0)flow.moveTo(x,y);else flow.lineTo(x,y);
            }
            float bottom=LiquidFlowMath.edge(progress,getHeight(),getHeight(),getWidth(),density,phase);liquid.lineTo(bottom,getHeight());flow.lineTo(bottom,getHeight());
            liquid.lineTo(0,getHeight());liquid.close();
            paint.setColor(LiquidContrastStyle.fill(base,accent));canvas.drawPath(liquid,paint);
            if(progress>0&&progress<1){paint.setColor(LiquidContrastStyle.edge(base,accent));paint.setAlpha(190);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density);canvas.drawPath(flow,paint);}
            paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
        }
        super.dispatchDraw(canvas);canvas.restoreToCount(save);
        if(animate&&(!live||Math.abs(targetEnergy-energy)>.002f))postInvalidateDelayed(40);
    }
    private boolean hitsControl(MotionEvent event){for(View view:controls){bounds.set(0,0,view.getWidth(),view.getHeight());offsetDescendantRectToMyCoords(view,bounds);if(bounds.contains((int)event.getX(),(int)event.getY()))return true;}return false;}
    private void begin(MotionEvent event){downX=event.getX();downY=event.getY();dragging=false;cancelled=false;excluded=hitsControl(event);}
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){begin(event);return false;}
        if(action==MotionEvent.ACTION_POINTER_DOWN)cancelled=true;
        if(action==MotionEvent.ACTION_MOVE&&!excluded&&!cancelled&&canSeek&&NavSwipeMath.horizontal(event.getX()-downX,event.getY()-downY,slop)){dragging=true;getParent().requestDisallowInterceptTouchEvent(true);return true;}
        return dragging;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){begin(event);return true;}
        if(action==MotionEvent.ACTION_POINTER_DOWN)cancelled=true;
        if(action==MotionEvent.ACTION_MOVE&&!excluded&&!cancelled){float dx=event.getX()-downX,dy=event.getY()-downY;if(canSeek&&NavSwipeMath.horizontal(dx,dy,slop))dragging=true;else if(!dragging&&Math.abs(dy)>slop)cancelled=true;if(dragging){preview=Math.max(0,Math.min(1,event.getX()/Math.max(1,getWidth())));invalidate();}return true;}
        if(action==MotionEvent.ACTION_UP){boolean sought=dragging;if(sought&&!cancelled&&canSeek)listener.to(Math.max(0,Math.min(1,event.getX()/Math.max(1,getWidth()))));dragging=false;if(!sought&&!cancelled&&!excluded)performClick();invalidate();return true;}
        if(action==MotionEvent.ACTION_CANCEL){dragging=false;cancelled=true;invalidate();return true;}return true;
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);if(canSeek){info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,0,1,fraction()));info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(canSeek){float value=fraction();if(action==AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()&&args!=null)value=args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,value);else if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)value+=.05f;else if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)value-=.05f;else return super.performAccessibilityAction(action,args);listener.to(Math.max(0,Math.min(1,value)));return true;}return super.performAccessibilityAction(action,args);}
}
