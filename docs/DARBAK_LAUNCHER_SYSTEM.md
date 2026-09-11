# Darbak Launcher System

## Product boundary

Darbak Launcher is the private Android shell for the owner's car head unit. It runs on top of Android and coordinates the private Darbak applications without turning them into one monolithic APK.

Darbak Maps is explicitly outside this system. It is an independent public product that may share Darbak visual identity, but it must not depend on Darbak Launcher, Vehicle Hub, or private head-unit services.

## Target device

- Android 7.1 / API 25 minimum
- 1024x600 landscape
- low-memory Allwinner T3 class hardware
- offline-first operation
- sideloaded signed APKs

## Architecture

Android -> Darbak Launcher -> Darbak Core -> Darbak App Bridge -> private Darbak apps

The private system catalog currently contains:

1. Darbak Launcher (`com.aistudio.carlauncher.lzrk26`)
2. Darbak Vehicle Hub (`com.abosultan.darbakvehiclehub`)
3. Darbak Maintenance (`com.abosultan.darbakmaintenance`)
4. Darbak Media (`com.abosultan.darbakmedia`)
5. Darbak Kids TV (`com.abosultan.darbakkidstv`)
6. Laqqinni (`com.abosultan.laqqinni`)
7. Darbak Adhkar (`com.abosultan.darbakadhkar`)

## Rules

- Launcher remains the Home/default shell and the only system-level entry point.
- Companion apps remain separate APKs so one app failure does not take down the launcher.
- Launcher may display compact status/widgets from companion apps, but detailed screens remain owned by the companion app.
- Missing companion apps must never crash Launcher.
- All cross-app integration must go through a small stable contract instead of directly coupling UI code to another app.
- Darbak Maps remains independent and is never added to the private system catalog.
- Existing stable launcher behavior is preserved while the new system is built on a separate branch.

## Delivery phases

### Phase 1 - Core boundary
- Darbak system module model
- stable package catalog
- App Bridge installation/version/launch checks
- runtime ownership
- Darbak Launcher branding

### Phase 2 - System dashboard
- Home module cards/widgets
- installed/available state
- compact Vehicle Hub, Maintenance and Media status
- graceful unavailable state for missing apps

### Phase 3 - Shared service contracts
- Vehicle Hub telemetry contract
- Maintenance odometer/service-due contract
- Media playback contract
- common diagnostics and version reporting

### Phase 4 - Unified system settings and updates
- one Darbak system settings surface
- app version inventory
- update status per private module
- hidden technical diagnostics

### Phase 5 - Stability release
- Android 7/API 25 regression tests
- long-use and repeated hand-off tests
- low-memory recovery
- signed release build and car-screen validation
