# Security

Keep Play Protect enabled. An APK signature is not an independent safety audit or Google endorsement. Download official APKs from this repository's Releases and verify the published hash/certificate.

Never upload keystores, key passwords or the owner-only PRIVATE-SIGNING archive. The repository contains only a public certificate and public rotation proof. `.gitignore` is a precaution—not a substitute for reviewing staged files. Previous development source archives included a test key and must not be published as public releases.

The private release identity is used on Android 9+. Android 8 uses the legacy signer for compatibility; see [Signing](signing/README.md). Losing the new release key can jeopardize future upgrades. Keep encrypted offline backups.

For suspected vulnerabilities, use GitHub private vulnerability reporting if the repository owner enables it. Otherwise, open an issue asking for a private reporting channel **without publishing exploit details or personal data**. Never include media files, credentials or signing material in reports.

Host tests are not a security audit or real device verification. Third-party streams/artwork and device-native codecs/effects have independent behavior and risks.
