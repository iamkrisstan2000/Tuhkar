/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
import java.util.*;
/** Search scopes: 0 all, 1 audio, 2 visual. No listening profile. */
final class SearchPolicy {
    static final long IDLE_MS=15000;
    static boolean allows(int scope,String kind){return scope==0||(scope==1&&(kind.equals("music")||kind.equals("radio")))||(scope==2&&(kind.equals("video")||kind.equals("tv")));}
    static boolean matches(String query,String title,String detail){String q=query.trim().toLowerCase(Locale.ROOT);return !q.isEmpty()&&(title+" "+detail).toLowerCase(Locale.ROOT).contains(q);}
}
