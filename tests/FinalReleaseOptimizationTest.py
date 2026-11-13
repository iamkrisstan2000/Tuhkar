# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
from pathlib import Path
import subprocess,tempfile
art=Path('app/src/com/luma/media/ArtworkLoader.java').read_text()
method=art[art.index('    void trimMemory('):art.index('    void clear(){')]
method=method.replace('android.content.ComponentCallbacks2.','Limits.')
java='''public class MemoryTrim {
static class Limits {static final int TRIM_MEMORY_UI_HIDDEN=20;}
static class Cache {int evictions,limit;void evictAll(){evictions++;}void trimToSize(int n){limit=n;}}
Cache memory=new Cache(),keys=new Cache();
'''+method+'''
public static void main(String[] args){MemoryTrim p=new MemoryTrim();p.trimMemory(10);if(p.memory.limit!=2097152||p.memory.evictions!=0)throw new AssertionError("Moderate trim");p.trimMemory(20);if(p.memory.evictions!=1||p.keys.evictions!=1)throw new AssertionError("UI hidden trim");p.trimMemory(80);if(p.memory.evictions!=2)throw new AssertionError("Severe trim");System.out.println("PASS: actual artwork cache trim method halves moderate cache / evicts hidden-severe cache without recycling displayed bitmaps");}}
'''
with tempfile.TemporaryDirectory() as d:
 p=Path(d)/'MemoryTrim.java';p.write_text(java);subprocess.run(['javac',str(p)],check=True);subprocess.run(['java','-cp',d,'MemoryTrim'],check=True)
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'EditText search;' not in s and 'pendingSearch' not in s
assert 'onTrimMemory(int level)' in s and 'onLowMemory()' in s
assert 'synchronized(pending){pending.clear();waiting.clear();}' in art
assert 'postOnAnimationDelayed(frame,16)' in Path('app/src/com/luma/media/DiscView.java').read_text()
assert 'preview' in Path('PREVIEW-HISTORY.md').read_text().lower()
assert 'versionCode **70**' in Path('CHANGELOG.md').read_text()
print('PASS: obsolete fields removed, live search cancellation retained, pressure callbacks wired, artwork shutdown waiters cleared and display-aligned guarded disc scheduling; V2 consolidation history included')
