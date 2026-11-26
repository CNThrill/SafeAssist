# SafeAssist — Accessible Emergency Assistance App

**Project Title:** Investigating Emergency Triage and Response Accessibility for Vulnerable Communities in Maramag  
**Authors:** Marc Randolph Arpon, Ted Emmanuel Balaba, Rovic James Duron, Jose Gilberto Sadicon (Central Mindanao University — HCI Term Project)  
**Target Users:** Persons with disabilities (PWDs), senior citizens, residents with chronic medical conditions, family caregivers, and local responders (MDRRMO, BHWs, RCY volunteers) in Maramag, Bukidnon, Philippines.

---

## Overview

SafeAssist is an offline-first Android application (Kotlin, XML, Material 3) designed to help vulnerable residents and caregivers in **Maramag, Bukidnon** prepare and send clear emergency alerts quickly under stress or weak connectivity.

All personal details, emergency contacts, and message templates are stored strictly on-device so users can prepare alerts offline and hand them off to their phone's SMS, dialer, or Messenger apps.

---

## Key Features

### First-Time Setup & My Info
- Saves the user's full name, callback phone number, **Maramag Barangay** and matching **Purok** dropdown selection, street/house description, nearest landmark, mobility/assistance profile, and optional handling notes locally on the device.
- Automatically pre-fills SOS messages and emergency reports so users do not need to type during an emergency.

### Two Modes on Home: Helping Myself vs. Helping Someone Else
- **Helping: Myself:** Tapping **SOS** starts an optional configurable countdown (**No countdown**, **3 sec**, or **5 sec** in Settings), plays the optional alarm siren (persisting with an ongoing notification and **Stop Siren** button), and opens the Messages app with all saved trusted contacts and the MDRRMO hotline pre-selected alongside the formatted SOS message and optional GPS coordinates.
- **Helping: Someone Else:** Tapping **SOS** opens the **Ready to Send** screen directly with the siren off by default (and toggleable on-screen), preventing the caregiver's personal home address or mobility notes from being included in someone else's emergency alert.

### Report What Happened (Guided 4 W's Flow)
- **Step 1 — What Happened:** Select who needs help (`Me`, `Family`, or `Someone else`), where the emergency is (with Maramag Barangay & Purok dropdowns or a **Not sure** shortcut), what happened (`Medical`, `Flood`, `Fire`, `Landslide`, `Other`), needed assistance chips, and optional extra details.
- **Step 2 — Review & Send:** Review the compiled report, adjust the timestamp if needed, copy the message for Messenger, **Send Text to MDRRMO**, **Send Text to Someone Else** (choosing from saved contacts), or **Call MDRRMO**.

### Emergency Contacts & Quick Call
- Displays and allows editing the local MDRRMO hotline (`0966-768-2770` by default) and copying a ready-to-paste summary for Messenger.
- Lets users add, validate, remove, **Pin**, and **Unpin** trusted contacts. The **Quick Call Trusted** card on Home displays and dials the pinned contact (or the first saved contact if none is pinned).

### Offline GPS & Privacy Guardrails
- **Manual handoff only:** SafeAssist does not auto-send SMS or auto-place calls (`SEND_SMS`, `CALL_PHONE`, and `INTERNET` permissions are not requested). All texts and calls open in the system Messages or Phone app for user confirmation.
- **Offline GPS with address fallback:** Reads device GPS coordinates with a one-tap Location Settings prompt when device Location is off, and falls back to the saved Barangay, Purok, and landmark when GPS is unavailable.
- **Local-only storage:** Profiles, contacts, and settings are stored locally using Room and DataStore Preferences (`android:allowBackup="false"`).

---

## Architecture

- **Pattern:** MVVM with Kotlin Coroutines and `StateFlow`
- **Persistence:** Room Database (`SafeAssistDatabase`) and DataStore Preferences (`SettingsDataStore`)
- **Navigation:** Android Navigation Component (`nav_graph.xml`)
- **Packages:** `audio`, `communication`, `data`, `database`, `location`, `model`, `navigation`, `repository`, `ui`, `utils`, `viewmodel`

---

## Permissions Used

- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: Used optionally to include plain-text GPS coordinates in SOS messages.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` / `POST_NOTIFICATIONS`: Used to keep the optional emergency siren active with an ongoing **Stop Siren** notification control when switching to Messages.

---

## Building & Testing

### Prerequisites
- Android Studio (Ladybug or newer) or JDK 17+
- Android SDK Platform 34 (`compileSdk = 34`, `minSdk = 24`, `targetSdk = 34`)

### Command Line
From the `app/` project root:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Debug APK output:
`app/build/outputs/apk/debug/app-debug.apk`
