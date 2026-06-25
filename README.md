# PacePal

This repository contains two Android apps:

- **`app/`** — PacePal, the original BAC tracking app.
- **`tutor/`** — AoPS Tutor, a Socratic math tutor for the book
  *Introduction to Counting & Probability* (David Patrick).
- **`solar/`** — Solar System, an offline orbit simulator (planets around the
  Sun, moons around the planets, one Earth year per hour).

## AoPS Tutor

The tutor is a small Android app: a WebView wrapping a single offline HTML page
(`tutor/src/main/assets/index.html`) that talks directly to the Anthropic
Messages API. No server is needed.

### Install on your phone

1. Push to `main` (or run the **Build Tutor APK** workflow manually in the
   Actions tab). The workflow attaches `tutor-debug.apk` to a GitHub release.
2. On your Android phone, download `tutor-debug.apk` from the release, open it,
   and allow installation from unknown sources when prompted.
3. On first launch, paste your Anthropic API key (`sk-ant-...`). It is stored
   only on the device (WebView localStorage) and sent only to
   `api.anthropic.com`.

### Use

- Pick a **Chapter**, **Topic** (Chapter 2 has specific techniques; other
  chapters offer "Any topic from this chapter"), and **Level**
  (Exercise = end-of-section, Challenge = end-of-chapter Challenge Problems).
- **New problem** poses one numbered problem. **Primer** explains the selected
  topic in plain language with a tiny example.
- Type your work in the box — Enter makes a new line, only **Submit** sends.
  **Hint** climbs a 3-rung hint ladder (reframe → technique → first step).
  **Solution** reveals the full solution.
- The tutor never reveals answers otherwise: wrong answers get the likely
  misstep named plus one pointed question; correct answers get a brief
  confirmation and one extension question. The solved/attempted tally is in
  the header.
- Math is plain text only: `C(8,3)`, `8!/(3!*5!)`, `|A ∪ B|`.

### Build locally (optional)

```sh
gradle :tutor:assembleDebug
# APK at tutor/build/outputs/apk/debug/tutor-debug.apk
```

Requires JDK 17 and the Android SDK (compileSdk 34).

### Note on the API key

The original web version kept the key on a Node server. On a phone, the
simplest approach is calling the API straight from the app with your own key
entered once in the app — fine for personal use; don't share the APK with the
key already saved on a device.

## Solar System

A small Android app: a WebView wrapping a single offline HTML page
(`solar/src/main/assets/index.html`) that animates the solar system on a
canvas. No server, no network, no API key.

### Model

- The eight planets orbit the Sun; notable moons orbit Earth, Mars, Jupiter,
  and Saturn (Saturn also gets a ring).
- Time is scaled so **one Earth year takes one hour** at 1x. Every body moves
  at its true relative rate: Mercury laps the Sun about four times per Earth
  orbit, Neptune barely creeps, the Moon circles Earth roughly twelve times a
  year, and so on.
- Distances and body sizes are **not** to scale (true scale is unwatchable on a
  phone): orbital radii are compressed with a power curve and sizes are
  enlarged, but the relative ordering is preserved.

### Use

- **Pinch** to zoom, **drag** to pan, **double-tap** to reset the view.
- The slider sets speed, from about 0.25 up to ~365 years per hour; default is
  1 year per hour as requested.
- Toggle planet **Labels** and **Orbits**, or **Pause**.

### Build locally (optional)

```sh
gradle :solar:assembleDebug
# APK at solar/build/outputs/apk/debug/solar-debug.apk
```
