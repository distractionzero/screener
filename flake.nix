{
  description = "Android development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    { nixpkgs, ... }:
    let
      systems = [ "x86_64-linux" ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
    in
    {
      devShells = forAllSystems (
        system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config = {
              allowUnfree = true;
              android_sdk.accept_license = true;
            };
          };
          buildTools = "36.0.0";
          android = pkgs.androidenv.composeAndroidPackages {
            platformVersions = [ "29" "37.0" ];
            buildToolsVersions = [ buildTools ];
            includeEmulator = true;
            includeSystemImages = true;
            systemImageTypes = [ "google_apis" ];
            abiVersions = [ "x86_64" ];
            includeNDK = false;
          };
          sdk = "${android.androidsdk}/libexec/android-sdk";
        in
        {
          default = pkgs.mkShell {
            packages = [
              android.androidsdk
              pkgs.android-studio
              pkgs.jdk25
              pkgs.pngquant
              pkgs.oxipng
              pkgs.python314
              pkgs.gh
            ];
            ANDROID_HOME = sdk;
            ANDROID_SDK_ROOT = sdk;
            JAVA_HOME = pkgs.jdk25.home;
            # Maven's aapt2 is a generic Linux binary that fails on NixOS
            GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${sdk}/build-tools/${buildTools}/aapt2";
            # avdmanager writes to ~/.config/.android while emulator reads ~/.android
            shellHook = ''
              export ANDROID_AVD_HOME="$HOME/.android/avd"
            '';
          };
        }
      );
    };
}
