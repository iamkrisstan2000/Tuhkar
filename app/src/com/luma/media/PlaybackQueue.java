/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import java.util.*;
import java.util.function.Function;

/** Logical queue state, independent of Android playback and views. */
final class PlaybackQueue<T> {
    static final int OFF=0,ALL=1,ONE=2;
    private final ArrayList<T> original=new ArrayList<>(),order=new ArrayList<>();
    private final Function<T,String> key;
    private final Random random;
    private boolean shuffled;
    private int repeat;
    PlaybackQueue(Function<T,String> key){this(key,new Random());}
    PlaybackQueue(Function<T,String> key,Random random){this.key=key;this.random=random;}
    int size(){return order.size();}
    boolean isEmpty(){return order.isEmpty();}
    int repeatMode(){return repeat;}
    boolean isShuffled(){return shuffled;}
    List<T> snapshot(){return new ArrayList<>(order);}
    void setRepeatMode(int mode){repeat=mode>=OFF&&mode<=ONE?mode:OFF;}
    void clear(){original.clear();order.clear();}
    void reset(List<T> entries,T selected){
        clear();LinkedHashMap<String,T> distinct=new LinkedHashMap<>();for(T entry:entries)if(entry!=null)distinct.putIfAbsent(key.apply(entry),entry);original.addAll(distinct.values());rebuild(selected);
    }
    void setShuffle(boolean enabled,T current){shuffled=enabled;rebuild(current);}
    private void rebuild(T current){order.clear();order.addAll(original);if(shuffled&&order.size()>1){T anchor=null;int index=indexOf(current);if(index>=0)anchor=order.remove(index);Collections.shuffle(order,random);if(anchor!=null)order.add(0,anchor);}}
    private int indexOf(T current){if(current==null)return -1;String id=key.apply(current);for(int i=0;i<order.size();i++)if(Objects.equals(id,key.apply(order.get(i))))return i;return -1;}
    T peekNext(T current){
        int index=indexOf(current);if(index<0||order.isEmpty())return null;
        if(repeat==ONE)return order.get(index);
        if(index+1<order.size())return order.get(index+1);
        // Do not mutate/guess the next shuffled cycle while preloading.
        return repeat==ALL&&!shuffled?order.get(0):null;
    }
    T step(T current,int direction,boolean automatic){
        if(order.isEmpty())return null;int index=indexOf(current);if(automatic&&repeat==ONE&&index>=0)return order.get(index);
        int delta=direction<0?-1:1;int target=index<0?(delta>0?0:order.size()-1):index+delta;
        if(target>=0&&target<order.size())return order.get(target);
        if(automatic&&repeat!=ALL)return null;
        if(delta>0&&shuffled&&order.size()>1){Collections.shuffle(order,random);if(current!=null&&Objects.equals(key.apply(order.get(0)),key.apply(current)))Collections.swap(order,0,1);}
        return order.get(delta>0?0:order.size()-1);
    }
}
