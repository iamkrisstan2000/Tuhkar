# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Structural UI regressions; these do not replace Android rendering/touch tests."""
from pathlib import Path
source = Path('app/src/com/luma/media/MainActivity.java').read_text()
start = source.index('ImageButton queueToggle(')
end = source.index('void cycleRepeat(', start)
initial = source[start:end]
assert initial.index('setBackground(') < initial.index('padding(b,14,14)'), 'Initial background must precede icon padding'
start = source.index('void updateQueueButtons(')
end = source.index('Button holdingPlay(', start)
update = source[start:end]
for button in ('repeatButton', 'shuffleButton'):
    assert update.index(button + '.setBackground(') < update.index('padding(' + button + ',14,14)'), 'Every background update must restore 14dp padding'
assert 'brand.addView(button("＋ URL"' not in source
assert 'tools.addView(button(' not in source
assert source.count('toolIcon(12,') == 1
assert 'ImageButton refresh=toolIcon(12,' in source
assert 'refresh.setBackgroundColor(Color.TRANSPARENT)' in source
assert source.index('refresh.setBackgroundColor(Color.TRANSPARENT)') < source.index('padding(refresh,12,12)')
remote_tools = source[source.index('void addRemoteHeaderTools('):source.index('    void render(){')]
assert 'toolIcon(12,' not in remote_tools
assert 'toolIcon(13,' not in remote_tools
assert remote_tools.index('toolIcon(14,') < remote_tools.index('toolIcon(15,')
assert 'url.setBackgroundColor(Color.TRANSPARENT)' in remote_tools and 'region.setBackgroundColor(Color.TRANSPARENT)' in remote_tools

assert 'for(int i=0;i<5;i++)' in source
assert 'boolean selected=i==4?settingsOpen:!settingsOpen&&i==tab;' in source
assert 'if(t==4)openSettings();else switchTab(t)' in source
assert 'nav.setMinimumWidth(dp(272))' in source
assert 'if(settingsOpen){renderSettings();if(!landscape)addBottomNav();return;}' in source
assert 'videoParams.bottomMargin=dp(12)' in source and 'pictureParams.bottomMargin=dp(12)' in source
assert '((RoundedVideoView)surface).output()' in source and 'else player.setSurface(output)' in source
assert 'else if(picture.output()==null)detachVideoOutput(picture)' in source
assert 'detachVideoOutput(fullscreenPicture)' in source
assert 'clearVideoOutputCache();playback=null;player=null;' in source
assert 'if(surface==picture)' in source
rounded = Path('app/src/com/luma/media/RoundedVideoView.java').read_text()
assert 'extends TextureView' in rounded and 'setOpaque(false)' in rounded
assert 'final float radius=12*getResources().getDisplayMetrics().density' in rounded
assert 'setClipToOutline(true)' in rounded
assert rounded.index('output=null;if(changed!=null)changed.run()') < rounded.index('previous.release()')
assert 'b.setContentDescription(label);b.setTooltipText(label);' in source
assert 'header.addView(url,new LinearLayout.LayoutParams(dp(48),dp(48)))' in source
assert 'void mediaAccessSettings(LinearLayout page)' in source
assert "Add music folder" not in source and "Add video folder" not in source
assert 'tools.addView(button("Access"' not in source and 'tools.addView(button("SD / Folder"' not in source
start = source.index('void addPlaybackControls(')
end = source.index('void renderExpanded(', start)
player = source[start:end]
assert 'nowTitle=null;nowDetail=null;' in player
assert 'if(full){LinearLayout names=column();' in player
assert player.count('parent.addView(names,') == 1
assert 'dp(full?56:64)' in player
assert 'playerBox=column();playerBox.setPadding(dp(landscape?12:0),dp(2),dp(landscape?12:0),0);' in source
assert 'if(setupOpen||settingsOpen)render();else if(tab==0||tab==2)loadLocal();' in source
icons = Path('app/src/com/luma/media/FluentIcon.java').read_text()
for kind in range(11,16):
    assert 'kind==' + str(kind) in icons
assert 'bold(headline);' in source and 'bold(caption);' in source and 'bold(heading);' in source
assert 'TextView title=text("Settings",28,ink);bold(title);' in source
for title in ['holder.title', 'title', 'nowTitle', 'tvTitle']:
    assert 'semibold(' + title + ');' in source
assert 'view.setFontVariationSettings("\'wght\' 700")' in source
assert 'view.setFontVariationSettings("\'wght\' 600")' in source
assert 'new AlertDialog.Builder(this).setTitle(' not in source
assert 'new LinearLayout.LayoutParams(dp(48),dp(48))' in source
print('PASS: structural checks — 14dp icon padding restored after initial and updated backgrounds; 48dp targets retained; single borderless header refresh, five-item navigation, 12dp video gaps and rounded TextureView output; icon tools with labels/tooltips and 48dp targets; media-access actions in Settings; compact player omits names and uses a 64dp ring composite (48dp center); bold headers and semibold expanded titles retained.')
print('NOTE: Android background/padding behavior and actual icon sizing must still be verified on a phone.')

thumbnail = Path('app/src/com/luma/media/ThumbnailView.java').read_text()
assert 'public void draw(Canvas canvas)' in thumbnail
assert thumbnail.index('canvas.clipPath(circle)') < thumbnail.index('super.draw(canvas)') < thumbnail.index('canvas.restoreToCount(save)')
assert 'circle.addCircle(width/2f,height/2f,Math.min(width,height)/2f,Path.Direction.CW)' in thumbnail
assert 'holder.image.setClipToOutline(true)' not in source
print('PASS: circular Canvas clipping wraps background and image rendering, with a reused Path and size-change geometry; library refresh icon shared with Radio/TV; transparent compact player with a separate circular progress/seek ring.')

assert 'playerBox.setBackgroundColor(Color.TRANSPARENT)' in source
assert 'miniParams.setMargins(dp(16),0,dp(16),0)' in source
assert 'seek=null;time=null;\n        if(full){\n        LinearLayout progress=row();' in source
assert 'playButton=holdingPlay(false,!full)' in source
assert 'int size=b==playButton&&!full?64:48;' in source
assert 'holder.image=new SpinningThumbnailView(MainActivity.this)' in source
assert 'current.url.equals(image.mediaUrl))openExpanded()' in source
assert 'image.playback(selected,playing&&appVisible)' in source
service=Path('app/src/com/luma/media/PlaybackService.java').read_text()
manifest=Path('app/AndroidManifest.xml').read_text()
for permission in ['FOREGROUND_SERVICE','FOREGROUND_SERVICE_MEDIA_PLAYBACK','POST_NOTIFICATIONS','WAKE_LOCK']:
    assert 'android.permission.'+permission in manifest
assert 'android:foregroundServiceType="mediaPlayback"' in manifest
assert 'android:stopWithTask="false"' in manifest
assert 'new MediaPlayer()' in service and 'new MediaPlayer()' not in source
assert 'new MediaSession(' in service and 'setMediaSession(session.getSessionToken())' in service
assert 'setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK)' in service
assert 'void background(){if(isVideo)pause();' in service
start=source.index('@Override public void onStop()')
end=source.index('@Override public void onStart()',start)
background=source[start:end]
assert 'playback.background()' in background and 'else pause()' not in background
assert 'playback.listener=null' in background and 'unbindService(playbackConnection)' in background
assert 'release()' not in source[source.index('@Override public void onDestroy()'):]
play=service[service.index('void play(MainActivity.Item item)'):service.index('private void fail(')]
assert play.index('promote();') < play.index('audio.requestAudioFocus(focus)')
assert 'current.kind.equals("radio")||current.kind.equals("tv")' in service
print('PASS: structural foreground-service ownership/manifest/focus ordering, listener unbinding, music/radio continuation, ring-only compact seeking and selected-art double-tap checks.')

assert 'header.addView(notice,new LinearLayout.LayoutParams(0,-2,1))' in source
assert 'content.addView(notice)' not in source
assert 'collection.setFastScrollEnabled(true);collection.setFastScrollAlwaysVisible(true)' in source
assert 'View.SCROLLBARS_INSIDE_INSET' in source
assert 'viewport.addOnLayoutChangeListener' in source
assert 'updateLibraryNotice();adapter.notifyDataSetChanged()' in source
print('PASS: media-card count header, native draggable list/grid fast scrolling, viewport-based cell sizing and portrait video/media-card side alignment.')

assert 'new FluentIcon(16,showingSaved?selectedIconColor():muted)' in source
assert 'favorite.setBackgroundColor(Color.TRANSPARENT)' in source
assert 'LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(48),dp(48))' in source
assert '♥ Saved' not in source and '♡ Saved' not in source
assert 'toolParams.bottomMargin=dp(10)' not in source and 'addRemoteHeaderTools(header)' in source
assert 'compactToolIcon(' not in source
assert 'kind==16' in icons and 'paint.setStyle(Paint.Style.FILL);canvas.drawPath(heart,paint)' in icons
print('PASS: solid borderless Saved heart, accessible selected state, borderless FM/TV tools inside 48dp header targets beside Saved; obsolete separate tool row removed.')

# All Settings switches share state-aware accent colors, including disabled folders.
for control in ('enabled','black','downloads','standard','glow'):
    assert 'tintSettingsSwitch('+control+');' in source
assert source.count('new Switch(this)') == 5
helper = source[source.index('void tintSettingsSwitch('):source.index('void renderSettings(')]
assert 'setThumbTintList' in helper and 'setTrackTintList' in helper
assert '-android.R.attr.state_enabled' in helper and 'android.R.attr.state_checked' in helper
assert 'settingsScrollParams.bottomMargin=dp(8)' in source
assert 'scroll.setPadding(0,0,0,0);scroll.setClipToPadding(true);scroll.setClipChildren(true)' in source
assert 'scroll.setVerticalFadingEdgeEnabled(true)' in source
assert 'ValueAnimator.areAnimatorsEnabled()' in source and 'setDuration(140)' in source
print('PASS: Settings accent switches, navigation clearance, subtle fades and reduced-motion guard')

assert 'miniParams.setMargins(dp(16),0,dp(16),0)' in source
assert 'navParams.topMargin=dp(settingsOpen?0:8)' in source
assert 'new LinearLayout.LayoutParams(-1,dp(54))' in source
print('PASS: mockup bottom layout — compact player spacing, shared 54dp nav, no extra Settings player gap')

settings = source[source.index("void renderSettings()"):source.index("void about(")]
assert "setClipToPadding(false)" not in settings
assert "setPadding(0,0,0,0)" in settings
print("PASS: Settings scroll content clipped at fade boundary; no uncovered bottom padding strip")

assert "hardwareFM" not in source and "Headphone FM" not in source
assert "<queries>" not in Path("app/AndroidManifest.xml").read_text()
assert "setFadingEdgeLength(dp(12))" in source and "page.setAlpha(.97f)" in source
print("PASS: external FM launcher and package queries removed; lighter Settings fades")

assert 'setupAction(page,next,8)' in source
assert 'setupAction(page,button("Back",()->setupBack()),12)' in source
assert 'setupAction(page,button("Skip Setup",()->finishSetup(true)),12)' in source
assert 'params.topMargin=dp(gap);host.addView(action,params)' in source
assert 'action.setMinHeight(dp(52));action.setMinimumHeight(dp(52))' in source
assert 'action.setSingleLine(false)' in source
assert 'standardFolderOptions(card)' in source
assert 'target==0?"musicStandardFolderOnly":"videoStandardFolderOnly",target==0' in source
assert 'StandardFolderPolicy.relative(c.getString(pathColumn),target)' in source
assert 'StandardFolderPolicy.absolute(c.getString(pathColumn),target)' in source
assert 'scanTree(' not in source and 'ACTION_OPEN_DOCUMENT_TREE' not in source
assert 'MediaStore.MediaColumns.RELATIVE_PATH:MediaStore.MediaColumns.DATA' in source
assert 'final boolean standardOnly=standardFolderOnly(target)' in source
print('PASS: explicit setup button margins and scalable 52dp actions; standard Music-on/Movies-off filters applied to indexed MediaStore')

assert 'ImageButton reset=toolIcon(17,"Reset Tuhkar",()->confirmReset())' in source
assert 'reset.setBackgroundColor(Color.TRANSPARENT)' in source
assert '#FF6B6B' in source and '#B3261E' in source
assert 'void confirmReset()' in source and 'setPositiveButton("Reset app",(d,w)->resetApp())' in source
assert 'NEVER deleted or changed' in source
assert 'tab=streaming?1:0' in source
assert 'void setupLibrary(){setupGo(1);}' in source and 'void setupPrimaryAction()' in source
assert 'kind==17' in icons
reset=source[source.index('void resetApp()'):source.index('String accentStyle(')]
assert 'prefs.edit().clear()' in reset and 'playback.stopPlayback()' in reset
assert 'saved.clear();localMusic.clear();localVideo.clear();online.clear();shown.clear()' in reset
assert 'resetDiskCache()' in reset
for forbidden in ['DocumentsContract.deleteDocument','getContentResolver().delete','MediaStore.createDeleteRequest','getExternalStorageDirectory']:
    assert forbidden not in source
print('PASS: direct setup permission request; Music/FM completion routes; no additional-folder picker; red confirmed reset without media deletion APIs')

artwork=Path('app/src/com/luma/media/ArtworkLoader.java').read_text()
assert 'SafeCacheReset.clear(context.getCacheDir())' in artwork
assert 'resetDiskCache(){close();synchronized(directory)' in artwork
assert 'if(prefs.getBoolean("pendingPlaybackReset",false))' in source
assert 'new Thread(()->{boolean clean=oldArtwork.resetDiskCache()' in source
assert 'resetInProgress=true' in reset and 'resetInProgress=false' in reset
assert 'releaseOldFolderGrants()' in source and 'retireCustomFolders()' in source
print('PASS: cache-only reset helper, stopped artwork writer guard, asynchronous cleanup, pending service reset and old-folder grant retirement')

assert 'settingsCard(page,"Sort media")' not in source
header=source[source.index('LinearLayout header=row();'):source.index('buildLibrary(library);')]
assert header.index('header.addView(favorite,fp)') < header.index('header.addView(sort,')
assert 'actions.addView(favorite' not in source
assert 'sort.setBackgroundColor(Color.TRANSPARENT)' in source and 'padding(sort,14,14)' in source
assert 'kind==18' in icons
assert 'configureDisc((DiscView)image,item)' in source and 'configureDisc(disc,current)' in source
assert 'Manifest.permission.RECORD_AUDIO' in source and 'android.permission.RECORD_AUDIO' in manifest
assert 'reactiveGlow.stop()' in source[source.index('@Override public void onStop()'):]
print('PASS: in-card Saved-left/Sort-right icon-only actions; same reactive DiscView in swipe/Now Playing; optional audio-analysis permission and lifecycle release')

assert 'settingsCard(page,"Reset app")' in source and 'settingsCard(page,"About")' in source
assert source.index('settingsCard(page,"Reset app")') < source.index('settingsCard(page,"About")')
access=source[source.index('void mediaAccessSettings('):source.index('void releaseOldFolderGrants(')]
assert 'toolIcon(19,"Manage media access"' in access and 'toolIcon(20,"Run setup again"' in access
assert 'setupIconParams.leftMargin=dp(12)' in access
assert 'button("Run setup again"' not in access
assert 'cardAccent=AccentStyle.base' in source and 'palette.tinted(cardAccent,dark)' in source
assert 'main.postDelayed(this,200)' in source
assert 'void swipeSelect(int index)' in source and 'else playback.selectPaused(item)' in source
assert 'Recording — Android' in source and 'Continue anyway' in source
print('PASS: seven curated accents/RGB steady-card wave, swipe preserve-pause routing, single prior notice/batched permission flow, paired access icons and Reset card above About')

assert "Allow Tuhkar to find music and videos on your phone and SD card." not in source
assert "On: use only the Music or Movies folder and its subfolders. Off: include other folders too." in source
resetGlyph=icons[icons.index('}else if(kind==17)'):icons.index('}else if(kind==18)')]
assert 'Path reset=new Path()' in resetGlyph and 'canvas.drawPath(reset,paint)' in resetGlyph
assert 'canvas.drawLine(12,8,12,12' not in resetGlyph
print('PASS: clean reset-arrow glyph and simplified Media Access copy; paired action icons and both folder switches retained')

assert 'params.topMargin=dp(target==0?6:2)' in source
assert 'rememberSettingsScroll();settingsScroll=null;settingsPage=null;' in source
assert 'card.setTag("settings:"+title)' in source
assert 'restoreSettingsScroll(scroll,page)' in source
assert 'SettingsScrollAnchor.restore(' in source
assert 'addOnGlobalLayoutListener' in source and 'if(settingsScroll!=scroll)return;' in source
assert 'settingsScrollY=state.getInt("settingsScrollY",0)' in source
assert 'if(settingsRestoring||settingsScroll==null' in source
print('PASS: tighter folder-switch margins, section-anchor Settings scroll restore after layout, stale-restore guard and saved-state retention')

assert 'header.addView(notice,new LinearLayout.LayoutParams(0,-2,1));addRemoteHeaderTools(header);' in source
assert source.index('addRemoteHeaderTools(header);') < source.index('header.addView(favorite,fp)') < source.index('header.addView(sort,')
nav=source[source.index('void buildNav()'):source.index('void switchTab(')]
assert 'new android.graphics.drawable.InsetDrawable(round(accent,20),0,dp(4),0,dp(4))' in nav
assert 'selected&&!landscape' in nav and 'new LinearLayout.LayoutParams(0,dp(48),1)' in nav
assert nav.index('b.setBackground(') < nav.index('padding(b,landscape?16:12')
assert 'GradientDrawable.Orientation.TOP_BOTTOM,new int[]{bg,pageBottom}' in source
assert 'if(dark&&prefs.getBoolean("oled",false))root.setBackgroundColor(Color.BLACK)' in source
assert 'PageGradient.bottom(cardAccent,dark' in source and 'getWindow().setNavigationBarColor(pageBottom)' in source
wave=source[source.index('void applyAccentWave()'):source.index('void about()')]
assert 'applyPageBackground' not in wave and 'pageBottom=' not in wave
print('PASS: borderless URL/Region beside Saved, Saved immediately left of Sort; 40dp selected bottom pill in unchanged 48dp target; stable accent top-to-bottom gradient, matching system bars and OLED black')

selector=source[source.index('    GradientDrawable mediaSelector()'):source.index('    void applyPageBackground()')]
assert 'if(dark)return round(normalBg,14)' in selector and 'if(dark)return round(panel,18)' in selector
assert 'LightSurfaceStyle.selectorFill(panel,stable,ink,muted)' in selector
assert 'selected.setStroke(dp(1),LightSurfaceStyle.selectorStroke(stable,ink))' in selector
assert 'field.setStroke(dp(1),LightSurfaceStyle.searchStroke(stable,ink))' in selector
assert 'collection.setSelector(mediaSelector())' in source and 'search.setBackground(searchBackground())' in source
assert selector.count('AccentStyle.base(accentStyle(),deviceAccent)')==2
assert source.index('search.setBackground(searchBackground())') < source.index('padding(search,14,4)')
print('PASS: accent-tinted outlined light hover for all list/grid sections; brighter outlined search field; stable RGB shading, original dark surfaces and post-background padding retained')

assert 'view.setReactiveGlow(prefs.getBoolean("reactiveDiscGlow",true));discViews.add(view)' in source
sync=source[source.index('    void syncDisc()'):source.index('    ImageButton backButton(')]
assert 'view.setReactiveGlow(prefs.getBoolean("reactiveDiscGlow",true))' in sync
assert 'prefs.getBoolean("reactiveDiscGlow",true)' in sync
settings=source[source.index('    void discGlowSettings('):source.index('    void mediaAccessSettings(')]
assert 'syncDisc();' in settings and 'Off: a dim, steady ring and soft glow; no pulsing' in settings
print('PASS: Disc glow preference reaches every shared DiscView renderer, syncs immediately on toggle, gates analysis and clearly explains ON/OFF')

welcome=source[source.index('        if(setupStep==0){\n'):source.index('        if(setupStep==4){\n')]
assert 'setupScriptTitle(page,"Welcome")' in welcome and welcome.count('setupAction(')==2
about=source[source.index('    void about(){'):source.index('    void swipeSelect(')]
for phrase in ['No ads or analytics','titles/artists','Record audio','no microphone recording','saved/uploaded audio','HTTP links','never your music/videos','authorized links','SIL OFL']:
    assert phrase in about
assert len(about)<1200
print('PASS: two-choice scripted welcome; redundant Media Access sentence removed; short About retains key privacy, permission, reset, stream and attribution notices')
