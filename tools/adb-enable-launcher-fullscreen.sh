#!/usr/bin/env sh
PACKAGE="com.aistudio.carlauncher.lzrk26"
echo "[Launcher 2026] Enabling status-bar immersive mode for $PACKAGE"
adb shell settings put global policy_control "immersive.status=$PACKAGE"
adb shell am force-stop "$PACKAGE"
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1
