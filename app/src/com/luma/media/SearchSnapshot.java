/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
import java.util.*;
/** Short-lived bounded local search snapshot. Locks never cover provider or network work. */
final class SearchSnapshot<T> {
    static final long TTL_MS=60000;
    private String key="";private long savedAt;private int generation;
    private ArrayList<T> items;
    synchronized int generation(){return generation;}
    synchronized void clear(){generation++;items=null;}
    synchronized ArrayList<T> get(String expected,long now){return items!=null&&key.equals(expected)&&now>=savedAt&&now-savedAt<TTL_MS?new ArrayList<>(items):null;}
    synchronized void save(String value,List<T> source,long now,int token){if(token==generation&&source.size()<=5000){key=value;items=new ArrayList<>(source);savedAt=now;}}
}
