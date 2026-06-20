# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build and install on connected device/emulator
./gradlew clean assembleDebug    # Clean build

./gradlew lint                   # Run lint checks
./gradlew test                   # Unit tests (JVM)
./gradlew connectedAndroidTest   # Instrumented tests (requires device/emulator)

# Run a single unit test class
./gradlew test --tests "com.project.wellbeingapp.ExampleUnitTest"
```

## Tech Stack

- **Language**: Kotlin 2.2.10
- **UI**: Jetpack Compose (no XML layouts) with Material3
- **AGP**: 9.1.1 · **Compose BOM**: 2026.02.01
- **minSdk**: 24 · **targetSdk/compileSdk**: 36
- All dependency versions are managed in `gradle/libs.versions.toml` (version catalog)

## Architecture

Single-module app (`app/`). Currently a blank-slate template — `MainActivity` is the only entry point and calls `setContent {}` with Jetpack Compose directly.

**Theme** (`ui/theme/`): `WellbeingAppTheme` wraps `MaterialTheme` and enables dynamic color (Material You) on Android 12+. Light/dark schemes are defined in `Color.kt`; typography in `Type.kt`.

When adding features, follow Compose-first patterns: `@Composable` functions for UI, `ViewModel` + `StateFlow`/`State` for state, no XML layouts. Navigation should use `androidx.navigation.compose` when screens are introduced.
