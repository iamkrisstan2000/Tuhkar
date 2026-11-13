/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import java.util.*;
import java.util.function.*;

/** Local, random discovery policy; no listening profiling or fabricated media. */
final class HomeDiscovery {
    static final int SONGS=6,FM=2,VIDEOS=1,TV=1;
    static String greeting(int hour){return hour<5?"Good evening":hour<12?"Good morning":hour<18?"Good afternoon":"Good evening";}
    static final String[] CALLSIGNS={"Senpai","Master","Adventurer","Listener","Supreme Listener","Audiophile","Boss","Explorer","Dev","Partner","Captain","NPC"};
    static String callsign(Random random){return CALLSIGNS[random.nextInt(CALLSIGNS.length)];}
    static boolean validCallsign(String value){return Arrays.asList(CALLSIGNS).contains(value);}
    static <T> ArrayList<T> sample(List<T> source,int count,Function<T,String> key,Random random){
        LinkedHashMap<String,T> unique=new LinkedHashMap<>();for(T item:source)if(item!=null)unique.putIfAbsent(key.apply(item),item);
        ArrayList<T> items=new ArrayList<>(unique.values());Collections.shuffle(items,random);return new ArrayList<>(items.subList(0,Math.max(0,Math.min(count,items.size()))));
    }
    static <T> ArrayList<T> recent(List<T> source,int count,ToLongFunction<T> added){ArrayList<T> items=new ArrayList<>();for(T item:source)if(added.applyAsLong(item)>0)items.add(item);items.sort((a,b)->Long.compare(added.applyAsLong(b),added.applyAsLong(a)));return new ArrayList<>(items.subList(0,Math.max(0,Math.min(count,items.size()))));}
    static String updatesPath(String region){String value=RadioDirectory.normalize(region);return "/json/stations/search?limit=40&hidebroken=true&order=changetimestamp&reverse=true"+("all".equals(value)?"":"&countrycode="+value);}
}
