# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Settings routing/restore/theme wiring; not Android appearance/accessibility screenshots."""
from pathlib import Path
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'AppearanceMode.dark(prefs.getString("appearanceMode","system"),' in s
assert 'String[] ids={"system","light","dark"},labels={"System","Light","Dark"}' in s
assert 'prefs.edit().putString("appearanceMode",id).apply();render();' in s
assert 'b.putString("settingsCategory",settingsCategory)' in s
assert 'SettingsNavigation.normalize(state.getString("settingsCategory",""))' in s
assert 'void settingsBack(){if(!landscape&&!settingsCategory.isEmpty())chooseSettingsCategory("");else closeSettings();}' in s
assert 'if(settingsOpen){settingsBack();return;}' in s
assert 'settingsCategory="";settingsScroll=null;settingsPage=null;settingsRestoring=false;' in s
assert 'if(settingsCategory.isEmpty())settingsHome(page);' in s
assert 'case "appearance":appearanceSettings(page);break;' in s
assert 'case "library":librarySettings(page);artworkSettings(page);break;' in s
assert 'case "playback":playbackSettings(page);discGlowSettings(page);break;' in s
assert 'case "audio":audioEnhancementsSettings(page);break;' in s
assert 'case "access":mediaAccessSettings(page);break;' in s
assert 'case "about":aboutSettings(page);break;' in s
assert 'links.addView(settingsCategoryRow(index,SettingsNavigation.IDS[index].equals(settingsCategory),true),rp)' in s
assert 'body.addView(settingsHost,new LinearLayout.LayoutParams(0,-1,1))' in s
assert 'item.setMinimumHeight(dp(76))' in s and 'item.setFocusable(true)' in s
assert 'String[] groups={"Personalize","Listening","Your app"}' in s
assert 'styleSettingsSwitchText(page);restoreSettingsScroll(scroll,page);' in s
assert 'settingsCard(page,"Reset app")' in s and 'settingsCard(page,"About")' in s
assert s.index('settingsCard(page,"Reset app")')<s.index('settingsCard(page,"About")')
assert 'settingsOpen&&"audio".equals(settingsCategory)' in s
print('PASS: category home/detail routing, landscape split panes, back navigation, saved category/scroll, mode persistence/System default, all original settings access and reset/theme safety wiring')
