<p align="center"><img src="app/res/drawable-nodpi/tuhkar_wordmark.png" alt="Tuhkar" width="300"></p>

# Tuhkar
### Your music. Your videos. Your vibe. 🎶

A lightweight native Android media player, created by **KnightWin**.

## Make yourself at home

- Play music and videos from your phone or SD card.
- Explore online radio and TV through community directories.
- Enjoy spinning artwork, beat-reactive disc effects and your phone’s native equalizer.
- Fine-tune Audio Enhancements: music-only room reverb and optional crossfade up to 2 seconds.
- Keep the TV preview playing while browsing other sections; select another item to switch playback.
- Browse FM stations by country or Worldwide, with a region preference separate from TV.
- Use Android 13+ launcher themed icons to match your system accent.
- Pick from ten accent themes, System/Light/Dark app mode, OLED mode and your favorite media layout.
- Save favorites and keep music/radio playing in the background.

## What’s new in V2 (2.1.0)

- Category-based Settings with a landscape split-pane layout and a saved System/Light/Dark appearance override.
- Higher-contrast liquid progress for clearer light-mode visibility.

- **Home** adds a greeting/callsign, recently added media, fresh region-scoped FM directory entries and random 6-song/2-FM/1-video/1-TV discovery targets.
- Music and FM share **Music**, with top source tabs; Videos and TV share **Video**. Bottom navigation is now **Home → Music → Video → Settings**.
- Music/FM mini-player with spinning cover art (library covers stay still), current title, Play/Pause and Next; tap to open full Now Playing.
- Whole-card progress with a minimal liquid fill and softly moving edge: drag the card to seek or tap for Now Playing. FM uses a subtle playback-reactive glow (with audio-analysis permission), with seeking disabled.
- Videos/TV controls overlay the picture and hide after five seconds without touch; single-tap reveals them.
- **2 column list** replaces Small grid. Existing layout selection migrates automatically.
- V1 background playback, local-video fullscreen, themes, effects and media permissions are preserved. See [changelog](CHANGELOG.md).

## Get the app

Download **Tuhkar-v2.1.0.apk** from this repository’s **Releases** tab. Requires Android 8.0 or newer; some audio effects depend on your device.

Try installing over your current Tuhkar build first—don’t uninstall just to upgrade. Key-rotation history is included for Android 9+. See [Signing](signing/README.md) for the Android 8 fallback and verification details.

The APK is signed, but signing is not a Google safety approval. Play Protect may still ask to scan an unfamiliar APK. Keep its protection enabled.

## A little privacy, a lot of music

No ads or analytics. Permissions are your choice. Beat glow analyzes Tuhkar playback, **not microphone recordings**; no captured audio is saved or uploaded. Optional cover searches share titles/artists with third-party artwork services; disable downloads in Settings.

Streams contact third-party providers; availability and viewing rights vary. Use links you’re authorized to play. [Privacy details](PRIVACY.md).

## Buy KnightWin a coffee ☕

If Tuhkar adds rhythm to your day, a little support helps keep it growing. Totally optional—no features are locked behind donations. Thank you! 💚

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/N7D7292WUD)

## Open source

**GNU GPL v3 (GPL-3.0-only)** — use, study, modify and share under the [license](LICENSE). Bundled fonts retain their SIL Open Font Licenses.

[Build it](BUILDING.md) · [Credits](NOTICE.md) · [Security](SECURITY.md)

### Combined media search
Use the magnifying glass beside Refresh to toggle search. Home covers Music/Internet Radio/Video/Online TV; Music covers local songs and radio; Video covers local videos and TV. Enter or a result selection fades the field away; after 15 seconds without interaction both field and results dismiss. Back first restores the media library. Local access and folder filters apply. Internet Radio sends your query to Radio Browser; online searches use selected regions and bounded public directories, not an exhaustive worldwide catalogue. Cutie replaces Lambo with a pastel pink–white–powder-blue control gradient.

## Public V2 release identity

This source is the **v2.1.0 hotpatch** to the consolidated v2.0.0 release, internal **versionCode 77**, superseding public v2.0.0/code70 and internal hotpatch builds through 2.1.5/code76. See CHANGELOG.md for the short hotpatch notes; DEVELOPMENT-CHANGELOG.md and PREVIEW-HISTORY.md retain the detailed history. Release files are prepared for manual upload; availability on any hosting service depends on the owner publishing them.

Search in both orientations now opens as a dimmed floating field and animated results dropdown; the original page stays behind it. Landscape uses the same floating search with rail-aware width and keyboard-safe height. Enter/selection hide the field; Back, backdrop tap or 15 seconds of inactivity dismiss the entire search.
