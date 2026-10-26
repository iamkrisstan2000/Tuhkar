/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.SharedPreferences;
import android.media.*;
import android.net.Uri;
import android.os.*;

/** At most two decoders, near track end only. No radio/TV/video overlap or idle preparation. */
final class MusicCrossfade {
    final PlaybackService service;
    final SharedPreferences prefs;
    final Handler handler=new Handler(Looper.getMainLooper());
    MediaPlayer waiting,tail;
    MainActivity.Item waitingItem,tailItem;
    NativeEq tailEq;
    boolean ready,scheduled;
    long fadeStart;
    int fadeDuration;
    String failed="",observed="";
    MusicCrossfade(PlaybackService service,SharedPreferences prefs){this.service=service;this.prefs=prefs;}
    int amount(){return CrossfadeMath.clamp(prefs.getInt("crossfadeMs",0));}
    private boolean local(MainActivity.Item item){String url=item.url;return url!=null&&(url.startsWith("content://")||url.startsWith("file://")||url.startsWith("/")||url.indexOf(':')<0);}
    private boolean music(){return service.current!=null&&"music".equals(service.current.kind)&&local(service.current);}
    void sync(){
        String key=service.current==null?"":service.current.url;if(!key.equals(observed)){observed=key;failed="";}
        if(amount()==0||!music()||!service.prepared||!service.playing){cancel();return;}
        if(!scheduled){scheduled=true;handler.postDelayed(tick,tail==null?100:32);}
    }
    void settingsChanged(){cancel();sync();}
    final Runnable tick=new Runnable(){public void run(){scheduled=false;
        if(amount()==0||!music()||!service.prepared||!service.playing){cancel();return;}
        try{
            if(tail!=null){
                float progress=CrossfadeMath.progress(SystemClock.elapsedRealtime()-fadeStart,fadeDuration);
                service.player.setVolume(CrossfadeMath.incoming(progress),CrossfadeMath.incoming(progress));tail.setVolume(CrossfadeMath.outgoing(progress),CrossfadeMath.outgoing(progress));
                if(progress>=1)finishTail();
            }else{
                long duration=service.duration(),remaining=duration-service.position();
                MainActivity.Item next=service.queue.peekNext(service.current);
                boolean eligible=duration>0&&remaining>0&&next!=null&&"music".equals(next.kind)&&local(next)&&!next.url.equals(failed);
                if(waiting!=null&&(!eligible||!waitingItem.url.equals(next.url)))clearWaiting();
                if(eligible&&remaining<=amount()+3000){
                    if(waiting==null)prepare(next);
                    if(ready&&waiting!=null){int ms=CrossfadeMath.duration(amount(),remaining,waiting.getDuration());if(ms>=100&&remaining<=amount())begin(ms);}
                }
            }
        }catch(Exception e){if(tail!=null){finishTail();}else if(waiting!=null){failed=waitingItem.url;clearWaiting();}}
        sync();
    }};
    private void prepare(MainActivity.Item item){
        try{
            MediaPlayer candidate=new MediaPlayer();waiting=candidate;waitingItem=item;ready=false;
            candidate.setWakeMode(service,PowerManager.PARTIAL_WAKE_LOCK);
            candidate.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            candidate.setVolume(0,0);
            candidate.setOnPreparedListener(p->{if(waiting==p)ready=true;});
            candidate.setOnErrorListener((p,w,e)->{if(waiting==p){failed=item.url;clearWaiting();}return true;});
            candidate.setDataSource(service,Uri.parse(item.url));candidate.prepareAsync();
        }catch(Exception e){failed=item.url;clearWaiting();}
    }
    private void begin(int millis){
        if(waiting==null||!ready)return;
        MediaPlayer incoming=waiting;MainActivity.Item next=waitingItem;
        NativeEq incomingEq=null;
        try{
            incoming.start();incomingEq=new NativeEq(prefs);incomingEq.attach(incoming.getAudioSessionId());service.reverb.apply(incoming,true);
            tail=service.player;tailItem=service.current;tailEq=service.equalizer;
            tail.setOnCompletionListener(null);tail.setOnErrorListener((p,w,e)->{if(tail==p)finishTail();return true;});
            waiting=null;waitingItem=null;ready=false;fadeDuration=millis;fadeStart=SystemClock.elapsedRealtime();
            service.queue.step(service.current,1,true);
            service.adoptCrossfade(incoming,next,incomingEq);
        }catch(Exception e){
            if(incomingEq!=null)incomingEq.close();failed=next.url;
            if(waiting==incoming)clearWaiting();else release(incoming);
            if(tail!=null){service.player=tail;service.current=tailItem;service.equalizer=tailEq;tail=null;tailEq=null;tailItem=null;service.watchPlayer(service.player);volumeOne(service.player);}
        }
    }
    boolean recover(MediaPlayer failedPlayer){
        if(failedPlayer!=service.player||tail==null)return false;
        try{
            if(tail.getCurrentPosition()>=tail.getDuration())return false;
            failed=service.current.url;NativeEq failedEq=service.equalizer;
            service.player=tail;service.current=tailItem;service.equalizer=tailEq;tail=null;tailItem=null;tailEq=null;
            observed=service.current.url;service.watchPlayer(service.player);volumeOne(service.player);release(failedPlayer);if(failedEq!=null)failedEq.close();service.status="Playing";service.publish();return true;
        }catch(Exception e){return false;}
    }
    private void clearWaiting(){MediaPlayer p=waiting;waiting=null;waitingItem=null;ready=false;release(p);}
    private void finishTail(){MediaPlayer p=tail;tail=null;tailItem=null;release(p);if(tailEq!=null){tailEq.close();tailEq=null;}volumeOne(service.player);}
    void cancel(){handler.removeCallbacksAndMessages(null);scheduled=false;clearWaiting();finishTail();}
    void reset(){cancel();failed="";observed="";}
    void applyReverb(){if(tail!=null)service.reverb.apply(tail,true);}
    private void volumeOne(MediaPlayer p){if(p!=null)try{p.setVolume(1,1);}catch(Exception ignored){}}
    private void release(MediaPlayer p){if(p!=null)try{p.setOnPreparedListener(null);p.setOnCompletionListener(null);p.setOnErrorListener(null);p.release();}catch(Exception ignored){}}
}
