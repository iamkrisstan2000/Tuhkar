/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import android.net.Uri;
import android.os.*;
import android.view.KeyEvent;
import java.util.*;

/** The service, not an Activity, owns playback, focus, queue, EQ and the media session. */
public final class PlaybackService extends Service {
    static final String PLAY="com.luma.media.PLAY",PAUSE="com.luma.media.PAUSE",NEXT="com.luma.media.NEXT",PREVIOUS="com.luma.media.PREVIOUS",STOP="com.luma.media.STOP";
    private static final String CHANNEL="playback";
    private static final int NOTIFICATION=20;
    final Handler main=new Handler(Looper.getMainLooper());
    final LocalBinder binder=new LocalBinder();
    final PlaybackQueue<MainActivity.Item> queue=new PlaybackQueue<>(item->item.url);
    NativeEq equalizer;
    MediaPlayer player;
    MainActivity.Item current;
    boolean prepared,playing,isVideo,foreground;
    String status="Choose something to play";
    Runnable listener;
    AudioManager audio;
    AudioFocusRequest focus;
    MediaSession session;
    final BroadcastReceiver noisy=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){pause();}};
    public final class LocalBinder extends Binder {PlaybackService service(){return PlaybackService.this;}}
    @Override public IBinder onBind(Intent intent){return binder;}
    @Override public void onCreate(){
        super.onCreate();
        android.content.SharedPreferences prefs=getSharedPreferences("luma",0);
        equalizer=new NativeEq(prefs);queue.setRepeatMode(prefs.getInt("repeatMode",0));queue.setShuffle(prefs.getBoolean("shuffle",false),null);
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(change->{if(change<0)pause();},main).build();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(CHANNEL,"Media playback",NotificationManager.IMPORTANCE_LOW));
        session=new MediaSession(this,"Tuhkar");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setSessionActivity(openApp());
        session.setCallback(new MediaSession.Callback(){
            @Override public void onPlay(){resume();}
            @Override public void onPause(){pause();}
            @Override public void onStop(){stopPlayback();}
            @Override public void onSkipToNext(){next(1);}
            @Override public void onSkipToPrevious(){next(-1);}
            @Override public void onSeekTo(long position){seekTo(position);}
        },main);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){
        String action=intent==null?null:intent.getAction();
        if(action!=null){
            try{promote();}catch(Exception e){fail("Background playback could not start");return START_NOT_STICKY;}
            if(current==null){stopPlayback();return START_NOT_STICKY;}
            if(PLAY.equals(action))resume();else if(PAUSE.equals(action))pause();else if(NEXT.equals(action))next(1);else if(PREVIOUS.equals(action))next(-1);else if(STOP.equals(action))stopPlayback();
            else if(Intent.ACTION_MEDIA_BUTTON.equals(action)){KeyEvent event=intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);if(event!=null)session.getController().dispatchMediaButtonEvent(event);}
        }
        return START_NOT_STICKY;
    }
    private PendingIntent openApp(){return PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).putExtra("openNowPlaying",true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private PendingIntent action(String action,int code){return PendingIntent.getForegroundService(this,code,new Intent(this,PlaybackService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private Notification notification(){
        Notification.Builder b=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_stat_music).setContentTitle(current==null?"Tuhkar":current.title).setContentText(status).setContentIntent(openApp()).setOnlyAlertOnce(true).setShowWhen(false).setVisibility(Notification.VISIBILITY_PUBLIC).setCategory(Notification.CATEGORY_TRANSPORT).setDeleteIntent(action(STOP,5)).setOngoing(playing||player!=null&&!prepared);
        b.addAction(new Notification.Action.Builder(android.R.drawable.ic_media_previous,"Previous",action(PREVIOUS,1)).build());
        b.addAction(new Notification.Action.Builder(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing?"Pause":"Play",action(playing?PAUSE:PLAY,2)).build());
        b.addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next,"Next",action(NEXT,3)).build());
        b.addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,"Stop",action(STOP,4)).build());
        b.setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2));
        return b.build();
    }
    private void promote(){
        if(foreground)return;
        startForegroundService(new Intent(this,PlaybackService.class));
        try{startForeground(NOTIFICATION,notification());foreground=true;}catch(RuntimeException e){stopSelf();throw e;}
    }
    private void demote(){if(foreground){stopForeground(STOP_FOREGROUND_DETACH);foreground=false;}}
    long duration(){if(player!=null&&prepared&&!live())try{return Math.max(0,player.getDuration());}catch(Exception ignored){}return 0;}
    long position(){if(player!=null&&prepared)try{return Math.max(0,player.getCurrentPosition());}catch(Exception ignored){}return 0;}
    boolean live(){return current!=null&&(current.kind.equals("radio")||current.kind.equals("tv"));}
    void seekTo(long value){long duration=duration();if(duration<=0)return;try{player.seekTo((int)Math.max(0,Math.min(duration,value)));publish();}catch(Exception ignored){}}
    void publish(){
        if(session!=null){
            long duration=duration(),actions=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_STOP|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS;
            if(duration>0)actions|=PlaybackState.ACTION_SEEK_TO;
            int state=current==null?PlaybackState.STATE_STOPPED:!prepared&&player!=null?PlaybackState.STATE_BUFFERING:playing?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED;
            session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,current==null?"Tuhkar":current.title).putString(MediaMetadata.METADATA_KEY_ARTIST,current==null?"":current.artist).putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,status).putLong(MediaMetadata.METADATA_KEY_DURATION,duration).build());
            session.setPlaybackState(new PlaybackState.Builder().setActions(actions).setState(state,position(),playing?1f:0f,SystemClock.elapsedRealtime()).build());session.setActive(current!=null);
            if(current!=null)try{((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION,notification());}catch(Exception ignored){}
        }
        if(listener!=null)listener.run();
    }
    void selectPaused(MainActivity.Item item){if(item==null)return;releasePlayer();current=item;isVideo=item.kind.equals("video")||item.kind.equals("tv");playing=false;status="Paused";demote();publish();}
    void play(MainActivity.Item item){
        if(item==null)return;
        releasePlayer();current=item;isVideo=item.kind.equals("video")||item.kind.equals("tv");status="Connecting…";
        try{
            // Android 15 requires visible UI or an already foreground service before focus.
            promote();
            if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){fail("Audio focus unavailable. Tap Play to retry.");return;}
            MediaPlayer p=new MediaPlayer();player=p;
            p.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            p.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(isVideo?AudioAttributes.CONTENT_TYPE_MOVIE:AudioAttributes.CONTENT_TYPE_MUSIC).build());
            p.setOnVideoSizeChangedListener((mp,w,h)->{if(mp==player)publish();});
            p.setOnPreparedListener(mp->{if(mp!=player)return;prepared=true;try{mp.start();playing=true;equalizer.attach(mp.getAudioSessionId());status="Playing";publish();}catch(Exception e){fail("Unable to start playback");}});
            p.setOnCompletionListener(mp->{if(mp!=player)return;playing=false;MainActivity.Item following=null;if(!live())following=queue.step(current,1,true);if(following!=null){play(following);return;}status="Finished";audio.abandonAudioFocusRequest(focus);demote();publish();});
            p.setOnErrorListener((mp,what,extra)->{if(mp==player)fail("Can't play. Offline, restricted, or unsupported media.");return true;});
            p.setOnInfoListener((mp,what,extra)->{if(mp==player){if(what==MediaPlayer.MEDIA_INFO_BUFFERING_START)status="Buffering…";else if(what==MediaPlayer.MEDIA_INFO_BUFFERING_END)status=playing?"Playing":"Paused";publish();}return false;});
            p.setOnSeekCompleteListener(mp->{if(mp==player)publish();});
            publish();p.setDataSource(this,Uri.parse(item.url));p.prepareAsync();
            main.postDelayed(()->{if(player==p&&!prepared)fail("Connection timed out. Try another item.");},30000);
        }catch(Exception e){fail("Unable to open media. Check access or URL.");}
    }
    private void fail(String message){releasePlayer();status=message;demote();publish();}
    void pause(){
        if(player!=null&&!prepared)releasePlayer();
        if(player!=null&&prepared)try{player.pause();}catch(Exception ignored){}
        playing=false;status="Paused";audio.abandonAudioFocusRequest(focus);demote();publish();
    }
    void resume(){
        if(current==null){stopPlayback();return;}
        if(player==null||!prepared){play(current);return;}
        try{promote();if(audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED){player.start();playing=true;status="Playing";}else{status="Audio focus unavailable";demote();}publish();}catch(Exception e){fail("Unable to resume playback");}
    }
    void toggle(){if(playing)pause();else resume();}
    void next(int direction){MainActivity.Item next=queue.step(current,direction,false);if(next!=null)play(next);else if(current==null)stopPlayback();}
    void background(){if(isVideo)pause();if(player!=null&&isVideo)try{player.setDisplay(null);}catch(Exception ignored){}}
    void stopPlayback(){releasePlayer();current=null;isVideo=false;status="Playback stopped";queue.clear();publish();stopForeground(STOP_FOREGROUND_REMOVE);foreground=false;((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION);stopSelf();}
    private void releasePlayer(){
        main.removeCallbacksAndMessages(null);
        if(equalizer!=null)equalizer.close();
        if(player!=null){try{player.setOnErrorListener(null);player.setOnPreparedListener(null);player.setOnCompletionListener(null);player.setOnInfoListener(null);player.setOnSeekCompleteListener(null);player.setOnVideoSizeChangedListener(null);player.release();}catch(Exception ignored){}player=null;}
        prepared=false;playing=false;if(audio!=null&&focus!=null)audio.abandonAudioFocusRequest(focus);
    }
    @Override public void onDestroy(){listener=null;releasePlayer();if(session!=null)session.release();try{unregisterReceiver(noisy);}catch(Exception ignored){}stopForeground(STOP_FOREGROUND_REMOVE);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION);super.onDestroy();}
}
