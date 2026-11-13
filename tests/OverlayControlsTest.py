# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Overlay lifecycle/timer wiring, not device touch validation."""
from pathlib import Path
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'inlineVideoStage.addView(inlineControls,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM))' in s
assert 'addPlaybackControls(inlineControls,false)' in s
assert 'main.postDelayed(hideInlineControls,5000)' in s and 'main.postDelayed(hideTV,5000)' in s
assert '4000' not in s
assert 'onSingleTapConfirmed(MotionEvent event){revealInlineControls();return true;}' in s
assert 'surface.setOnClickListener(v->revealTV())' in s
assert 'main.removeCallbacks(hideInlineControls)' in s[s.index('@Override public void onStop()'):]
assert 'inlineControls.touching||dragging' in s and 'isHolding()' in s
assert 'if(!touchExploration())inlineControls.setVisibility(View.GONE)' in s
assert 'if(appVisible&&!touchExploration())main.postDelayed(hideTV,5000)' in s
updates=s[s.index('void updatePlayer()'):s.index('String format(')]
assert 'postDelayed(hideTV' not in updates and 'revealTV()' not in updates
assert 'new FrameLayout.LayoutParams(fittedW,fittedH,Gravity.CENTER)' in s
assert 'boolean visibleFm=miniCard!=null' in s and '(visibleDisc||visibleFm)&&playing' in s
assert 'miniCard.energy((glowLow+glowMid+glowHigh)/3f)' in s
assert 'prefs.getBoolean("reactiveDiscGlow",true)' in s
assert 'if(!allowed(Manifest.permission.RECORD_AUDIO))' in s
assert 'only on its own playback session' in s
print('PASS: overlay placement, five-second touch-only reset, paused hiding, hold/drag/TalkBack guards, background cleanup, aspect sizing and real FM playback-session analysis wiring')
