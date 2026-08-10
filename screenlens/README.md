# ScreenLens

**See it. Understand it.**

ScreenLens is a native Android app that reads, translates and organizes text
from your screen, photos, the camera and QR/barcodes — using real on-device
Android and Google ML Kit APIs. It runs local-first: recognition, language
detection and (once downloaded) translation all happen on the device, and
history and settings are stored locally with Room and DataStore. ScreenLens
has no backend of its own and never uploads screen captures, photos or your
saved history anywhere.

This app lives in this repository as an independent Gradle project under
`/screenlens` (package `com.screenlens.app`), separate from the top-level
Phone Doctor app in `/app`.

## Features

- **Screen Scanner** — captures the current screen via Android
  `MediaProjection` (with the mandatory system consent dialog every time),
  lets you optionally crop to a region, then runs on-device OCR. The
  MediaProjection session is torn down immediately after one frame is
  captured — nothing is recorded or kept running in the background.
- **Image OCR** — pick a photo from the gallery (system Photo Picker, no
  storage permission needed) or take one with the camera app, then extract
  text with ML Kit Text Recognition.
- **Live OCR** — a CameraX live viewfinder for reading text from documents,
  signs, menus, books and labels in real time.
- **OCR Result screen** — original text, detected language, and Copy /
  Translate / Share / Save / Edit actions.
- **Translator** — source/target language pickers with swap, free-form text
  input, and on-device translation via ML Kit Translate.
- **Language Manager** — see installed vs. available translation languages
  and download/remove on-device models explicitly (download progress and
  installed state are real, not simulated).
- **Clipboard Translator** — reads the clipboard only when you tap "Check
  Clipboard," never continuously or in the background.
- **Floating Lens** — an optional draggable bubble shown over other apps
  (via the "Display over other apps" permission), off by default and only
  ever started when you turn it on in Settings or Tools.
- **QR & Barcode Scanner** — scans QR codes, EAN-13/8, UPC-A/E, Code 128,
  Code 39 and Data Matrix via ML Kit Barcode Scanning. Links are shown, never
  opened automatically.
- **History** — every OCR/translation/QR result you save is stored locally
  in Room, with full-text search and type filters, a detail screen, and
  delete with confirmation.
- **Settings** — Appearance (System/Light/Dark), app language (English/
  العربية, full RTL support), Floating Lens toggle, Auto-save History,
  History limit, Clear History, Language Manager shortcut, notification
  channel management, Privacy, About, Rate, Share.
- **Onboarding & Splash** — Android 12+ SplashScreen API brand moment, then a
  4-page onboarding flow shown once (re-openable from Settings).
- **Premium** — a real Free/Premium comparison UI and a genuine Google Play
  Billing Library v7 integration. Because no product has been created in
  Play Console for this build, it honestly reports "not configured" instead
  of faking a purchase — see [Known limitations](#known-limitations).
- **Ads** — Google Mobile Ads SDK (banner on Home, interstitial only shown
  when returning to Home after finishing a scan/translation/QR read — never
  mid-capture, mid-OCR, mid-camera or mid-translation).

## Architecture

- **Language:** Kotlin
- **UI:** Android Views (XML layouts) + ViewBinding — no Jetpack Compose
- **Pattern:** MVVM + Repository
- **Navigation:** Jetpack Navigation Component with Safe Args, single-Activity
  (`MainActivity`) hosting all bottom-nav and pushed screens as fragments,
  plus separate `SplashActivity`, `OnboardingActivity`, `CropActivity` and the
  transparent `MediaProjectionPermissionActivity` trampoline
- **Concurrency:** Kotlin Coroutines + Flow
- **Persistence:** Room (`history` table), DataStore Preferences (settings)
- **DI:** a small manual `ServiceLocator` (no Hilt/Dagger — the dependency
  graph is small enough that a DI framework would add build complexity
  without a matching benefit)
- **Camera:** CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`,
  `camera-view`)
- **Screen capture:** `MediaProjection` + `VirtualDisplay` + `ImageReader`,
  run inside a foreground service (`FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`)
  that captures exactly one frame and stops itself
- **On-device ML:** Google ML Kit — Text Recognition (Latin script),
  Language Identification, Translation, Barcode Scanning
- **Ads / Billing:** Google Mobile Ads SDK, Google Play Billing Library v7

```
app/src/main/java/com/screenlens/app/
├── ScreenLensApp.kt            # Application: theme/locale/notification channels/AdMob init
├── ServiceLocator.kt           # Manual DI container
├── ads/                        # AdManager (banner + interstitial wrapper)
├── billing/                    # BillingRepository (Play Billing v7)
├── capture/                    # MediaProjection permission trampoline, ScreenCaptureService, CaptureResultBus
├── data/
│   ├── db/                     # Room: HistoryEntity, HistoryDao, AppDatabase
│   ├── repository/             # HistoryRepository
│   └── settings/                # SettingsDataStore (DataStore Preferences)
├── domain/                     # HistoryItem, HistoryType, AppSettings, ThemeMode, AppLanguage
├── ocr/                        # TextRecognitionEngine, LanguageDetectionEngine, OcrOutcome
├── overlay/                    # FloatingLensService (draggable bubble)
├── translate/                  # TranslationEngine, LanguageCatalog, TranslationOutcome
├── ui/
│   ├── splash/, onboarding/    # Splash + 4-page onboarding
│   ├── home/, scan/, history/, tools/, settings/   # Bottom-nav tabs
│   ├── crop/                   # CropActivity + CropOverlayView (select-area screen)
│   ├── ocrresult/, imageocr/, liveocr/
│   ├── translator/, languagemanager/, clipboard/
│   ├── qrscanner/               # Scanner + result screen
│   └── privacy/, premium/
└── util/                        # AppearanceManager, OverlayPermissionHelper, ImageCacheStore, FloatingLensController
```

## Tech stack

Kotlin · Android Views + ViewBinding · Material 3 · MVVM + Repository ·
Navigation Component (Safe Args) · Coroutines + Flow · Room · DataStore ·
CameraX · MediaProjection · Google ML Kit (Text Recognition, Language ID,
Translation, Barcode Scanning) · Google Mobile Ads SDK · Play Billing
Library v7

## Requirements

- JDK 17
- Android SDK, `compileSdk`/`targetSdk` 36, `minSdk` 24 (Android 7.0+)
- Gradle 8.14.3 (via the included wrapper), Android Gradle Plugin 8.9.1,
  Kotlin 2.0.21

## Build instructions

This is a **separate Gradle project** from the root Phone Doctor app — run
all Gradle commands from inside `screenlens/`, not the repo root.

```bash
cd screenlens
./gradlew test            # unit tests (JUnit + Robolectric + Mockito)
./gradlew lint
./gradlew assembleDebug    # -> app/build/outputs/apk/debug/app-debug.apk
```

### Debug build

Debug builds always use Google's public AdMob test IDs and are debuggable.
No secrets are required to build or run a debug APK.

### Release build

Release builds are minified/shrunk and need a signing config, supplied
**either** via a local, git-ignored `secrets.properties` file **or** via
environment variables (what CI uses):

```bash
cp secrets.properties.example secrets.properties
# then fill in KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD,
# and optionally ADMOB_APP_ID / BANNER_AD_UNIT_ID / INTERSTITIAL_AD_UNIT_ID
./gradlew assembleRelease
./gradlew bundleRelease
```

If no signing config is present (locally or via env vars), `assembleRelease`
still builds but produces an **unsigned** APK; `secrets.properties` is
git-ignored and `secrets.properties.example` documents the required keys
with no real values.

## Signing

- Never commit a real keystore, `secrets.properties`, or any password.
- `.gitignore` excludes `*.keystore`, `*.jks`, `secrets.properties`, and
  `screenlens/local.properties`.
- If you generate a release keystore locally, keep it **outside** this
  repository and reference it via `KEYSTORE_FILE` in your local
  `secrets.properties`.

## GitHub Actions

`.github/workflows/screenlens-android-build.yml` (at the repo root, scoped to
`screenlens/**` via path filters) runs on every push/PR:

1. Checkout, JDK 17, Android SDK, Gradle cache
2. `./gradlew test`
3. `./gradlew lint` (fails the job on lint errors; the HTML report is
   uploaded as an artifact either way)
4. `./gradlew assembleDebug` → uploads `screenlens-debug-apk`
5. If release-signing secrets are present: `assembleRelease` +
   `bundleRelease` → uploads `screenlens-release-apk` / `screenlens-release-aab`

### Secrets setup (for release builds in CI)

Configure these under **Settings → Secrets and variables → Actions** on the
GitHub repository (kept separate from the Phone Doctor app's `KEYSTORE_*`
secrets so the two apps' signing material never collides):

| Secret | Purpose |
| --- | --- |
| `SCREENLENS_KEYSTORE_BASE64` | `base64` of your release `.keystore`/`.jks` file |
| `SCREENLENS_KEYSTORE_PASSWORD` | Keystore password |
| `SCREENLENS_KEY_ALIAS` | Key alias |
| `SCREENLENS_KEY_PASSWORD` | Key password |
| `SCREENLENS_ADMOB_APP_ID` | *(optional)* production AdMob App ID |
| `SCREENLENS_BANNER_AD_UNIT_ID` | *(optional)* production banner ad unit ID |
| `SCREENLENS_INTERSTITIAL_AD_UNIT_ID` | *(optional)* production interstitial ad unit ID |

Without these, CI still runs tests/lint/debug build successfully — the
release-build steps are skipped with a clear `::notice::` in the log, not a
failure.

## Permissions

ScreenLens requests the smallest permission set each feature genuinely
needs:

| Permission | Why |
| --- | --- |
| `CAMERA` | Live OCR and QR/Barcode scanning |
| `SYSTEM_ALERT_WINDOW` | Floating Lens bubble (opt-in, off by default) |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `FOREGROUND_SERVICE_SPECIAL_USE` | Required by Android to run the screen-capture and Floating Lens foreground services |
| `POST_NOTIFICATIONS` | Shows the (mandatory, OS-level) foreground-service notifications |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Downloading on-device translation models from Google's ML Kit model service, and checking connectivity first |

MediaProjection's own screen-capture consent is a separate, per-request
system dialog Android shows every time — ScreenLens cannot and does not try
to bypass it. No Contacts, Location, SMS, Phone, or Microphone permissions
are requested; ScreenLens has no feature that needs them.

## Privacy

- Text recognition, language detection, translation and barcode scanning run
  via Google ML Kit's on-device models.
- Screen captures are never uploaded to a ScreenLens server — there isn't
  one. Screenshots are held in memory / a private cache file only for the
  seconds it takes to OCR them, then deleted.
- Saved History (search included) lives entirely in a local Room database
  and can be cleared at any time from Settings.
- The only network calls ScreenLens makes are: downloading an on-device
  translation language model when you tap Download, and serving ads (free
  builds only, via Google AdMob).
- Full text lives in-app under Settings → Privacy (`ui/privacy/PrivacyFragment`).

## Offline features & translation models

- OCR, language identification, and barcode scanning work fully offline
  once ML Kit's small bundled models are available on the device (Google
  Play services may fetch them once, the first time each is used).
- Translation is on-device once **both** the source and target language
  models are downloaded via Language Manager or the in-context "Download
  language model" prompt on the Translator/OCR Result screens. Until then,
  ScreenLens shows *"Download the language model to translate offline"* —
  it never claims a language works offline before its model is installed,
  and it never downloads a model without you tapping Download.
- Downloading a model requires an internet connection; if none is available,
  ScreenLens says so explicitly instead of failing silently.

## Known limitations

- **Arabic OCR (reading Arabic text out of images/screens) is not
  available.** Google ML Kit's on-device Text Recognition only decodes
  Latin-alphabet script (English, French, Spanish, German, Italian,
  Portuguese, Turkish, etc.) — there is no on-device ML Kit model for Arabic,
  Chinese, Japanese, Korean or Devanagari glyphs. This is a real,
  documented ML Kit limitation, not a bug in this app; ScreenLens surfaces
  it honestly (see `error_language_not_supported`) rather than pretending it
  works. **Arabic *translation* and the Arabic *app UI* both work fully** —
  only recognizing Arabic glyphs from a photo/screen does not.
- **Premium purchases are not live.** The Play Billing Library v7
  integration is real and connects to Google Play Billing, but no product
  (`screenlens_premium_lifetime`) has been created in Play Console for this
  app, so the Premium screen correctly reports "not configured" instead of
  faking a purchase. Wiring it up only requires creating that in-app product
  in Play Console — no code changes.
- **This build environment has no Android SDK and no network access to
  `dl.google.com`**, so `./gradlew` could not be executed here to produce a
  verified local build. See `BUILD_REPORT.md` for exactly what was and
  wasn't verified, and why — GitHub Actions (which does have both) is the
  source of truth for build status.
- **No production AdMob/Play Billing identifiers are configured.** Debug
  builds use Google's public test ad unit IDs; release builds fall back to
  the same test IDs unless real ones are supplied via secrets.
- Store screenshots are not included — see `store-assets/README.md`.

## Testing

`app/src/test/java/com/screenlens/app/...` (JUnit 4 + Robolectric +
Mockito + kotlinx-coroutines-test):

- `data/db/HistoryDaoTest` — Room search, type filtering, ordering, and
  `trimToLimit` against a real in-memory database
- `data/repository/HistoryRepositoryTest` — save/trim/search logic against a
  fake in-memory DAO
- `data/settings/SettingsDataStoreTest` — defaults and persistence for every
  setting (theme, language, history limit, onboarding flag, …)
- `translate/LanguageCatalogTest` — English/Arabic presence, no duplicate
  codes, sorted order
- `domain/HistoryMappingTest` — entity ⇄ domain round-trip, safe fallback for
  an unknown stored type
- `ui/common/HistoryDisplayTest` — icon/label mapping per history type, date
  formatting
- `ocr/OcrOutcomeTest` — sealed-class OCR outcome basics

Run with `./gradlew test`.
