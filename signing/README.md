# Release signing

## Official certificate (Android 9+)

- Publisher: **KnightWin** (`CN=KnightWin, O=KnightWin`)
- Key: RSA 3072-bit, private key **not included**
- SHA-256 certificate fingerprint:

```text
ec1bfd2789fe59bd55c8170258798468cdecd7e2e231c7d0205b648dec29cc7a
```

`knightwin-release.crt` is a **public certificate**. `upgrade-lineage.bin` is a **public signed proof of rotation**, from the earlier development certificate to KnightWin's private release certificate. Neither can sign an APK without the private keys.

Android 9+ uses the new key through APK signature scheme v3, with rotation targeted to API 28. **Android 8.0–8.1 uses the legacy development certificate as a v2 fallback**, not the new identity. Old pre-release source bundles contained development signing material: never publish those archives. Treat legacy material as exposed; prefer Android 9+ for the new signing identity. Future official APKs must retain the same lineage/fallback strategy while Android 8 support is maintained.

The v1.1.0 signatures have been checked for API26/27/28/31/35 and the lineage permits installed-data continuity. This is not an actual on-device upgrade test. Try installing over the current app before uninstalling; report a signature conflict rather than erasing your settings unnecessarily. Upgrades from independently/debug-signed forks are not supported by this lineage.

## Verify a download

With Android SDK Build Tools:

```bash
apksigner verify --verbose --print-certs Tuhkar-v1.1.0.apk
sha256sum Tuhkar-v1.1.0.apk
```

Compare the certificate and APK hash with the release's published checksum/details. On Android 8 the legacy fingerprint is `0099fddb1985e8be5c1328b8e726bbf59d650e73d3f222443f6fed587b752d1b`.

## What signing does not do

Signing protects publisher continuity/integrity; it is not Google review, Play Protect approval or a guarantee that a new sideloaded app won't trigger a scan/warning. Do not disable Play Protect as a publishing strategy.

See [Google's Play Protect help](https://support.google.com/android/answer/2812853) and [app-signing documentation](https://developer.android.com/studio/publish/app-signing). GitHub distribution is not a Google Play Store listing or Play App Signing enrollment. That would require a separate Play Console publishing process.

Keys/passwords live only in KnightWin's private signing kit. [Build instructions](../BUILDING.md).
