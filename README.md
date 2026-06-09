# PacePal

This repository contains two Android apps:

- **`app/`** — PacePal, the original BAC tracking app.
- **`tutor/`** — AoPS Tutor, a Socratic math tutor for the book
  *Introduction to Counting & Probability* (David Patrick).

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
