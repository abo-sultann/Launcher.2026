# Launcher 2026 — إخفاء الشريط العلوي عبر ADB

التطبيق نفسه يدخل وضع Fullscreen ويخفي شريط الحالة على Android 7.1. بعض شاشات السيارات تعيد إظهار شريطها العلوي عبر واجهة النظام الخاصة بها؛ في هذه الحالة شغّل الأمر التالي مرة واحدة والجهاز متصل بـ ADB:

```bash
adb shell settings put global policy_control immersive.status=com.aistudio.carlauncher.lzrk26
```

أو شغّل الملف:

- Windows: `tools/adb-enable-launcher-fullscreen.bat`
- macOS/Linux: `tools/adb-enable-launcher-fullscreen.sh`

هذا الوضع يستهدف **الشريط العلوي فقط** ويُبقي شريط/أزرار التنقل دون تغيير.

## إلغاء التعديل والرجوع للوضع الأصلي

```bash
adb shell settings delete global policy_control
```

أو استخدم:

- Windows: `tools/adb-disable-launcher-fullscreen.bat`
- macOS/Linux: `tools/adb-disable-launcher-fullscreen.sh`

لا يحذف أي تطبيق ولا يغيّر Launcher المصنع؛ هو إعداد واجهة نظام ويمكن إلغاؤه بالأمر أعلاه.
