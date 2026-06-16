# Orbit: Solar System Explorer

A calm, interactive Android app for exploring our solar system. No score, no
fail state, just a place to drift through space and learn about the planets.

## What you can do

- **Pan** to drift across the system and **pinch to zoom** in and out.
- Watch the eight planets orbit the Sun in real time at their own speeds.
- **Tap** any planet (or the Sun) to open a detail view where it spins on its
  axis, alongside a card of facts and a short description.
- Tap the recenter button to fit the whole system back into view.

## Tech

- Kotlin + Jetpack Compose, single module (`:game`).
- Everything is drawn procedurally on a Compose `Canvas`, with no image assets.
- Min SDK 26, target/compile SDK 34.

Sizes, orbital distances and speeds are tuned for a readable, pleasant view;
they are deliberately **not** to true astronomical scale.

## Building

```
gradle :game:assembleDebug
```

The debug APK lands in `game/build/outputs/apk/debug/game-debug.apk`. CI also
builds it on every push and attaches it to a GitHub release for easy install.
