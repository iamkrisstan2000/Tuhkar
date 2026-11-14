# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
from pathlib import Path
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'int mediaSearchScope(){return homeOpen?0:tab<2?1:2;}' in s
assert 'content.addView(brand);addMediaSearch(content)' in s
assert 'attachLibrarySearchResults(content)' in s
assert 'addMediaSearchIcon(branding);parent.addView(branding);addMediaSearch(parent)' in s
assert 'mediaSearchBox.setVisibility(mediaSearchVisible?View.VISIBLE:View.GONE)' in s
assert 'SearchPolicy.IDLE_MS-(SystemClock.uptimeMillis()-searchLastTouch)' in s
assert 'searchLastTouch=SystemClock.uptimeMillis();scheduleMediaSearchIdle();' in s
hide=s[s.index('    void hideMediaSearchField()'):s.index('    void dismissMediaSearch()')]
assert 'box.setVisibility(View.GONE)' in hide
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
print('PASS: search icon/panel wiring, 15s search timeout and graceful field hiding, retained results, debounce/stale guards, permission-scoped local scan, bounded online search and Cutie selected-control gradient')
print('NOTE: source contracts, not real keyboard/touch timing, live directory availability or Android gradient screenshots.')
render=s[s.index('        LinearLayout brand=row();'):s.index('    void openHome(){')]
assert render.index('content.addView(brand);addMediaSearch(content)')<render.index('if(!landscape){headline=')<render.index('attachLibrarySearchResults(content)')<render.index('LinearLayout library=column()')
layout=s[s.index('    void updateLibrarySearchLayout()'):s.index('    void drawMediaSearchResults()')]
assert 'updatePortraitSearchOverlay();' in layout
assert 'mediaSearchResultsHost' not in s
intro=s[s.index('    void homeIntro('):s.index('    void playHomeSongPicks(')]
assert 'if(!landscape){HeaderWordmark wordmark=' in intro
assert 'else branding.addView(welcome,new LinearLayout.LayoutParams(0,-2,1))' in intro
assert 'if(!landscape)parent.addView(welcome)' in intro
assert 'welcome.addView(greeting)' in intro and 'welcome.addView(callsign)' in intro
assert 'actions.setPadding(0,dp(landscape?10:18)' in intro
assert 'railLogo.setImageResource(R.mipmap.ic_launcher_round)' in s
print('PASS: search field precedes Music/Video heading in portrait, separate weighted results host occupies library slot; landscape Home wordmark removed but portrait wordmark and rail icon retained')
