/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Time-based fade/cycle math, independent of frame cadence. */
final class DiscMotion {
    static float fade(float value,boolean on,long elapsed){float v=AudioGlowMath.clamp(value);float step=Math.max(0,Math.min(100,elapsed))/(on?280f:420f);return AudioGlowMath.clamp(on?v+step:v-step);}
    static float cycle(float angle,float energy,long elapsed){return (angle+Math.max(0,Math.min(100,elapsed))*(.006f+AudioGlowMath.clamp(energy)*.012f))%360f;}
}
