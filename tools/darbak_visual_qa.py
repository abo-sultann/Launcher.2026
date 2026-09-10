import pathlib, subprocess, sys, time, re, json
import xml.etree.ElementTree as ET

package = sys.argv[1]
out = pathlib.Path('qa-visual')
out.mkdir(exist_ok=True)
def adb(*args, check=True):
    return subprocess.run(['adb', *args], check=check, capture_output=True).stdout
def capture(name):
    (out / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p'))
    adb('shell', 'uiautomator', 'dump', '/sdcard/darbak-qa.xml')
    data = adb('shell', 'cat', '/sdcard/darbak-qa.xml')
    (out / (name + '.xml')).write_bytes(data)
    return ET.fromstring(data)
adb('logcat', '-c')
adb('shell', 'monkey', '-p', package, '-c', 'android.intent.category.LAUNCHER', '1')
time.sleep(5)
assert adb('shell', 'pidof', package).strip(), 'App not running after launch'
root = capture('01-home')
visited=[]
for label in ['الإعدادات', 'الموسيقى', 'مشغل الموسيقى', 'حول دربك']:
    node = next((n for n in root.iter('node') if label in [n.get('text'), n.get('content-desc')]), None)
    if node is None:
        continue
    nums = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
    if len(nums) != 4 or nums[2] <= nums[0] or nums[3] <= nums[1]:
        continue
    adb('shell', 'input', 'tap', str((nums[0]+nums[2])//2), str((nums[1]+nums[3])//2))
    time.sleep(2)
    capture('screen-' + str(len(visited)+1))
    visited.append(label)
    adb('shell', 'input', 'keyevent', '4')
    time.sleep(1)
    adb('shell', 'monkey', '-p', package, '-c', 'android.intent.category.LAUNCHER', '1')
    time.sleep(2)
    root=capture('return-' + str(len(visited)))
assert adb('shell', 'pidof', package).strip(), 'App stopped during navigation'
crashes=adb('logcat', '-b', 'crash', '-d').decode(errors='replace')
(out/'crashes.txt').write_text(crashes)
(out/'visited.json').write_text(json.dumps(visited, ensure_ascii=False))
assert 'FATAL EXCEPTION' not in crashes, crashes
