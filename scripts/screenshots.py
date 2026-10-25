#!/usr/bin/env python3
"""Seed demo data on an emulator and capture each tab in light and dark mode for the Play listing."""
import json
import random
import re
import shutil
import subprocess
import sys
import time
import uuid
from pathlib import Path

PACKAGE = "dev.codingcoffee.screener"
TABS = ["Activity", "Rules", "Stats", "Settings"]
OUT = Path(__file__).resolve().parent.parent / "fastlane/metadata/android/en-US/images/phoneScreenshots"

# (raw number, caller name, blocked, matched rule)
CALLS = [
    ("+911401234{:03d}", None, True, "Telemarketing (140 series)"),
    ("+911409876{:03d}", None, True, "Telemarketing (140 series)"),
    ("+918071234{:03d}", None, True, "Bangalore cloud telephony (8071)"),
    ("+917935123{:03d}", None, True, "Ahmedabad cloud telephony (7935)"),
    ("+919876543{:03d}", "Suspected Spam", True, "Carrier spam label"),
    ("+911600123{:03d}", "HDFC Bank", False, "Service calls (160 series)"),
    ("+919876500{:03d}", None, False, "none"),
]
WEIGHTS = [5, 4, 3, 2, 3, 2, 2]


def adb(*args, input=None, check=True):
    result = subprocess.run(["adb", *args], input=input, capture_output=True, check=check)
    return result.stdout.decode(errors="replace").strip()


def sh(cmd, check=True):
    return adb("shell", cmd, check=check)


def demo_log():
    rng = random.Random(7)
    now_ms = int(time.time() * 1000)
    entries = []
    for day in range(7):
        for _ in range(rng.randint(3, 8)):
            fmt, name, blocked, matched = rng.choices(CALLS, WEIGHTS)[0]
            number = fmt.format(rng.randint(0, 999))
            entries.append({
                "id": str(uuid.uuid4()),
                "timestamp": now_ms - day * 86_400_000 - rng.randint(10, 600) * 60_000,
                "rawNumber": number,
                "number": number,
                "callerDisplayName": name,
                "callDirection": 0,
                "decision": "blocked" if blocked else "allowed",
                "matched": matched,
            })
    entries.sort(key=lambda e: e["timestamp"])
    return "".join(json.dumps(e) + "\n" for e in entries)


def tap_tab(label):
    sh("uiautomator dump /sdcard/ui.xml >/dev/null")
    xml = sh("cat /sdcard/ui.xml")
    pattern = rf'(?:text|content-desc)="{label}"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    boxes = [tuple(map(int, m)) for m in re.findall(pattern, xml)]
    if not boxes:
        sys.exit(f"Couldn't find the {label} tab on screen")
    x1, y1, x2, y2 = max(boxes, key=lambda b: b[1])
    sh(f"input tap {(x1 + x2) // 2} {(y1 + y2) // 2}")
    time.sleep(1.5)


def demo_status_bar(on):
    if not on:
        sh("am broadcast -a com.android.systemui.demo -e command exit", check=False)
        return
    sh("settings put global sysui_demo_allowed 1")
    for extras in ["command enter", "command clock -e hhmm 1000",
                   "command battery -e level 100 -e plugged false",
                   "command network -e wifi show -e level 4",
                   "command network -e mobile show -e datatype none -e level 4",
                   "command notifications -e visible false"]:
        sh(f"am broadcast -a com.android.systemui.demo -e {extras}")


def optimize_images():
    pngs = [str(p) for p in OUT.glob("*.png")]
    subprocess.run(["pngquant", "--force", "--ext", ".png", "--skip-if-larger", "--quality", "80-95", *pngs])
    subprocess.run(["oxipng", "-o", "max", "--strip", "safe", *pngs], check=True)


def main():
    missing = [tool for tool in ["pngquant", "oxipng"] if not shutil.which(tool)]
    if missing:
        sys.exit(f"Missing {', '.join(missing)}, needed to compress the screenshots")
    if sh("getprop ro.kernel.qemu", check=False) != "1" and sh("getprop ro.boot.qemu", check=False) != "1":
        sys.exit("Not an emulator. This script overwrites the app's call log, so it only runs on an emulator.")
    size = re.search(r"(\d+)x(\d+)", sh("wm size").splitlines()[-1])
    if not size:
        sys.exit("Couldn't read the screen size from `adb shell wm size`")
    w, h = map(int, size.groups())
    if w * 16 != h * 9:
        sys.exit(f"Screen is {w}x{h}, Play needs 9:16. Create the AVD with `-d pixel` (1080x1920).")
    if PACKAGE not in sh("pm list packages"):
        sys.exit(f"{PACKAGE} isn't installed. Run ./gradlew installDebug first.")

    sh(f"am force-stop {PACKAGE}")
    sh(f"cmd role add-role-holder android.app.role.CALL_SCREENING {PACKAGE}")
    if subprocess.run(["adb", "shell", "run-as", PACKAGE, "mkdir", "-p", "files"]).returncode != 0:
        sys.exit("run-as failed. Install a debug build: ./gradlew installDebug")
    adb("exec-in", "run-as", PACKAGE, "sh", "-c", "cat > files/call_log.jsonl", input=demo_log().encode())

    OUT.mkdir(parents=True, exist_ok=True)
    demo_status_bar(True)
    try:
        n = 1
        for mode in ["no", "yes"]:
            sh(f"cmd uimode night {mode}")
            sh(f"am force-stop {PACKAGE}")
            sh(f"am start -W -n {PACKAGE}/.MainActivity >/dev/null")
            time.sleep(2)
            for tab in TABS:
                tap_tab(tab)
                (OUT / f"{n}.png").write_bytes(subprocess.run(
                    ["adb", "exec-out", "screencap", "-p"], capture_output=True, check=True).stdout)
                print(f"{n}.png  {tab} ({'dark' if mode == 'yes' else 'light'})")
                n += 1
    finally:
        demo_status_bar(False)
        sh("cmd uimode night no", check=False)
    optimize_images()
    print(f"Saved to {OUT}")


if __name__ == "__main__":
    main()
