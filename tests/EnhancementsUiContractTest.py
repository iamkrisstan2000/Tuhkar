# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Compile actual video output guards against doubles; audit new UI resource wiring.
Not an Android GPU, launcher, accessibility or audible-stream test.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
import xml.etree.ElementTree as ET
source=Path('app/src/com/luma/media/MainActivity.java').read_text()
def method(name):
    start=source.index(name);start=source.rfind('\n',0,start)+1
    opening=source.index('{',start);depth=1;end=opening+1
    while depth:
        depth+=(source[end]=='{')-(source[end]=='}');end+=1
    return source[start:end]
methods='\n'.join(method(name) for name in ['void clearVideoOutputCache(', 'void detachVideoOutput(', 'boolean inlineVideoVisible(', 'void attachVideoOutput('])
harness='''
public class VideoBrowsingTest {
 static class View {}
 static class Surface {boolean isValid(){return true;}}
 static class RoundedVideoView extends View {Surface value=new Surface();Surface output(){return value;}}
 static class SurfaceView extends View {Holder getHolder(){return new Holder();}}
 static class Holder {Surface getSurface(){return new Surface();}}
 static class Player {int binds;void setSurface(Surface s){binds++;}void setDisplay(Holder h){binds++;}}
 static class Item {String kind;Item(String k){kind=k;}}
 boolean settingsOpen,setupOpen,isVideo,appVisible=true,tvMode,expanded;
 int tab;
 Item current;
 View surface,attachedVideoView;
 Player player,attachedVideoPlayer;
 Surface attachedVideoSurface;
 static int checks;
 static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 METHODS
 public static void main(String[] args){
  VideoBrowsingTest t=new VideoBrowsingTest();t.player=new Player();t.current=new Item("tv");t.isVideo=true;t.tab=3;t.surface=new RoundedVideoView();
  check(t.inlineVideoVisible(),"TV preview restored");t.attachVideoOutput();check(t.player.binds==1,"initial output bound once");t.attachVideoOutput();check(t.player.binds==1,"cached surface not rebound");
  Surface shared=((RoundedVideoView)t.surface).value;
  for(int cycle=0;cycle<25;cycle++)for(int tab:new int[]{0,1,2,3}){
   View old=t.surface;t.tab=tab;RoundedVideoView next=new RoundedVideoView();next.value=shared;t.surface=next;
   check(t.inlineVideoVisible(),"v1.0.0 preview stays visible across all four sections");t.attachVideoOutput();check(t.player.binds==1,"same retained output never rebinds decoder during section change");check(t.attachedVideoView==next,"cached view ownership follows transfer");t.detachVideoOutput(old);check(t.player.binds==1,"stale old-view detach cannot disconnect current output");
  }
  t.settingsOpen=true;t.attachVideoOutput();check(!t.inlineVideoVisible()&&t.player.binds==1,"Settings retains original no-preview layout");t.settingsOpen=false;
  t.setupOpen=true;t.attachVideoOutput();check(!t.inlineVideoVisible()&&t.player.binds==1,"setup never attaches preview");t.setupOpen=false;
  t.current.kind="video";for(int tab=0;tab<4;tab++){t.tab=tab;check(t.inlineVideoVisible(),"v1.0.0 local-video preview behavior also preserved");}
  t.current.kind="music";t.isVideo=false;check(!t.inlineVideoVisible(),"audio has no inline video");t.attachVideoOutput();check(t.player.binds==1,"audio never receives surface");
  t.current.kind="tv";t.isVideo=true;t.tvMode=true;t.surface=new SurfaceView();t.attachVideoOutput();check(t.player.binds==2,"fullscreen SurfaceView still attaches");
  t.tvMode=false;t.expanded=true;t.surface=new RoundedVideoView();t.attachVideoOutput();check(t.player.binds==3,"expanded video still attaches");
  t.appVisible=false;t.surface=new RoundedVideoView();t.attachVideoOutput();check(t.player.binds==3,"background Activity never binds");
  System.out.println("PASS: "+checks+" actual video visibility/output assertions: v1.0.0 all-section previews, 100 section transfers without decoder rebinds, stale-view safety, Settings/setup/background gates and fullscreen/expanded output.");
 }

}
'''.replace('METHODS',methods)
with TemporaryDirectory(prefix='tuhkar-video-browsing-') as temp:
    root=Path(temp);file=root/'VideoBrowsingTest.java';file.write_text(harness)
    subprocess.run(['javac','-d',str(root),str(file)],check=True)
    subprocess.run(['java','-cp',str(root),'VideoBrowsingTest'],check=True)
assert source.count('videoBox.setVisibility(inlineVideoVisible()?View.VISIBLE:View.GONE)')==2
assert 'videoOutput.setRetaining(appVisible&&isVideo' in method('void render(')
assert 'new RoundedVideoView(this,videoOutput)' in method('void addRoundedPicture(')
assert method('void addRoundedPicture(').index('detachVideoOutput(picture)') < method('void addRoundedPicture(').index('attachVideoOutput()')
assert 'videoOutput.setRetaining(false)' in method('void onStop(')
assert 'videoOutput.setRetaining(false)' in method('void onDestroy(')
switch=method('void switchTab(')
assert not any(word in switch for word in ['.play(','.pause(','.seekTo(','.release('])
assert 'audioEnhancementsSettings(page)' in method('void renderSettings(')
assert 'equalizerSettings(card)' in method('void audioEnhancementsSettings(')
actions=method('void setupAction(')
assert '"Back".contentEquals(action.getText())' in actions
assert '"Skip Setup".contentEquals(action.getText())' in actions
assert 'semibold(action);else bold(action)' in actions
knob=Path('app/src/com/luma/media/MinimalKnob.java').read_text()
for feature in ['ACTION_CANCEL','requestDisallowInterceptTouchEvent(false)','performClick()','ACTION_SET_PROGRESS','RangeInfo.obtain','ACTION_SCROLL_BACKWARD','KEYCODE_DPAD_UP','KEYCODE_DPAD_DOWN']:
    assert feature in knob
assert 'fade?2000:100,fade?100:1' in source
android='{http://schemas.android.com/apk/res/android}'
adaptive=ET.parse('app/res/mipmap-anydpi-v33/ic_launcher.xml').getroot()
assert adaptive.find('monochrome').attrib[android+'drawable']=='@drawable/ic_monochrome'
assert ET.parse('app/res/drawable/ic_monochrome.xml').getroot().tag=='vector'
assert Path('app/res/mipmap-anydpi-v26/ic_launcher.xml').exists()
print('PASS: Audio Enhancements/EQ wiring, bold/semibold setup actions, bounded native knob input/accessibility paths, Android 13+ monochrome adaptive icon and existing adaptive fallback')
print('NOTE: host doubles/structural checks; actual TV audio/video continuity, launcher tint, knob touch/accessibility and visual layout require Android device testing.')
