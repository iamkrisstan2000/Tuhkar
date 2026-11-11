# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Fullscreen wiring regression checks; real device gestures/decoding still required."""
from pathlib import Path
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'boolean fullscreenVideoAvailable(){return current!=null&&(current.kind.equals("video")||current.kind.equals("tv"));}' in s
assert 'if(tvMode&&!fullscreenVideoAvailable()){exitTV();return;}' in s
inline=s[s.index('void installInlineVideoGesture()'):s.index('void enterTV()')]
assert 'onDoubleTap' in inline and 'if(fullscreenVideoAvailable()){preview.performClick();return true;}' in inline
assert 'Open video fullscreen' in inline
entry=s[s.index('void enterTV()'):s.index('final Runnable hideTV')]
assert 'fullscreenWasExpanded=expanded' in entry and 'SCREEN_ORIENTATION_SENSOR_LANDSCAPE' in entry
assert 'setRequestedOrientation(priorOrientation)' in entry
assert 'expanded=fullscreenWasExpanded&&fullscreenVideoAvailable()' in entry
assert 'play(' not in entry and 'seekTo(' not in entry and 'pause(' not in entry
full=s[s.index('void renderTV()'):s.index('void closeSettings()')]
assert 'onSingleTapConfirmed' in full and 'surface.performClick()' in full
assert 'onDoubleTap(MotionEvent event){if(tvMode)exitTV();return true;}' in full
assert 'surface.setOnTouchListener(fullscreenTouch);tvStage.setOnTouchListener(fullscreenTouch)' in full
assert 'detachVideoOutput(fullscreenPicture)' in full and 'attachVideoOutput()' in full
assert 'if(tvMode){exitTV();return;}' in s[s.index('@Override public void onBackPressed()'):]
print('PASS: local-video/TV fullscreen entry, return gesture, confirmed single-tap controls, orientation/expanded-state restoration and no playback restart wiring')
print('NOTE: source-level checks, not actual Android touch/surface/orientation validation.')
