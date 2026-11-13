# Changelog

## 2.0.0 — consolidated public V2 release

Internal versionCode **70** deliberately exceeds every development preview, including 2.0.16/code69, so the signed APK can upgrade existing installations despite the consolidated public version label.

### Road to V2
1. **Native foundation:** internal/SD MediaStore libraries, Internet Radio/Online TV, foreground playback, GPLv3 licensing and KnightWin branding/signing.
2. **Playback and layouts:** portrait/landscape navigation, remembered source tabs, Disc swipe, compact two-column list, grid/list modes, sorting/Saved, liquid seek mini-player, full Now Playing, video/TV fullscreen continuity and auto-hiding controls.
3. **Audio and personalization:** supported-band native EQ, music-only crossfade, reverb, own-session visualizer/reactive disc, system/light/dark appearance, OLED surfaces and ten palettes.
4. **Setup and settings:** unified setup notice/permissions/recovery, Congratulations → Home, Skip Setup → Internet Radio, adaptive settings, independent radio region and safe reset that never deletes media.
5. **Home:** restored TUHKAR wordmark, smaller semibold callsigns, recent media/FM discovery shelves, randomized recommendations, portrait scrolling edge spacing, swapped landscape columns/fades and an idle/stopped-hidden mini-player.
6. **Unified search and final polish:** Collections/Internet Radio/Online TV labels, toggleable scoped combined search, 30-second field-only hiding, retained results, Cutie pastel control gradient and exact requested callsigns. Adaptive Video/TV library reclaims idle player space in landscape.
7. **Release cleanup/efficiency:** removed dead standalone search field and obsolete debounce variable; reused the live search cancellation hook. Bounded artwork caches now yield to Android memory-pressure callbacks, cached local search snapshots clear under pressure, and artwork waiter registries clear on shutdown. Disc frames use display-synchronized scheduling with a 16ms minimum delay, retaining pause/hidden/reduced-motion guards. Existing cached TV search, recycled result rows and local search snapshots reduce repeated work.

### Included and unchanged
- Approved launcher/wordmark assets and fonts, private upgrade signing identity, permissions, playback service ownership and authorized-stream caveats.
- Android 12+ compatibility safeguards retained; minimum API26 and target API35 unchanged.
- Source ZIP includes release notes, manual-upload instructions, licensing, public signing certificates/lineage, build/test scripts and development history. No signing secrets or generated build output.

### Validation limits
Host tests and exact-source rebuild are not device performance measurements. No claims of measured RAM savings, frame-rate guarantees, on-device installation or certification across Android versions. Review the release notes for phone checks before publishing.

See [PREVIEW-HISTORY.md](PREVIEW-HISTORY.md) for the detailed internal iteration log leading to this release.
