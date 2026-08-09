# Store Assets

This folder holds graphics for the Google Play Store listing. It currently
contains a generated 512×512 app icon (`ic_launcher_play_store_512.png`)
matching the in-app adaptive icon. Everything else below is a checklist of
what still needs to be produced from real, on-device screenshots before
submitting to Play Console — **no placeholder or fake screenshots are
included in this repository**, since presenting mockups as real product
screenshots would be misleading to users.

## Required assets and sizes

| Asset | Size | Format | Notes |
|---|---|---|---|
| App icon | 512 × 512 px | 32-bit PNG (with alpha) | `ic_launcher_play_store_512.png` provided |
| Feature graphic | 1024 × 500 px | JPG or 24-bit PNG (no alpha) | Shown at the top of the store listing |
| Phone screenshots | min 2, max 8 | JPG or 24-bit PNG | 16:9 or 9:16, min dimension 320px, max 3840px |
| 7" tablet screenshots | optional, max 8 | JPG or 24-bit PNG | Only if the app is promoted for tablets |
| 10" tablet screenshots | optional, max 8 | JPG or 24-bit PNG | Only if the app is promoted for tablets |
| Promo video | optional | YouTube URL | Short product demo |

## Suggested screenshots to capture

1. Home Dashboard with a completed health score
2. Full Device Scan in progress
3. Scan Results with categories
4. Display Test (color screen)
5. Sensors Test with live readings
6. Test History
7. Settings (showing Dark Mode / Light Mode / RTL Arabic)

## How to capture real screenshots

1. Build and install a debug or release APK on a physical device or emulator.
2. Run through the flows above so each screen has realistic, populated data
   (run a full scan first so Home and History are not empty).
3. Use the device's screenshot function (or `adb shell screencap`) to capture
   PNGs at native device resolution.
4. Crop/resize to the dimensions above before uploading to Play Console.

## Feature graphic guidance

Keep it on-brand: dark navy background, cyan/blue accent, the Phone Doctor
mark, and the tagline "Check. Diagnose. Protect." No screenshots should be
composited into the feature graphic without following Play Store policy on
device frames and marketing copy.
