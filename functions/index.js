const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions, logger } = require("firebase-functions/v2");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { handleZoneEvent } = require("./src/handler");

initializeApp();

// Same region as the Firestore database (asia-south1 / Mumbai). Firestore triggers must match it.
// cpu 1 + concurrency 80 lets each instance handle many events at once, so bursts don't queue up.
setGlobalOptions({
  region: "asia-south1",
  maxInstances: 10,
  cpu: 1,
  memory: "512MiB",
  concurrency: 80,
});

/**
 * Fires once per new zone event. Only EXIT events produce a push; the member who left is never notified.
 * Event ids are deterministic, so an upload retry overwrites the same doc and does NOT fire this again.
 */
exports.notifyGroupOnZoneEvent = onDocumentCreated("groups/{groupId}/events/{eventId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;
  await handleZoneEvent({
    db: getFirestore(),
    messaging: getMessaging(),
    groupId: event.params.groupId,
    zoneEvent: snapshot.data(),
    now: Date.now(),
    logger,
  });
});