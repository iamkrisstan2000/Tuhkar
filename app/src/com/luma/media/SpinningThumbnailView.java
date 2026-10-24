/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;

/** Recycled measurement views must wait until the collection has really reattached them. */
final class SpinningThumbnailView extends ThumbnailView {
    String mediaUrl="";
    private boolean active,temporaryDetach;
    private ObjectAnimator rotation;
    private ViewTreeObserver drawObserver;
    private final ViewTreeObserver.OnPreDrawListener readyToSpin=()->{clearReady();syncSpin();return true;};
    private final Runnable resumeSpin=()->{syncSpin();};
    SpinningThumbnailView(Context context){super(context);}
    void bindUrl(String url){if(!mediaUrl.equals(url)){clearReady();stopSpin();setRotation(0);mediaUrl=url;active=false;}}
    void playback(boolean selected,boolean playing){
        active=selected&&playing;if(!active)clearReady();
        if(!selected){clearReady();stopSpin();setRotation(0);}else{syncSpin();awaitReady();}
    }
    private void syncSpin(){
        if(active&&!temporaryDetach&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE&&getWidth()>0&&getHeight()>0){
            if(rotation==null){float angle=getRotation();rotation=ObjectAnimator.ofFloat(this,"rotation",angle,angle+360);rotation.setDuration(16000);rotation.setRepeatCount(ObjectAnimator.INFINITE);rotation.setInterpolator(new LinearInterpolator());rotation.start();}
        }else stopSpin();
    }
    private void awaitReady(){
        if(!active||temporaryDetach||!isAttachedToWindow()||rotation!=null||drawObserver!=null)return;
        ViewTreeObserver observer=getViewTreeObserver();if(observer.isAlive()){drawObserver=observer;observer.addOnPreDrawListener(readyToSpin);}
    }
    private void clearReady(){if(drawObserver!=null){if(drawObserver.isAlive())drawObserver.removeOnPreDrawListener(readyToSpin);drawObserver=null;}}
    private void stopSpin(){if(rotation!=null){rotation.cancel();rotation=null;}}
    @Override public void onStartTemporaryDetach(){temporaryDetach=true;removeCallbacks(resumeSpin);clearReady();stopSpin();super.onStartTemporaryDetach();}
    @Override public void onFinishTemporaryDetach(){super.onFinishTemporaryDetach();temporaryDetach=false;post(resumeSpin);awaitReady();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();temporaryDetach=false;syncSpin();awaitReady();}
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){super.onSizeChanged(w,h,oldW,oldH);syncSpin();awaitReady();}
    @Override protected void onDetachedFromWindow(){removeCallbacks(resumeSpin);clearReady();stopSpin();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);syncSpin();awaitReady();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);syncSpin();awaitReady();}
}
