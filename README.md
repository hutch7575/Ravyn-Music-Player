# Music Player

Music Player is an open-source Android app for listening to local music and streaming from YouTube Music.

Previously called Ravyn, Music Player installs as an update and keeps your library, playlists, and listening history.

## Development

The Android project uses Gradle, Kotlin/Jetpack Compose, Chaquopy and Python for audio extraction. Source packages currently retain their original `app.vitune.*` namespaces to avoid unnecessary refactors.

Music Player has the Android application ID `app.ravyn.music`. It installs separately from previous ViTune Classic and debug builds. Back up your library in the old app before switching, then restore it in Music Player.

For release signing and publication instructions, see [docs/RELEASING.md](docs/RELEASING.md).

## Credits and license

Music Player is based on [ViTune](https://github.com/bartoostveen/ViTune) and retains the work, copyrights and licence notices of its original contributors. It uses or references other third-party works, including [YouTube-Internal-Clients](https://github.com/zerodytrash/YouTube-Internal-Clients), [Ionicons](https://github.com/ionic-team/ionicons), and the original ViTune icon credited to [Ilham Fitrotul Hayat](https://www.flaticon.com/authors/ilham-fitrotul-hayat). The Music Player icon is an original, separate design.

Licensed under the [GNU General Public License v3.0](LICENSE).

Music Player is not affiliated with, funded, authorized or endorsed by YouTube, Google LLC, or their affiliates.
