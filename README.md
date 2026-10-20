# Screen

A call blocker for Android (India). Screen rejects incoming calls that match your rules: a regex on the
number, or a regex on the caller name the network sends. It does **not** replace your phone app. It only
holds the call screening role (`ROLE_CALL_SCREENING`) and works alongside your dialer (e.g. Google Phone).

Every call that reaches the service is logged with all the fields and extras the system passes in. Use the
log to see what Jio, Airtel, Vi and BSNL actually send.

- Saved contacts never reach the service, so they always ring.
- Allow rules are checked first, then block rules, then "Block all unknown callers" if it's on.
- The 160 series (banks, insurance, government) is on the allow list by default.
- No `INTERNET` permission, and backups are disabled. Everything stays on the device.

## Layout

```
app/app/src/main/java/dev/codingcoffee/screen/
  RegexCallScreeningService.kt   # onScreenCall: decide, respond, then log
  ScreenApp.kt                   # app singletons, in-memory rule cache
  rules/RuleEngine.kt            # normalization, default rules, evaluation order
  data/SettingsStore.kt          # rules + settings (Preferences DataStore)
  data/CallLogStore.kt           # filesDir/call_log.jsonl (JSON Lines, append-only)
  ui/                            # Compose screens
```

## Build

```sh
nix develop
cd app
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit tests (rule engine)
./gradlew lintDebug            # optional
```

## Run on an emulator

```sh
avdmanager create avd -n pixel37 -d pixel_8 -k "system-images;android-37.0;google_apis;x86_64"
emulator -avd pixel37

adb devices # ensure device is running
cd app && ./gradlew installDebug
adb shell am start -n dev.codingcoffee.screen/.MainActivity

# in case its not killed
adb emu kill

emulator -list-avds
avdmanager delete avd -n <name>
```

Open the app and grant the call screening role. Then simulate incoming calls:

```sh
adb emu gsm call +911409806457   # 140 series: blocked by default
adb emu gsm call +911601234567   # 160 series: allowed by default
adb emu gsm call +919876543210   # no rule: allowed (blocked if "Block all unknown callers" is on)
adb emu gsm cancel +919876543210 # hang up an allowed call
```

The emulator can't send a network caller name, so you can't test the "Carrier spam label" rule there. Use
the rule editor's Test field instead.

On a real device, only real calls from numbers that aren't in your contacts reach the service. To check
what your carrier sends (for example, whether Jio's "Spam Alert" arrives in `callerDisplayName`), open
an entry in Activity and look at the raw JSON.

To pull the raw log from a debug build:

```sh
adb shell run-as dev.codingcoffee.screen cat files/call_log.jsonl
```

## Release build

### Signing key (once)

```sh
keytool -genkeypair -v -keystore ~/.android/screen-release.jks \
  -alias screen -keyalg RSA -keysize 4096 -validity 10000
```

Back up the keystore. Updates must be signed with the same key; losing it means uninstalling, which wipes
rules and the call log.

Add the credentials to `~/.gradle/gradle.properties` (user-level, never the repo), then
`chmod 600 ~/.gradle/gradle.properties`:

```properties
SCREEN_STORE_FILE=/home/<you>/.android/screen-release.jks
SCREEN_STORE_PASSWORD=<password>
SCREEN_KEY_ALIAS=screen
SCREEN_KEY_PASSWORD=<password>
```

Use an absolute path; Gradle doesn't expand `~`. Without these properties the release APK is built unsigned.

### Build and verify

```sh
nix develop
cd app
./gradlew assembleRelease
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

The output is `app/build/outputs/apk/release/app-release.apk`. If it's named `app-release-unsigned.apk`, the
signing properties weren't picked up.

### Install and run on the emulator

Debug and release builds are signed with different keys, so uninstall the debug build first. This wipes the
app's data.

```sh
adb uninstall dev.codingcoffee.screen
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n dev.codingcoffee.screen/.MainActivity
```

Later release builds install over the previous one with `adb install -r`, keeping data.

### Install on a phone

With USB debugging on: `adb install -r app/build/outputs/apk/release/app-release.apk`. Or copy the APK to the
phone and open it, allowing "Install unknown apps" when asked. If granting the call screening role is
blocked as a restricted setting, go to Settings → Apps → Screen → ⋮ → Allow restricted settings.

### Debugging a release crash

```sh
adb logcat -c
adb shell am start -n dev.codingcoffee.screen/.MainActivity
adb logcat -d -b crash
```

R8 obfuscates class names. Map them back with `app/build/outputs/mapping/release/mapping.txt` from the same
build. `run-as` doesn't work on release builds; use the Copy button in Log detail to get raw entries.
