const { test } = require("node:test");
const assert = require("node:assert/strict");
const notify = require("../src/notify");

const NOW = 1_800_000_000_000;
const group = { name: "Home", memberIds: ["a", "b", "c"], memberNames: { a: "Asha", b: "Bala", c: "Chetan" } };
const exitEvent = (overrides = {}) => ({ userId: "a", userName: "Asha", type: "EXIT", timestamp: NOW, ...overrides });

test("valid EXIT from a member passes validation", () => {
  assert.deepEqual(notify.validateEvent(exitEvent(), group, NOW), { ok: true });
});

test("event from a non-member is rejected", () => {
  assert.equal(notify.validateEvent(exitEvent({ userId: "stranger" }), group, NOW).ok, false);
});

test("unknown event type is rejected", () => {
  assert.equal(notify.validateEvent(exitEvent({ type: "TELEPORT" }), group, NOW).ok, false);
});

test("missing or non-numeric timestamp is rejected", () => {
  assert.equal(notify.validateEvent(exitEvent({ timestamp: undefined }), group, NOW).ok, false);
  assert.equal(notify.validateEvent(exitEvent({ timestamp: "yesterday" }), group, NOW).ok, false);
});

test("timestamp far in the future is rejected", () => {
  assert.equal(notify.validateEvent(exitEvent({ timestamp: NOW + 10 * 60_000 }), group, NOW).ok, false);
});

test("stale event (older than the max age) is not notified", () => {
  const old = NOW - notify.MAX_EVENT_AGE_MS - 1;
  assert.equal(notify.validateEvent(exitEvent({ timestamp: old }), group, NOW).ok, false);
});

test("missing event or group is rejected", () => {
  assert.equal(notify.validateEvent(null, group, NOW).ok, false);
  assert.equal(notify.validateEvent(exitEvent(), null, NOW).ok, false);
});

test("recipients are all other members, deduplicated, never the actor", () => {
  const g = { memberIds: ["a", "b", "b", "c"] };
  assert.deepEqual(notify.recipientIds(g, "a"), ["b", "c"]);
});

test("EXIT builds a notification naming the member and the group", () => {
  const n = notify.buildNotification({ groupName: "Home", userName: "Asha", type: "EXIT", timestamp: NOW, now: NOW });
  assert.equal(n.title, "Home: member left the zone");
  assert.equal(n.body, "Asha has left the Home zone.");
});

test("ENTER does not produce a push", () => {
  assert.equal(notify.buildNotification({ groupName: "Home", userName: "Asha", type: "ENTER", timestamp: NOW, now: NOW }), null);
});

test("delayed uploads mention how long ago the exit happened", () => {
  const n = notify.buildNotification({ groupName: "Home", userName: "Asha", type: "EXIT", timestamp: NOW - 12 * 60_000, now: NOW });
  assert.equal(n.body, "Asha has left the Home zone (12 min ago).");
});

test("names are sanitised: control characters stripped, length capped, blank falls back", () => {
  assert.equal(notify.cleanName("  Asha\n\u0007Evil  "), "Asha Evil");
  assert.equal(notify.cleanName("x".repeat(100)).length, notify.MAX_NAME_LENGTH);
  assert.equal(notify.cleanName("   "), "A member");
  assert.equal(notify.cleanName(undefined), "A member");
});

test("tokens are read from snapshots, skipping missing/blank and duplicate tokens", () => {
  const snap = (id, data) => ({ id, exists: data !== undefined, data: () => data });
  const result = notify.tokensFromSnapshots([
    snap("b", { token: "t1" }),
    snap("c", undefined),
    snap("d", { token: "" }),
    snap("e", { token: "t1" }),
    snap("f", { token: "t2" }),
  ]);
  assert.deepEqual(result, [{ uid: "b", token: "t1" }, { uid: "f", token: "t2" }]);
});

test("only tokens FCM reports as dead are scheduled for removal", () => {
  const recipients = [{ uid: "b", token: "t1" }, { uid: "c", token: "t2" }, { uid: "d", token: "t3" }];
  const responses = [
    { success: true },
    { success: false, error: { code: "messaging/registration-token-not-registered" } },
    { success: false, error: { code: "messaging/internal-error" } },
  ];
  assert.deepEqual(notify.invalidTokenOwners(responses, recipients), ["c"]);
});

test("chunk splits arrays into FCM-sized batches", () => {
  const chunks = notify.chunk(Array.from({ length: 1201 }, (_, i) => i), 500);
  assert.deepEqual(chunks.map((c) => c.length), [500, 500, 201]);
});

test("cooldown: no previous alert means not in cooldown", () => {
  assert.equal(notify.isInCooldown(undefined, NOW, 300_000), false);
});

test("cooldown: a second exit within the window is suppressed, after the window it is not", () => {
  assert.equal(notify.isInCooldown(NOW, NOW + 30_000, 300_000), true);
  assert.equal(notify.isInCooldown(NOW, NOW + 300_000, 300_000), false);
});

test("cooldown: an older exit arriving late (out of order) is suppressed", () => {
  assert.equal(notify.isInCooldown(NOW, NOW - 60_000, 300_000), true);
});