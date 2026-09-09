import pathlib, subprocess, wave, time
out = pathlib.Path("qa-visual")
out.mkdir(exist_ok=True)
package = "com.aistudio.carlauncher.lzrk26"
def adb(*args, check=True):
    return subprocess.run(["adb", *args], capture_output=True, check=check).stdout
# Real local audio fixtures. No network library or invented on-screen app data.
for name in ["QA_evening_drive_long_title_for_layout_review", "QA_second_track", "QA_third_track"]:
    path = out / (name + ".wav")
    with wave.open(str(path), "wb") as wav:
        wav.setparams((1, 2, 8000, 0, "NONE", "not compressed"))
        wav.writeframes(b"\0\0" * 8000 * 90)
    dest = "/sdcard/Music/" + path.name
    adb("push", str(path), dest)
    adb("shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", "file://" + dest)
    path.unlink()
time.sleep(3)
adb("logcat", "-c")
run = subprocess.run(["adb", "shell", "am", "instrument", "-w", package + ".test/com.example.LauncherReviewRunner"],
    capture_output=True, text=True, timeout=240)
(out / "instrumentation.txt").write_text(run.stdout + run.stderr)
adb("pull", "/sdcard/Android/data/"+package+"/files/launcher-review/.", str(out), check=False)
crashes = adb("logcat", "-b", "crash", "-d").decode(errors="replace")
(out / "crashes.txt").write_text(crashes)
assert "FATAL EXCEPTION" not in crashes, crashes
result = out / "result.txt"
assert result.exists() and result.read_text().startswith("PASS:"), run.stdout + run.stderr
print(result.read_text())
