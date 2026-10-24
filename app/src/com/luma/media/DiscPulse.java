/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Playback-energy envelope, not a timer-driven/fabricated beat. */
final class DiscPulse {
    static float target(float low,float mid,float high){
        float energy=AudioGlowMath.clamp(AudioGlowMath.clamp(low)*.75f+AudioGlowMath.clamp(mid)*.20f+AudioGlowMath.clamp(high)*.05f);
        return energy<.008f?0:energy;
    }
    static float average(float previous,float energy,long elapsed){
        float dt=Math.max(0,Math.min(100,elapsed));
        return AudioGlowMath.clamp(previous+(energy-previous)*(float)(1-Math.exp(-dt/900f)));
    }
    static float contrast(float energy,float average){
        if(energy<=0)return 0;
        return AudioGlowMath.clamp(energy*.25f+Math.max(0,energy-average)*5f);
    }
    static float follow(float previous,float target,long elapsed){
        float p=AudioGlowMath.clamp(previous),t=AudioGlowMath.clamp(target);
        float dt=Math.max(0,Math.min(100,elapsed));
        float next=p+(t-p)*(float)(1-Math.exp(-dt/(t>p?45f:140f)));
        return Math.abs(next-t)<.0005f?t:AudioGlowMath.clamp(next);
    }
    private static float blend(float off,float on,float mode){return off+(on-off)*AudioGlowMath.clamp(mode);}
    static int halo(float mode,float playing,float beat){return Math.round(blend(55,(40+210*AudioGlowMath.clamp(beat))*AudioGlowMath.clamp(playing),mode));}
    static int rim(float mode,float playing,float beat){return Math.round(blend(85,(55+200*AudioGlowMath.clamp(beat))*AudioGlowMath.clamp(playing),mode));}
    static int shadow(float mode,float playing,float beat){return Math.round(blend(45,45+(10+200*AudioGlowMath.clamp(beat))*AudioGlowMath.clamp(playing),mode));}
    static float width(float mode,float beat){return .90f+1.90f*AudioGlowMath.clamp(mode)*AudioGlowMath.clamp(beat);}
    static float haloScale(float mode,float beat){return 1-.10f*AudioGlowMath.clamp(mode)*(1-AudioGlowMath.clamp(beat));}
    static float shadowScale(float mode,float beat){return 1-.12f*AudioGlowMath.clamp(mode)*(1-AudioGlowMath.clamp(beat));}
    static float rimScale(float mode,float beat){return 1+.055f*AudioGlowMath.clamp(mode)*AudioGlowMath.clamp(beat);}
}

