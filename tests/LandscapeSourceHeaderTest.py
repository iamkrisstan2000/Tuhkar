# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
from pathlib import Path
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'activeSourceTab=null;' in s
assert 'choice.setSelected(selected);if(selected)activeSourceTab=choice;' in s
rgb=s[s.index('void applyAccentWave()'):s.index('void about()')]
assert 'colorBackground(activeSourceTab,accent);activeSourceTab.setTextColor(accentInk)' in rgb
assert 'render()' not in rgb and 'setSurface' not in rgb
assert 'railLogo.setImageResource(R.mipmap.ic_launcher_round)' in s
assert s.index('nav.addView(railLogo,logoParams)')<s.index('host.addView(scroll,new LinearLayout.LayoutParams(dp(80),-1));buildNav()')
assert 'if(!landscape){HeaderWordmark logo=' in s
assert 'if(tab<2)addSourceTabs(brand,true)' in s
assert 'if(!(landscape&&tab<2))addSourceTabs(content)' in s
assert 'if(landscape&&tab>=2){playerBox.setGravity(Gravity.CENTER_VERTICAL)' in s
assert 'else if(landscape){playerBox.setPadding(0,dp(8),0,0);content.addView(playerBox,new LinearLayout.LayoutParams(-1,-2));}' in s
assert 'body.addView(playerBox,pp)' in s
assert 'if(tab<2)addAudioMiniPlayer(playerBox);else addInlineControls();' in s
print('PASS: RGB selected source tracking without rebuild, original app icon rail placement, compact landscape audio header, full-width library/under-list mini-player and retained video side pane')
print('NOTE: source wiring, not phone pixel/layout/audio-continuity verification.')
