/**
 * Pure helpers for turning a zone event into push notifications.
 * No Firebase imports here, so everything is unit-testable in milliseconds.
 */

const EVENT_TYPES = new Set(["EXIT", "ENTER"]);
const MAX_FUTURE_SKEW_MS = 5 * 60_000;        // tolerate small clock differences between phones and server
const MAX_EVENT_AGE_MS = 6 * 60 * 60_000;      // don't alert about exits that happened more than 6 h ago
const DELAY_NOTE_THRESHOLD_MS = 2 * 60_000;    // mention "(N min ago)" when the upload was delayed
const MAX_NAME_LENGTH = 40;
const EXIT_ALERT_COOLDOWN_MS = 5 * 60_000;     // GPS jitter at the edge can cause repeated exits
const FCM_MULTICAST_LIMIT = 500;
const DEAD_TOKEN_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

function validateEvent(event, group, now) {
  if (!event || typeof event !== "object") return { ok: false, reason: "missing-event" };
  if (!group || !Array.isArray(group.memberIds)) return { ok: false, reason: "missing-group" };
  if (typeof event.userId !== "string" || !group.memberIds.includes(event.userId)) {
    return { ok: false, reason: "not-a-member" };
  }
  if (!EVENT_TYPES.has(event.type)) return { ok: false, reason: "bad-type" };
  if (typeof event.timestamp !== "number" || !Number.isFinite(event.timestamp)) {
    return { ok: false, reason: "bad-timestamp" };
  }
  if (event.timestamp > now + MAX_FUTURE_SKEW_MS) return { ok: false, reason: "future-timestamp" };
  if (now - event.timestamp > MAX_EVENT_AGE_MS) return { ok: false, reason: "stale-event" };
  return { ok: true };
}

function recipientIds(group, actorId) {
  return [...new Set(group.memberIds)].filter((id) => id !== actorId);
}

/** Strips control characters/newlines and caps length so a name can't be used to fake notification text. */
function cleanName(name) {
  if (typeof name !== "string") return "A member";
  const cleaned = name.replace(/[\u0000-\u001F\u007F]+/g, " ").replace(/\s+/g, " ").trim();
  if (!cleaned) return "A member";
  return cleaned.slice(0, MAX_NAME_LENGTH);
}

function buildNotification({ groupName, userName, type, timestamp, now }) {
  if (type !== "EXIT") return null;
  const group = cleanName(groupName);
  const user = cleanName(userName);
  const delayMs = now - timestamp;
  const delayNote = delayMs >= DELAY_NOTE_THRESHOLD_MS ? ` (${Math.round(delayMs / 60_000)} min ago)` : "";
  return {
    title: `${group}: member left the zone`,
    body: `${user} has left the ${group} zone${delayNote}.`,
  };
}

function tokensFromSnapshots(snapshots) {
  const seen = new Set();
  const result = [];
  for (const snap of snapshots) {
    if (!snap.exists) continue;
    const token = snap.data()?.token;
    if (typeof token !== "string" || token.length === 0 || seen.has(token)) continue;
    seen.add(token);
    result.push({ uid: snap.id, token });
  }
  return result;
}

function invalidTokenOwners(responses, recipients) {
  const owners = [];
  responses.forEach((response, index) => {
    if (!response.success && DEAD_TOKEN_CODES.has(response.error?.code)) {
      owners.push(recipients[index].uid);
    }
  });
  return owners;
}

/**
 * True when this member already triggered an alert for this group less than `windowMs` ago
 * (by event time), or when this event is older than the last alerted one (arrived out of order).
 */
function isInCooldown(lastAlertAt, eventTimestamp, windowMs) {
  if (typeof lastAlertAt !== "number") return false;
  return eventTimestamp < lastAlertAt + windowMs;
}

function chunk(items, size) {
  const chunks = [];
  for (let i = 0; i < items.length; i += size) chunks.push(items.slice(i, i + size));
  return chunks;
}

module.exports = {
  EVENT_TYPES,
  MAX_EVENT_AGE_MS,
  MAX_NAME_LENGTH,
  EXIT_ALERT_COOLDOWN_MS,
  FCM_MULTICAST_LIMIT,
  validateEvent,
  recipientIds,
  cleanName,
  buildNotification,
  tokensFromSnapshots,
  invalidTokenOwners,
  isInCooldown,
  chunk,
};