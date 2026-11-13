# Internal development history

## 2.0.16

- Restored the 8dp source-to-library/results gap when Search is hidden, including timeout and manual toggles. Visible Search retains its existing 8dp top/bottom spacing; queries/results remain intact.
- Reduced repeated search work: one-minute bounded local snapshots, invalidated on foreground return/Refresh and keyed by folder policy; selected-region TV search reuses the existing six-hour bounded catalogue cache. Search rows recycle native views.
- Android 12+ compatibility review retains immutable PendingIntents, media-playback foreground service, API-gated media/notification grants and IME/system-bar insets. Minimum Android 8 support and target API35 unchanged.

## 2.0.15

- Hidden-by-default search field toggled by a magnifying-glass button beside Refresh. Home searches all four media types; Music searches songs and Internet Radio; Video searches local videos and Online TV in one combined result list.
- Field hides after 30 seconds of search inactivity or a second icon tap, without clearing results. Queries are debounced, background-loaded and protected from stale callbacks. Local grants/folder filters and selected online regions apply; scans/directories retain safety caps.
- Callsigns now use exactly Senpai, Master, Adventurer, Listener, Supreme Listener, Audiophile, Boss, Explorer, Dev, Partner, Captain and NPC.
- Replaced Lambo with Cutie (migrating saved Lambo selection): pastel pink–white–powder blue gradients on selected/primary control backgrounds, with steady cards and readable dark text.

## 2.0.14

- Renamed top source buttons: Music/Videos → Collections, FM → Internet Radio, TV → Online TV, including accessibility labels. Existing source icons, RGB tint and routing retained.
- Removed the Home Settings shortcut beside Refresh; Settings remains available through main navigation.

## 2.0.13

- Landscape Video/TV browsing collapses the unused right player pane when no visual media item exists, allowing the weighted library pane to fill the available width.
- Starting/loading or pausing visual playback retains the player pane; stopping releases that space. Portrait, Home, Music, Settings and fullscreen behavior unchanged.

## 2.0.12

- Landscape Home artwork shelves use subtle 24dp horizontal fading edges. Android adjusts the left/right fade with scroll position, fading only where more content is available. Portrait shelves are unchanged.

## 2.0.11

- Portrait Home shelves have matching 16dp leading/trailing content insets that scroll with the cards, leaving edge-to-edge viewports.
- Landscape Home columns swapped: recommendations on the left, Recently Added/FM updates on the right. Portrait order and other destinations unchanged.

## 2.0.10

- Portrait Home shelves start with a 16dp leading content inset that scrolls away. The viewport remains edge-to-edge, without a fixed right inset. Landscape is unchanged.

## 2.0.9

- Portrait Home Recently Added and Recent FM updates artwork shelves now span the page width with no outer horizontal inset; headings, intro, recommendations and mini-player retain their inset. Landscape spacing is unchanged.
- Completed setup opens Home, including permission-declined continuation. Skip Setup still opens FM without permission prompts.

## 2.0.8

- Simplified Home recommendation headings to Songs, FM, Video and TV without counts or “picks”. Sampling quotas are unchanged.
- Hide the audio mini-player and its container when no current item exists, including fresh launch and explicit Stop. Paused and loading items keep the bar visible.

## 2.0.7

- Restored the TUHKAR wordmark at the top of Home, above the greeting, and reduced the callsign to semibold 28sp (24sp in landscape).
- Restyled Home with horizontal artwork shelves for Recently Added and fresh FM discoveries, airy section headings and compact recommendation rows with play hints.
- Added Play picks/Shuffle picks actions for the current song recommendations, plus browse actions for the local libraries and FM. These controls do not claim to play/shuffle the entire library.
- Home branding and intro scroll with the feed to retain usable landscape space; landscape keeps two content columns.

## 2.0.6

- Added Fluent-style pocket-player, radio, film-reel and TV icons to the source header pills.
- Added Home as the leftmost destination with a time-based greeting, randomized playful callsign, recently added local media and region-scoped FM directory changes.
- Added random discovery targets of 6 songs, 2 FM stations, 1 video and 1 TV channel, with honest empty/shortfall states, read-only media access and saved-stream fallbacks.
- Home uses two columns in landscape and retains mini-player/video controls without automatic playback.

## 2.0.5

- Fixed selected Music/FM and Videos/TV source pills staying green with RGB accent: they now follow the existing control color cycle.
- Landscape branding uses the approved app icon above Music on the left navigation rail instead of the wordmark.
- Landscape Music/FM uses the wider content area, compact header and mini-player below the library. Videos/TV retains its side-by-side list/player arrangement.

## 2.0.4

- Improved liquid progress visibility with an accent-toned contrast fill and defined leading edge, especially in Light mode.
- Added remembered System / Light / Dark app appearance under Settings → Appearance, independent of device mode. System remains the default.
- Redesigned Settings with grouped categories and detail pages in portrait, plus a two-pane category/detail tablet-style layout in landscape. Existing controls, reset safeguards and licensing remain available.

## 2.0.3

- Balanced source-header/search spacing by removing doubled padding above Search; search-to-library spacing is unchanged.
- Renamed Grid big to Grid in Media layout.
- Made the two-column list more compact with an eight-row viewport target, smaller artwork/gutters, and a font-scale-aware minimum row height. Updated its Settings preview.

## 2.0.2

- Moved rotating album art to the mini Now Playing bar only; library list/grid covers stay still. Full Now Playing and Disc swipe retain their existing disc behavior.
- Replaced the mini-player wave edge with a minimal liquid progress fill and softly moving leading edge. Drag/tap/button behavior and real FM glow are unchanged.

## 2.0.1

- Whole mini-player card now displays subtle progress flow. Drag across it to seek; tap to open Now Playing. Play/Pause and Next retain independent touch targets.
- FM replaces progress with an accented glow driven by the app playback session, subject to existing audio-analysis permission and device support.
- Moved Videos/TV controls inside the picture with five-second inactivity hiding; single-tap reveals controls. Long presses/seeking and touch-exploration accessibility are protected.

## 2.0.0

- Grouped Music/FM and Videos/TV under three bottom destinations: Music, Video and Settings. Top source tabs keep local and online libraries distinct, with remembered choices.
- New Music/FM mini-player with artwork, current title, Play/Pause and Next. Tap the card for full Now Playing.
- Animated Material-inspired wave progress with drag seeking for finite media; FM uses a live indicator with seeking disabled. Waves are progress animation, not audio analysis.
- Replaced Small grid with a responsive two-column list and updated Settings preview, retaining existing layout preferences.

## 1.2.0

- Added optional **Background video audio** in Settings → Playback for local Videos when leaving the app or locking the screen. Off by default; TV still pauses. Music/FM background playback is unchanged.
- Added left/right swipes **on the bottom navigation bar** between Music, FM, Videos, TV and Settings, with a subtle transition that respects disabled animations. Page and media gestures are unchanged; navigation ends do not wrap.
- Added **double-tap landscape fullscreen for local Videos**, with TV-style auto-hiding controls and aspect fitting. Double-tap again or press Back to return without intentionally restarting playback.
- Reviewed release code and cleaned unused imports and public release metadata. Preserved approved artwork/icons, application identity, signing history, GPLv3 licensing and font notices.

## 1.1.0

- Added **Audio Enhancements**: a cleaner equalizer, Custom-only band controls with offline editing, optional music reverb, and local-music crossfade up to 2 seconds.
- Added **FM region selection**, including Worldwide and country-filtered searches, remembered independently from TV.
- Improved **TV continuity while browsing** other sections, retaining the preview without replacing its video output on every tab change.
- Added **Cyan, Lambo and Deep Violet** themes, with two-column theme choices and refined switch/highlight colors.
- Refined **Settings** readability, spacing and Media Access layout; tap the TUHKAR logo in About for privacy and credits.
- Centered the landscape player layouts and polished setup button typography.
- Rebuilt the launcher icon with clean vector artwork and a matching system-themed design.
- Cleaned obsolete code and updated release metadata and documentation. Existing application identity, signing history, GPLv3 license and font notices are preserved.

## 1.0.0

- Initial public release: local music/video playback, online radio/TV, background audio, native equalizer, reactive disc artwork, themes, favorites and guided setup.

These labels identify internal development snapshots, not a newer public release series. The consolidated public release is v2.0.0 (versionCode 70), superseding the internal builds through 2.0.16/code69.
