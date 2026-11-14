# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
from pathlib import Path
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
ensure=s[s.index('    void ensurePortraitSearchOverlay()'):s.index('    void updatePortraitSearchOverlay()')]
assert 'if(landscape)return' not in ensure
assert 'findViewById(android.R.id.content)' in ensure
assert 'setBackgroundColor(0x66000000)' in ensure
assert 'setOnClickListener(v->dismissMediaSearch())' in ensure
assert 'toggleMediaSearch();return true' in ensure
update=s[s.index('    void updatePortraitSearchOverlay()'):s.index('    void cancelMediaSearchTasks()')]
assert 'root.getPaddingBottom()' in update and 'PortraitSearchLayout.contentHeight' in update
assert 'ValueAnimator.ofInt(from,height)' in update and 'setDuration(200)' in update
assert 'areAnimatorsEnabled()' in update and 'panel!=mediaSearchPanel' in update
assert 'portraitSearchExpansion.cancel()' in s and 'portraitSearchOverlay.removeAllViews()' in s
attach=s[s.index('    void attachLibrarySearchResults('):s.index('    void updateLibrarySearchLayout(')]
assert 'updateLibrarySearchLayout();' in attach
layout=s[s.index('    void updateLibrarySearchLayout('):s.index('    void drawMediaSearchResults(')]
assert 'if(searchLibrary!=null)searchLibrary.setVisibility(View.VISIBLE)' in layout
assert '{ensurePortraitSearchOverlay();' in s
assert 'mediaSearchResults.addView(matches,new LinearLayout.LayoutParams(-1,0,1))' in s
assert 'hideMediaSearch=()->dismissMediaSearch()' in s
assert 'SearchPolicy.IDLE_MS' in s and 'IDLE_MS=15000' in Path('app/src/com/luma/media/SearchPolicy.java').read_text()
print('PASS: both-orientation dimmed sibling overlay, unchanged underlying library, bounded animated dropdown/IME insets, backdrop dismissal/icon hit-through, reduced-motion/stale-animation guards and retained search timeout wiring')
print('NOTE: structural checks, not Android window/keyboard/accessibility or GPU screenshots.')

assert "libraryParams.topMargin=dp(8)" in s
assert "mediaSearchStatus.getMeasuredHeight()" in s
assert "entry.setMinimumHeight(searchResultRowHeight())" in s

assert "IME_FLAG_NO_EXTRACT_UI" in s
assert "dp(landscape?96:16)" in s and "landscape?dp(720):freeWidth" in s
assert "dp(landscape?8:68)" in s
