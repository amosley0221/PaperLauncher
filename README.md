# Paper Launcher

A minimalist Android home-screen launcher in a "printed paper" style: cream paper, ink rules, one accent colour, Oswald type and mono Material Symbols. Every tile is a resizable widget on a grid, with separate layouts for a foldable's inner and cover screens and for tablets.

Kotlin + Jetpack Compose, min Android 8.0 (API 26).

## Get the APK

Every push builds the APK with GitHub Actions (`.github/workflows/build-apk.yml`):

- **Releases** → `build-latest` → `PaperLauncher.apk` (easiest from a phone), or
- **Actions** → latest "Build APK" run → `PaperLauncher-apk` artifact.

Install it, then choose **Paper Launcher** when Android asks for a Home app (or Settings → Apps → Default apps → Home app).

All builds are signed with the same project key (`keystore/`), so a new build installs over an old one.

## Build locally

```bash
./gradlew assembleRelease
```

Needs JDK 17 and the Android SDK (platform 35). Output: `app/build/outputs/apk/release/app-release.apk`.

## Using it

- **Long-press** anywhere → edit mode: drag widgets, resize from the accent corner, × removes, tap a widget to configure it (pick apps, photo, agent). Hold a widget at the screen edge to move it to the next page. **+ ADD WIDGET**, **SETTINGS** and **DONE** sit along the bottom.
- **Swipe left/right** to change pages; **swipe up** for All Apps.
- Widget size (S / M / L) follows its span; see `design/README.md`.

### Connected widgets

| Widget | Setup |
|---|---|
| Weather | Tap it to allow location. Data from Open-Meteo (no key). |
| Calendar | Tap it to allow calendar access; pick calendars in Settings → Calendars. |
| Music | Tap it to allow notification access (needed to read and control media sessions). |
| Car · Tessie | Settings → Tessie: paste your Tessie API token. |
| Sports | Settings → Sports teams, e.g. `NBA:GSW` (ESPN public scores). |
| Ask an agent | Sends text to the Claude / Grok / ChatGPT app, or opens the web version if the app isn't installed. |

## Design

The original design handoff (HTML prototypes, screenshots, spec) is in `design/`.
