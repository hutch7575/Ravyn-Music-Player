# Publishing Ravyn APK releases

Ravyn checks [GitHub Releases](https://github.com/hutch7575/Ravyn/releases) for newer versions and can download a **signed universal APK** from within the app. Android asks users to approve installation and enforces update signing compatibility.

## Before the first release

1. Rename this repository to **Ravyn** in **Settings → General → Repository name**. The update checker and installer expect `hutch7575/Ravyn`; do not publish while the repository still has the previous name.
2. Create a permanent Android release signing keystore on a trusted computer. **Keep an offline copy** of the file, alias and passwords. If you lose it, Android cannot install future updates over existing releases.
3. Add these GitHub Actions repository secrets in **Settings → Secrets and variables → Actions**:
   - `ANDROID_RELEASE_KEYSTORE_BASE64`: base64 encoding of the complete keystore
   - `ANDROID_RELEASE_STORE_PASSWORD`
   - `ANDROID_RELEASE_KEY_ALIAS`
   - `ANDROID_RELEASE_KEY_PASSWORD`
4. Never commit a keystore, its base64 contents, or passwords to GitHub, and don't paste them into chat.

Example using a JDK:

```bash
keytool -genkeypair -v -keystore ravyn-release.jks -alias ravyn -keyalg RSA -keysize 3072 -validity 10000
base64 -w 0 ravyn-release.jks
```

On Windows PowerShell, obtain base64 with `[Convert]::ToBase64String([IO.File]::ReadAllBytes("ravyn-release.jks"))`. Save the keystore securely and enter the base64 as the corresponding GitHub secret. On macOS, run `base64 < ravyn-release.jks | tr -d '\n'`.

## Publishing

Merge the tested changes to `master`, then create and push a version tag:

```bash
git checkout master
git pull
git tag v1.0.0
git push origin v1.0.0
```

The [release workflow](../.github/workflows/android-release.yml) builds a release-signed **universal** APK and creates a public GitHub Release with the asset named `Ravyn-v1.0.0.apk`. The in-app updater only selects production APK assets, not debug/nightly artifacts.

Use a new semantic version for every subsequent release. The workflow derives `versionCode` from `MAJOR.MINOR.PATCH`; never reuse a tag.

## Important

- Production Android application ID: `app.ravyn.music` (changed before the first Ravyn release).
- Debug ID: `app.ravyn.music.debug`. Debug builds use different signing keys and cannot be updated in place with production APKs.
- Existing `app.vitune.classic` and `app.vitune.classic.debug` installs remain separate. Export a library backup from the previous app, install Ravyn, and import the backup.
- Keep using the **same release signing keystore** for every future production version.
- Test backup/restore, favourites and playback on a device before tagging a release.
