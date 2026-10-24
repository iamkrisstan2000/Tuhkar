/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import java.util.Comparator;

final class MediaOrder {
    interface Entry {String label();String mediaType();long added();long modified();}
    static <T extends Entry> Comparator<T> comparator(int mode){
        Comparator<T> names=(a,b)->String.CASE_INSENSITIVE_ORDER.compare(a.label(),b.label());
        if(mode==0)return Comparator.<T,String>comparing(Entry::mediaType,String.CASE_INSENSITIVE_ORDER).thenComparing(names);
        if(mode==1)return Comparator.<T>comparingLong(Entry::added).reversed().thenComparing(names);
        if(mode==2)return Comparator.<T>comparingLong(Entry::modified).reversed().thenComparing(names);
        return names;
    }
}
