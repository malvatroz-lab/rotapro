const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_driver_123";
const BOB_UID = "bob_driver_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validEntryPayload(uid, overrides = {}) {
  return {
    userId: uid,
    year: 2026,
    month: 11,
    day: 12,
    yearMonth: "2026-11",
    uber: 250.5,
    pop99: 110.0,
    combustivel: 85.0,
    recarga: 20.0,
    aluguel: 70.0,
    alimento: 30.0,
    manutencao: 0.0,
    kmRodado: 195.0,
    notes: "Bom movimento na sexta",
    createdAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or list daily entries", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("daily_entries").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("daily_entries")
      .doc("2026-11-12")
      .get()
  );
});

test("Authenticated user can create, list with yearMonth filter, update, and delete own daily entry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("daily_entries")
    .doc("2026-11-12");

  await assertSucceeds(docRef.set(validEntryPayload(ALICE_UID)));

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("daily_entries")
      .where("yearMonth", "==", "2026-11")
      .get()
  );

  await assertSucceeds(
    docRef.update({
      uber: 310.0,
      kmRodado: 220.0,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(docRef.delete());
});

test("Authenticated user cannot read, write, or delete another user's daily entry", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("daily_entries")
      .doc("2026-11-12")
      .set(validEntryPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDoc = aliceDb
    .collection("users")
    .doc(BOB_UID)
    .collection("daily_entries")
    .doc("2026-11-12");

  await assertFails(bobDoc.get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("daily_entries").get()
  );
  await assertFails(bobDoc.set(validEntryPayload(ALICE_UID)));
  await assertFails(bobDoc.update({ uber: 999.0, updatedAt: new Date() }));
  await assertFails(bobDoc.delete());
});

test("Shadow update with ghost field or immutable field tamper is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("daily_entries")
    .doc("2026-11-12");

  // Ghost field on create
  await assertFails(
    docRef.set({
      ...validEntryPayload(ALICE_UID),
      isAdmin: true,
    })
  );

  // Valid create
  await assertSucceeds(docRef.set(validEntryPayload(ALICE_UID)));

  // Immutable yearMonth tamper on update
  await assertFails(
    docRef.update({
      yearMonth: "2026-12",
      updatedAt: new Date(),
    })
  );

  // Negative expense value rejected
  await assertFails(
    docRef.update({
      combustivel: -10.0,
      updatedAt: new Date(),
    })
  );
});

test("Spoofed userId on create is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("daily_entries")
    .doc("2026-11-12");

  await assertFails(docRef.set(validEntryPayload(BOB_UID)));
});
