# AlbumPulse for Android

AlbumPulse is an early Android prototype for Nothing Phone (3a) / Nothing OS. It listens to Android media sessions and applies the current track artwork as the lock-screen wallpaper, then restores a chosen fallback image when playback stops.

## Build the APK with GitHub Actions

1. Open the repository's **Actions** tab.
2. Select **Build Android APK** in the left sidebar.
3. Click **Run workflow**, choose the `main` branch, then click **Run workflow** again.
4. When the run finishes successfully, open the run and download the `AlbumPulse-debug-apk` artifact.
5. Extract the downloaded ZIP. The APK is `app-debug.apk`.

The workflow uses Java 17 and Gradle 8.13. GitHub documents the manual workflow process [here](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow).

## First-time setup

1. Install the APK on the Android phone.
2. Open AlbumPulse and choose the image to restore when music is not playing.
3. Open **Notification Access settings** and enable AlbumPulse.
4. Return to AlbumPulse and enable dynamic lock-screen artwork.
5. Play music in Spotify, YouTube Music, or another app that exposes Android media-session metadata and artwork.

## Important limitations

- This is an untested prototype; it has not yet been built and tested on a physical Nothing Phone (3a).
- Android requires the user to grant Notification Access.
- Other music apps work only if they expose compatible media-session metadata and album artwork.
- Nothing OS background restrictions may delay or stop updates.
- The generated debug APK is for testing and is not a Play Store release.
