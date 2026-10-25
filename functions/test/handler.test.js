const { test } = require("node:test");
const assert = require("node:assert/strict");
const { handleZoneEvent } = require("../src/handler");

const NOW = 1_800_000_000_000;
const silentLogger = { info() {}, warn() {}, error() {} };

/** Minimal in-memory stand-in for the Admin Firestore API used by the handler. */
function fakeDb(docs) {
  const store = new Map(Object.entries(docs));
  const ref = (path) => ({
    path,
    id: path.split("/").pop(),
    async get() {
      const data = store.get(path);
      return { id: path.split("/").pop(), exists: data !== undefined, data: () => data };
    },
    async delete() { store.delete(path); },
    async set(data) { store.set(path, data); },
  });
  return {
    store,
    doc: ref,
    async getAll(...refs) { return Promise.all(refs.map((r) => r.get())); },
  };
}

function fakeMessaging({ deadTokens = [] } = {}) {
  const calls = [];
  return {
    calls,
    async sendEachForMulticast(message) {
      calls.push(message);
      const responses = message.tokens.map((t) =>
        deadTokens.includes(t)
          ? { success: false, error: { code: "messaging/registration-token-not-registered" } }
          : { success: true });
      return { successCount: responses.filter((r) => r.success).length, responses };
    },
  };
}

const baseDocs = () => ({
  "groups/g1": { name: "Home", memberIds: ["a", "b", "c"], memberNames: { a: "Asha", b: "Bala", c: "Chetan" } },
  "fcmTokens/a": { token: "token-a" },
  "fcmTokens/b": { token: "token-b" },
  "fcmTokens/c": { token: "token-c" },
});

const exitByA = { userId: "a", userName: "Asha", type: "EXIT", timestamp: NOW };

test("EXIT notifies every other member and never the member who left", async () => {
  const db = fakeDb(baseDocs());
  const messaging = fakeMessaging();
  const result = await handleZoneEvent({ db, messaging, groupId: "g1", zoneEvent: exitByA, now: NOW, logger: silentLogger });

  assert.equal(result.sent, 2);
  assert.equal(messaging.calls.length, 1);
  assert.deepEqual(messaging.calls[0].tokens.sort(), ["token-b", "token-c"]);
  assert.equal(messaging.calls[0].notification.body, "Asha has left the Home zone.");
  assert.equal(messaging.calls[0].android.notification.channelId, "zone_alerts");
  assert.equal(messaging.calls[0].data.groupId, "g1");
});

test("ENTER sends nothing", async () => {
  const messaging = fakeMessaging();
  const result = await handleZoneEvent({
    db: fakeDb(baseDocs()), messaging, groupId: "g1",
    zoneEvent: { ...exitByA, type: "ENTER" }, now: NOW, logger: silentLogger,
  });
  assert.equal(result.sent, 0);
  assert.equal(messaging.calls.length, 0);
});

test("event for a missing group sends nothing", async () => {
  const messaging = fakeMessaging();
  const result = await handleZoneEvent({ db: fakeDb({}), messaging, groupId: "nope", zoneEvent: exitByA, now: NOW, logger: silentLogger });
  assert.equal(result.reason, "group-not-found");
  assert.equal(messaging.calls.length, 0);
});

test("forged event from a non-member sends nothing", async () => {
  const messaging = fakeMessaging();
  const result = await handleZoneEvent({
    db: fakeDb(baseDocs()), messaging, groupId: "g1",
    zoneEvent: { ...exitByA, userId: "intruder" }, now: NOW, logger: silentLogger,
  });
  assert.equal(result.sent, 0);
  assert.equal(messaging.calls.length, 0);
});

test("members without a token are skipped; no send when nobody has one", async () => {
  const docs = baseDocs();
  delete docs["fcmTokens/b"];
  delete docs["fcmTokens/c"];
  const messaging = fakeMessaging();
  const result = await handleZoneEvent({ db: fakeDb(docs), messaging, groupId: "g1", zoneEvent: exitByA, now: NOW, logger: silentLogger });
  assert.equal(result.reason, "no-tokens");
  assert.equal(messaging.calls.length, 0);
});

test("dead tokens reported by FCM are deleted", async () => {
  const db = fakeDb(baseDocs());
  const messaging = fakeMessaging({ deadTokens: ["token-c"] });
  const result = await handleZoneEvent({ db, messaging, groupId: "g1", zoneEvent: exitByA, now: NOW, logger: silentLogger });
  assert.equal(result.sent, 1);
  assert.equal(result.removedTokens, 1);
  assert.equal(db.store.has("fcmTokens/c"), false);
  assert.equal(db.store.has("fcmTokens/b"), true);
});

test("display name comes from the group (server-side), not from the client-written event", async () => {
  const messaging = fakeMessaging();
  await handleZoneEvent({
    db: fakeDb(baseDocs()), messaging, groupId: "g1",
    zoneEvent: { ...exitByA, userName: "The Admin Team: tap here" }, now: NOW, logger: silentLogger,
  });
  assert.equal(messaging.calls[0].notification.body, "Asha has left the Home zone.");
});

test("flapping at the zone edge: a second EXIT by the same member within 5 min is not pushed again", async () => {
  const db = fakeDb(baseDocs());
  const messaging = fakeMessaging();
  const first = await handleZoneEvent({ db, messaging, groupId: "g1", zoneEvent: exitByA, now: NOW, logger: silentLogger });
  const again = { ...exitByA, timestamp: NOW + 60_000 };
  const second = await handleZoneEvent({ db, messaging, groupId: "g1", zoneEvent: again, now: NOW + 60_000, logger: silentLogger });
  const later = { ...exitByA, timestamp: NOW + 6 * 60_000 };
  const third = await handleZoneEvent({ db, messaging, groupId: "g1", zoneEvent: later, now: NOW + 6 * 60_000, logger: silentLogger });

  assert.equal(first.sent, 2);
  assert.equal(second.reason, "cooldown");
  assert.equal(third.sent, 2);
  assert.equal(messaging.calls.length, 2);
});

test("cooldown is per member: another member's exit is still pushed", async () => {
  const db = fakeDb(baseDocs());
  const messaging = fakeMessaging();
  await handleZoneEvent({ db, messaging, groupId: "g1", zoneEvent: exitByA, now: NOW, logger: silentLogger });
  const byB = { userId: "b", userName: "Bala", type: "EXIT", timestamp: NOW + 1000 };
  const result = await handleZoneEvent({ db, messaging, groupId: "g1", zoneEvent: byB, now: NOW + 1000, logger: silentLogger });
  assert.equal(result.sent, 2);
});

test("notifications carry a per-member tag so repeated alerts replace each other on the device", async () => {
  const messaging = fakeMessaging();
  await handleZoneEvent({ db: fakeDb(baseDocs()), messaging, groupId: "g1", zoneEvent: exitByA, now: NOW, logger: silentLogger });
  assert.equal(messaging.calls[0].android.notification.tag, "g1-a");
});