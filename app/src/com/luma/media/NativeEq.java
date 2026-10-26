/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;

/** Session-scoped native EQ. Never attaches to global output session 0. */
final class NativeEq {
    static final String[] PRESETS={"Flat","Phone balanced","Voice focus","Warm speaker","Clear detail","Night gentle","Custom"};
    private static final float[] REF={60,120,250,500,1000,2000,4000,8000};
    private static final float[][] CURVES={{0,0,0,0,0,0,0,0},{-5,-3,-1,0,0,0,-1,-2},{-10,-8,-5,-2,0,0,-2,-5},{-3,-2,-1,-1,-2,-3,-4,-5},{-8,-6,-4,-2,-1,0,0,-2},{-8,-6,-3,-1,-1,-2,-4,-6}};
    final SharedPreferences prefs;
    Equalizer effect;
    volatile float[] frequencies;
    int min=-15,max=15;
    volatile String error="";
    NativeEq(SharedPreferences prefs){this.prefs=prefs;}
    boolean enabled(){return prefs.getBoolean("eqEnabled",false);}
    int preset(){return Math.max(0,Math.min(PRESETS.length-1,prefs.getInt("eqPreset",1)));}
    float[] editorFrequencies(){return frequencies==null?REF.clone():frequencies;}
    float editorLevel(int band){return frequencies==null?(band>=0&&band<REF.length?prefs.getFloat("eqDraft_"+Math.round(REF[band]),0):0):level(band);}
    private static float interpolate(float[] centers,float[] levels,float hz){
        if(hz<=centers[0])return levels[0];if(hz>=centers[centers.length-1])return levels[levels.length-1];
        int i=0;while(i<centers.length-2&&hz>centers[i+1])i++;
        double fraction=Math.log(hz/centers[i])/Math.log(centers[i+1]/centers[i]);return (float)(levels[i]+fraction*(levels[i+1]-levels[i]));
    }
    private void applyDraft(){
        if(!prefs.getBoolean("eqDraftPending",false))return;
        float[] draft=new float[REF.length];for(int i=0;i<draft.length;i++)draft[i]=prefs.getFloat("eqDraft_"+Math.round(REF[i]),0);
        SharedPreferences.Editor editor=prefs.edit();for(float hz:frequencies)editor.putFloat("eqBand_"+Math.round(hz),Math.max(min,Math.min(max,interpolate(REF,draft,hz))));editor.putBoolean("eqDraftPending",false).apply();
    }
    private void capabilities(Equalizer eq){short count=eq.getNumberOfBands();if(count<1||count>32)throw new IllegalStateException("Unsupported band count");float[] centers=new float[count];short[] range=eq.getBandLevelRange();int low=range[0]/100,high=range[1]/100;if(high<low)throw new IllegalStateException("Invalid gain range");for(short b=0;b<count;b++){centers[b]=eq.getCenterFreq(b)/1000f;if(centers[b]<=0)throw new IllegalStateException("Invalid center frequency");}min=low;max=high;frequencies=centers;applyDraft();}
    synchronized void attach(int session){
        close();if(session<=0)return;
        try{
            effect=new Equalizer(0,session);capabilities(effect);
            if(!enabled()){close();error="";return;}
            if(!effect.hasControl())throw new IllegalStateException("Effect is controlled by another app");
            if(!apply()){close();return;}error="";
        }catch(Exception e){error="Couldn't connect EQ to this playback session. Turn off other sound effects, then reconnect.";close();}
    }
    float level(int band){if(frequencies==null||band>=frequencies.length)return 0;int p=preset();float result;if(p==6)result=prefs.getFloat("eqBand_"+Math.round(frequencies[band]),0);else{float hz=frequencies[band];float[] c=CURVES[p];if(hz<=REF[0])result=c[0];else if(hz>=REF[7])result=c[7];else{int i=0;while(i<6&&hz>REF[i+1])i++;double t=Math.log(hz/REF[i])/Math.log(REF[i+1]/REF[i]);result=(float)(c[i]+t*(c[i+1]-c[i]));}}return Math.max(min,Math.min(max,result));}
    void setPreset(int preset){prefs.edit().putInt("eqPreset",preset).apply();apply();}
    void setCustom(int band,float gain){
        float[] centers=editorFrequencies();if(band<0||band>=centers.length)return;
        gain=Math.max(min,Math.min(max,gain));
        if(frequencies==null){prefs.edit().putInt("eqPreset",6).putFloat("eqDraft_"+Math.round(centers[band]),gain).putBoolean("eqDraftPending",true).apply();return;}
        float[] levels=new float[centers.length];for(int i=0;i<levels.length;i++)levels[i]=level(i);levels[band]=gain;
        SharedPreferences.Editor editor=prefs.edit().putInt("eqPreset",6);
        for(int i=0;i<levels.length;i++)editor.putFloat("eqBand_"+Math.round(centers[i]),levels[i]);
        for(float hz:REF)editor.putFloat("eqDraft_"+Math.round(hz),interpolate(centers,levels,hz));
        editor.putBoolean("eqDraftPending",false).apply();apply();
    }
    synchronized boolean apply(){
        if(effect==null||frequencies==null)return false;
        try{
            if(!effect.hasControl())throw new IllegalStateException("Effect control lost");
            for(short b=0;b<frequencies.length;b++)effect.setBandLevel(b,(short)Math.round(level(b)*100));
            if(effect.setEnabled(enabled())!=android.media.audiofx.AudioEffect.SUCCESS)throw new IllegalStateException("Enable rejected");
            error="";return true;
        }catch(Exception e){error="Couldn't apply EQ to this session. Turn off other sound effects, then reconnect.";close();return false;}
    }
    synchronized void close(){if(effect!=null){try{effect.setEnabled(false);}catch(Exception ignored){}try{effect.release();}catch(Exception ignored){}effect=null;}}
}
