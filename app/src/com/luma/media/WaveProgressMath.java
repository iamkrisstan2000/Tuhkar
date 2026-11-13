/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Progress interpolation only; unrelated to audio amplitude. */
final class WaveProgressMath {
    static float fraction(long position,long duration,long elapsed){
        if(duration<=0)return 0;
        return (float)Math.max(0,Math.min(1,((double)Math.max(0,position)+Math.max(0,elapsed))/duration));
    }
}
