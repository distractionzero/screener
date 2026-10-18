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
