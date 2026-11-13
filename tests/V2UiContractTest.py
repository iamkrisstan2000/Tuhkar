# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""V2 source-wiring regressions; host checks are not Android layout/touch tests."""
from pathlib import Path
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'for(int i=0;i<4;i++)' in s and 'i==1+tab/2' in s
assert 'String label=i==0?"Home":i==1?"Music":i==2?"Video":"Settings"' in s
assert 'int first=tab<2?0:2;' in s and 'index<first+2' in s
assert 'audioSourceTab' in s and 'videoSourceTab' in s
assert 'remembered>=fallback&&remembered<=fallback+1?remembered:fallback' in s
assert 'selectDestination(target)' in s
assert 'if(tab<2)addAudioMiniPlayer(playerBox);else addInlineControls()' in s
mini=s[s.index('void addAudioMiniPlayer('):s.index('void addPlaybackControls(')]
assert 'card.setOnClickListener(v->openExpanded())' in mini
assert 'playButton=holdingPlay(false,false)' in mini
assert 'transport("›","Next",()->next(1))' in mini
assert 'new SpinningThumbnailView(this)' in mini and 'nowTitle.setSingleLine(true)' in mini
assert '!playback.live()' in mini and 'playback.seekTo((long)(playback.duration()*fraction))' in mini
assert 'new MiniPlayerCard(this,panel,accent,' in mini
assert '"LIVE · "+status' in mini
assert 'gridColumns=mode==1?2:3' in s and 'int maxColumns=layoutMode()==1?2:3' in s
assert 'boolean tiles=mode==2' in s and '"2 column list"' in s
assert 'prefs.getInt("layoutMode",3)' in s # keep old preference index; small grid becomes new list
assert 'Grid small' not in s
assert 'miniCard.media(position,duration,playing&&appVisible,live,prepared)' in s
card=Path('app/src/com/luma/media/MiniPlayerCard.java').read_text()
assert 'extends FrameLayout' in card
assert 'ValueAnimator.areAnimatorsEnabled()' in card
assert 'isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE' in card
assert 'postInvalidateDelayed(40)' in card
assert 'Visualizer' not in card and 'RECORD_AUDIO' not in card
assert 'WaveProgressMath.fraction(' in card and 'controls(playButton,next)' in s
print('PASS: V2 grouped top/bottom navigation, remembered source guards, mini-player card/controls, live-vs-finite seek policy, two-column preference continuity, whole-card progress and accessible seek actions and animation bounds wiring')
print('NOTE: source checks, not on-device screenshots, touch dispatch, TalkBack or real streaming behavior.')
