# AlbumPulse

Android app prototype for dynamic lock-screen album artwork.

## Build

Open this repository in Android Studio, or use the GitHub Actions workflow under `.github/workflows/android.yml` to build a debug APK.

The app listens to Android media sessions and updates the lock-screen wallpaper with the current track artwork. It requires notification access. Compatibility depends on each music app exposing media-session metadata and artwork.

This is an untested prototype; verify it on your device before relying on it.
