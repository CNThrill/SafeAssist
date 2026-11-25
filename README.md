# SafeAssist — Accessible Emergency Assistance Guide

**Project Title:** Investigating Emergency Triage and Response Accessibility for Vulnerable Communities in Maramag  
**Authors:** Marc Randolph Arpon, Ted Emmanuel Balaba, Rovic James Duron, Jose Gilberto Sadicon (Central Mindanao University — HCI Term Project)  
**Target Users:** Persons with disabilities (PWDs), senior citizens, residents with chronic medical conditions, family caregivers, and local responders (MDRRMO, BHWs, RCY volunteers) in Maramag, Bukidnon, Philippines.

---

## 1. Background & Design Rationale

SafeAssist is an offline-first native Android application (Kotlin, XML, Material 3) built directly from our **HCI Discover & Design Phase Reports**. Our field interviews with Maramag residents, caregivers, an RCY volunteer, and MDRRMO personnel identified four core bottlenecks (`F1`–`F4`):

1. **Vague locations and missing street names (`F1`):** Panicked callers struggle to describe where they are, forcing dispatchers into repeated callbacks. We structured location around **Barangay, Purok, Street/House Description, and Nearest Landmark**, paired with optional **plain-text GPS coordinates** (instead of map links that require mobile data).
2. **Communication and disclosure friction (`F2`):** Residents with mobility needs or mental health conditions reported difficulty explaining safe handling under pressure or reluctance to disclose diagnoses over voice calls. SafeAssist lets users pre-save mobility/assistance notes once and choose **text-first SMS/Messenger handoffs** without forcing medical diagnoses.
3. **Fragmented, low-signal connectivity (`F3`):** Rural barangays often experience weak cellular voice/SMS signal, and residents may need to fall back to Wi-Fi and Facebook Messenger to reach MDRRMO Maramag. SafeAssist keeps all profiles, contacts, settings, and message templates offline on-device so users can prepare alerts anytime and copy them for SMS or Messenger.
4. **Informal caregiver/family buffer (`F4`):** Residents often rely on family, neighbors, or BHWs before reaching MDRRMO. SafeAssist supports **Helping: Someone Else** proxy reporting and keeps **Trusted Contacts** one tap away.

---

## 2. Implemented Features

### First-Time Setup & My Info (`SetupFragment`, `ProfileFragment`)
- Collects full name, **callback phone number**, Barangay, Purok, street/house description, nearest landmark, assistance/mobility profile, and optional handling notes locally on the device.
- Automatically pre-fills SOS messages and self-reported incident drafts so users do not have to type under stress.

### Primary Path: One-Tap SOS (`SosCountdownFragment` -> `SosReadyToSendFragment`)
- **Cancelable countdown:** Configurable 3s, 5s (default), or 10s countdown with a full-width **CANCEL SOS** button and hardware back-press cancellation so accidental taps never trigger a false alert (`D8`).
- **Post-countdown siren:** Optional alarm-stream siren (`AudioAttributes.USAGE_ALARM`) triggers only *after* the countdown finishes without cancellation, with an immediate **Stop Siren** control (`D10`).
- **Post-countdown GPS + fallback:** Reads offline GPS/Network/Fused coordinates via Android `LocationManager` after the countdown finishes (respecting a 15-minute cache freshness limit), includes an opt-in **Included / Excluded** toggle chip, and falls back to the saved Barangay/Purok/landmark outdoors or indoors (`D9`).
- **Manual handoff actions:** Displays a prominent **Not Sent Yet** banner and lets the user tap **Send Text to MDRRMO**, **Call MDRRMO**, **Send Text to [Trusted Contact]**, or **Copy My Message**.

### Secondary Path: Report What Happened (`WhereAndNeedsFragment` -> `ReviewAndShareFragment`)
- Guided 2-step **4 W's** preparation flow:
  - **Step 1 (`What Happened`):** `Who needs help?` (`Me` / `Family` / `Someone else`), `Where` (with a **Not sure** skip button), `What happened?` radio choices, multi-select `Assistance needs` chips, and `More details` (120-character cap).
  - **Step 2 (`Review & Send`):** Shows the compiled report with an editable **When** timestamp, a **Change / Edit** path back to Step 1, and **Copy My Message**, **Send SMS to [Trusted Contact]**, and **Call MDRRMO** actions.

### Caregiver Mode & Emergency Contacts (`HomeFragment`, `EmergencyContactsFragment`)
- **Helping: Myself / Helping: Someone Else** switch on Home prevents SOS from leaking the caregiver's own saved home address or personal mobility notes when requesting help for another person while keeping the phone's callback number.
- **Contacts tab:** Displays the editable MDRRMO hotline (`0966-768-2770` by default), provides a 1-tap **Copy Message for Messenger** relay button, and manages locally saved **Trusted Contacts**.

### Core Safety & Privacy Guardrails
- **Never auto-sends or auto-calls:** We intentionally omit `SEND_SMS`, `CALL_PHONE`, and `INTERNET` permissions. All calls and texts open through `ACTION_DIAL` and `ACTION_SENDTO` (`smsto:`) so the user always sees and confirms delivery in their system dialer or SMS app (`D7`).
- **Local-only storage:** Room and DataStore Preferences store everything strictly on the device (`android:allowBackup="false"`).

---

## 3. Refinements Between Design Wireframes & Current Prototype

As we translated our Design Phase wireframes into a testable Android prototype, we made a few focused simplifications to reduce clutter and panic-time friction:

- **Removed Home summary cards, status badges, and external URL launchers:** We stripped non-interactive badges (`Offline ready`, `Saved only on this device`, `Awaiting verification`) and kept Home unscrollable (wrapped in a `fillViewport` scroll container only as an overflow safeguard for small screens or large system font sizes).
- **Direct action buttons on Ready-to-Send:** We show **Send Text to MDRRMO**, **Call MDRRMO**, and **Copy My Message** directly on the Ready-to-Send screen so users can pick their channel immediately while the `"Not Sent Yet"` boundary stays visible.
- **Manual Barangay & Purok entry:** We kept Barangay and Purok as plain text inputs rather than hardcoded dropdown lists so we do not ship unverified municipal/purok records.

---

## 4. Architecture & Package Structure

We structured the app with **MVVM**, **Kotlin Coroutines & StateFlow**, **Room**, **DataStore Preferences**, and **Android Navigation Component**:

```text
com.safeassist.maramag
├── audio/           # SirenManager (MediaPlayer with USAGE_ALARM on res/raw/emergency_siren.wav)
├── communication/   # CommunicationHelper (ACTION_SENDTO, ACTION_DIAL, ClipboardManager)
├── data/            # SettingsDataStore (DataStore Preferences for setup, timer, siren, GPS, caregiver, hotline)
├── database/        # SafeAssistDatabase, UserProfileDao, EmergencyContactDao, SafeAssistConverters
├── location/        # LocationProvider (LocationManager offline provider racing + 15-min stale-fix guard)
├── model/           # UserProfile, EmergencyContact, EmergencyReport, AppSettings, LocationResult, Enums
├── navigation/      # SafeAssistNavigator (navigation actions & SOS backstack reset)
├── repository/      # SafeAssistRepository (combines Room + DataStore flows)
├── ui/
│   ├── MainActivity.kt
│   ├── setup/       # SetupFragment
│   ├── home/        # HomeFragment
│   ├── contacts/    # EmergencyContactsFragment
│   ├── profile/     # ProfileFragment
│   ├── settings/    # SettingsFragment
│   ├── prepare/     # WhereAndNeedsFragment, ReviewAndShareFragment
│   └── sos/         # SosCountdownFragment, SosReadyToSendFragment
├── utils/           # EmergencyMessageFormatter, DateTimeUtils
└── viewmodel/       # HomeViewModel, ProfileViewModel, ContactsViewModel, SettingsViewModel,
                     # PrepareEmergencyViewModel, SosViewModel, SetupViewModel, SafeAssistViewModelFactory
```

---

## 5. Permissions Used

- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: Requested once on initial launch (and again on SOS Ready-to-Send if GPS is enabled and permission was not yet granted). Works offline via `LocationManager`; if denied or indoors, the app falls back to the saved address.
- **Intentionally omitted:** `SEND_SMS`, `CALL_PHONE`, and `INTERNET`.

---

## 6. Building the APK & Running Unit Tests

### Prerequisites
- Android Studio (Ladybug or newer) or JDK 17+
- Android SDK Platform 34 (`compileSdk = 34`, `minSdk = 24`, `targetSdk = 34`)

### Command Line Build & Test
From the `app/` project root:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Debug APK output:
`app/build/outputs/apk/debug/app-debug.apk`

Install onto a device or emulator via ADB:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 7. Known Limitations & Pending Validation

- **No automatic dispatch or delivery tracking:** SafeAssist prepares messages and dialer handoffs only; actual transmission still requires cellular or data signal (or showing the screen to a neighbor/BHW when signal is out).
- **Hotline verification pending:** `0966-768-2770` is taken from the Sept 29, 2026 MDRRMO Maramag Facebook post (`EV-06`) and still requires direct confirmation with MDRRMO; users can update it in **Contacts**.
- **Pending field usability testing:** Features introduced during the design phase—such as the SOS countdown, plain-text GPS preview, and alarm siren—still need field usability testing with Maramag PWDs, seniors, caregivers, and responders.
- **Hardware volume bounds:** Android does not allow apps to override a hardware-muted alarm channel; the siren plays at the device's permitted alarm volume.
