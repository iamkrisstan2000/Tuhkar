/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Pure access/setup decisions; actual permissions are always checked with Android. */
final class MediaAccessPolicy {
    static boolean needsSetup(boolean complete,boolean started,boolean existing){
        return !complete&&(started||!existing);
    }
    static String status(boolean full,boolean selected){
        return full?"Allowed":selected?"Selected videos only":"Not allowed";
    }
}
