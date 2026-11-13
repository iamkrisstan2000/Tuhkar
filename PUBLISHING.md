# Manual upload: TUHKAR v2.0.0

The two release files are **Tuhkar-v2.0.0.apk** and **Tuhkar-v2.0.0-source.zip**. Nothing was uploaded or published on your behalf.

1. Install the signed APK over your current compatible preview and perform the phone checks in RELEASE-NOTES.md. Do not uninstall first: uninstalling can erase settings/Saved items.
2. Extract the source ZIP. Upload the contents of its **Tuhkar/** directory to the repository root, including hidden `.github/` and `.gitignore`. Prefer a clean source-tree replacement: web uploads do not delete old files automatically.
3. Remove obsolete files left from older repositories that are absent in this archive, especially `app/src/com/luma/media/WaveSeekBar.java`, `tests/WaveSeekBarContractTest.py`, and conflicting raster `drawable-nodpi/ic_foreground.png` / `ic_monochrome.png`. Do not delete the approved current XML launcher layers or wordmark.
4. Preserve LICENSE, PRIVACY.md, attribution/font licenses, tests, build scripts and public signing certificates/lineage. Never upload private signing kits, password files, `.signing/`, SDKs, `classes/`, `generated/`, `dex/` or old output APKs.
5. Prepare release/tag **v2.0.0** and attach these two files. Use RELEASE-NOTES.md for the release description; CHANGELOG.md and PREVIEW-HISTORY.md describe the development history. If a previous v2.0.0 tag/release already exists, review it before deciding whether to replace a draft or choose another tag; do not silently retarget a published tag.

## Upgrade identity
Public versionName: **2.0.0**. Internal versionCode: **70**, greater than the final internal preview 2.0.16/code69. Keep the existing application ID/signing identity and increment the internal code for every future release. The source ZIP contains public certificates only; future signed builds need the owner's private kit, which is deliberately not included.

The CI workflow creates development/debug builds, not the official release signature. A debug build cannot replace this signed APK.
