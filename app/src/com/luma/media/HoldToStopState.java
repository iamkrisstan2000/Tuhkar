/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Monotonic-time gesture state. Stop never depends on animation speed or the OS long-click timeout. */
final class HoldToStopState {
    static final long STOP_AFTER_MS=3000;
    private long start;
    private boolean down,fired;
    void begin(long now){start=now;down=true;fired=false;}
    boolean holding(){return down;}
    boolean stopped(){return fired;}
    long remaining(long now){return Math.max(0,STOP_AFTER_MS-(now-start));}
    float progress(long now){return down?Math.max(0,Math.min(1,(now-start)/(float)STOP_AFTER_MS)):0;}
    boolean fireIfReady(long now){if(down&&!fired&&now-start>=STOP_AFTER_MS){fired=true;return true;}return false;}
    boolean finishAsTap(long now,long tapLimit){boolean tap=down&&!fired&&now-start<tapLimit;cancel();return tap;}
    void cancel(){down=false;fired=false;}
}
