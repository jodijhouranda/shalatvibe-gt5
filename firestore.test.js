const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_nida_123";
const BOB_UID = "bob_nida_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validKamusPayload(overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    arabic: "تَصَرُّفٌ",
    transliteration: "tasharruf",
    meaning: "tindakan hukum / transaksi",
    turatsOrigin: "ص-ر-ف",
    synonymNote: "berbeda dengan fi'il biasa",
    kind: "word",
    lessonId: "v01",
    lessonTitle: "Video 01",
    segmentLabel: "00:10 – 01:00",
    segmentStartSec: 10,
    contextSentence: "هَذَا تَصَرُّفٌ صَحِيحٌ",
    personalNote: "Penting di fiqh muamalah",
    masteryStage: 1,
    nextReviewEpochDay: 20400,
    createdAtMillis: Date.now() - 5000,
    reviewCount: 1,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validProgressPayload(overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    completedArabicIds: ["v01", "v02"],
    favoriteArabicIds: ["v01"],
    completedCourseBlockIds: ["ep01_b01"],
    lastOpenedLessonId: "v02",
    createdAt: now,
    updatedAt: now,
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

// --- UNAUTHENTICATED ACCESS DENIAL ---

test("Unauthenticated user: cannot read or write kamus or progress", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("kamus").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("kamus").doc("km_abc123").set(validKamusPayload())
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("progress").doc("learning_v1").set(validProgressPayload())
  );
});

// --- KAMUS SAYA CRUD, SRS UPDATES & SECURITY BOUNDS ---

test("Authenticated owner: valid Kotlin kamus create, list, SRS update, edit update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const kamusCol = aliceDb.collection("users").doc(ALICE_UID).collection("kamus");

  // Empty subcollection list must succeed (no existing() check in allow list)
  await assertSucceeds(kamusCol.get());

  // Create without @DocumentId 'id' field
  await assertSucceeds(kamusCol.doc("km_abc123").set(validKamusPayload()));
  await assertSucceeds(kamusCol.doc("km_abc123").get());
  await assertSucceeds(kamusCol.get());

  // Partial SRS update (preserves createdAt & createdAtMillis)
  await assertSucceeds(
    kamusCol.doc("km_abc123").update({
      masteryStage: 2,
      nextReviewEpochDay: 20403,
      reviewCount: 2,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Full edit update (preserves createdAt & createdAtMillis)
  await assertSucceeds(
    kamusCol.doc("km_abc123").update({
      meaning: "tindakan hukum yang sah",
      personalNote: "Diperbarui dari web",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Delete succeeds
  await assertSucceeds(kamusCol.doc("km_abc123").delete());
});

test("Kamus security bounds: rejects ghost fields, id field, invalid kind/stage, future timestamps, and immutable mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const kamusCol = aliceDb.collection("users").doc(ALICE_UID).collection("kamus");

  // Reject ghost field (Shadow Update Test)
  await assertFails(
    kamusCol.doc("km_ghost").set(validKamusPayload({ isAdmin: true }))
  );

  // Reject embedded 'id' field when @DocumentId is used
  await assertFails(
    kamusCol.doc("km_with_id").set(validKamusPayload({ id: "km_with_id" }))
  );

  // Reject invalid enum kind
  await assertFails(
    kamusCol.doc("km_badkind").set(validKamusPayload({ kind: "WORD" }))
  );

  // Reject out-of-bounds masteryStage
  await assertFails(
    kamusCol.doc("km_badstage").set(validKamusPayload({ masteryStage: 9 }))
  );

  // Reject future tampered timestamp
  await assertFails(
    kamusCol.doc("km_future").set(validKamusPayload({ createdAt: new Date(Date.now() + 60000) }))
  );

  // Seed valid entry then reject immutable createdAt / createdAtMillis mutation
  await assertSucceeds(kamusCol.doc("km_imm").set(validKamusPayload()));
  await assertFails(
    kamusCol.doc("km_imm").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    kamusCol.doc("km_imm").update({
      createdAtMillis: 999999999,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

// --- CROSS-USER ISOLATION ---

test("Cross-user isolation: Alice cannot read, list, create, update, or delete Bob's kamus or progress", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).collection("kamus").doc("km_bob1").set(validKamusPayload());
    await db.collection("users").doc(BOB_UID).collection("progress").doc("learning_v1").set(validProgressPayload());
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobKamus = aliceDb.collection("users").doc(BOB_UID).collection("kamus");
  const bobProgress = aliceDb.collection("users").doc(BOB_UID).collection("progress");

  await assertFails(bobKamus.get());
  await assertFails(bobKamus.doc("km_bob1").get());
  await assertFails(bobKamus.doc("km_bob1").update({ meaning: "Hijacked", updatedAt: new Date(Date.now() - 500) }));
  await assertFails(bobKamus.doc("km_bob1").delete());
  await assertFails(bobKamus.doc("km_new").set(validKamusPayload()));

  await assertFails(bobProgress.get());
  await assertFails(bobProgress.doc("learning_v1").get());
  await assertFails(
    bobProgress.doc("learning_v1").update({
      completedArabicIds: ["v99"],
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

// --- LEARNING PROGRESS CRUD & BOUNDS ---

test("Authenticated owner: valid learning progress create and partial update succeed; immutable createdAt rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const progressDoc = aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("learning_v1");

  await assertSucceeds(progressDoc.set(validProgressPayload()));
  await assertSucceeds(progressDoc.get());

  await assertSucceeds(
    progressDoc.update({
      completedArabicIds: ["v01", "v02", "v03"],
      lastOpenedLessonId: "v03",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field
  await assertFails(
    progressDoc.update({
      role: "admin",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject createdAt mutation
  await assertFails(
    progressDoc.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});
