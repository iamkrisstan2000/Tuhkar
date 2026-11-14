# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
from pathlib import Path
import subprocess,tempfile,xml.etree.ElementTree as ET
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
method=s[s.index('    void updateLibrarySearchLayout()'):s.index('    int searchResultRowHeight()')]
java='''public class SearchSpacing {
static class View {static final int VISIBLE=0,GONE=8;}
static class LinearLayout {int visibility=View.GONE;void setVisibility(int v){visibility=v;}}
boolean landscape,homeOpen,mediaSearchVisible;String mediaSearchQuery="";int updates;
LinearLayout searchLibrary=new LinearLayout();void updatePortraitSearchOverlay(){updates++;}
'''+method+'''
public static void main(String[] args){for(boolean landscape:new boolean[]{false,true})for(boolean visible:new boolean[]{false,true})for(boolean active:new boolean[]{false,true}){
SearchSpacing p=new SearchSpacing();p.landscape=landscape;p.mediaSearchVisible=visible;p.mediaSearchQuery=active?"song":"";p.updateLibrarySearchLayout();if(p.searchLibrary.visibility!=0||p.updates!=1)throw new AssertionError("Underlying library must remain visible with overlay");}
SearchSpacing p=new SearchSpacing();p.searchLibrary=null;p.homeOpen=true;p.updateLibrarySearchLayout();if(p.updates!=1)throw new AssertionError("Home overlay update");
System.out.println("PASS: actual layout method keeps underlying library visible with floating search in both orientations and all field/query states; Home/null guard");}}
'''
with tempfile.TemporaryDirectory() as d:
 p=Path(d)/'SearchSpacing.java';p.write_text(java);subprocess.run(['javac',str(p)],check=True);subprocess.run(['java','-cp',d,'SearchSpacing'],check=True)
assert 'updateLibrarySearchLayout();scheduleMediaSearchIdle();' in s[s.index('    void hideMediaSearchField()'):s.index('    void dismissMediaSearch()')]
service=Path('app/src/com/luma/media/PlaybackService.java').read_text()
assert service.count('PendingIntent.FLAG_IMMUTABLE')>=2
assert 'Context.RECEIVER_NOT_EXPORTED' in service and 'Build.VERSION.SDK_INT>=33' in service
assert 'Build.VERSION.SDK_INT>=33?allowed(Manifest.permission.READ_MEDIA_AUDIO)' in s
assert 'Build.VERSION.SDK_INT>=34&&allowed(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)' in s
assert 'WindowInsets.Type.ime()' in s and 'android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK' in Path('app/AndroidManifest.xml').read_text()
assert 'searchSongs.clear();searchVideos.clear();appVisible=true' in s
assert 'searchSongs.clear();searchVideos.clear();if(!mediaSearchQuery' in s
feed=Path('app/src/com/luma/media/SearchFeed.java').read_text()
assert 'cache.length()<=6000000' in feed and 'age<6*60*60*1000L' in feed
assert 'entry=(LinearLayout)convert;holder=(MediaRow)entry.getTag()' in s
print('PASS: Android12+ immutable playback intents, gated grants/receiver flags, media foreground-service declaration and IME insets retained; search snapshot invalidation, TV cache caps and recycled rows wired')
print('NOTE: compatibility source review, not device testing, performance benchmarking or newer-Android certification.')
