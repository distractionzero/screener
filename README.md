

```sh
nix develop
avdmanager create avd -n pixel37 -d pixel_8 -k "system-images;android-37.0;google_apis;x86_64"
emulator -avd pixel37

# in case its not killed
adb emu kill

emulator -list-avds
avdmanager delete avd -n <name>


adb devices # ensure device is running
./gradlew installDebug
adb shell am start -n dev.codingcoffee.screen/.MainActivity
```
