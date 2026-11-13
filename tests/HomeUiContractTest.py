# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
from pathlib import Path
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'for(int i=0;i<4;i++)' in s
assert 'String label=i==0?"Home":i==1?"Music":i==2?"Video":"Settings"' in s
assert 'if(homeOpen){renderHome();return;}' in s
assert 'homeOpen=state.getBoolean("home",true)' in s and 'b.putBoolean("home",homeOpen)' in s
assert 'homeCallsign=state.getString("homeCallsign","")' in s
assert 'currentSection=settingsOpen?3:homeOpen?0:1+tab/2' in s
assert 'source==0?22:source==1?1:source==2?23:3' in s
assert 'choice.setCompoundDrawables(sourceIcon,null,null,null)' in s
assert 'activeSourceTab.setCompoundDrawableTintList' in s
assert 'if((expanded||homeOpen)&&previousVideo!=isVideo)rebuild=true' in s
assert 'if(current!=null&&isVideo){videoBox=column()' in s
assert 'HomeDiscovery.greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))' in s
assert 'LinearLayout columns=landscape?row():column()' in s
assert 'left.rightMargin=dp(12)' in s
assert 'if(homeLocalLoading||homeOnlineLoading)' in s and 'homePendingRefresh=true' in s and 'token!=homeEpoch' in s
assert 'SystemClock.elapsedRealtime()-homeLoadedAt<600000' in s
assert 'queue.reset(sameKind,item);start(item)' in s
assert 'setupOpen=false;settingsOpen=false;homeOpen=!streaming;' in s # completed setup Home / Skip-to-FM
feed=Path('app/src/com/luma/media/HomeFeed.java').read_text()
assert 'getContentResolver().query(collection,columns,null,null,"date_added DESC")' in feed
assert 'items.size()<5000' in feed and 'channels.size()<3000' in feed
assert 'StandardFolderPolicy.relative' in feed and 'if(!allowed)return items' in feed
assert 'ContentUris.withAppendedId' in feed and 'albumart/' in feed
assert 'lastchangetime_iso8601' in feed and 'not station news' in feed
assert 'if(stations.isEmpty())for' in feed and 'if(channels.isEmpty())for' in feed
assert 'FileOutputStream' not in feed and 'MediaRecorder' not in feed
icons=Path('app/src/com/luma/media/FluentIcon.java').read_text()
for kind in [21,22,23]:assert 'kind=='+str(kind) in icons
print('PASS: four-destination Home routing/state, source icons/RGB tint, bounded background discovery, permission/filter policy, same-kind tap queues, no autoplay and landscape/video-output integration wiring')
print('NOTE: source wiring, not real MediaStore, directory availability, Android touches or screenshots.')

intro=s[s.index('    void homeIntro('):s.index('    void playHomeSongPicks(')]
assert intro.index('HeaderWordmark')<intro.index('HomeDiscovery.greeting')<intro.index('semibold(callsign)')
assert 'landscape?24:28' in intro
assert 'Shuffle picks' in intro and 'Play picks' in intro
assert 'Collections.shuffle(choices)' in s
assert 'homeShelf(recent,' in s and 'homeShelf(updates,' in s
assert 'HorizontalScrollView shelf' in s and 'HomeShelfLayout.tileWidth' in s
assert 'homeItems(songs,' in s
assert 'homeShuffleButton.setCompoundDrawableTintList' in s
print('PASS: restored original wordmark above greeting, reduced semibold callsign, scrollable artwork shelves, compact song rows and explicit picks actions')

for title in ['Songs','FM','Video','TV']:
    assert 'homeSection(recommendations,"'+title+'",' in s
for old in ['Songs · 6 picks','FM · 2 picks','Video · 1 pick','TV · 1 pick']:
    assert old not in s
mini=s[s.index('    void updateAudioMiniPlayer('):s.index('    void addPlaybackControls(')]
assert 'miniCard.setVisibility(current==null?View.GONE:View.VISIBLE)' in mini
assert 'playerBox.setVisibility(current==null?View.GONE:View.VISIBLE)' in mini
assert 'setVisibility(playing?' not in mini
service=Path('app/src/com/luma/media/PlaybackService.java').read_text()
stop=service[service.index('    void stopPlayback()'):].split('\n',1)[0]
assert 'current=null' in stop and 'publish()' in stop
print('PASS: count-free Home section headings; idle/stopped mini-player and wrapper hidden, paused/loading current item retained; Stop clears/publishes current')

home=s[s.index('    void renderHome('):s.index('    void homeIntro(')]
assert 'padding(page,landscape?16:0,landscape?8:12)' in home
assert 'playerBox.setPadding(dp(landscape?0:16)' in home
assert 'padding(intro,landscape?0:16,0)' in s
assert 'padding(recommendations,landscape?0:16,0)' in s
assert 'int inset=!landscape&&browse!=null?dp(16):0' in s
assert 'note.setPadding(inset,dp(4),inset,dp(12))' in s
shelf=s[s.index('    void homeShelf('):s.index('    void homeItems(')]
assert 'parent.addView(shelf,new LinearLayout.LayoutParams(-1,-2))' in shelf
assert 'tp.rightMargin=item==items.get(items.size()-1)?0:dp(12)' in shelf
assert 'if(streaming)loadOnline();' in s
print('PASS: portrait shelf viewport edge-to-edge with inset intro/headings/recommendations/player, unchanged landscape insets; completed setup opens Home and skip opens FM')

assert 'cards.setPadding(dp(landscape?0:16),0,dp(landscape?0:16),0)' in shelf
assert 'shelf.setPadding(' not in shelf
print('PASS: portrait initial 16dp leading inset belongs to scrolling shelf content, not viewport; matching trailing content inset and no landscape shelf inset change')

assert 'columns.addView(recommendations,left);columns.addView(discoveries,new LinearLayout.LayoutParams(0,-2,1))' in s
assert 'else{columns.addView(discoveries);columns.addView(recommendations);}' in s
print('PASS: landscape Home columns swapped with recommendations left/discoveries right; portrait section order preserved')

assert 'shelf.setHorizontalFadingEdgeEnabled(landscape)' in shelf
assert 'shelf.setFadingEdgeLength(dp(24))' in shelf
print('PASS: landscape-only 24dp native horizontal fading edges on Home shelves; Android derives fade strength from scroll extent')

sourceTabs=s[s.index('    void addSourceTabs(LinearLayout host,boolean compact)'):s.index('    void buildNav(')]
assert sourceTabs.count('source==0?"Collections":source==1?"Internet Radio":source==2?"Collections":"Online TV"')==2
assert '()->switchTab(source)' in sourceTabs
assert 'source==0?22:source==1?1:source==2?23:3' in sourceTabs
assert 'Open Settings' not in intro and 'openSettings()' not in intro
assert 'Refresh Home discoveries' in intro
assert 'String label=i==0?"Home":i==1?"Music":i==2?"Video":"Settings"' in s
print('PASS: top source labels/accessibility renamed, source icons/routing retained, Home Settings shortcut removed, navigation Settings retained')
