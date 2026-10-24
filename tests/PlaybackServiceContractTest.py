# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Run the real PlaybackService + PlaybackQueue with deterministic Java framework doubles.
This tests service ownership/state transitions, not Android foreground-service eligibility,
real media decoding, notifications, device power management or audio focus dispatch.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
stubs = {
'android/content/SharedPreferences.java':'package android.content;public class SharedPreferences {public int getInt(String k,int d){return d;}public boolean getBoolean(String k,boolean d){return d;}}',
'android/content/Context.java':'''package android.content;
import android.app.*;import android.media.*;
public class Context {
 public static final String AUDIO_SERVICE="audio",NOTIFICATION_SERVICE="notification";public static final int RECEIVER_NOT_EXPORTED=4;
 public boolean nativeForeground;public int starts;public final AudioManager audio=new AudioManager(this);public final NotificationManager notifications=new NotificationManager();
 public Object getSystemService(String kind){return kind.equals("audio")?audio:notifications;}
 public SharedPreferences getSharedPreferences(String name,int mode){return new SharedPreferences();}
 public void registerReceiver(BroadcastReceiver r,IntentFilter f){}public void registerReceiver(BroadcastReceiver r,IntentFilter f,int flags){}public void unregisterReceiver(BroadcastReceiver r){}
 public void startForegroundService(Intent i){starts++;}
}''',
'android/content/Intent.java':'''package android.content;public class Intent {
 public static final int FLAG_ACTIVITY_SINGLE_TOP=1,FLAG_ACTIVITY_CLEAR_TOP=2;public static final String ACTION_MEDIA_BUTTON="media",EXTRA_KEY_EVENT="key";String action;
 public Intent(){}public Intent(Context c,Class<?> type){}public Intent putExtra(String k,boolean v){return this;}public Intent addFlags(int f){return this;}public Intent setAction(String a){action=a;return this;}public String getAction(){return action;}public <T>T getParcelableExtra(String key){return null;}
}''',
'android/content/IntentFilter.java':'package android.content;public class IntentFilter {public IntentFilter(String action){}}',
'android/content/BroadcastReceiver.java':'package android.content;public abstract class BroadcastReceiver {public abstract void onReceive(Context c,Intent i);}',
'android/app/Service.java':'''package android.app;import android.content.*;import android.os.*;public class Service extends Context {
 public static final int START_NOT_STICKY=2,STOP_FOREGROUND_DETACH=2,STOP_FOREGROUND_REMOVE=1;public boolean selfStopped;
 public void onCreate(){}public IBinder onBind(Intent i){return null;}public int onStartCommand(Intent i,int f,int id){return 0;}public void onDestroy(){}
 public void startForeground(int id,Notification n){nativeForeground=true;}public void stopForeground(int flags){nativeForeground=false;}public void stopSelf(){selfStopped=true;}
}''',
'android/app/NotificationManager.java':'package android.app;public class NotificationManager {public static final int IMPORTANCE_LOW=2;public int sent,cancelled;public void createNotificationChannel(NotificationChannel c){}public void notify(int id,Notification n){sent++;}public void cancel(int id){cancelled++;}}',
'android/app/NotificationChannel.java':'package android.app;public class NotificationChannel {public NotificationChannel(String id,String name,int importance){}}',
'android/app/PendingIntent.java':'''package android.app;import android.content.*;public class PendingIntent {public static final int FLAG_UPDATE_CURRENT=1,FLAG_IMMUTABLE=2;public static PendingIntent getActivity(Context c,int code,Intent i,int flags){return new PendingIntent();}public static PendingIntent getForegroundService(Context c,int code,Intent i,int flags){return new PendingIntent();}}''',
'android/app/Notification.java':'''package android.app;import android.content.Context;public class Notification {
 public static final int VISIBILITY_PUBLIC=1;public static final String CATEGORY_TRANSPORT="transport";
 public static class Builder {public Builder(Context c,String channel){}public Builder setSmallIcon(int i){return this;}public Builder setContentTitle(String s){return this;}public Builder setContentText(String s){return this;}public Builder setContentIntent(PendingIntent p){return this;}public Builder setDeleteIntent(PendingIntent p){return this;}public Builder setOnlyAlertOnce(boolean b){return this;}public Builder setShowWhen(boolean b){return this;}public Builder setVisibility(int v){return this;}public Builder setCategory(String s){return this;}public Builder setOngoing(boolean b){return this;}public Builder addAction(Action a){return this;}public Builder setStyle(MediaStyle s){return this;}public Notification build(){return new Notification();}}
 public static class Action {public static class Builder {public Builder(int icon,String text,PendingIntent p){}public Action build(){return new Action();}}}
 public static class MediaStyle {public MediaStyle setMediaSession(Object token){return this;}public MediaStyle setShowActionsInCompactView(int... slots){return this;}}
}''',
'android/os/IBinder.java':'package android.os;public interface IBinder {}',
'android/os/Binder.java':'package android.os;public class Binder implements IBinder {}',
'android/os/Build.java':'package android.os;public class Build {public static class VERSION {public static int SDK_INT=35;}}',
'android/os/Looper.java':'package android.os;public class Looper {public static Looper getMainLooper(){return new Looper();}}',
'android/os/Handler.java':'''package android.os;import java.util.*;public class Handler {public final ArrayList<Runnable> pending=new ArrayList<>();public Handler(Looper l){}public void postDelayed(Runnable r,long delay){pending.add(r);}public void removeCallbacksAndMessages(Object token){pending.clear();}public void runPending(){ArrayList<Runnable> copy=new ArrayList<>(pending);pending.clear();for(Runnable r:copy)r.run();}}''',
'android/os/PowerManager.java':'package android.os;public class PowerManager {public static final int PARTIAL_WAKE_LOCK=1;}',
'android/os/SystemClock.java':'package android.os;public class SystemClock {public static long elapsedRealtime(){return 1;}}',
'android/view/KeyEvent.java':'package android.view;public class KeyEvent {}',
'android/net/Uri.java':'package android.net;public class Uri {public final String value;private Uri(String s){value=s;}public static Uri parse(String s){return new Uri(s);}}',
'android/media/AudioAttributes.java':'package android.media;public class AudioAttributes {public static final int USAGE_MEDIA=1,CONTENT_TYPE_MUSIC=2,CONTENT_TYPE_MOVIE=3;public static class Builder {public Builder setUsage(int u){return this;}public Builder setContentType(int c){return this;}public AudioAttributes build(){return new AudioAttributes();}}}',
'android/media/AudioManager.java':'''package android.media;import android.content.Context;public class AudioManager {
 public static final int AUDIOFOCUS_GAIN=1,AUDIOFOCUS_REQUEST_GRANTED=1;public static final String ACTION_AUDIO_BECOMING_NOISY="noisy";public boolean grant=true;final Context owner;
 public AudioManager(Context c){owner=c;}public interface OnAudioFocusChangeListener {void onAudioFocusChange(int value);}
 public int requestAudioFocus(AudioFocusRequest r){if(!owner.nativeForeground)throw new AssertionError("focus requested before foreground");return grant?1:0;}public void abandonAudioFocusRequest(AudioFocusRequest r){}
}''',
'android/media/AudioFocusRequest.java':'''package android.media;import android.os.Handler;public class AudioFocusRequest {public AudioManager.OnAudioFocusChangeListener listener;public static class Builder {final AudioFocusRequest r=new AudioFocusRequest();public Builder(int g){}public Builder setAudioAttributes(AudioAttributes a){return this;}public Builder setOnAudioFocusChangeListener(AudioManager.OnAudioFocusChangeListener l,Handler h){r.listener=l;return this;}public AudioFocusRequest build(){return r;}}}''',
'android/media/MediaPlayer.java':'''package android.media;
import android.content.Context;import android.net.Uri;
public class MediaPlayer {
 public static final int MEDIA_INFO_BUFFERING_START=701,MEDIA_INFO_BUFFERING_END=702;public boolean started,released,wake,ready;public int pos,duration=100000;public Object display;
 public interface OnPreparedListener{void onPrepared(MediaPlayer p);}public interface OnCompletionListener{void onCompletion(MediaPlayer p);}public interface OnErrorListener{boolean onError(MediaPlayer p,int w,int e);}public interface OnInfoListener{boolean onInfo(MediaPlayer p,int w,int e);}public interface OnVideoSizeChangedListener{void onVideoSizeChanged(MediaPlayer p,int w,int h);}public interface OnSeekCompleteListener{void onSeekComplete(MediaPlayer p);}
 public OnPreparedListener prep;public OnCompletionListener complete;public OnErrorListener error;public OnInfoListener info;public OnVideoSizeChangedListener size;public OnSeekCompleteListener seek;
 public void setWakeMode(Context c,int mode){wake=true;}public void setAudioAttributes(AudioAttributes a){}public void setOnPreparedListener(OnPreparedListener l){prep=l;}public void setOnCompletionListener(OnCompletionListener l){complete=l;}public void setOnErrorListener(OnErrorListener l){error=l;}public void setOnInfoListener(OnInfoListener l){info=l;}public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener l){size=l;}public void setOnSeekCompleteListener(OnSeekCompleteListener l){seek=l;}
 public void setDataSource(Context c,Uri uri){if(uri.value.equals("bad"))throw new IllegalArgumentException("bad source");}public void prepareAsync(){}public void emitPrepared(){ready=true;if(prep!=null)prep.onPrepared(this);}public void emitCompletion(){started=false;if(complete!=null)complete.onCompletion(this);}public void start(){if(released||!ready)throw new IllegalStateException();started=true;}public void pause(){started=false;}public void release(){started=false;released=true;}public int getAudioSessionId(){return 7;}public int getDuration(){if(!ready)throw new IllegalStateException();return duration;}public int getCurrentPosition(){return pos;}public void seekTo(int p){pos=p;if(seek!=null)seek.onSeekComplete(this);}public void setDisplay(Object holder){display=holder;}
}''',
'android/media/MediaMetadata.java':'''package android.media;public class MediaMetadata {public static final String METADATA_KEY_TITLE="title",METADATA_KEY_ARTIST="artist",METADATA_KEY_DISPLAY_SUBTITLE="subtitle",METADATA_KEY_DURATION="duration";public static class Builder {public Builder putString(String k,String v){return this;}public Builder putLong(String k,long v){return this;}public MediaMetadata build(){return new MediaMetadata();}}}''',
'android/media/session/PlaybackState.java':'''package android.media.session;public class PlaybackState {public static final long ACTION_PLAY=1,ACTION_PAUSE=2,ACTION_PLAY_PAUSE=4,ACTION_STOP=8,ACTION_SKIP_TO_NEXT=16,ACTION_SKIP_TO_PREVIOUS=32,ACTION_SEEK_TO=64;public static final int STATE_STOPPED=1,STATE_BUFFERING=2,STATE_PLAYING=3,STATE_PAUSED=4;public int state;public static class Builder {final PlaybackState s=new PlaybackState();public Builder setActions(long a){return this;}public Builder setState(int state,long p,float speed,long time){s.state=state;return this;}public PlaybackState build(){return s;}}}''',
'android/media/session/MediaSession.java':'''package android.media.session;
import android.content.Context;import android.app.PendingIntent;import android.media.MediaMetadata;import android.os.Handler;import android.view.KeyEvent;
public class MediaSession {
 public static final int FLAG_HANDLES_MEDIA_BUTTONS=1,FLAG_HANDLES_TRANSPORT_CONTROLS=2;public boolean active,released;public Callback callback;public PlaybackState state;
 public MediaSession(Context c,String tag){}public void setFlags(int f){}public void setSessionActivity(PendingIntent p){}public void setCallback(Callback c,Handler h){callback=c;}public Object getSessionToken(){return this;}public void setMetadata(MediaMetadata m){}public void setPlaybackState(PlaybackState s){state=s;}public void setActive(boolean a){active=a;}public void release(){released=true;}public Controller getController(){return new Controller();}
 public static class Controller {public void dispatchMediaButtonEvent(KeyEvent e){}}
 public static class Callback {public void onPlay(){}public void onPause(){}public void onStop(){}public void onSkipToNext(){}public void onSkipToPrevious(){}public void onSeekTo(long value){}}
}''',
'android/R.java':'package android;public class R {public static class drawable {public static final int ic_media_previous=1,ic_media_next=2,ic_media_play=3,ic_media_pause=4,ic_menu_close_clear_cancel=5;}}',
'com/luma/media/R.java':'package com.luma.media;public class R {public static class drawable {public static final int ic_stat_music=1;}}',
'com/luma/media/NativeEq.java':'package com.luma.media;import android.content.SharedPreferences;class NativeEq {boolean attached;NativeEq(SharedPreferences p){}void attach(int session){attached=true;}void close(){attached=false;}}',
'com/luma/media/MainActivity.java':'package com.luma.media;public class MainActivity {static class Item {String title,url,kind,artist="";Item(String title,String kind){this.title=title;url=title;this.kind=kind;}}}',
'com/luma/media/PlaybackServiceContractTest.java':'''package com.luma.media;
import android.content.Intent;import android.media.MediaPlayer;import java.util.*;
public class PlaybackServiceContractTest {
 static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static MainActivity.Item item(String title,String kind){return new MainActivity.Item(title,kind);}
 static PlaybackService fresh(){PlaybackService s=new PlaybackService();s.onCreate();return s;}
 static void start(PlaybackService s,MainActivity.Item... items){s.queue.reset(Arrays.asList(items),items[0]);s.play(items[0]);s.player.emitPrepared();}
 public static void main(String[] args){
  MainActivity.Item a=item("a","music"),b=item("b","music"),radio=item("radio","radio"),video=item("video","video"),tv=item("tv","tv");
  PlaybackService s=fresh();check(s.onBind(new Intent())==s.binder&&s.binder.service()==s,"local binder owns service");check(!s.nativeForeground&&s.player==null,"idle bind creates no player/foreground");
  start(s,a,b);check(s.playing&&s.prepared&&s.nativeForeground&&s.player.wake,"prepared playback, foreground and partial wake mode");check(s.equalizer.attached&&s.session.active,"EQ and session active");
  MediaPlayer first=s.player;s.listener=null;s.background();check(s.player==first&&s.playing&&!first.released,"audio continues with no Activity listener");first.emitCompletion();check(s.current==b&&s.player!=first&&first.released,"background auto-next uses service queue");s.player.emitPrepared();check(s.playing,"background next starts");
  s.seekTo(25000);check(s.position()==25000,"service seek");s.session.callback.onSeekTo(30000);check(s.position()==30000,"media session seek");s.seekTo(Long.MAX_VALUE);check(s.position()==100000,"clamp seek end");s.seekTo(-3);check(s.position()==0,"clamp seek start");
  s.session.callback.onPause();check(!s.playing&&!s.nativeForeground&&s.current==b,"session pause demotes foreground but keeps current");s.session.callback.onPlay();check(s.playing&&s.nativeForeground,"session resume promotes before focus");
  s.focus.listener.onAudioFocusChange(-1);check(!s.playing,"focus loss pauses");s.resume();s.noisy.onReceive(s,new Intent());check(!s.playing,"headphone disconnect pauses");
  s.resume();s.onStartCommand(new Intent().setAction(PlaybackService.PREVIOUS),0,1);check(s.current==a,"notification previous");s.player.emitPrepared();s.onStartCommand(new Intent().setAction(PlaybackService.NEXT),0,2);check(s.current==b,"notification next");s.player.emitPrepared();
  s.onStartCommand(new Intent().setAction(PlaybackService.STOP),0,3);check(s.current==null&&s.player==null&&s.queue.isEmpty()&&!s.nativeForeground&&!s.session.active&&s.selfStopped,"stop clears player/queue/session/foreground and stops service");check(s.notifications.cancelled>0&&!s.equalizer.attached,"stop removes notification and releases EQ");s.onDestroy();check(s.session.released&&s.main.pending.isEmpty(),"destroy releases session and timers");
  s=fresh();s.onStartCommand(new Intent().setAction(PlaybackService.PAUSE),0,1);check(s.selfStopped&&!s.nativeForeground&&s.player==null,"stale notification action cannot leave an idle foreground service");s.onDestroy();
  s=fresh();s.queue.setRepeatMode(PlaybackQueue.ONE);start(s,a,b);first=s.player;first.emitCompletion();check(s.current==a&&s.player!=first,"background repeat-one");s.player.emitPrepared();s.next(1);check(s.current==b,"manual skip overrides repeat-one");s.onDestroy();
  s=fresh();s.queue.setRepeatMode(PlaybackQueue.ALL);start(s,radio);s.seekTo(50000);check(s.duration()==0&&s.position()==0,"live seek disabled");s.background();check(s.playing,"radio continues in background");first=s.player;first.emitCompletion();check(s.player==first&&!s.playing&&!s.nativeForeground,"live completion does not loop/reconnect");s.onDestroy();
  for(MainActivity.Item v:new MainActivity.Item[]{video,tv}){s=fresh();start(s,v);s.background();check(!s.playing&&!s.nativeForeground&&s.player!=null,"video/TV pause when backgrounded");s.onDestroy();}
  s=fresh();s.play(video);first=s.player;s.background();first.emitPrepared();check(s.player==null&&!s.playing&&first.released,"background video connection cannot start later");s.onDestroy();
  s=fresh();s.play(a);s.listener=null;s.player.emitPrepared();check(s.playing,"pending audio prepares after Activity leaves");s.onDestroy();
  s=fresh();s.play(a);first=s.player;s.main.runPending();check(s.player==null&&first.released&&!s.nativeForeground&&s.status.contains("timed out"),"connection timeout releases and demotes");s.onDestroy();
  s=fresh();s.audio.grant=false;s.play(a);check(s.player==null&&!s.playing&&!s.nativeForeground&&s.status.contains("focus"),"focus denial is safe");s.onDestroy();
  s=fresh();s.play(item("bad","music"));check(s.player==null&&!s.nativeForeground&&s.status.contains("Unable"),"source failure releases and demotes");s.onDestroy();
  s=fresh();start(s,a);first=s.player;first.error.onError(first,1,2);check(s.player==null&&first.released&&!s.nativeForeground,"decoder error releases and demotes");s.onDestroy();
  s=fresh();start(s,a,b);first=s.player;s.selectPaused(b);check(s.current==b&&!s.playing&&s.player==null&&first.released,"swipe-paused selection releases old playback without preparing/starting replacement");check(s.status.equals("Paused")&&!s.nativeForeground&&s.session.state.state==android.media.session.PlaybackState.STATE_PAUSED,"paused swipe publishes paused session and no foreground audio");s.resume();check(s.player!=null&&!s.playing,"resume explicitly prepares paused selection");s.player.emitPrepared();check(s.playing&&s.current==b,"explicit resume starts the selected track");s.onDestroy();
  System.out.println("PASS: "+checks+" real PlaybackService/queue contract assertions: service ownership, background audio/preparation/auto-next/repeat, notification/session controls, seek, focus/headphone loss, video pause, stop and error cleanup.");
  System.out.println("NOTE: deterministic Java framework doubles; NOT Android foreground-service eligibility, audio playback, media notification, power or focus-dispatch verification.");
 }
}'''
}
with TemporaryDirectory(prefix='tuhkar-service-') as temp:
    root=Path(temp);files=[]
    for name,content in stubs.items():
        file=root/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(content);files.append(str(file))
    actual=[str(Path('app/src/com/luma/media/'+name+'.java').resolve()) for name in ['PlaybackService','PlaybackQueue']]
    subprocess.run(['javac','-d',str(root/'classes'),*files,*actual],check=True)
    subprocess.run(['java','-cp',str(root/'classes'),'com.luma.media.PlaybackServiceContractTest'],check=True)
