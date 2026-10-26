/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.audiofx.PresetReverb;

/** One native auxiliary room shared only by our players; no output capture or microphone. */
final class NativeReverb {
    final SharedPreferences prefs;
    private PresetReverb effect;
    String error="";
    NativeReverb(SharedPreferences prefs){this.prefs=prefs;}
    int amount(){return Math.max(0,Math.min(100,prefs.getInt("reverbAmount",0)));}
    void apply(MediaPlayer player,boolean music){
        if(player==null)return;
        try{
            if(!music||amount()==0){player.setAuxEffectSendLevel(0);player.attachAuxEffect(0);if(amount()==0)close();error="";return;}
            if(effect==null){
                // Android auxiliary reverbs require session 0; attachAuxEffect routes only our players.
                effect=new PresetReverb(0,0);effect.setPreset(PresetReverb.PRESET_MEDIUMROOM);
                if(effect.setEnabled(true)!=android.media.audiofx.AudioEffect.SUCCESS)throw new IllegalStateException();
            }
            player.attachAuxEffect(effect.getId());player.setAuxEffectSendLevel(amount()/100f);error="";
        }catch(Exception e){error="Reverb is unavailable on this device/output. Playback is unchanged.";try{player.setAuxEffectSendLevel(0);player.attachAuxEffect(0);}catch(Exception ignored){}close();}
    }
    void close(){if(effect!=null){try{effect.setEnabled(false);}catch(Exception ignored){}try{effect.release();}catch(Exception ignored){}effect=null;}}
}
