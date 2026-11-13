# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
from pathlib import Path
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'int mediaSearchScope(){return homeOpen?0:tab<2?1:2;}' in s
assert 'addMediaSearchIcon(brand);addMediaSearch(content)' in s
assert 'addMediaSearchIcon(branding);parent.addView(branding);addMediaSearch(parent)' in s
assert 'mediaSearchBox.setVisibility(mediaSearchVisible?View.VISIBLE:View.GONE)' in s
assert 'SearchPolicy.IDLE_MS-(SystemClock.uptimeMillis()-searchLastTouch)' in s
assert 'searchLastTouch=SystemClock.uptimeMillis();scheduleMediaSearchIdle();' in s
hide=s[s.index('    void hideMediaSearchField()'):s.index('    void touchMediaSearch()')]
assert 'mediaSearchBox.setVisibility(View.GONE)' in hide
assert 'searchResults.clear' not in hide and 'mediaSearchResults.setVisibility' not in hide
assert 'touchMediaSearch();searchEpoch++' in s
assert 'main.postDelayed(mediaSearchPending,450)' in s
assert s.count('if(destroyed||epoch!=searchEpoch)return')==2
assert 'searchLocalItems(0,music,musicOnly)' in s
assert 'searchLocalItems(2,video,moviesOnly)' in s
assert 'playHomeItem(searchResults.get(i),new ArrayList<>(searchResults))' in s
feed=Path('app/src/com/luma/media/SearchFeed.java').read_text()
assert 'RadioDirectory.path(query,fm)' in feed and 'count<3000' in feed and 'i<150' in feed
assert 'result.note+=' in feed and 'SearchPolicy.allows(scope,"radio")' in feed and 'SearchPolicy.allows(scope,"tv")' in feed
assert 'new int[]{0xFFF4C2D7,Color.WHITE,0xFFB0D8ED}' in s
assert 'color==accent&&"cutie".equals(accentStyle())' in s
assert '"Cutie"' in Path('app/src/com/luma/media/AccentStyle.java').read_text()
print('PASS: search icon/panel wiring, 30s field-only hiding, retained results, debounce/stale guards, permission-scoped local scan, bounded online search and Cutie selected-control gradient')
print('NOTE: source contracts, not real keyboard/touch timing, live directory availability or Android gradient screenshots.')
import tempfile,subprocess
hide=s[s.index('    void hideMediaSearchField()'):s.index('    void addMediaSearchIcon(')]
# The actual methods use Android classes; equivalent minimal host doubles track operations.
hide=hide.replace('android.view.inputmethod.InputMethodManager','Keyboard')
java='''public class SearchIdle {
static class View {static final int GONE=8;int visibility;void setVisibility(int v){visibility=v;}}
static class Field {void clearFocus(){}Object getWindowToken(){return null;}}
static class Keyboard {void hideSoftInputFromWindow(Object token,int flags){}}
static class SystemClock {static long now;static long uptimeMillis(){return now;}}
static class SearchPolicy {static final long IDLE_MS=30000;}
static class Handler {long delay=-1;void removeCallbacks(Runnable r){delay=-1;}void postDelayed(Runnable r,long ms){delay=ms;}}
final Handler main=new Handler();final Runnable hideMediaSearch=()->{};static final String INPUT_METHOD_SERVICE="keyboard";
boolean mediaSearchVisible=true;long searchLastTouch;View mediaSearchBox=new View();Field mediaSearchField=new Field();
void updateLibrarySearchLayout(){}
String mediaSearchQuery="hello";int results=12;Object getSystemService(String id){return new Keyboard();}
'''+hide+'''
public static void main(String[] args){SearchIdle p=new SearchIdle();SystemClock.now=100;p.touchMediaSearch();if(p.main.delay!=30000)throw new AssertionError("30 second deadline");
SystemClock.now=10100;p.scheduleMediaSearchIdle();if(p.main.delay!=20000)throw new AssertionError("Render does not reset idle");
p.touchMediaSearch();if(p.main.delay!=30000)throw new AssertionError("Interaction resets idle");
p.hideMediaSearchField();if(p.mediaSearchVisible||p.mediaSearchBox.visibility!=8||!p.mediaSearchQuery.equals("hello")||p.results!=12)throw new AssertionError("Hide must preserve results");
p.mediaSearchBox=null;p.mediaSearchField=null;p.hideMediaSearchField();
System.out.println("PASS: actual search idle/hide methods: 30s, interaction reset, render retains deadline, manual/timeout hide preserves query/results, null view guards");
}}
'''
with tempfile.TemporaryDirectory(prefix='tuhkar-search-idle-') as d:
 p=Path(d)/'SearchIdle.java';p.write_text(java);subprocess.run(['javac',str(p)],check=True);subprocess.run(['java','-cp',d,'SearchIdle'],check=True)
assert 'searchLibrary.setVisibility(active?View.GONE:View.VISIBLE)' in s
assert 'mediaSearchResults.setLayoutParams(active?new LinearLayout.LayoutParams(-1,0,1)' in s
