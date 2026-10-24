# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Real NativeEq with deterministic AudioEffect doubles; not device DSP/audio validation."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
stubs={
'android/content/SharedPreferences.java':'''package android.content;import java.util.*;public class SharedPreferences {Map<String,Object> values=new HashMap<>();public boolean getBoolean(String k,boolean d){return (Boolean)values.getOrDefault(k,d);}public int getInt(String k,int d){return (Integer)values.getOrDefault(k,d);}public float getFloat(String k,float d){return (Float)values.getOrDefault(k,d);}public Editor edit(){return new Editor();}public class Editor{public Editor putInt(String k,int v){values.put(k,v);return this;}public Editor putBoolean(String k,boolean v){values.put(k,v);return this;}public Editor putFloat(String k,float v){values.put(k,v);return this;}public void apply(){}}}''',
'android/media/audiofx/AudioEffect.java':'package android.media.audiofx;public class AudioEffect {public static final int SUCCESS=0;}',
'android/media/audiofx/Equalizer.java':'''package android.media.audiofx;public class Equalizer {public static int creations,releases,lastSession,bandResult,enableResult;public static boolean failCreate,noControl,failDisable;public static Equalizer last;public boolean enabled,released;public short[] levels=new short[5];public Equalizer(int priority,int session){creations++;lastSession=session;if(session<=0||failCreate)throw new IllegalStateException();last=this;}public short getNumberOfBands(){return 5;}public short[] getBandLevelRange(){return new short[]{-1500,1500};}public int getCenterFreq(short band){return new int[]{60000,230000,910000,3600000,14000000}[band];}public boolean hasControl(){return !noControl;}public void setBandLevel(short band,short gain){if(bandResult!=0)throw new IllegalStateException();levels[band]=gain;}public int setEnabled(boolean value){if(!value&&failDisable)throw new IllegalStateException();enabled=value;return value?enableResult:0;}public void release(){released=true;releases++;}}''',
'com/luma/media/NativeEqContract.java':'''package com.luma.media;import android.content.SharedPreferences;import android.media.audiofx.Equalizer;public class NativeEqContract {
 static int checks;static void check(boolean value,String msg){checks++;if(!value)throw new AssertionError(msg);}
 public static void main(String[] args){SharedPreferences prefs=new SharedPreferences();NativeEq eq=new NativeEq(prefs);
 check(Equalizer.creations==0&&eq.frequencies==null,"idle constructs no dummy/global audio effect");eq.setPreset(3);check(eq.preset()==3&&Equalizer.creations==0,"idle preset edits persist without effect or playback");eq.attach(0);eq.attach(-1);check(Equalizer.creations==0,"invalid/global sessions never attached");
 eq.attach(47);check(Equalizer.lastSession==47&&eq.frequencies.length==5,"discovers real session bands even when EQ disabled");check(eq.effect==null&&Equalizer.last.released,"disabled discovery releases temporary effect");check(eq.error.isEmpty(),"disabled state is not an error");
 prefs.edit().putBoolean("eqEnabled",true).apply();eq.attach(48);check(eq.effect!=null&&eq.effect.enabled&&Equalizer.lastSession==48,"enabled EQ attaches and enables on real playback session");for(short gain:eq.effect.levels)check(gain<=0&&gain>=-1500,"saved cut-only preset applied to hardware bands");
 eq.setCustom(2,2.5f);check(eq.preset()==6&&eq.effect.levels[2]==250,"custom gain applied and remembered");Equalizer prior=eq.effect;eq.attach(49);check(prior.released&&eq.effect!=prior&&Equalizer.lastSession==49,"track/session changes release previous EQ");check(eq.effect.levels[2]==250,"custom setting survives session change");
 eq.close();check(eq.effect==null&&Equalizer.last.released,"stop releases EQ");check(eq.frequencies.length==5,"known device bands remain editable while idle");
 Equalizer.failCreate=true;eq.attach(50);check(eq.effect==null&&!eq.error.isEmpty(),"unsupported native EQ reports actionable real-session failure");Equalizer.failCreate=false;eq.attach(50);check(eq.effect!=null&&eq.error.isEmpty(),"manual/new session reconnect recovers");
 Equalizer.noControl=true;eq.attach(51);check(eq.effect==null&&Equalizer.last.released&&!eq.error.isEmpty(),"control conflict cleans up effect");Equalizer.noControl=false;
 Equalizer.bandResult=-5;eq.attach(52);check(eq.effect==null&&!eq.error.isEmpty(),"band failure is not overwritten by attach success");Equalizer.bandResult=0;
 Equalizer.enableResult=-5;eq.attach(53);check(eq.effect==null&&!eq.error.isEmpty(),"enable failure is not reported as connected");Equalizer.enableResult=0;eq.attach(54);check(eq.error.isEmpty()&&eq.effect!=null,"recovery clears error");
 Equalizer.failDisable=true;Equalizer closing=eq.effect;eq.close();check(closing.released&&eq.effect==null,"release still runs when disable throws");Equalizer.failDisable=false;
 prefs.edit().putBoolean("eqEnabled",false).apply();eq.attach(55);check(eq.effect==null&&eq.error.isEmpty()&&!eq.enabled(),"disabling releases without changing saved enabled flag");
 System.out.println("PASS: "+checks+" actual NativeEq checks: idle presets, real/disabled session discovery, band application, session transitions, native failures and release safety");System.out.println("NOTE: deterministic AudioEffect doubles, not device EQ support or audible DSP verification.");
 }}'''
}
source=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'equalizer.probe(' not in source and 'eqProbePending' not in source
assert 'Start a song or station to detect' in source
assert 'Reconnect equalizer' in source and 'if(player!=null&&prepared)' in source
assert 'if(settingsOpen&&!eqSettingsState.equals(equalizerUiState()))rebuild=true;' in source
native=Path('app/src/com/luma/media/NativeEq.java').read_text()
assert 'generateAudioSessionId' not in native
service=Path('app/src/com/luma/media/PlaybackService.java').read_text()
assert 'mp.start();playing=true;equalizer.attach(mp.getAudioSessionId());' in service
with TemporaryDirectory(prefix='tuhkar-native-eq-') as tmp:
    root=Path(tmp);files=[]
    for name,text in stubs.items():
        p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(str(p))
    subprocess.run(['javac','-encoding','UTF-8','-d',str(root/'classes'),*files,str(Path('app/src/com/luma/media/NativeEq.java').resolve())],check=True)
    subprocess.run(['java','-cp',str(root/'classes'),'com.luma.media.NativeEqContract'],check=True)
print('PASS: no idle dummy-session probe/retry loop; idle preset UI, playback-only reconnect and capability/error-driven Settings refresh')
