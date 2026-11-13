# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
from pathlib import Path
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'miniArtwork=new SpinningThumbnailView(this)' in s
assert '((SpinningThumbnailView)miniArtwork).bindUrl(url)' in s
assert '((SpinningThumbnailView)miniArtwork).playback(current!=null,playing&&appVisible&&android.animation.ValueAnimator.areAnimatorsEnabled())' in s
assert '((SpinningThumbnailView)miniArtwork).playback(false,false)' in s
assert '((SpinningThumbnailView)holder.image).playback(' not in s
assert 'movingArt' not in s
assert 'configureArtwork((SpinningThumbnailView)holder.image)' in s # retain list art click/doubletap/save gestures
card=Path('app/src/com/luma/media/MiniPlayerCard.java').read_text()
assert 'LiquidFlowMath.edge(' in card and 'liquid.close()' in card
assert 'LiquidContrastStyle.fill(base,accent)' in card and 'LiquidContrastStyle.edge(base,accent)' in card
assert 'getHeight()-3*density' not in card # no old bottom wave stroke
assert 'if(live)' in card and 'RadialGradient' in card
print('PASS: mini-player-only thumbnail spin, pause/background/reduced-motion guards, static library gesture continuity and minimal liquid fill wiring')
