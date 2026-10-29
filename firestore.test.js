const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_commandant_1";
const MOD_UID = "mod_officer_2";
const ALICE_UID = "cadet_alice_3";
const BOB_UID = "cadet_bob_4";
const CHARLIE_UID = "cadet_charlie_5";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function buildValidCadetProfile(userId, now) {
  return {
    userId,
    corpsId: "geajao_cadet_corps",
    displayName: "Cadet Alice Vance",
    cadetNumber: "CDT-2026-042",
    batchYear: "Batch 54",
    academyHouse: "Valor Squadron",
    bio: "Duty, Honor, Academy.",
    verificationStatus: "PENDING",
    primaryBadge: "Unverified",
    badges: [],
    idCardImageBase64: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJ",
    idCardNote: "Submitted official Academy ID card.",
    followersCount: 0,
    profileViews: 0,
    totalEngagement: 0,
    createdAt: now,
    updatedAt: now,
  };
}

test("1. Unauthenticated user cannot read or list cadet profiles, posts, friendships, chats, or calls", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("posts").where("corpsId", "==", "geajao_cadet_corps").get());
  await assertFails(unauthDb.collection("friendships").where("participantUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("chats").where("participantUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("calls").where("participantUids", "array-contains", ALICE_UID).get());
});

test("2. Cadet can register PENDING profile with ID card, but cannot self-assign Admin/Cadet badge on signup", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  const spoofedAdminProfile = {
    ...buildValidCadetProfile(ALICE_UID, now),
    verificationStatus: "APPROVED",
    primaryBadge: "Admin",
    badges: ["Admin"],
  };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(spoofedAdminProfile));

  const validPendingProfile = buildValidCadetProfile(ALICE_UID, now);
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validPendingProfile));
});

test("3. Moderator unlocked via passcode 5246 can approve Cadet ID and grant Cadet or Ex-cadet badge, but CANNOT grant Admin or Moderator badge", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).set(buildValidCadetProfile(ALICE_UID, now));
  });

  const modDb = testEnv.authenticatedContext(MOD_UID).firestore();

  // Moderator self-activates with passcode 5246 -> SUCCEEDS
  await assertSucceeds(
    modDb.collection("moderators").doc(MOD_UID).set({
      uid: MOD_UID,
      grantedBy: MOD_UID,
      bootstrapCode: "5246",
      createdAt: now,
    })
  );

  // Moderator approving Alice and assigning "Cadet" badge -> SUCCEEDS
  await assertSucceeds(
    modDb.collection("users").doc(ALICE_UID).update({
      verificationStatus: "APPROVED",
      primaryBadge: "Cadet",
      badges: ["Cadet"],
      idCardNote: "Verified by Moderator Officer",
      updatedAt: new Date(),
    })
  );

  // Moderator trying to escalate Alice to "Admin" or "Moderator" -> FAILS
  await assertFails(
    modDb.collection("users").doc(ALICE_UID).update({
      primaryBadge: "Admin",
      badges: ["Cadet", "Admin"],
      updatedAt: new Date(),
    })
  );
  await assertFails(
    modDb.collection("users").doc(ALICE_UID).update({
      primaryBadge: "Moderator",
      badges: ["Cadet", "Moderator"],
      updatedAt: new Date(),
    })
  );
});

test("4. Admin unlocked via passcode A54h can approve user, grant all badges, and upload custom App Icon from gallery", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).set(buildValidCadetProfile(BOB_UID, now));
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Admin self-activates with passcode A54h -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      grantedBy: ADMIN_UID,
      bootstrapCode: "A54h",
      createdAt: now,
    })
  );

  // Admin granting Moderator + Ex-cadet badges simultaneously -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("users").doc(BOB_UID).update({
      verificationStatus: "APPROVED",
      primaryBadge: "Moderator",
      badges: ["Moderator", "Ex-cadet"],
      idCardNote: "Approved by Commandant Admin as Moderator + Ex-cadet",
      updatedAt: new Date(),
    })
  );

  // Admin granting Admin + Cadet badges simultaneously -> SUCCEEDS
  await assertSucceeds(
    adminDb.collection("users").doc(BOB_UID).update({
      verificationStatus: "APPROVED",
      primaryBadge: "Admin",
      badges: ["Admin", "Cadet"],
      idCardNote: "Approved by Commandant Admin as Admin + Cadet",
      updatedAt: new Date(),
    })
  );

  // Admin can upload custom gallery app icon in /app_config/global, non-admin cannot
  await assertSucceeds(
    adminDb.collection("app_config").doc("global").set({
      configId: "global",
      selectedIconKey: "CUSTOM_GALLERY",
      customIconBase64: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      updatedBy: ADMIN_UID,
      updatedAt: new Date(),
    })
  );
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("app_config").doc("global").set({
      configId: "global",
      selectedIconKey: "EMERALD_EAGLE",
      customIconBase64: "",
      updatedBy: ALICE_UID,
      updatedAt: new Date(),
    })
  );
});

test("5. Friendships & Encrypted Private Chat with GIF and Sticker messages enforce strict participant isolation", async () => {
  const now = new Date();
  const friendshipId = `friend_${ALICE_UID}_${BOB_UID}`;
  const chatId = "chat_alice_bob";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Alice sends friend request to Bob -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("friendships").doc(friendshipId).set({
      participantUids: [ALICE_UID, BOB_UID],
      requesterId: ALICE_UID,
      requesterName: "Cadet Alice Vance",
      receiverId: BOB_UID,
      receiverName: "Cadet Bob Sterling",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob accepts friend request -> SUCCEEDS
  await assertSucceeds(
    bobDb.collection("friendships").doc(friendshipId).update({
      status: "ACCEPTED",
      updatedAt: new Date(),
    })
  );

  // Charlie cannot read Alice & Bob's friendship -> FAILS
  await assertFails(charlieDb.collection("friendships").doc(friendshipId).get());

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      participantUids: [ALICE_UID, BOB_UID],
      participantNames: "Alice Vance & Bob Sterling",
      chatTitle: "Bob Sterling",
      lastMessagePreview: "[AES-256-GCM Encrypted]",
      disappearingTtlSeconds: 300,
      isEncrypted: true,
      encryptionKeyHint: "GCM-SHA256-9F4A",
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice sends an encrypted GIF message -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_gif").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Cadet Alice Vance",
      senderBadge: "Cadet",
      participantUids: [ALICE_UID, BOB_UID],
      cipherText: "U2FsdGVkX19k3j4h5g6f7d8s9a0=",
      ivBase64: "MTIzNDU2Nzg5MDEy",
      mediaType: "GIF",
      mediaDataBase64: "GIF_SALUTE_RESPECT",
      ttlSeconds: 300,
      expiresAtEpochMs: Date.now() + 300000,
      createdAt: now,
    })
  );

  // Bob sends an encrypted STICKER message -> SUCCEEDS
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_sticker").set({
      chatId,
      senderId: BOB_UID,
      senderName: "Cadet Bob Sterling",
      senderBadge: "Cadet",
      participantUids: [ALICE_UID, BOB_UID],
      cipherText: "U2FsdGVkX19k3j4h5g6f7d8s9a0=",
      ivBase64: "MTIzNDU2Nzg5MDEy",
      mediaType: "STICKER",
      mediaDataBase64: "STICKER_BRAVO_ZULU",
      ttlSeconds: 300,
      expiresAtEpochMs: Date.now() + 300000,
      createdAt: now,
    })
  );

  // Charlie (non-participant) cannot read the chat room or messages -> FAILS
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_gif").get());
});

test("6. Voice & Video Call Signaling enforces participant isolation and Terminal State Locking", async () => {
  const now = new Date();
  const callId = "call_session_1";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("calls").doc(callId).set({
      callerId: ALICE_UID,
      callerName: "Cadet Alice",
      callerBadge: "Cadet",
      calleeId: BOB_UID,
      calleeName: "Cadet Bob",
      participantUids: [ALICE_UID, BOB_UID],
      callType: "VIDEO",
      status: "RINGING",
      encryptionFingerprint: "SRTP-DTLS-7B2E-99A1",
      isCallerMuted: false,
      isCalleeMuted: false,
      isVideoEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("calls").doc(callId).update({
      status: "CONNECTED",
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("calls").doc(callId).update({
      status: "ENDED",
      updatedAt: new Date(),
    })
  );

  await assertFails(
    bobDb.collection("calls").doc(callId).update({
      status: "CONNECTED",
      updatedAt: new Date(),
    })
  );
});

test("7. FCM Device Tokens (/fcm_tokens) and Real-Time Push Notification Queue (/notifications) enforce strict user ownership and recipient isolation", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Alice registers her own FCM token -> SUCCEEDS
  await assertSucceeds(
    aliceDb.collection("fcm_tokens").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fcmToken: "fcm_token_alice_12345",
      platform: "ANDROID",
      updatedAt: now,
    })
  );

  // Bob cannot overwrite Alice's FCM token -> FAILS
  await assertFails(
    bobDb.collection("fcm_tokens").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fcmToken: "spoofed_token",
      platform: "ANDROID",
      updatedAt: now,
    })
  );

  // Alice dispatches a push notification event to Bob -> SUCCEEDS
  const notifId = "notif_msg_alice_to_bob";
  await assertSucceeds(
    aliceDb.collection("notifications").doc(notifId).set({
      recipientId: BOB_UID,
      senderId: ALICE_UID,
      senderName: "Cadet Alice",
      senderBadge: "Cadet",
      type: "PRIVATE_MESSAGE",
      title: "Cadet Alice • Cadet",
      body: "New encrypted message received",
      targetId: "chat_alice_bob",
      isDelivered: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Charlie cannot read Bob's push notification -> FAILS
  await assertFails(charlieDb.collection("notifications").doc(notifId).get());

  // Bob can read and mark his push notification as delivered -> SUCCEEDS
  await assertSucceeds(bobDb.collection("notifications").doc(notifId).get());
  await assertSucceeds(
    bobDb.collection("notifications").doc(notifId).update({
      isDelivered: true,
      updatedAt: new Date(),
    })
  );
});

