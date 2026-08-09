# Phone Doctor

**Check. Diagnose. Protect.**

Phone Doctor is a native Android diagnostic app that checks and tests the
health of your device — battery, storage, memory, display, touch, audio,
microphone, vibration, flashlight, camera, sensors, and connectivity — using
real Android APIs. It runs local-first: diagnostics happen on the device, and
results, history and settings are stored locally with Room and DataStore.
There is no Phone Doctor backend and no server that diagnostic data is
uploaded to.

## Features

- **Home Dashboard** — live health score ring, last check time, quick-test
  grid (Battery, Storage, Memory, Display, Touch, Speaker, Microphone,
  Camera, Sensors, Connectivity) plus a "More Tools" row for CPU/Device Info,
  Vibration and Flashlight.
- **Full Device Scan** — sequential, real checks across 11 categories, with
  inline interactive confirmation for Display/Touch/Audio/Microphone rather
  than a fake timer.
- **Scan Results, Report & History** — health score, per-category status,
  detected issues, shareable text report, exportable PDF report (generated
  on-device with `android.graphics.pdf.PdfDocument`), and a local Room-backed
  history list.
- **14 hardware test screens** — Battery, Storage, Memory, CPU/Device Info,
  Display, Touch, Speaker, Microphone, Vibration, Flashlight, Camera
  (CameraX), Sensors (live readings), Connectivity, and a comprehensive
  Device Information screen with a "Copy Device Info" action.
- **Settings** — Appearance (System/Light/Dark), Language (English/Arabic,
  full RTL support), Notifications, Auto Health Check (WorkManager, weekly),
  Clear History.
- **Premium** — ad-free/advanced-report benefits UI, a real AdMob rewarded-ad
  flow, and an honest "billing not configured" message instead of a fake
  purchase (see [Known limitations](#known-limitations)).
- **Onboarding & Splash** — Android 12+ SplashScreen API, 3-page onboarding.

## Architecture

- **Language:** Kotlin
- **UI:** Android Views (XML layouts) + ViewBinding — no Jetpack Compose
- **Pattern:** MVVM + Repository
- **Navigation:** Jetpack Navigation Component, single-Activity (`MainActivity`)
  hosting all screens as fragments, plus a separate `SplashActivity`
- **Concurrency:** Kotlin Coroutines + Flow
- **Persistence:** Room (scan history), DataStore Preferences (settings)
- **DI:** a small manual `ServiceLocator` (no Hilt/Dagger, kept intentionally
  simple for this app's single dependency graph)
- **Camera:** CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`,
  `camera-view`)
- **Background work:** WorkManager (weekly auto health check, opt-in)
- **Ads:** Google Mobile Ads SDK (banner + rewarded), gated by build type

```
app/src/main/java/com/phonedoctor/app/
├── PhoneDoctorApp.kt          # Application: theme/locale/notifications/AdMob init
├── ServiceLocator.kt          # Manual DI container
├── ads/                       # AdManager (rewarded ad wrapper)
├── data/
│   ├── audio/                 # TonePlayer (AudioTrack), MicRecorder (MediaRecorder)
│   ├── datastore/             # SettingsRepository (DataStore)
│   ├── local/                 # Room AppDatabase, entities, DAO
│   ├── report/                # ReportTextGenerator, PdfReportGenerator
│   └── repository/            # Battery/Storage/Memory/DeviceInfo/Sensors/
│                               # Connectivity repositories + ScanEngine orchestrator
├── domain/
│   ├── model/                 # Pure data classes (BatteryInfo, ScanReport, ...)
│   └── util/                  # HealthScoreCalculator, FormatUtils
├── ui/
│   ├── common/                 # ViewBinding delegate, status/category UI mappers
│   ├── splash/, onboarding/, home/, scan/, results/, report/, history/
│   ├── battery/, storage/, memory/, cpu/, display/, touch/, speaker/,
│   │   microphone/, vibration/, flashlight/, camera/, sensors/,
│   │   connectivity/, deviceinfo/
│   └── settings/, about/, privacy/, premium/, help/
└── work/                       # AutoHealthCheckWorker, WorkScheduler
```

## Requirements

- Android Studio Ladybug (or newer) / JDK 17+
- Gradle Wrapper (bundled — no local Gradle install required)
- `compileSdk` / `targetSdk` 35, `minSdk` 24 (Android 7.0+)
- Kotlin 2.0.21, Android Gradle Plugin 8.6.1

## Build instructions

### Debug build

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Debug builds always use Google's public AdMob **test** ad unit IDs and never
show ads unless explicitly testing that flow — see `BuildConfig.SHOW_ADS`.

### Release build

Release builds need a signing configuration (see below). Once configured:

```bash
./gradlew assembleRelease   # APK
./gradlew bundleRelease     # AAB, for Play Store upload
```

### Unit tests & lint

```bash
./gradlew test    # JVM unit tests (domain logic, Room converters)
./gradlew lint
```

## Signing setup

**No keystore is committed to this repository.** Signing is configured two
ways, matching whichever environment you're building in:

### Local builds

1. Copy `secrets.properties.example` to `secrets.properties` (already
   git-ignored).
2. Generate a release keystore if you don't have one:
   ```bash
   keytool -genkeypair -v -keystore release.keystore -alias phonedoctor \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
3. Fill in `secrets.properties` with `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`,
   `KEY_ALIAS`, `KEY_PASSWORD`. **Never commit this file or the keystore.**

### CI (GitHub Actions)

Configure the repository secrets below (Settings → Secrets and variables →
Actions). If they're absent, the workflow still runs and produces a **debug**
APK — it does not fail, it just skips the release build with a notice.

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 release.keystore` output |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |
| `ADMOB_APP_ID` *(optional)* | Real AdMob application ID for release builds |
| `BANNER_AD_UNIT_ID` *(optional)* | Real banner ad unit ID |
| `INTERSTITIAL_AD_UNIT_ID` *(optional)* | Real interstitial ad unit ID |
| `REWARDED_AD_UNIT_ID` *(optional)* | Real rewarded ad unit ID |

Omitting the AdMob secrets is safe — release builds fall back to Google's
public test ad unit IDs rather than failing.

## GitHub Actions

`.github/workflows/android-build.yml` runs on every push/PR:

1. Checkout, JDK 17, Android SDK, Gradle cache
2. Validate the Gradle wrapper
3. `./gradlew test` (unit tests)
4. `./gradlew lint`
5. `./gradlew assembleDebug` → uploaded as an artifact on every run
6. If signing secrets are present: `assembleRelease` + `bundleRelease` →
   uploaded as artifacts

## Privacy

See the in-app Privacy screen (Settings → Privacy) and
[`PRIVACY.md`](#) equivalent copy below:

> Phone Doctor is designed to perform device diagnostics locally whenever
> possible. Diagnostic results and personal files are not uploaded to a
> Phone Doctor server. Phone Doctor does not collect IMEI, serial number,
> advertising ID, or MAC address. The free version may show ads served by
> Google AdMob, which may use data according to Google's advertising and
> consent settings.

## Permissions

Every dangerous permission is requested **at the moment its test screen is
opened**, never at launch:

| Permission | Used for |
|---|---|
| `CAMERA` | Camera Test screen only (live preview) |
| `RECORD_AUDIO` | Microphone Test screen only |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` (API 33+) or `READ_EXTERNAL_STORAGE` (API ≤32) | Storage screen's optional category breakdown |
| `POST_NOTIFICATIONS` (API 33+) | Only if "Auto Health Check" or "Notifications" is enabled in Settings |
| `VIBRATE`, `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `BLUETOOTH_CONNECT` | Normal/install-time permissions for Vibration Test and Connectivity Test |

Flashlight uses `CameraManager.setTorchMode`, which does **not** require the
`CAMERA` permission.

## Known limitations

- **Battery Health**: Android exposes no public API for a precise battery
  health percentage. Only the coarse `EXTRA_HEALTH` enum (Good/Overheat/Dead/
  etc.) is available on some devices; the app shows "Not available on this
  device" rather than inventing a number.
- **Storage → Apps category size**: a true per-app storage breakdown requires
  `StorageStatsManager` + the `PACKAGE_USAGE_STATS` special access, which
  cannot be granted through a normal runtime permission dialog. This is left
  at 0 rather than shown as an inaccurate estimate.
- **Premium purchases**: the Premium screen's "Upgrade" button is a real UI
  with no fake purchase flow behind it — it explains that Google Play Billing
  needs to be connected (a Play Console step outside what a repository can
  configure). The Rewarded Ad flow, however, is fully functional.
- **AdMob production IDs**: this repo never contains a real, production AdMob
  App ID. Builds fall back to Google's public test IDs unless real IDs are
  supplied via `secrets.properties` (local) or GitHub Secrets (CI).
- **Sandbox build verification**: this project was authored in a sandboxed
  environment without outbound access to `dl.google.com` / Android SDK
  manager endpoints, so the initial build/test/lint pass was verified via
  GitHub Actions CI (which has full internet access) rather than locally —
  see the Actions tab for the actual run.

## Testing

- `./gradlew test` — JVM unit tests covering `HealthScoreCalculator`,
  `FormatUtils`, and the Room `CategoryResultListConverter` round trip.
- `./gradlew lint` — Android Lint.
- Manual QA: every screen was designed to work fully offline except the
  optional rewarded ad and the internet-reachability check in Connectivity
  Test (both degrade gracefully without network).
