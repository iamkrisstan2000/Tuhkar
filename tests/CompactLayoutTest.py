# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Spacing/density source regressions; not an on-device layout measurement."""
from pathlib import Path
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'LinearLayout tabs=row();tabs.setPadding(0,dp(8),0,0);' in s
assert 'mediaSearchBox=row();padding(mediaSearchBox,0,8);' in s
assert 'mediaSearchBox.addView(mediaSearchField,new LinearLayout.LayoutParams(-1,dp(46)))' in s
assert '"Disc swipe","2 column list","Grid","List view · up to 10"' in s
assert 'Grid big' not in s
assert 'int rows=mode==1?8:' in s
assert 'mode==1?Math.max(dp(52),(int)(30*scale)+dp(8))' in s
assert 'padding(r,tiles?4:mode==1?6:8,tiles?4:mode==1?4:6)' in s
assert 'dp(mode==1?32:62)' in s
assert 'padding(words,mode==1?8:10,0)' in s
assert 'gridColumns=mode==1?2:3' in s
print('PASS: toggleable Search 8dp vertical gaps, unchanged Search height, plain Grid label and compact font-aware two-column row geometry')
print('NOTE: phone/font-scale/landscape visual verification remains required.')
