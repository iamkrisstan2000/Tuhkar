/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.os.Looper;

/** Visible-screen playback-session analysis only. No microphone recording, session zero, file or network output. */
final class AudioReactiveGlow {
    interface Listener {void energy(float low,float mid,float high);void unavailable();}
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Listener listener;
    private Visualizer visualizer;
    private int session=-1,failed=-1,generation;
    AudioReactiveGlow(Listener listener){this.listener=listener;}
    boolean start(int audioSession){
        if(audioSession<=0)return false;
        if(session==audioSession&&visualizer!=null)return true;
        if(failed==audioSession)return false;
        stop();final int token=generation;
        try{
            visualizer=new Visualizer(audioSession);session=audioSession;
            int[] range=Visualizer.getCaptureSizeRange();int size=Math.max(range[0],Math.min(512,range[1]));
            if(visualizer.setCaptureSize(size)!=Visualizer.SUCCESS)throw new IllegalStateException();
            visualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
            final float[] bands=new float[3];
            int result=visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener(){
                @Override public void onWaveFormDataCapture(Visualizer v,byte[] waveform,int rate){}
                @Override public void onFftDataCapture(Visualizer v,byte[] fft,int rate){
                    AudioGlowMath.bands(fft,rate,bands);final float low=bands[0],mid=bands[1],high=bands[2];
                    main.post(()->{if(token==generation&&visualizer!=null)listener.energy(low,mid,high);});
                }
            },Math.min(10000,Visualizer.getMaxCaptureRate()),false,true);
            if(result!=Visualizer.SUCCESS||visualizer.setEnabled(true)!=Visualizer.SUCCESS)throw new IllegalStateException();
            return true;
        }catch(RuntimeException e){stop();failed=audioSession;listener.unavailable();return false;}
    }
    void stop(){generation++;session=-1;failed=-1;main.removeCallbacksAndMessages(null);if(visualizer!=null){try{visualizer.setEnabled(false);}catch(RuntimeException ignored){}try{visualizer.release();}catch(RuntimeException ignored){}visualizer=null;}}
}
