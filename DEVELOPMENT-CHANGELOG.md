# Changelog

## 2.1.5 — landscape floating search

- Extends the dimmed floating field/results dropdown to landscape. Underlying Home/library/player layout stays in place in both orientations.
- Landscape panel avoids the rail, caps its width at 720dp and uses a higher top anchor for short screens; dropdown stays content-sized and keyboard-safe. Disables fullscreen IME extraction so landscape typing stays in the overlay.
- Same scope, graceful Enter/selection field hiding, backdrop/Back exit and 15-second full dismissal. Removes obsolete inline result-host state.

## 2.1.4 — portrait spacing and content-sized search

- Restores an independent 8dp gap between portrait source buttons and the underlying library card, unaffected by the floating search overlay.
- Portrait dropdown height now follows field visibility, measured status text and font-aware result row count. Few/zero results stay compact; long lists retain keyboard-safe/capped scrolling.

## 2.1.3 — portrait search dropdown

- Portrait-only floating search field/dropdown over a dimmed page, without moving the underlying media/feed. Results expand with bounded IME-safe height; reduced motion disables expansion/fade animation.
- Back, backdrop tap and 15-second inactivity dismiss the overlay; Enter/selection hide only the field while results remain until dismissal. Search scope and landscape behavior retained.

## 2.1.2 — compact landscape Home

- Reuses the removed landscape wordmark slot for the greeting/callsign beside Refresh/Search, eliminating the blank branding row above them.
- Reduces landscape Home action/section gaps so both columns rise with the compact intro. Portrait typography, logo and spacing retained.

## 2.1.1 — search placement polish

- Music/Video search fields now appear immediately beneath the top brand/header row, matching Home. Search results remain below source buttons in the weighted media-list area rather than following the moved field.
- Home wordmark hidden in landscape only; approved portrait wordmark and rail app icon retained.
- Retains 15-second full search timeout, graceful selection/Enter field hiding and search-first Back behavior.

## 2.1.0 — search/navigation hotpatch

- Selecting a search result or submitting with Enter/Search gracefully fades the search field away while keeping results available.
- Back first exits search and returns to the current media library/feed before normal destination navigation.
- After 15 seconds without search interaction, both field and results dismiss, cancelling stale work and restoring the library. This also runs while the field is already hidden.
- Landscape Video/Online TV source pills now sit beside the Video heading like Music. Portrait unchanged.

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
