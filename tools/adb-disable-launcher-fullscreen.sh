#!/usr/bin/env sh
echo "[Launcher 2026] Restoring Android system bars"
adb shell settings delete global policy_control
adb shell am force-stop com.aistudio.carlauncher.lzrk26
adb shell monkey -p com.aistudio.carlauncher.lzrk26 -c android.intent.category.LAUNCHER 1
