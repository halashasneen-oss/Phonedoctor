# ScreenLens — Store Assets

This folder is a checklist and spec sheet for Google Play Store listing assets.
No image files are included here — screenshots must come from an actual running
build of the app (see [Known limitations](../README.md#known-limitations) for
why none were generated in this environment), never mocked up or faked.

## App icon

- Already implemented in-app as an Adaptive Icon:
  `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (foreground + background
  layers), with a flat-vector fallback (`res/mipmap/ic_launcher.xml`) for
  Android 7.0–7.1 (API 24–25), which predate Adaptive Icons.
- Play Console **Hi-res icon** upload: **512 × 512 px**, 32-bit PNG (with alpha),
  ≤ 1 MB. Export the same lens/scan-frame mark used in
  `ic_launcher_foreground.xml` + `ic_launcher_background.xml` at that
  resolution.

## Feature graphic

- **1024 × 500 px**, JPG or 24-bit PNG (no alpha).
- Suggested content: dark navy/near-black background matching
  `color_background`, the ScreenLens lens/scan-frame mark, wordmark
  "ScreenLens", and the tagline "See it. Understand it." in electric
  cyan/blue (`brand_primary`).

## Phone screenshots

- **Minimum 2, up to 8.** JPG or 24-bit PNG.
- Portrait: 16:9 to 9:16 aspect ratio; each side between 320 px and 3840 px
  (e.g. 1080 × 1920 or 1080 × 2400 is a safe modern choice).
- Capture directly from a device/emulator running a real debug or release
  build — do not composite or fabricate screens.

### Suggested screenshot set and captions

1. **Home** — "See it. Understand it." (hero scan card + quick actions)
2. **Scan permission screen** — "Scan any screen, with your permission every time"
3. **OCR result screen** — "Extract text instantly with on-device OCR"
4. **Translator screen** — "Translate offline once a language is downloaded"
5. **Language Manager** — "Choose exactly which languages live on your device"
6. **QR & Barcode scanner** — "Scan codes without uploading anything"
7. **History with search** — "Every scan stays local and searchable"
8. **Settings — Appearance & Privacy** — "Dark, light, or system — your call"

## 7-inch and 10-inch tablet screenshots (optional)

- Same aspect-ratio rules as phone screenshots, at tablet resolutions
  (e.g. 1200 × 1920 / 1600 × 2560). Optional for initial release since
  ScreenLens targets phones first.

## Promo video (optional)

- YouTube URL only, no asset stored in this repo.

## Store listing copy (reference — keep in sync with README.md)

- **App name:** ScreenLens
- **Short description (≤ 80 chars):** Read, translate and scan text from
  your screen and camera — fully on-device.
- **Full description:** see the *Features* section of the root `README.md`,
  written to be copy-ready and to avoid any claim ScreenLens itself doesn't
  actually implement.

## What is intentionally NOT in this folder

- No pre-rendered PNG/JPG screenshots — they must be produced from a real
  build, on a real or emulated device, after the app is signed and installed.
- No fabricated user reviews, ratings, or install counts.
