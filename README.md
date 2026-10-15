# Ravyn

A standalone, open-source Android music player built on the work of [ViTune](https://github.com/bartoostveen/ViTune), with the spirit of classic 2015–2017 Material Design.

Ravyn focuses on a familiar music-library experience, fast playback, playlists, favourites, lyrics, and a simple interface. The classic Material Design-inspired UI is a design direction in progress; the first release preserves the existing, tested player layout.

## Downloads

Install the **signed universal APK** from [GitHub Releases](https://github.com/hutch7575/Ravyn-Music-Player/releases) once the first release is published. Do not install debug artifacts as production releases.

## Development

The Android project uses Gradle, Kotlin/Jetpack Compose, Chaquopy and Python for audio extraction. Source packages currently retain their original `app.vitune.*` namespaces to avoid unnecessary refactors.

Ravyn has the Android application ID `app.ravyn.music`. It installs separately from previous ViTune Classic and debug builds. Back up your library in the old app before switching, then restore it in Ravyn.

For release signing and publication instructions, see [docs/RELEASING.md](docs/RELEASING.md).

## Credits and license

Ravyn is based on ViTune and retains the work, copyrights and licence notices of its original contributors. It uses or references other third-party works, including [YouTube-Internal-Clients](https://github.com/zerodytrash/YouTube-Internal-Clients), [Ionicons](https://github.com/ionic-team/ionicons), and the original ViTune icon credited to [Ilham Fitrotul Hayat](https://www.flaticon.com/authors/ilham-fitrotul-hayat). Ravyn's new icon is an original, separate design.

Licensed under the [GNU General Public License v3.0](LICENSE).

Ravyn is not affiliated with, funded, authorized or endorsed by YouTube, Google LLC, or their affiliates.
