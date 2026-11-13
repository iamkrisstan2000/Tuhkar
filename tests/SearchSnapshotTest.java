/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
import java.util.*;
public final class SearchSnapshotTest {
 static void check(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[] args){SearchSnapshot<String> cache=new SearchSnapshot<>();int g=cache.generation();check(cache.get("music",0)==null);ArrayList<String> data=new ArrayList<>(Arrays.asList("one"));cache.save("music",data,100,g);data.clear();check(cache.get("music",100).size()==1);ArrayList<String> copy=cache.get("music",100);copy.clear();check(cache.get("music",100).size()==1);check(cache.get("other",100)==null);check(cache.get("music",99)==null);check(cache.get("music",60099)!=null);check(cache.get("music",60100)==null);cache.clear();check(cache.get("music",100)==null);cache.save("music",Arrays.asList("stale"),100,g);check(cache.get("music",100)==null);cache.save("music",Collections.nCopies(5001,"large"),100,cache.generation());check(cache.get("music",100)==null);cache.save("music",Arrays.asList("fresh"),100,cache.generation());check(cache.get("music",100).get(0).equals("fresh"));System.out.println("PASS: local search snapshot 60s expiry, bounds, key separation, copy isolation and invalidation/stale-writer guard");}
}
