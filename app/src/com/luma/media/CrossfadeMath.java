/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

final class CrossfadeMath {
    static int clamp(int millis){return Math.max(0,Math.min(2000,millis));}
    static int duration(int requested,long remaining,long nextDuration){return (int)Math.max(0,Math.min(clamp(requested),Math.min(remaining,nextDuration/2)));}
    static float progress(long elapsed,int duration){return duration<=0?1:Math.max(0,Math.min(1,elapsed/(float)duration));}
    static float incoming(float progress){return AudioGlowMath.clamp(progress);}
    static float outgoing(float progress){return 1-AudioGlowMath.clamp(progress);}
}
