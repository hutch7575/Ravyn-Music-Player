# Publishing ViTune Classic APK updates

ViTune Classic checks GitHub Releases (this repository) for newer versions and can
download a **signed universal APK** from within the app. Android still asks the
user to approve installation, and enforces that updates use the same signing key.

## One-time setup

1. Create a permanent Android release signing keystore with `keytool` on a
   trusted computer. Keep an offline copy of the keystore and passwords; losing
   it means existing installations cannot receive compatible APK updates.
2. Set these GitHub Actions repository secrets under
   **Settings → Secrets and variables → Actions**:
   - `ANDROID_RELEASE_KEYSTORE_BASE64` — base64 of the complete keystore file
   - `ANDROID_RELEASE_STORE_PASSWORD`
   - `ANDROID_RELEASE_KEY_ALIAS`
   - `ANDROID_RELEASE_KEY_PASSWORD`
3. Never commit the keystore or its passwords to this repository.

For example, create a keystore on a computer with a JDK:

```bash
keytool -genkeypair -v -keystore vitune-classic-release.jks -alias vitune-classic -keyalg RSA -keysize 3072 -validity 10000
base64 -w 0 vitune-classic-release.jks
```

On macOS use `base64 < vitune-classic-release.jks | tr -d '\n'`
instead of `-w 0`. Do not paste the private key or secret into a chat.

## Publish an update

After merging and testing the changes on `master`, choose a new semantic version,
for example `v1.2.5` (the original fork's version is `1.2.4`).

Create a tag at the intended `master` commit and push it:

```bash
git checkout master
git pull
git tag v1.2.5
git push origin v1.2.5
```

GitHub Actions builds a universal, signed APK and publishes a GitHub Release.
The app discovers the release by its version tag and downloadable `.apk` asset.
The workflow derives a monotonically increasing Android `versionCode` from
MAJOR.MINOR.PATCH; never reuse a version number.

## Important limitations

- The production APK uses application ID `app.vitune.classic`.
- GitHub debug builds use `app.vitune.classic.debug` and usually have **different,
  disposable debug signing keys**. They are not eligible for in-app production
  updates. Export a library backup before changing debug builds.
- You must keep the same release signing key for every subsequent update.
- If Android hasn't allowed this app to install updates, the app opens the
  Android permission screen and the user must retry after approving.
- No GitHub release exists until the first signed release is published.
- Always verify backup/restore and favourites on a test device before releasing.
