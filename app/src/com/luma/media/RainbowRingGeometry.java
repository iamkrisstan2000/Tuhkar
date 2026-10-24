/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Bounded circular glow geometry; audio changes opacity, never expands beyond these limits. */
final class RainbowRingGeometry {
    static final float ART_SCALE=.76f;
    final float cx,cy,haloRadius,rimRadius,rimWidth,shadowRadius,shadowOffset;
    RainbowRingGeometry(int width,int height,float density){
        cx=Math.max(0,width)/2f;cy=Math.max(0,height)/2f;float half=Math.min(cx,cy);
        haloRadius=Math.max(.01f,half*.98f);
        rimRadius=half*.78f;rimWidth=Math.min(half*.012f,Math.max(.5f,density*1.6f));
        shadowRadius=Math.max(.01f,half*.965f);shadowOffset=Math.min(half*.012f,Math.max(0,density)*1.5f);
    }
}
