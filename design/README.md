# Handoff: Paper Launcher (Android, foldable + tablet)

## Overview
A minimalist Android home-screen launcher in a "printed paper" aesthetic: cream paper background, ink-black 2px rules, one accent color (default burgundy), condensed Oswald type, mono Material icons, grayscale photos. Every home-screen tile is a **widget** the user can add, move, resize and remove on a grid; the widget's appearance changes with its span (S / M / L variants). Multiple horizontally-swipeable pages per screen. Optimized for Galaxy Z Fold-class devices (inner + cover display) and Android tablets.

Target: a native Android APK (Kotlin, Jetpack Compose recommended; `Role.HOME` launcher). Use foldable posture APIs (Jetpack WindowManager) to switch layouts between cover and inner displays.

## About the Design Files
Files in `design/` are **design references created in HTML** — interactive prototypes showing the intended look and behavior. They are not production code. Recreate them in Kotlin/Compose using Android conventions (AppWidget-less: these are the launcher's own widgets, not AppWidgetHost widgets — though hosting third-party AppWidgets could be a later addition). Open `design/Paper Launcher.dc.html` and `design/Launcher Screens.dc.html` in a browser to interact (same folder; `support.js` and `image-slot.js` must sit beside them).

## Fidelity
**High-fidelity.** Colors, type, spacing, rules and widget layouts are final. Recreate pixel-close. Sample data (times, temperatures, song, scores) is placeholder.

## Screenshots
`screenshots/` — 01 home screens page 1 (all four form factors), 02 fold-inner page 2 (agents + shortcuts), 03 edit mode, 04 add-widget sheet, 05–06 widget gallery (all types × S/M/L), system-screens (Settings, Accent dialog, All Apps, Add Widget sheet).

---

## Design Tokens
Colors
- Paper (background): `#EDE7D9`
- Ink (text, rules, borders): `#1D1C1A`
- Muted text: `#5A5650`
- Track / empty bar: `#CFC9BB`
- Accent default **Burgundy `#7A1F2E`**; presets: Teal `#1E8A82`, Gold `#D9A21B`, Red `#D3312A`, Cobalt `#1E4FB8`, Olive `#6B8E23`. Fully user-customizable via HSV picker.
- Battery fill: `#8CC63F` (only non-palette color)
- Dark canvas behind dialogs / scrim: `rgba(70,67,60,.78)`

Typography — **Oswald** (Google Fonts, weights 400/500/600/700). ALL CAPS throughout.
- Display numerals (clock): 86–116 sp, weight 500, line-height 0.8, letter-spacing −0.02em
- Screen titles: 50 sp / 500 (e.g. SETTINGS, ALL APPS); dialog titles 36 sp / 500
- List rows: 26–30 sp / 500
- Widget primary values: 34–62 sp / 500
- Labels/eyebrows: 9–12 sp / 600, letter-spacing 0.15–0.3em, often muted color
- Body small: 11–14 sp / 600, letter-spacing 0.1–0.15em

Icons — **Material Symbols Rounded**, FILL 1, weight 400, rendered in Ink (or Accent for the widget's "hero" glyph). App shortcut icons are rendered **monochrome** (tint app icon to Ink, or use icon-pack glyphs).

Rules & shapes
- All borders/dividers: 2px solid Ink. Dashed dividers: 2px dashed Ink.
- Widget frame: 2px Ink border, paper fill, **0 radius** (Settings → Corner rounding: Square | Rounded = 22dp).
- Bars: 2px Ink border, 1–2px inner padding, Accent fill, Track as empty.
- Accent "tick": 20–22 × 2px Accent bar used as a decorative mark under eyebrow text.
- Photos: `grayscale(1) contrast(1.05–1.1)`.
- No shadows anywhere. No gradients.

Spacing
- Screen padding 12dp; grid gap 8dp; widget inner padding 12–16dp.
- Page indicators row reserved at bottom: 36dp.

---

## Grid system
Column count per form factor; cell is square: `cell = (screenWidth − 24 − 8·(cols−1)) / cols`.
- Fold inner (≈775×930dp): **8 columns**
- Fold cover (≈386×990dp): **4 columns**
- Tablet landscape (1280×800dp): **12 columns**
- Tablet portrait (800×1280dp): **8 columns**

Rows flow downward; the screen does not scroll vertically — add pages instead. Each widget stores `{type, col, row, w, h, page, config}` (1-indexed col/row, spans in cells). Each display (inner / cover / tablet orientations) keeps its own layout set; layouts persist (DataStore/Room).

### Widget size variant from span
Variant is computed from the span, not chosen directly: `L` if w ≥ L.w && h ≥ L.h, else `M` if ≥ M mins, else `S`. Minimum spans (w×h):

| type | S | M | L |
|---|---|---|---|
| hero (clock+photo) | 4×4 | 4×6 | 4×8 |
| clock | 2×1 | 2×2 | 4×2 |
| date | 2×1 | 1×2 | 2×2 |
| weather | 2×1 | 4×1 | 4×3 |
| music | 2×1 | 4×1 | 4×2 |
| photo/quote | 4×1 | 4×2 | 8×3 |
| battery & signal | 2×1 | 4×1 | 2×2 |
| storage & RAM | 2×1 | 4×1 | 4×2 |
| calendar | 2×1 | 4×2 | 4×3 |
| search | 2×1 | 4×1 | 8×1 |
| app shortcuts | 1×1 | 2×1 | 2×2 |
| car (Tessie) | 2×1 | 4×1 | 4×3 |
| sports | 2×1 | 4×1 | 4×3 |
| ask (AI agent) | 2×1 | 4×1 | 4×2 |

Add Widget places the chosen S/M/L minimum span at the first free slot (row-major) on the current page.

---

## Screens / Views

### 1. Home — Fold inner (default preset "Hero left")
Left half (cols 1–4, rows 1–9): **Hero** widget = clock block on top (hours Ink / minutes Accent stacked, date column OCT/23/2026, vertical "FRIDAY" rotated 180° reading bottom-to-top, right-aligned mantras "KEEP IT SIMPLE", "GO OUTSIDE TODAY" with accent ticks) over a grayscale landscape photo with a large accent "06" (calendar-week or day-of-month numeral, user-configurable) and a right-aligned caption "CLIMB / PAUSE / LISTEN / REPEAT" (last word Accent).
Right half (cols 5–8): Weather L (rows 1–3), Battery M (4), Storage M (5), Car M (6), Calendar M (7–8), Sports S + App shortcuts M (9).
**Page 2:** Hero again on left; right: Ask L (combined), Ask M ×3 (Claude, Grok, Codex), Music M, App shortcuts L (6 apps).
Alternative preset "Widget grid" (Settings → Fold inner layout): no hero; Clock L, Date L, Battery L top row, Photo M + Weather L, Music M, Storage M, Calendar M, Search M, two App-shortcut L, Photo S.

### 2. Home — Fold cover (4 col)
Clock M + Date L top; Photo M; then Weather M, Battery M, Storage M, Music M, Search M (each 4×1); App shortcuts M ×2.

### 3. Home — Tablet landscape (12 col) / portrait (8 col)
See `Paper Launcher.dc.html` DEFAULTS `tabL` / `tabP` for exact placements.

### 4. Page indicators
Centered at bottom (12dp from edge), 8×8dp squares, gap 10dp. Active = solid Ink; inactive = 2px Ink outline. Tap to jump; horizontal swipe on empty screen area pages. In edit mode an extra empty page is appended so widgets can be dragged onto it.

### 5. Edit mode
Enter: long-press any widget (500ms) or empty area. Visual: 25% paper wash over widget content, 2px Accent inset ring, type/size/span badge top-left (9sp Accent), 24×24 "×" remove button top-right (2px Ink border), Accent triangle resize handle 26dp bottom-right. Top bar actions: "+ ADD WIDGET" (Accent outline), "DONE" (paper fill). Drag snaps to cells; drop that overlaps another widget reverts to origin. Resize clamps to grid bounds; min 1×1.

### 6. Add Widget sheet
Centered modal over scrim; 2px Ink border; header "ADD WIDGET" 36sp + Accent close glyph; grouped list with Accent group labels and Ink rule: INFORMATION (Battery & Signal, Storage & RAM, Weather), MEDIA (Music, Photo), TEXT (Quote, Search), TIME (Calendar, Clock, Date / Day, Clock + Photo hero), CONNECTED (Car · Tessie, Sports Scores, Ask an Agent). Each row: name 26–28sp + S / M / L outline buttons (or span hint).

### 7. Settings
Full-screen on paper, 2px Ink frame inset 10dp. Header "SETTINGS" 50sp + Accent close. Groups (Accent 16sp label + Ink rule): 
- GENERAL: Home app, Clock, Temperature, Week starts on, Status bar, Backup settings, Restore settings
- ACCOUNTS: Tessie (token), Sports teams, Calendars (pick from device calendars), AI agents
- LOOK: Accent color (swatch), Fold inner layout (Hero left | Widget grid), Wallpaper, Icon pack, App icon size, Corner rounding (Square | Rounded), App names on home
Row: label 26sp left, value 11sp/600/0.18em muted right (128dp column), height 52dp.

### 8. Accent color dialog
Preview block "07 23" (hours Ink, minutes Accent) + "SAME SKY / DIFFERENT STORY" with accent tick; 6-column swatch grid (40dp, 2px Ink border; selected shows paper+ink inner ring); HUE / SATURATION / BRIGHTNESS sliders — 14dp pill tracks, Ink filled portion, Track remainder, 4×36dp Ink thumb bar; HEX readout 26sp; RESET (outline) and USE THIS (Ink fill) buttons.

### 9. All Apps drawer
Swipe up from home. Header "ALL APPS" 50sp; search row 30sp muted; alphabetical sections (Accent letter + rule), app names 30sp text-only (no icons); bottom dock of 6 mono icons + 34dp outlined "tune" button.

---

## Widgets — content per variant
(Exact markup in `design/Widget.dc.html`; summary here.)
- **Clock** S: "12 21" inline, minutes Accent. M: stacked 84sp + FRI / OCT 23 bottom-right. L: stacked 92sp + date column + vertical FRIDAY + mantras.
- **Date** S: "OCT 23" + FRI accent. M: vertical "FRIDAY". L: OCT/23/2026 stacked + FRIDAY accent.
- **Weather** S: icon + temp + condition. M: icon, temp, condition, location (Accent) | 3-day strip (day, icon, hi/lo). L: adds H/L, feels-like, refresh glyph, stats row (humidity, wind w/ rotated arrow, UV, sunrise–sunset) with 2px vertical dividers, 24-hour temperature polyline (Ink 2.5px) with Accent dots and labels, hour axis.
- **Music** S: title, artist, progress line w/ Accent dot, outlined Accent pause circle. M: + 66dp grayscale album art, prev/next, times. L: 160dp art, playlist eyebrow, 30sp title, album, controls.
- **Photo/Quote** S: text mantra only. M: photo + 3-line mantra + tick. L: photo + big accent numeral + 4-line caption.
- **Battery & Signal** S: battery outline + 43%. M: dashed signal bars (cell / Wi-Fi, 8px dashes), carrier label, long battery + %. L: 60sp %, bar, "CHARGING · 1H 20M TO FULL", signal.
- **Storage & RAM** S: two labeled bars. M: dashed divider + broom icon (Accent, −20°) | bars with used/total. L: larger with 22sp values.
- **Calendar** S: 54sp day + OCT/FRI. M: "OCTOBER" 34sp + next event (Accent time · title) + year; weekday header (M T W T F S S, Monday-first) with rule; 5-row grid, today = Accent fill box with paper text; Accent 4px dot under days with events. L: 48sp month, next-3-events column, same grid at 14sp. Data: Android `CalendarContract` (Outlook app syncs into it; no Graph API). User picks calendars in Settings → Accounts.
- **Search** S: search (Accent) + mic glyphs. M: + "SEARCH" label. L: "SEARCH APPS, CONTACTS, WEB" + LENS/camera/mic. Opens launcher search (apps, contacts, web).
- **App shortcuts** configurable list; S: 1 icon, M: 2, L: up to 6 in a 2- or 3-column grid; 26sp mono glyphs. Long-press in edit mode → pick apps.
- **Car · Tessie** S: % + lock glyph, bar, "214 MI · CHARGING". M: 72×56 car image | % + range, bar, charging line (Accent) | 2px divider | LOCK + CLIMATE 21° (38dp square buttons; active = Ink fill). L: 124×78 car image, 44sp %, model/location, charging line, "4 MIN AGO · TESSIE" stamp + refresh glyph, bar with Ink limit tick at 80%, stats row (inside, outside, tires, odometer), 4 action buttons LOCK / 21° climate / PORT / HONK (40dp tall).
- **Sports** S: GSW 98 · Q4 2:14 (Accent) · LAL 95. M: live score block | NEXT two fixtures. L: LIVE eyebrow, 62sp scores with records, venue/TV, quarter box-score table with rules, next fixtures.
- **Ask (AI agent)** config `agent ∈ {claude, grok, codex}`. S: agent glyph (Accent) + "ASK CLAUDE" + arrow. M: glyph, agent eyebrow (Accent), text field "ASK CLAUDE…" 22sp, mic, 34dp Ink send square. L: "ASK — SENDS TO THE SELECTED AGENT" eyebrow, agent segmented row (CLAUDE Ink-filled when selected, GROK, CODEX outlined), 30sp text field with bottom rule, "RECENT · …" line, mic, 38dp send.
  Glyphs: Claude `auto_awesome`, Grok `bolt`, Codex `terminal` (swap for official mono marks if licensed).

---

## Interactions & Behavior
- Page swipe: horizontal drag > 60dp on empty area → next/prev page; animate 250ms standard easing. Indicators update.
- Long-press 500ms → edit mode (haptic). Drag moves with cell snapping; resize from corner handle; × removes; DONE / back exits.
- Widget tap (normal mode) opens the relevant app/activity: clock → alarm/clock app; date/calendar → calendar app; weather → weather app or detail; music → media session app; battery → battery settings; storage → storage settings; search → launcher search; shortcuts → launch app; car actions → Tessie commands (confirm for unlock/honk); sports → scores app/web; ask → see below.
- **Ask**: typing + send (or IME action) opens the agent app with the text: Claude / Grok / ChatGPT-Codex via `ACTION_SEND` text intent to the package or an app deep link if available; fallback: launch the app and copy text to clipboard with a toast "COPIED · PASTE IN CLAUDE". Mic uses system `RecognizerIntent` speech-to-text into the field.
- **Car (Tessie API)**: `https://api.tessie.com`, Bearer token (Settings → Accounts → Tessie). Poll `GET /vehicles` (cached state, does not wake the car) every 3–5 min while launcher visible; `GET /{vin}/status` for asleep/awake (show "SLEEPING" muted when asleep); commands `POST /{vin}/command/{lock|unlock|start_climate|stop_climate|open_charge_port|honk}`. Show "UPDATED N MIN AGO". Car image: Tesla compositor render from option codes — Model 3 (2020), Pearl White Multi-Coat `PPSW`, 18" Aero wheels — rendered side view, cached locally, displayed with grayscale filter (allow user photo override).
- **Sports**: team picks in Settings; a free scores API (e.g. ESPN public JSON / TheSportsDB); poll 60s during live games, else hourly.
- **Weather**: device location + any weather provider; Celsius/Fahrenheit from Settings.
- Loading/empty: show widget frame with muted eyebrow label and a 2px Track bar; never spinners.
- Errors: muted text "NO DATA · TAP TO RETRY" in the widget.
- Fold/unfold: load the layout set for the new display; preserve current page index when possible.

## State Management
- `layouts: Map<displayKey, List<Widget>>` where displayKey ∈ {innerHero, innerGrid, cover, tabL, tabP}; `Widget{id,type,col,row,w,h,page,config:{agent?,apps?,…}}`. Persist with DataStore (JSON) or Room.
- `ui: {editingDisplay?, sheetOpenDisplay?, pageIndex per display, draggingId?}`.
- `settings: {accentHex, cornerRounding, innerLayoutPreset, tempUnit, weekStart, statusBarHidden, iconPack, iconSize, showAppNames, tessieToken, sportsTeams[], calendarIds[]}`.
- Data caches per connected widget with `updatedAt`.

## Assets
- Fonts: Oswald (OFL) — bundle TTFs. Icons: Material Symbols Rounded (Apache 2.0) — bundle font or vector drawables.
- Photos: user-supplied (hero/photo widgets, album art from MediaSession metadata). Car render: Tesla compositor (see above).
- No other imagery.

## Files
- `design/Paper Launcher.dc.html` — home screens (4 form factors), pages, edit mode, Add Widget sheet, gallery of all widget variants; contains the DEFAULTS layout tables and MIN span table.
- `design/Widget.dc.html` — every widget type × S/M/L (markup is the spec).
- `design/Launcher Screens.dc.html` — Settings, Accent color dialog, All Apps, Add Widget sheet.
- `design/support.js`, `design/image-slot.js` — runtime for the prototypes only.
- `screenshots/` — reference captures.
