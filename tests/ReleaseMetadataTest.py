# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Public release identity, licensing and signing-configuration checks (not Play review)."""
from pathlib import Path
import xml.etree.ElementTree as ET

root=Path('.')
props=dict(line.split('=',1) for line in Path('app.properties').read_text().splitlines() if '=' in line)
manifest=ET.parse('app/AndroidManifest.xml').getroot()
a='{http://schemas.android.com/apk/res/android}'
assert manifest.attrib['package']==props['app.applicationId']=='com.luma.media'
assert manifest.attrib[a+'versionName']==props['app.version']=='1.1.0'
assert manifest.attrib[a+'versionCode']==props['app.versionCode']=='49'
app=manifest.find('application')
meta={m.attrib[a+'name']:m.attrib[a+'value'] for m in app.findall('meta-data')}
assert meta['com.luma.media.CREATOR']==props['app.creator']=='KnightWin'
assert meta['com.luma.media.LICENSE']==props['app.license']=='GPL-3.0-only'
assert app.attrib.get(a+'debuggable','false')=='false'
assert app.attrib[a+'allowBackup']=='false'
license=Path('LICENSE').read_text()
assert 'GNU GENERAL PUBLIC LICENSE' in license and 'Version 3, 29 June 2007' in license and len(license)>30000
for source in Path('app/src').rglob('*.java'):
    assert 'SPDX-License-Identifier: GPL-3.0-only' in source.read_text()[:130]
for font in ['Manrope','SetupScript']:
    assert Path('app/assets/'+font+'.ttf').is_file()
    assert 'SIL OPEN FONT LICENSE' in Path('app/assets/'+font+'-LICENSE.txt').read_text()
readme=Path('README.md').read_text()
assert '[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/N7D7292WUD)' in readme
assert 'KnightWin' in readme and 'GPL-3.0-only' in readme
main=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'Tuhkar v1.1.0 · by KnightWin' in main and 'Created by KnightWin · GNU GPL v3' in main
build=Path('build.sh').read_text()
assert 'luma-test-only' not in build and 'test-signing.p12' not in build
assert 'RELEASE_PASSWORD_FILE:?' in build and 'LEGACY_PASSWORD_FILE:?' in build
assert '--rotation-min-sdk-version 28' in build and '--debuggable-apk-permitted false' in build
assert not Path('test-signing.p12').exists() and not Path('test-signing.key').exists()
assert Path('signing/knightwin-release.crt').is_file() and Path('signing/upgrade-lineage.bin').is_file()
for pattern in ['*.p12','*.key','*password*','*PRIVATE*','output/','.signing/']:
    assert pattern in Path('.gitignore').read_text()
print('PASS: v1.1.0/code49 and KnightWin identity, GPLv3-only/OFL notices, valid Ko-fi button, nondebuggable official signing, explicit secret-file inputs and public-key-only signing directory')

assert "Tuhkar/1.1.0 (Android)" in main
assert "Tuhkar/1.1.0 (Android)" in Path("app/src/com/luma/media/ArtworkLoader.java").read_text()

assert int(props["app.versionCode"])>48, "Final release must upgrade every internal preview"
assert "## 1.1.0" in Path("CHANGELOG.md").read_text()
assert "versionCode 49" in Path("BUILDING.md").read_text()
