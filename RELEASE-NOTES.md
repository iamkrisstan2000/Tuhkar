# TUHKAR v2.0.0 — public V2 release

KnightWin · GPL-3.0-only · com.luma.media

This is the consolidated V2 release, with internal **versionCode 70**. It upgrades the internal 2.0.1–2.0.16 previews and earlier compatible signed builds; Android compares the internal code, not the visible version label. The signing identity and lineage are unchanged. No publishing was performed.

## Included
- Native local Music/Video libraries, Internet Radio and Online TV, portrait/landscape navigation and responsive settings.
- Home discovery shelves, exact requested callsigns, source labels, landscape edge fades and adaptive idle Video/TV browsing width.
- Toggleable combined scoped search, 30-second field hiding without losing results, local permission/folder policies and selected-region online directories.
- Now Playing, liquid seek mini-player, spinning artwork, real own-session audio-reactive disc, background playback, inline/fullscreen video controls and native audio effects.
- Ten palettes including Cutie’s pastel pink–white–powder-blue selected controls, System/Light/Dark and OLED options.

## Cleanup, RAM and animation work
- Removed obsolete standalone search view/debounce state. Existing cancellation now targets the active search task rather than a dead variable.
- Artwork cache halves under moderate memory pressure and releases cache entries when hidden/severe; search snapshots clear under pressure. No displayed bitmap is forcibly recycled.
- Artwork loader clears pending/waiter registries at shutdown. Existing weak-reference view registries, bounded image caches/queues, sampled images, cached local/TV search and recycled result rows remain.
- Disc animation uses display-aligned scheduling with a 16ms minimum delay rather than a fixed 32ms handler delay; elapsed-time motion, fade-out, visibility and reduced-motion checks remain.
- Removed generated/intermediate build files from source delivery. Required migrations, compatibility code, licenses, fonts, public signing lineage and regression tests are intentionally retained.

## Road to V2
See **CHANGELOG.md** for the milestone narrative and **PREVIEW-HISTORY.md** for the detailed internal development log. Preview version numbers there are historical labels, not newer public releases.

## Verification and limits
- 58 host suites (26 Java / 32 Python), including actual memory-trim methods with doubles, animation lifecycle/geometry, search spacing, cache policies, permissions and playback contracts.
- Signed release and API26/27/28/31/35 signature checks; source ZIP independently extracted, rebuilt and tested.
- Android12+ safeguards retained; minimum Android8/API26 and target API35 unchanged.
- No device/emulator installation, GPU/frame-time or RAM benchmarking was performed. These are code-level optimizations, not claims of measured savings or guaranteed frame rates.

Before publishing, install over your current signed preview without uninstalling, then check search/timer/layout, portrait/landscape, light/dark/Cutie, paused/Stop states, streaming, background playback and disc animation on a phone.
