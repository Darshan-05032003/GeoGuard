const notify = require("./notify");

const ANDROID_CHANNEL_ID = "zone_alerts";

/**
 * Handles one new document at groups/{groupId}/events/{eventId}.
 * Dependencies (db, messaging, clock, logger) are passed in so tests can use fakes.
 */
async function handleZoneEvent({ db, messaging, groupId, zoneEvent, now, logger }) {
  const groupSnap = await db.doc(`groups/${groupId}`).get();
  if (!groupSnap.exists) {
    logger.warn("Event for missing group", { groupId });
    return { sent: 0, reason: "group-not-found" };
  }
  const group = groupSnap.data();

  const validation = notify.validateEvent(zoneEvent, group, now);
  if (!validation.ok) {
    logger.warn("Ignoring invalid zone event", { groupId, reason: validation.reason });
    return { sent: 0, reason: validation.reason };
  }

  // Trust the server-side group document for names, not the text the client wrote into the event.
  const userName = group.memberNames?.[zoneEvent.userId] ?? zoneEvent.userName;
  const notification = notify.buildNotification({
    groupName: group.name,
    userName,
    type: zoneEvent.type,
    timestamp: zoneEvent.timestamp,
    now,
  });
  if (!notification) return { sent: 0, reason: "not-notifiable" };

  // Server-side state (no client rule allows access to alertState) to suppress repeated alerts.
  const stateRef = db.doc(`groups/${groupId}/alertState/${zoneEvent.userId}`);
  const stateSnap = await stateRef.get();
  const lastAlertAt = stateSnap.exists ? stateSnap.data().lastExitAlertAt : undefined;
  if (notify.isInCooldown(lastAlertAt, zoneEvent.timestamp, notify.EXIT_ALERT_COOLDOWN_MS)) {
    logger.info("Exit alert suppressed by cooldown", { groupId, userId: zoneEvent.userId });
    return { sent: 0, reason: "cooldown" };
  }

  const recipients = notify.recipientIds(group, zoneEvent.userId);
  if (recipients.length === 0) return { sent: 0, reason: "no-recipients" };

  const tokenSnaps = await db.getAll(...recipients.map((uid) => db.doc(`fcmTokens/${uid}`)));
  const tokens = notify.tokensFromSnapshots(tokenSnaps);
  if (tokens.length === 0) return { sent: 0, reason: "no-tokens" };

  let sent = 0;
  const deadTokenOwners = [];
  for (const batch of notify.chunk(tokens, notify.FCM_MULTICAST_LIMIT)) {
    const response = await messaging.sendEachForMulticast({
      tokens: batch.map((t) => t.token),
      notification,
      android: {
        priority: "high",
        // Same tag = a newer alert about the same member replaces the old one in the tray.
        notification: { channelId: ANDROID_CHANNEL_ID, tag: `${groupId}-${zoneEvent.userId}` },
      },
      data: { groupId, eventType: zoneEvent.type, userId: zoneEvent.userId },
    });
    sent += response.successCount;
    deadTokenOwners.push(...notify.invalidTokenOwners(response.responses, batch));
  }

  await Promise.all([
    stateRef.set({ lastExitAlertAt: zoneEvent.timestamp }),
    ...deadTokenOwners.map((uid) => db.doc(`fcmTokens/${uid}`).delete()),
  ]);
  logger.info("Zone alert sent", { groupId, sent, removedTokens: deadTokenOwners.length });
  return { sent, removedTokens: deadTokenOwners.length };
}

module.exports = { handleZoneEvent };