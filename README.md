# HealthBridge – FCM Two-App Architecture

Real-time GPS tracking between two Android phones using **Firebase Cloud Messaging (FCM)** and **Firestore**, with no custom backend server.

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│  Michel's Tablet (Controller)        Alain's Phone (Sender)          │
│  app: HealthBridgeMichel             app: HealthBridgeAlain          │
│                                                                      │
│  [Start Tracking] button             FCM data message (START)        │
│        │                                      ▲                      │
│        ▼                                      │                      │
│  Firestore: write                    Cloud Function:                 │
│  devices/alain/commands/{id}  ──────► onNewCommand                  │
│                                       sends FCM to Alain token       │
│                                                │                     │
│                                                ▼                     │
│                                      LocationForegroundService       │
│                                      (FusedLocationProviderClient)   │
│                                      every 60 seconds:               │
│                                      writes to Firestore             │
│                                      devices/alain/location/latest   │
│                                                │                     │
│  MichelMainActivity                  Cloud Function:                 │
│  (Firestore listener)          ◄───── onLocationUpdate              │
│  updates map + polyline               sends FCM to topic             │
│                                       "alain_location"               │
│                                                                      │
│  Multiple Michel tablets can subscribe to the same topic.            │
└──────────────────────────────────────────────────────────────────────┘
```

### Data flow in detail

| Step | Who | What |
|------|-----|------|
| 1 | Michel presses **Start Tracking** | Reads Alain's FCM token from `devices/alain/info/token` |
| 2 | Michel app | Writes `{type:"START", targetToken, createdAt}` to `devices/alain/commands/{uuid}` |
| 3 | Cloud Function `onNewCommand` | Sends FCM data message `{type:"START"}` to Alain's token |
| 4 | Alain's `AlainFcmService` | Receives FCM, starts `LocationForegroundService` |
| 5 | Alain's `LocationForegroundService` | Every 60 s: writes `{lat, lon, accuracy, timestamp}` to `devices/alain/location/latest` |
| 6 | Cloud Function `onLocationUpdate` | Sends FCM data message to topic `alain_location` |
| 7 | Michel's `MichelFcmService` + Firestore listener | Receives update, draws marker + polyline on map |
| 8 | Michel presses **Stop Tracking** | Writes `{type:"STOP", targetToken}` → same relay → Alain stops service |

---

## Repository Structure

```
HealthBridge/
├── michel/                  # HealthBridgeMichel Android app
│   ├── build.gradle
│   ├── google-services.json  ← REPLACE with real file
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/healthbridge/michel/
│           ├── MichelMainActivity.kt   (map + buttons)
│           └── MichelFcmService.kt     (receives location FCM)
│
├── alain/                   # HealthBridgeAlain Android app
│   ├── build.gradle
│   ├── google-services.json  ← REPLACE with real file
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/healthbridge/alain/
│           ├── AlainApp.kt                   (registers FCM token on startup)
│           ├── AlainMainActivity.kt          (status + manual override)
│           ├── LocationForegroundService.kt  (FusedLocation, 60 s interval)
│           └── AlainFcmService.kt            (receives START/STOP commands)
│
├── functions/               # Firebase Cloud Functions (Node.js 20)
│   ├── index.js             (onNewCommand + onLocationUpdate)
│   └── package.json
│
├── firestore.rules          # Firestore security rules
├── firebase.json            # Firebase project config
├── settings.gradle          # Android multi-module root
├── build.gradle             # Root Gradle config
├── gradlew / gradlew.bat    # Gradle wrapper
│
└── app/                     # LEGACY – original single-module app (Pushover-based)
    └── ...                  # Kept for reference; not part of the new architecture
```

---

## Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **Android SDK** Platform 35 + Build-Tools 35.0.0
- A **Google account** for Firebase
- **Node.js 20** and **npm** (for Cloud Functions)
- **Firebase CLI** (`npm install -g firebase-tools`)

---

## Step-by-Step Setup

### 1 – Create a Firebase Project

1. Go to <https://console.firebase.google.com/> and click **Add project**.
2. Name it `HealthBridge` (or any name you like).
3. Enable **Google Analytics** (optional but recommended).
4. Click **Create project**.

### 2 – Enable Firestore

1. In your Firebase project, navigate to **Build → Firestore Database**.
2. Click **Create database**.
3. Start in **production mode** (the rules in this repo will be deployed).
4. Choose a Cloud Firestore location close to you.

### 3 – Enable Firebase Cloud Messaging

FCM is enabled by default for every Firebase project. No extra step required.

### 4 – Register the two Android apps in Firebase

#### App A – HealthBridgeMichel

1. In the Firebase Console, click the **Android** icon (or **Add app → Android**).
2. Package name: `com.healthbridge.michel`
3. App nickname: `HealthBridgeMichel`
4. Click **Register app**.
5. Download **google-services.json** and save it as `michel/google-services.json`
   (replacing the placeholder file already in the repository).
6. Skip the Firebase SDK steps – they are already in `michel/build.gradle`.

#### App B – HealthBridgeAlain

Repeat the above with:
- Package name: `com.healthbridge.alain`
- App nickname: `HealthBridgeAlain`
- Save the downloaded file as `alain/google-services.json`.

### 5 – Deploy Firestore Rules

```bash
firebase login
firebase use --add          # select your project
firebase deploy --only firestore:rules
```

### 6 – Deploy Cloud Functions

```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

Expected output:
```
✔  functions[onNewCommand(us-central1)] Successful create operation.
✔  functions[onLocationUpdate(us-central1)] Successful create operation.
```

> **Note**: Cloud Functions require the **Blaze (pay-as-you-go)** billing plan.
> The free usage tier covers millions of invocations per month; normal HealthBridge
> usage (60-second location updates) will comfortably stay within the free tier.

### 7 – Build and Install the Android Apps

Open the root `HealthBridge` folder in Android Studio:

```
File → Open → /path/to/HealthBridge
```

Wait for Gradle sync to complete, then:

- Select the **michel** run configuration → deploy to Michel's tablet.
- Select the **alain** run configuration → deploy to Alain's phone.

Or from the command line:

```bash
./gradlew :michel:assembleDebug
./gradlew :alain:assembleDebug
```

The APKs will be in `michel/build/outputs/apk/debug/` and `alain/build/outputs/apk/debug/`.

---

## First Run Checklist

1. **Alain's phone**: Open **HealthBridgeAlain**.  
   - Grant **Precise Location** permission when prompted.  
   - Grant **Background Location** permission (required so the service keeps running when the screen is off).  
   - Grant **Notifications** permission (Android 13+).  
   - The app writes Alain's FCM token to Firestore automatically on startup.  

2. **Michel's tablet**: Open **HealthBridgeMichel**.  
   - Grant **Notifications** permission (Android 13+).  
   - Press **▶ Start Tracking**.  
   - The status bar shows "⏳ Start command sent to Alain…"  

3. Within a few seconds, Alain's phone shows "🟢 Tracking active – sending GPS every 60 s" and a notification.

4. After ~60 seconds, Michel's map updates with Alain's position (marker + blue polyline).

5. Press **⏹ Stop Tracking** on Michel to stop the service on Alain.

---

## Firestore Collection Structure

```
devices/
  alain/
    info/
      token:           { fcmToken: "ey...", updatedAt: timestamp }
    commands/
      {uuid}:          { type: "START"|"STOP", targetToken: "...", createdAt: timestamp }
    location/
      latest:          { lat: 48.8566, lon: 2.3522, accuracy: 5.0, timestamp: 1700000000000, deviceId: "alain" }
```

---

## Extending to Multiple Michel Devices

All Michel devices subscribe to the FCM topic `alain_location`.  
No additional configuration is needed – any number of Michel tablets can
simultaneously receive Alain's location updates.

Each Michel device writes commands to Firestore using the same Alain token.
The last write wins for simultaneous Start/Stop commands (acceptable for
a personal monitoring use-case).

---

## Future Extensions (Health & Smartwatch Data)

The Firestore `devices/alain/` collection is designed to be extensible:

```
devices/alain/
  health/
    latest:   { heartRate: 72, steps: 4200, timestamp: ... }
  location/
    latest:   { lat, lon, accuracy, timestamp }
```

A Cloud Function can broadcast health updates to a separate topic
(`alain_health`) following the same pattern as `onLocationUpdate`.

---

## Security Notes

- **Never commit real `google-services.json` files to a public repository.**  
  The files included in this repo are placeholders only.
- The Firestore rules in `firestore.rules` are open (`allow read, write: if true`)
  for the demo phase. Before going to production, add Firebase Authentication
  and restrict writes to authenticated users.
- FCM tokens are only used server-side (inside Cloud Functions). They are never
  exposed to other client apps, ensuring secure device-to-device messaging.

---

## Legacy Code

The `app/` directory contains the original Pushover-based single-app proof-of-concept.
It is kept for historical reference only and is **not part of the new architecture**.
The `dashboard/`, `src/`, `server.js`, and `index.html` files are similarly legacy web artefacts.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| "Alain's device token not found" on Michel | HealthBridgeAlain was never opened, or Firestore write failed | Open HealthBridgeAlain first; check Logcat for errors |
| Alain receives FCM but location never sends | Location permission denied | Open Settings → Apps → HealthBridgeAlain → Permissions → enable Location (Always) |
| Michel map never updates | Cloud Function not deployed, or Firestore rules blocking | Run `firebase deploy --only functions,firestore:rules` |
| Build fails with "google-services.json not found" | Placeholder file not replaced | Download real file from Firebase Console and replace `michel/google-services.json` / `alain/google-services.json` |
