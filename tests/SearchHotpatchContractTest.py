# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
from pathlib import Path
import tempfile,subprocess
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
methods=s[s.index('    void hideMediaSearchField()'):s.index('    void addMediaSearchIcon(')]
methods=methods.replace('android.view.inputmethod.InputMethodManager','Keyboard').replace('android.animation.ValueAnimator','ValueAnimator')
java='''import java.util.*;public class Hotpatch {
static class View {static final int VISIBLE=0,GONE=8;}
static class ValueAnimator {static boolean enabled;static boolean areAnimatorsEnabled(){return enabled;}}
static class Animator {Runnable end;Animator alpha(float a){return this;}Animator translationY(float y){return this;}Animator setDuration(long d){return this;}Animator withEndAction(Runnable r){end=r;return this;}void start(){if(end!=null)end.run();}void cancel(){end=null;}}
static class LinearLayout {int visibility;Animator a=new Animator();Animator animate(){return a;}void setVisibility(int v){visibility=v;}int getVisibility(){return visibility;}void setAlpha(float f){}void setTranslationY(float f){}}
static class Field {void clearFocus(){}Object getWindowToken(){return null;}void setText(String s){}}
static class Keyboard {void hideSoftInputFromWindow(Object token,int flags){}}
static class SystemClock {static long now;static long uptimeMillis(){return now;}}
static class SearchPolicy {static final long IDLE_MS=15000;}
static class Handler {long delay=-1;void removeCallbacks(Runnable r){delay=-1;}void postDelayed(Runnable r,long ms){delay=ms;}}
final Handler main=new Handler();final Runnable hideMediaSearch=()->{};Runnable mediaSearchPending=()->{};static final String INPUT_METHOD_SERVICE="keyboard";
boolean mediaSearchVisible=true,suppressSearchEdits,searchLocalLoading=true,searchOnlineLoading=true;long searchLastTouch;LinearLayout mediaSearchBox=new LinearLayout();Field mediaSearchField=new Field();String mediaSearchQuery="hello",searchNote="note";int searchEpoch,cancels,draws,searches;ArrayList<String> searchResults=new ArrayList<>(Arrays.asList("result"));
Object getSystemService(String s){return new Keyboard();}int dp(int v){return v;}void updateLibrarySearchLayout(){}void cancelMediaSearchTasks(){cancels++;}void drawMediaSearchResults(){draws++;}void runMediaSearch(){searches++;}
'''+methods+'''
static void check(boolean v){if(!v)throw new AssertionError();}
public static void main(String[] args){for(boolean animate:new boolean[]{false,true}){ValueAnimator.enabled=animate;Hotpatch p=new Hotpatch();SystemClock.now=100;p.touchMediaSearch();check(p.main.delay==15000);SystemClock.now=5100;p.scheduleMediaSearchIdle();check(p.main.delay==10000);p.submitMediaSearch();check(!p.mediaSearchVisible&&p.mediaSearchBox.visibility==8&&p.searches==1&&p.searchResults.size()==1&&p.main.delay==15000);p.dismissMediaSearch();check(p.searchResults.isEmpty()&&p.mediaSearchQuery.isEmpty()&&p.cancels==1&&p.searchEpoch==1&&!p.searchLocalLoading&&!p.searchOnlineLoading&&p.main.delay==-1);p.mediaSearchBox=null;p.mediaSearchField=null;p.dismissMediaSearch();}
System.out.println("PASS: actual hotpatch methods: Enter hides/fades field but keeps results; 15s idle survives field hide, deadline tracks interaction; full dismiss cancels work/clears results; animation and reduced-motion paths");}}
'''
with tempfile.TemporaryDirectory() as d:
 p=Path(d)/'Hotpatch.java';p.write_text(java);subprocess.run(['javac',str(p)],check=True);subprocess.run(['java','-cp',d,'Hotpatch'],check=True)
back=s[s.index('    @Override public void onBackPressed()'):s.index('    @Override public void onConfigurationChanged(')]
assert back.index('dismissMediaSearch();return;')<back.index('openHome();return;')
assert 'final Runnable hideMediaSearch=()->dismissMediaSearch()' in s
assert 'hideMediaSearchField();playHomeItem(searchResults.get(i)' in s
assert 'setOnEditorActionListener' in s and 'KEYCODE_ENTER' in s
assert 'if(!landscape)addSourceTabs(content)' in s
assert 'brand.addView(headline,hp);addSourceTabs(brand,true)' in s
print('PASS: back-to-library priority, idle full-dismiss wiring, result-selection hide, Enter listener and compact landscape Video source tabs')
