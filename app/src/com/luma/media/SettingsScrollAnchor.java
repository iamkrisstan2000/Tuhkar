/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Restore a visible Settings section despite changes to card heights above it. */
final class SettingsScrollAnchor {
    static int restore(int previous,int sectionTop,int sectionHeight,int offset,int maximum){
        long desired=previous;
        if(sectionTop>=0)desired=(long)sectionTop+Math.max(0,Math.min(offset,Math.max(0,sectionHeight-1)));
        return (int)Math.max(0,Math.min(desired,Math.max(0,maximum)));
    }
}
