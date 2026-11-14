# Manual upload — TUHKAR v2.1.0

Two files: **Tuhkar-v2.1.0.apk** and **Tuhkar-v2.1.0-source.zip**. Nothing was published on your behalf.

1. Install the signed APK over your compatible current build without uninstalling. Check search keyboard/dropdown, Enter/selection, Back/backdrop/15-second exit, orientation, landscape headers and playback continuity.
2. Extract the source ZIP and upload the contents of **Tuhkar/** to the repository root, including .github/ and .gitignore. Prefer clean source replacement: web uploads do not delete obsolete files automatically.
3. Remove superseded files absent from this source archive, notably old WaveSeekBar.java/test or conflicting raster ic_foreground.png/ic_monochrome.png. Preserve the approved current launcher XML/wordmark assets, public signing certificates/lineage, licenses and tests.
4. Prepare release/tag **v2.1.0**, attach these two files and use RELEASE-NOTES.md plus the short CHANGELOG.md. If the tag already exists, review it rather than silently moving a published tag.

VersionName **2.1.0**, internal **versionCode 77**, greater than internal 2.1.5/code76 and public v2.0.0/code70. Keep the same application ID/signing lineage and increment the code in future updates. The source ZIP includes public signing data only; keep the private signing kit/passwords private. Do not upload .signing/, SDK, classes/, dex/, generated/ or old APK output folders. CI debug builds are not the official release signature.
