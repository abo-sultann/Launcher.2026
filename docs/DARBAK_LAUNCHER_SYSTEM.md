# Darbak Launcher System

## Product boundary

Darbak Launcher is the private Android shell for the owner's car head unit. It runs on top of Android and coordinates the private Darbak applications without turning every feature into one monolithic APK.

Darbak Maps is an independent public product that may share Darbak visual identity, but it must not depend on Darbak Launcher, Vehicle Hub, or private head-unit services.

**Transition rule:** the existing offline map inside Darbak Launcher remains available as a fallback until Darbak Maps is tested and explicitly approved on the real car screen. The legacy Launcher map receives stability fixes only; no major new feature development is added to it. After Darbak Maps is approved and proven stable, removal or reduction of the legacy Launcher map can be evaluated separately.

Darbak Kids TV, Laqqinni and Darbak Adhkar are outside the active Launcher system scope. They may remain as ordinary standalone Android apps, but Launcher must not depend on them, request their status, include them in Update Center, or surface them as Darbak System modules.

## Target device

- Android 7.1 / API 25 minimum
- 1024x600 landscape
- low-memory Allwinner T3 class hardware
- offline-first operation
- sideloaded signed APKs

## Active architecture

Android -> Darbak Launcher -> Darbak Core -> Darbak App Bridge -> active private Darbak companion apps

Core system health depends on:

1. Darbak Launcher (`com.aistudio.carlauncher.lzrk26`)
2. Darbak Vehicle Hub (`com.abosultan.darbakvehiclehub`)
3. Darbak Maintenance (`com.abosultan.darbakmaintenance`)

Optional companion:

4. Darbak Media (`com.abosultan.darbakmedia`) — external/online media only; its absence must never make Darbak System unhealthy.

## Darbak Audio / Darb Al-Sout decision

Darb Al-Sout is no longer planned as a separate application in the final system.

Its useful function becomes an internal feature of the Darbak Launcher audio experience:

- local audio library and playback live in Launcher
- incoming/new audio appears directly in the same library
- manual **Sync now** starts the Darb Al-Sout acquisition/sync component
- successful downloads are moved into the local audio library with duplicate protection
- sync/network failure must not affect Launcher startup or playback
- the sync implementation remains isolated behind a small internal component so it can fail independently
- Launcher remembers the last audio file and playback position

Darbak Audio is therefore **not** an App Bridge module and has no separate package dependency.

The old DarbAlSout2 Google Drive implementation is not copied wholesale into Launcher because its account flow depends on Google Play Services authentication, which is a known weak point on the target Android 7 head unit. The final acquisition transport must remain isolated from Launcher core so changing Google Drive/Telegram/another transport never destabilizes playback or Home.

Darbak Media remains a separate optional companion for non-local/external media experiences. It must not duplicate the local-audio/Darb Al-Sout responsibilities owned by Launcher.

## Maintenance odometer rule

Darbak Maintenance owns the real vehicle odometer baseline.

- the user enters the current dashboard odometer manually once inside Darbak Maintenance
- Launcher does not accumulate or send GPS-derived kilometres until that baseline is confirmed
- after confirmation, trusted GPS distance may add incremental kilometres without keeping Maintenance or a second GPS listener open
- when Vehicle Hub eventually provides a proven real odometer source, it may replace GPS deltas as the preferred source
- a missing/unconfirmed odometer baseline is shown as a degraded configuration state, never as a fake odometer starting at zero

## Rules

- Launcher remains the Home/default shell and the system-level entry point.
- Vehicle Hub and Maintenance remain separate APKs so one companion failure does not take down Launcher.
- Darbak Media is optional and must not affect core system health.
- Local audio playback and Darb Al-Sout synchronization belong inside Launcher.
- Launcher may display compact status/widgets from active companion apps, while detailed companion screens remain owned by each companion.
- Missing companion apps must never crash Launcher.
- Cross-app integration goes through a small stable contract instead of directly coupling UI code to another app.
- Darbak Maps remains independent and is never added to the private system catalog.
- The legacy Launcher map stays temporarily as fallback until Darbak Maps is approved on the real screen.
- Kids TV, Laqqinni and Adhkar are excluded from Darbak System integration.
- Existing stable launcher behavior is preserved while the new system is built on a separate branch.

## Delivery phases

### Phase 1 - Core boundary
- Darbak system module model
- stable package catalog
- App Bridge installation/version/launch checks
- runtime ownership
- Darbak Launcher branding

### Phase 2 - System dashboard
- core Vehicle Hub and Maintenance state
- optional Media state
- installed/available state
- graceful unavailable state for missing apps

### Phase 3 - Darbak Audio integration
- local audio library in Launcher
- playback/resume state
- isolated Darb Al-Sout internal sync component
- new-audio inbox state
- duplicate prevention and safe file move
- sync failure isolation
- no hard Google Play Services dependency in Launcher core

### Phase 4 - Shared companion contracts
- Vehicle Hub telemetry contract
- Maintenance real-odometer baseline plus trusted delta contract
- Media external-media contract
- common diagnostics and version reporting

### Phase 5 - Unified system settings and updates
- one Darbak system settings surface
- app version inventory for active companion modules only
- update status per active private module
- hidden technical diagnostics

### Phase 6 - Stability release
- Android 7/API 25 regression tests
- long-use and repeated hand-off tests
- low-memory recovery
- signed release build and car-screen validation
- keep legacy Launcher map until Darbak Maps passes real-screen acceptance
