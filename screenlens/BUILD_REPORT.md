# ScreenLens — Build Report

Project: ScreenLens
Package: `com.screenlens.app`
Version: 1.0.0 (versionCode 1)
Build Date: 2026-08-10

## Environment inspected

| Tool | Version found | Notes |
| --- | --- | --- |
| Java | OpenJDK 21.0.10 | Project targets Java/Kotlin JVM compatibility 17 |
| Gradle | 8.14.3 (wrapper) | Reused the working wrapper already present in this repo |
| Android Gradle Plugin | 8.9.1 | Matches the repo's existing Phone Doctor app for consistency |
| Kotlin | 2.0.21 | |
| Android SDK (`ANDROID_HOME`/`ANDROID_SDK_ROOT`) | **Not installed** | No `cmdline-tools`, `platforms`, or `build-tools` present anywhere in this container |

## Local build attempt

```
$ cd screenlens && ./gradlew tasks
FAILURE: Build failed with an exception.
Plugin [id: 'com.android.application', version: '8.9.1', apply: false] was not found ...
Searched in the following repositories:
    Google
    MavenRepo
    Gradle Central Plugin Repository
```

**Root cause:** this session's outbound network policy blocks `dl.google.com`
(HTTP 403, confirmed via `curl` and the proxy status endpoint:
`"host": "dl.google.com:443"`, `"detail": "gateway answered 403 to CONNECT
(policy denial or upstream failure)"`). Google's Maven repository
(`google()` in `settings.gradle.kts`) and the Android SDK component
downloader both resolve through `dl.google.com`, so **neither the Android
Gradle Plugin, AndroidX/Material/ML Kit dependencies, nor an Android SDK
platform/build-tools could be downloaded in this environment.** This is an
infrastructure/network-policy limitation of the sandbox this session ran in,
not a defect in the project — the same constraint already applied to the
pre-existing Phone Doctor app in this repository, which is likewise built
exclusively via GitHub Actions rather than locally.

Per instructions, **no build step below is reported as passing unless it
actually ran and Gradle reported success.** None of the local build/test/
lint/assemble commands could execute at all in this environment, so none are
claimed to have passed.

| Step | Result |
| --- | --- |
| `./gradlew test` (unit tests) | **NOT RUN** — blocked by missing Android SDK / dependency resolution |
| `./gradlew lint` | **NOT RUN** — same reason |
| `./gradlew assembleDebug` | **NOT RUN** — same reason |
| `./gradlew assembleRelease` | **NOT RUN** — same reason (also no signing secrets present) |
| `./gradlew bundleRelease` | **NOT RUN** — same reason |

Debug APK: **not produced.**
Release APK: **not produced.**
Release AAB: **not produced.**

## What was verified instead

Since Gradle could not run, correctness was checked by direct inspection
rather than execution:

- Every Kotlin file compiles logically against the API surfaces it calls
  (ML Kit Text Recognition/Translate/Language ID/Barcode Scanning, CameraX,
  MediaProjection, Room, DataStore, Navigation Safe Args, Play Billing v7,
  Google Mobile Ads) based on their documented public APIs.
- All 158 files under `screenlens/` were written directly for this project;
  none are placeholders, and no OCR/translation/history result anywhere in
  the code is hardcoded or simulated — every result comes from a real
  Android/ML Kit API call, or the UI shows an explicit, honest limitation
  message when a real API constraint applies (see `README.md` → *Known
  limitations*).
- No `TODO`/`FIXME` markers were introduced.
- Repo-relative resource references (`@string`, `@drawable`, `@layout`,
  nav-graph action IDs, ViewBinding property names) were manually
  cross-checked between XML and Kotlin call sites.

**This is not a substitute for a real compile.** The authoritative build
status for this project is whatever `.github/workflows/screenlens-android-build.yml`
reports on GitHub Actions, which runs in an environment with real Android
SDK + Google Maven access. Until that workflow has run at least once (or you
build locally on a machine with normal internet access), treat the
project as **unverified by compilation**, even though it was authored
carefully and reviewed line-by-line.

## Tests

Written (see `README.md` → *Testing* for the full list): `HistoryDaoTest`,
`HistoryRepositoryTest`, `SettingsDataStoreTest`, `LanguageCatalogTest`,
`HistoryMappingTest`, `HistoryDisplayTest`, `OcrOutcomeTest`.

Unit Tests: **NOT RUN** (see above) — PASS/FAIL is unknown until CI runs them.
Lint: **NOT RUN** — PASS/FAIL is unknown until CI runs it.
Debug APK: **FAIL to produce** (network-blocked, not a code failure)
Release APK: **FAIL to produce** (no signing secrets configured + network-blocked)
AAB: **FAIL to produce** (same)

## GitHub

Repository: will be reported in the final chat response after `git push`
actually succeeds (this file is written before that push, per the required
build order).
Commit SHA: see final response.

## Security Scan

Performed before commit: recursive search across `screenlens/` and the new
workflow file for AWS/Google API key patterns, PEM private key headers,
OAuth bearer/Slack-style tokens, and by filename for `*.keystore`, `*.jks`,
`secrets.properties`, `.env`, `google-services.json`, `local.properties`.

Secrets Found: **NO**
Result: **PASS**

`secrets.properties.example` contains only empty placeholder keys, matching
the project's `.gitignore`, which excludes `secrets.properties`, `*.keystore`,
`*.jks`, `screenlens/local.properties`, and `screenlens/.idea/`.

## Known Limitations

See `README.md` → *Known limitations* for the full list, summarized:

1. Arabic **OCR** (reading Arabic glyphs from images/screens) is not
   available — ML Kit's on-device Text Recognition only supports
   Latin-alphabet script. Arabic **translation** and the Arabic **app UI**
   both work fully.
2. Premium purchases are architecturally real (Play Billing v7) but
   inactive — no product exists in Play Console yet, so the app correctly
   reports "not configured."
3. This sandbox has no Android SDK and no access to `dl.google.com`, so no
   local Gradle build could be executed or verified here.
4. No production AdMob/Play Billing identifiers are configured; debug and
   (until secrets are added) release builds use Google's public test IDs.
5. No store screenshots are included (see `store-assets/README.md`) — they
   must come from an actual running build.
