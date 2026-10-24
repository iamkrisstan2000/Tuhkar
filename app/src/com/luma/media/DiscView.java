/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.animation.ValueAnimator;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;

/** Continuous rainbow annular glow with a soft transparent perimeter; only the artwork rotates. */
final class DiscView extends ImageView {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip=new Path();
    private Shader rainbow,halo,diffusion;
    private final Matrix hueMatrix=new Matrix(),haloMatrix=new Matrix(),shadowMatrix=new Matrix();
    private String accentStyle="device";
    private int deviceAccent=0xFFC6E9B2;
    private RainbowRingGeometry ring;
    private Shader shadow;
    private int holeColor=Color.BLACK;
    private float angle,hueAngle,glowAmount,low,mid,high,beat,averageEnergy,reactiveAmount=1;
    private boolean reactiveEnabled=true;
    private long lastFrame;
    private boolean selected,playing,visible,scheduled;
    String mediaUrl="";
    private final Runnable frame=new Runnable(){public void run(){scheduled=false;if(!canAnimate())return;long now=SystemClock.uptimeMillis();long elapsed=lastFrame==0?32:Math.max(0,now-lastFrame);lastFrame=now;boolean on=selected&&playing&&visible;
        glowAmount=DiscMotion.fade(glowAmount,on,elapsed);reactiveAmount=DiscMotion.fade(reactiveAmount,reactiveEnabled,elapsed);float drive=on&&reactiveEnabled?DiscPulse.target(low,mid,high):0;averageEnergy=DiscPulse.average(averageEnergy,drive,elapsed);beat=DiscPulse.follow(beat,DiscPulse.contrast(drive,averageEnergy),elapsed);if(!on&&glowAmount==0){beat=0;averageEnergy=0;}if(on){angle=(angle+Math.min(100,elapsed)*360f/16000f)%360f;if(reactiveEnabled)hueAngle=DiscMotion.cycle(hueAngle,beat,elapsed);}invalidate();if(canAnimate())schedule();else lastFrame=0;
    }};
    DiscView(Context context){super(context);setScaleType(ScaleType.CENTER_CROP);setContentDescription("Album artwork disc with an accent-matched reactive ring glow");}
    void setHoleColor(int color){holeColor=color;invalidate();}
    void setAccent(String style,int device){String normalized=AccentStyle.normalize(style);if(normalized.equals(accentStyle)&&device==deviceAccent)return;accentStyle=normalized;deviceAccent=device;rebuildPalette();invalidate();}
    private void rebuildPalette(){if(ring==null||diffusion==null)return;rainbow=new SweepGradient(ring.cx,ring.cy,AccentStyle.ring(accentStyle,deviceAccent),null);halo=new ComposeShader(rainbow,diffusion,PorterDuff.Mode.DST_IN);}
    void setReactiveGlow(boolean enabled){
        if(reactiveEnabled==enabled)return;reactiveEnabled=enabled;
        if(!enabled)energy(0,0,0);
        if(!ValueAnimator.areAnimatorsEnabled()){reactiveAmount=enabled?1:0;beat=0;}
        if(canAnimate())schedule();invalidate();
    }
    void playback(boolean selected,boolean playing,boolean visible){this.selected=selected;this.playing=playing;this.visible=visible;
        if(!visible){glowAmount=0;beat=0;averageEnergy=0;reactiveAmount=reactiveEnabled?1:0;removeCallbacks(frame);scheduled=false;lastFrame=0;}
        else if(!ValueAnimator.areAnimatorsEnabled()){glowAmount=selected&&playing?1:0;beat=0;averageEnergy=0;reactiveAmount=reactiveEnabled?1:0;removeCallbacks(frame);scheduled=false;lastFrame=0;}
        else if(canAnimate())schedule();else{removeCallbacks(frame);scheduled=false;lastFrame=0;}
        if(!selected||!playing)energy(0,0,0);invalidate();
    }
    void energy(float low,float mid,float high){this.low=AudioGlowMath.clamp(low);this.mid=AudioGlowMath.clamp(mid);this.high=AudioGlowMath.clamp(high);invalidate();}
    private boolean canAnimate(){return visible&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==View.VISIBLE&&ValueAnimator.areAnimatorsEnabled()&&(selected&&playing||glowAmount>0||reactiveAmount!=(reactiveEnabled?1:0));}
    private void schedule(){if(!scheduled&&canAnimate()){scheduled=true;postDelayed(frame,32);}}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();schedule();}
    @Override protected void onDetachedFromWindow(){removeCallbacks(frame);scheduled=false;lastFrame=0;super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==View.VISIBLE)schedule();else{removeCallbacks(frame);scheduled=false;lastFrame=0;}}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(visibility==View.VISIBLE)schedule();else{removeCallbacks(frame);scheduled=false;lastFrame=0;}}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);ring=new RainbowRingGeometry(w,h,getResources().getDisplayMetrics().density);
        float cx=ring.cx,cy=ring.cy;
        diffusion=new RadialGradient(cx,cy,ring.haloRadius,new int[]{0x00FFFFFF,0x00FFFFFF,0x75FFFFFF,0xDDFFFFFF,0x66FFFFFF,0x18FFFFFF,0x00FFFFFF},new float[]{0,.70f,.765f,.80f,.87f,.94f,1},Shader.TileMode.CLAMP);
        rebuildPalette();
        shadow=new RadialGradient(cx,cy+ring.shadowOffset,ring.shadowRadius,new int[]{0x00000000,0x68000000,0x00000000},new float[]{0,.77f,1},Shader.TileMode.CLAMP);
        clip.reset();clip.addCircle(cx,cy,Math.max(0,Math.min(cx,cy)-2),Path.Direction.CW);
    }
    @Override protected void onDraw(Canvas canvas){
        float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-2;if(r<=0)return;
        if(ring==null)return;
        // Circular bounds + zero-alpha perimeter prevent the old rectangular cutoff even at peak energy.
        float pulse=ValueAnimator.areAnimatorsEnabled()?beat:0;
        float haloScale=DiscPulse.haloScale(reactiveAmount,pulse),shadowScale=DiscPulse.shadowScale(reactiveAmount,pulse);
        haloMatrix.setScale(haloScale,haloScale,cx,cy);halo.setLocalMatrix(haloMatrix);
        shadowMatrix.setScale(shadowScale,shadowScale,cx,cy+ring.shadowOffset);shadow.setLocalMatrix(shadowMatrix);
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha(DiscPulse.shadow(reactiveAmount,glowAmount,pulse));paint.setShader(shadow);canvas.drawCircle(cx,cy+ring.shadowOffset,ring.shadowRadius*shadowScale,paint);
        hueMatrix.setRotate(hueAngle,cx,cy);rainbow.setLocalMatrix(hueMatrix);
        paint.setShader(halo);paint.setAlpha(DiscPulse.halo(reactiveAmount,glowAmount,pulse));canvas.drawCircle(cx,cy,ring.haloRadius*haloScale,paint);
        paint.setShader(rainbow);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(ring.rimWidth*DiscPulse.width(reactiveAmount,pulse));paint.setAlpha(DiscPulse.rim(reactiveAmount,glowAmount,pulse));canvas.drawCircle(cx,cy,ring.rimRadius*DiscPulse.rimScale(reactiveAmount,pulse),paint);
        paint.setShader(null);paint.setAlpha(255);
        int discSave=canvas.save();canvas.translate(cx,cy);canvas.rotate(angle);canvas.scale(RainbowRingGeometry.ART_SCALE,RainbowRingGeometry.ART_SCALE);canvas.translate(-cx,-cy);
        int imageSave=canvas.save();canvas.clipPath(clip);canvas.drawColor(Color.rgb(22,24,23));super.onDraw(canvas);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(0x20FFFFFF);
        for(int i=0;i<5;i++)canvas.drawCircle(cx,cy,r*(.74f+i*.05f),paint);
        canvas.restoreToCount(imageSave);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xCC121614);canvas.drawCircle(cx,cy,r*.15f,paint);
        paint.setColor(holeColor);canvas.drawCircle(cx,cy,r*.075f,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(0x559DAAA2);canvas.drawCircle(cx,cy,r*.15f,paint);
        canvas.restoreToCount(discSave);
    }
}
