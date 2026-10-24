/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import java.util.*;

public final class QueueHoldTest {
    private static int assertions;
    private static void require(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        List<String> original=Arrays.asList("a","b","c","d");PlaybackQueue<String> q=new PlaybackQueue<>(s->s,new Random(11));
        q.reset(original,"b");require(q.step("b",1,true).equals("c"),"Default completion advances");require(q.step("d",1,true)==null,"Repeat off stops at queue end");
        q.setRepeatMode(PlaybackQueue.ALL);require(q.step("d",1,true).equals("a"),"Repeat all wraps");q.setRepeatMode(PlaybackQueue.ONE);require(q.step("c",1,true).equals("c"),"Repeat one replays the current item");require(q.step("c",1,false).equals("d"),"Manual Next overrides repeat one");require(q.step("a",-1,false).equals("d"),"Manual Previous wraps");
        q.setShuffle(true,"c");require(q.snapshot().get(0).equals("c"),"Shuffle keeps current item anchored");require(new HashSet<>(q.snapshot()).equals(new HashSet<>(original)),"Shuffle keeps membership");q.setShuffle(false,"c");require(q.snapshot().equals(original),"Turning shuffle off restores original order");
        q.reset(Arrays.asList("a","a","b"),"a");require(q.size()==2,"Duplicate URLs are deduplicated");q.setShuffle(true,"b");q.clear();require(q.isEmpty()&&q.isShuffled()&&q.repeatMode()==PlaybackQueue.ONE,"Stop clears items but preserves playback preferences");require(q.step("b",1,true)==null,"Stopped empty queue never loops");
        for(int n=1;n<=40;n++)for(int seed=0;seed<20;seed++){
            ArrayList<String> list=new ArrayList<>();for(int i=0;i<n;i++)list.add("track-"+i);String selected=list.get(n/2);PlaybackQueue<String> queue=new PlaybackQueue<>(s->s,new Random(seed));queue.setShuffle(true,null);queue.reset(list,selected);Set<String> heard=new HashSet<>();String item=selected,last=null;int steps=0;
            while(item!=null&&steps<=n){require(heard.add(item),"No repeats during a shuffle pass with repeat off");last=item;steps++;item=queue.step(item,1,true);}
            require(heard.size()==n&&item==null,"Shuffle off-completion consumes every item once then stops");queue.setRepeatMode(PlaybackQueue.ALL);String next=queue.step(last,1,true);require(next!=null,"Shuffled repeat-all cycle starts");require(n==1||!next.equals(last),"No immediate duplicate across a shuffled repeat-all boundary");queue.setShuffle(false,next);require(queue.snapshot().equals(list),"Unshuffle restores source order after cycles");
        }
        q=new PlaybackQueue<>(s->s,new Random(1));q.reset(Collections.singletonList("one"),"one");require(q.step("one",1,true)==null,"Single item off completes");q.setRepeatMode(PlaybackQueue.ALL);require(q.step("one",1,true).equals("one"),"Single item repeat all loops");q.setRepeatMode(PlaybackQueue.ONE);require(q.step("one",1,true).equals("one"),"Single item repeat one loops");
        HoldToStopState hold=new HoldToStopState();hold.begin(1000);require(!hold.fireIfReady(3999),"No stop at 2,999 ms");require(hold.fireIfReady(4000),"Stop fires at 3,000 ms");require(!hold.fireIfReady(5000),"Stop fires only once per press");require(!hold.finishAsTap(5001,500),"No play/pause after stop");
        hold.begin(10000);require(hold.finishAsTap(10200,500),"Short tap remains a click");hold.begin(20000);require(!hold.finishAsTap(22000,500),"Early release of a medium hold does not stop or toggle");require(!hold.fireIfReady(23000),"Released hold cannot fire later");hold.begin(30000);hold.cancel();require(!hold.fireIfReady(34000),"Lifecycle/move/focus cancellation prevents delayed stop");
        hold.begin(50000);require(hold.progress(50000)==0&&Math.abs(hold.progress(51500)-.5f)<.0001&&hold.progress(54000)==1,"Hold progress is monotonic and clamped");hold.cancel();require(hold.progress(55000)==0,"Cancelled gesture has no ring progress");
        System.out.println("PASS: "+assertions+" queue/hold assertions; 800 shuffled-queue scenarios; repeat modes, manual skips, order restoration, duplicate protection, 3-second threshold, cancellation, no post-stop click, and ring progress.");
    }
}
