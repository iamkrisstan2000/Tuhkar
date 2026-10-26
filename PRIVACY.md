# Privacy, in plain language

**Creator:** KnightWin · **App:** Tuhkar

- **No ads, analytics SDK or account requirement.** Your library is read from Android's shared-media index; Tuhkar does not upload your media files.
- **Media access** shows music and videos from shared phone/SD storage. Android permissions and Music/Movies-only filters control what is visible. Android 14+ can allow only selected videos. Tuhkar does not request all-files access.
- **Notifications** provide playback controls. Music/radio use a foreground playback service. Video/TV pause when the app leaves the foreground.
- **Record audio permission** is used only for Android Visualizer analysis of Tuhkar's own playback session, to animate the disc effect. Tuhkar does not use microphone-recording APIs, store captured audio/voice recordings or upload analysis data. Analysis runs while an eligible playing disc is visible and releases on stop/background. Hardware support varies.
- **Artwork downloads** are on by default. Missing music/video cover searches may send titles/artists to Apple iTunes. Directory artwork URLs and Wikipedia may provide radio/TV logos. These providers receive normal connection metadata such as your IP address. Covers may be imperfect. Turn **Download missing artwork** off in Settings to stop new optional downloads.
- **Radio/TV** directory requests and streams connect to external providers. Those providers have their own policies. Some links use HTTP and are not encrypted. Listings are not a promise of availability or viewing rights.
- **Local state** includes settings, favorites and bounded artwork caches. Reset clears app settings/state/cache, **not your actual music/videos**. Android permission grants may remain.
- **Optional permissions can be declined.** Setup reviews missing access and offers Retry or Continue anyway. Access can be changed later in Android/App settings.

This app is published as open-source software, without a security audit or a promise of Google endorsement. Report concerns without including private media, passwords or signing keys.

## Native Audio Enhancements

Equalizer settings use Tuhkar playback sessions. Optional room reverb routes only Tuhkar music players to one native auxiliary effect; its Android session-zero auxiliary API is not output recording or a global equalizer. Local-music crossfade prepares at most one upcoming track near the end of the current track. Neither feature records the microphone, stores analyzed audio, uploads audio or adds permissions. Both controls default to Off.

## FM region selection

FM country filtering sends the manually selected two-letter country code, and any station-name search, to Radio Browser. The selection does not use GPS or request location permission. Worldwide mode omits the country filter. Your FM region preference is stored locally and is separate from the TV region.
