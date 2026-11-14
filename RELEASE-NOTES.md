# TUHKAR v2.1.0 hotpatch

KnightWin · GPL-3.0-only · com.luma.media · versionCode 77

- Unified floating search in portrait/landscape, with dim backdrop and content-sized animated results.
- Enter/selection field hiding; search-first Back, backdrop exit and 15-second inactivity dismissal.
- Header/list spacing fixes and compact landscape Home/Video headers.
- Cleanup: obsolete fixed-height dropdown helper removed; cancelled queued search jobs purged; background worker threads retire after 20 seconds idle. Existing bounded caches, memory-pressure handling and recycled rows retained.

Public label is consolidated to **2.1.0**. Internal **code77** exceeds all internal builds through 2.1.5/code76, so normal upgrades remain possible without uninstalling. Signing identity unchanged.

61 host suites (27 Java / 34 Python), independent extracted-source rebuild/tests and signature checks for API26/27/28/31/35. No on-device installation, keyboard, GPU/frame-time, RAM or battery benchmarking performed. Optimizations are code-level changes, not measured performance guarantees. Verify on your phone before publishing. Nothing uploaded/published.

Short changelog: CHANGELOG.md. Detailed history retained separately. Android12+ safeguards, minimum API26 and target API35 unchanged.
