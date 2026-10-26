# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Custom-only band visibility and Settings typography/layout wiring; not device rendering."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
a=s.index('    void equalizerSettings(');b=s.index('    void notice(',a);eq=s[a:b]
guard='if(equalizer.preset()!=NativeEq.PRESETS.length-1)return;'
assert guard in eq and eq.index(guard)<eq.index('for(int band=0;band<bands;band++)')
assert eq.index('Button presetSelect=')<eq.index(guard)
assert 'equalizer.setPreset(w);d.dismiss();render();' in eq
media=s[s.index('    void mediaAccessSettings('):s.index('    void releaseOldFolderGrants(')]
assert 'accessActions.addView(text(mediaAccessStatus(),13,ink),new LinearLayout.LayoutParams(0,-2,1))' in media
assert media.index('accessActions.addView(text(mediaAccessStatus()')<media.index('ImageButton access=')<media.index('ImageButton setup=')<media.index('standardFolderOptions(card)')
assert 'headerParams.bottomMargin=dp(4)' in media
text=s[s.index('    TextView text('):s.index('    LinearLayout column(')]
assert 'if(settingsOpen&&!setupOpen)' in text and 'setFontVariationSettings("\'wght\' 500")' in text
assert 'Math.max(13,sp)' in text and 'SurfacePalette.mix(muted,ink,.25f)' in text
assert 'styleSettingsSwitchText(page);restoreSettingsScroll(scroll,page);' in s
# Compile and run the actual visibility condition, using all valid preset indices and
# null/detected native bands. Preset processing remains available regardless of visibility.
condition=guard.removeprefix('if(').removesuffix(')return;')
java='''public class CustomBandVisibilityTest {
 static class NativeEq {static final String[] PRESETS=new String[7];}
 static class Eq {int selected;float[] frequencies;int preset(){return selected;}}
 static Eq equalizer=new Eq();
 static boolean visible(){return !(CONDITION);}
 public static void main(String[] args){int checks=0;
  for(int preset=0;preset<7;preset++)for(int detected=0;detected<2;detected++){
   equalizer.selected=preset;equalizer.frequencies=detected==0?null:new float[]{60,250,1000,4000,16000};
   if(visible()!=(preset==6))throw new AssertionError("preset="+preset+" detected="+detected);checks++;
  }
  System.out.println("PASS: "+checks+" actual custom-band visibility predicate checks; all factory presets hide sliders, Custom always shows editable bands, including idle.");
 }
}'''.replace('CONDITION',condition)
with TemporaryDirectory(prefix='tuhkar-settings-readable-') as temp:
    root=Path(temp);p=root/'CustomBandVisibilityTest.java';p.write_text(java)
    subprocess.run(['javac','-d',str(root),str(p)],check=True)
    subprocess.run(['java','-cp',str(root),'CustomBandVisibilityTest'],check=True)
print('PASS: screenshot-style Media Access status/icons header and switch ordering; Settings-only 500-weight text, 13sp-minimum descriptions and stronger secondary color; scroll restoration retained.')
print('NOTE: structural checks/host predicate, not Android text legibility, font-scale layout, touch or screenshot pixel matching.')
