# Privacy, in plain language

**Creator:** KnightWin · **App:** Tuhkar

- **No ads, analytics SDK or account requirement.** Your library is read from Android's shared-media index; Tuhkar does not upload your media files.
- **Media access** shows music and videos from shared phone/SD storage. Android permissions and Music/Movies-only filters control what is visible. Android 14+ can allow only selected videos. Tuhkar does not request all-files access.
- **Notifications** provide playback controls. Music/radio use a foreground playback service. TV pauses when the app leaves the foreground. Local video also pauses by default; the optional Background video audio setting allows its audio to continue.
- **Record audio permission** is used only for Android Visualizer analysis of Tuhkar's own playback session, to animate the disc effect and FM mini-player glow. Tuhkar does not use microphone-recording APIs, store captured audio/voice recordings or upload analysis data. Analysis runs while an eligible playing disc or FM mini-player is visible and releases on stop/background. Hardware support varies.
- **Artwork downloads** are on by default. Missing music/video cover searches may send titles/artists to Apple iTunes. Directory artwork URLs and Wikipedia may provide radio/TV logos. These providers receive normal connection metadata such as your IP address. Covers may be imperfect. Turn **Download missing artwork** off in Settings to stop new optional downloads.
- **Radio/TV** directory requests and streams connect to external providers. Those providers have their own policies. Some links use HTTP and are not encrypted. Listings are not a promise of availability or viewing rights.
- **Local state** includes settings, favorites and bounded artwork caches. Reset clears app settings/state/cache, **not your actual music/videos**. Android permission grants may remain.
- **Optional permissions can be declined.** Setup reviews missing access and offers Retry or Continue anyway. Access can be changed later in Android/App settings.

This app is published as open-source software, without a security audit or a promise of Google endorsement. Report concerns without including private media, passwords or signing keys.

## Native Audio Enhancements

Equalizer settings use Tuhkar playback sessions. Optional room reverb routes only Tuhkar music players to one native auxiliary effect; its Android session-zero auxiliary API is not output recording or a global equalizer. Local-music crossfade prepares at most one upcoming track near the end of the current track. Neither feature records the microphone, stores analyzed audio, uploads audio or adds permissions. Both controls default to Off.

## FM region selection

FM country filtering sends the manually selected two-letter country code, and any station-name search, to Radio Browser. The selection does not use GPS or request location permission. Worldwide mode omits the country filter. Your FM region preference is stored locally and is separate from the TV region.

## Home discovery

Home reads accessible MediaStore date-added metadata using the same granted permissions and folder filters. Recently added items and random recommendations are selected locally, with scans capped at 5,000 readable items per local media type. No listening-history profile, recommendation account or analytics service is created. Shortfalls are shown rather than invented.

Opening or refreshing Home can contact Radio Browser for recent station-directory changes in your chosen FM region and iptv-org for the current TV region's channel list. Directory change times are not station news or verified broadcast updates. Providers receive normal request metadata such as IP address and the selected region. No location permission or GPS is used. Discovery results are held in the current Activity's memory, reused for about ten minutes, and can be refreshed. Streams play only when you choose an item. Existing optional artwork downloads may send titles/artists to cover providers as described above.

## Combined media search

Search is user-initiated. Local titles/artists are filtered on-device using granted MediaStore access and existing folder filters (bounded to 5,000 entries per type). Online Radio Browser searches send the typed query and selected radio region to the public directory; Online TV downloads the selected-region public IPTV list and filters channel names on-device (up to 3,000 entries). Radio query results are bounded to 150 entries. Saved items are also matched locally. Search results are held in activity memory; no search analytics or listening profile is created. Existing optional artwork lookup rules also apply to result thumbnails. Manually hiding the field, submitting, or selecting a result preserves query/results until Back or 15 seconds of inactivity dismisses the search. Clearing its text also clears results.

Search optimization: local search metadata snapshots are held in activity memory for up to one minute, bounded to 5,000 entries per type, and invalidated on foreground return and Refresh. TV search reuses the selected-region six-hour app-cache catalogue already used by TV browsing. No new tracking or search-history database is added.
