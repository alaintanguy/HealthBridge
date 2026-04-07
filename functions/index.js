/**
 * HealthBridge Cloud Functions
 *
 * Two Firestore-triggered functions form the secure relay between
 * Michel (controller) and Alain (sender):
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  onNewCommand                                                    │
 * │  Trigger: devices/{deviceId}/commands/{commandId} (onCreate)    │
 * │  Action : sends FCM data message to Alain's registered token.   │
 * │  This is the secure server-side relay – credentials never leave │
 * │  the Cloud Functions environment.                                │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  onLocationUpdate                                                │
 * │  Trigger: devices/{deviceId}/location/latest (onWrite)          │
 * │  Action : broadcasts FCM data message to the topic              │
 * │           "alain_location" so all subscribed Michel devices     │
 * │           receive the update simultaneously.                    │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * Deploy:
 *   cd functions && npm install
 *   firebase deploy --only functions
 */

const {onDocumentCreated, onDocumentWritten} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getMessaging} = require("firebase-admin/messaging");
const {getFirestore} = require("firebase-admin/firestore");

initializeApp();

const ALAIN_DEVICE_ID = "alain";
const FCM_TOPIC_LOCATION = "alain_location";

/**
 * Relays a START or STOP command from Michel to Alain's device via FCM.
 *
 * Firestore document structure:
 *   devices/alain/commands/{commandId}:
 *     type        : "START" | "STOP"
 *     targetToken : string  (Alain's FCM registration token)
 *     createdAt   : timestamp
 */
exports.onNewCommand = onDocumentCreated(
    "devices/{deviceId}/commands/{commandId}",
    async (event) => {
      const data = event.data.data();
      const {deviceId, commandId} = event.params;

      // Only relay commands for the known Alain device
      if (deviceId !== ALAIN_DEVICE_ID) {
        console.log(`Ignoring command for unknown device: ${deviceId}`);
        return null;
      }

      const {type, targetToken} = data;
      if (!type || !targetToken) {
        console.error(`Command ${commandId} missing type or targetToken`);
        return null;
      }

      console.log(`Relaying command "${type}" to Alain (commandId=${commandId})`);

      const message = {
        data: {type},
        token: targetToken,
        android: {
          priority: "high",
        },
      };

      try {
        const response = await getMessaging().send(message);
        console.log(`FCM command sent successfully: ${response}`);
      } catch (err) {
        console.error(`FCM send error for command ${commandId}:`, err);
      }

      return null;
    }
);

/**
 * Broadcasts Alain's latest location to all subscribed Michel devices via FCM topic.
 *
 * Firestore document structure:
 *   devices/alain/location/latest:
 *     lat       : number
 *     lon       : number
 *     accuracy  : number
 *     timestamp : number (ms)
 *     deviceId  : string
 */
exports.onLocationUpdate = onDocumentWritten(
    `devices/${ALAIN_DEVICE_ID}/location/latest`,
    async (event) => {
      const after = event.data.after;
      if (!after.exists) {
        console.log("Location document deleted – ignoring");
        return null;
      }

      const loc = after.data();
      const {lat, lon, accuracy, timestamp} = loc;

      if (lat === undefined || lon === undefined) {
        console.error("Location document missing lat/lon");
        return null;
      }

      console.log(`Broadcasting location update: lat=${lat}, lon=${lon}`);

      const message = {
        data: {
          lat: String(lat),
          lon: String(lon),
          accuracy: String(accuracy ?? 0),
          timestamp: String(timestamp ?? Date.now()),
        },
        topic: FCM_TOPIC_LOCATION,
        android: {
          priority: "high",
        },
      };

      try {
        const response = await getMessaging().send(message);
        console.log(`FCM topic message sent: ${response}`);
      } catch (err) {
        console.error("FCM topic broadcast error:", err);
      }

      return null;
    }
);
