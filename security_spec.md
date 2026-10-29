# Geajao Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **Authentication Gate**: No unauthenticated caller (`request.auth == null`) may read or write any collection in Geajao.
2. **Self-Role Escalation Prevention**: A standard user creating `/users/{userId}` MUST initialize `verificationStatus == 'PENDING'`, `primaryBadge == 'Unverified'`, and `badges == []` (unless they are already an Admin/Moderator). Standard users cannot self-approve their ID card or grant themselves `Cadet`, `Ex-cadet`, `Moderator`, or `Admin` badges.
3. **Tiered Badge & Verification Hierarchy**:
   - **Moderator (`isModerator()`)**: Can update `/users/{userId}` to set `verificationStatus` (`APPROVED` or `REJECTED`) and assign `Cadet` or `Ex-cadet` badges, but is strictly forbidden from assigning `Moderator` or `Admin` badges.
   - **Admin (`isAdmin()`)**: Can approve/reject users and assign `Cadet`, `Ex-cadet`, `Moderator`, and `Admin` badges, as well as manage `/moderators/{modUid}` and `/admins/{adminUid}`.
4. **Private Chat & Call Isolation**: `/chats/{chatId}`, `/chats/{chatId}/messages/{messageId}`, and `/calls/{callId}` are only readable and writable by authenticated users whose `request.auth.uid` is present in `participantUids`.
5. **Terminal State Locking on Calls**: Once a `/calls/{callId}` document transitions to `status == 'ENDED'`, no further updates are permitted by standard participants.
6. **Query Enforcer**: Every `allow list` rule evaluates `resource.data` (`corpsId == 'geajao_cadet_corps'` or `request.auth.uid in resource.data.participantUids`). Unfiltered queries are rejected.

## 2. The Dirty Dozen Payloads
1. **Unauthenticated Profile Read**: `unauthDb.collection("users").doc("u1").get()` -> Reject (`PERMISSION_DENIED`).
2. **Identity Spoofing on Profile Create**: Authenticated as `alice`, creating `/users/bob` or setting `userId: "bob"` -> Reject.
3. **Self-Assigned Admin Badge on Signup**: Creating `/users/alice` with `primaryBadge: "Admin"` as non-admin -> Reject.
4. **Shadow Ghost Field Injection**: Updating `/users/alice` with `{ bio: "Hello", secretAdminFlag: true }` -> Reject via `affectedKeys().hasOnly(...)`.
5. **Moderator Escalating to Admin Badge**: Moderator updating `/users/bob` with `primaryBadge: "Admin"` -> Reject.
6. **Cross-User Private Chat Read**: `charlie` reading `/chats/alice_bob_chat` where `participantUids: ["alice", "bob"]` -> Reject.
7. **Cross-User Encrypted Message Injection**: `charlie` creating `/chats/alice_bob_chat/messages/m1` -> Reject.
8. **Unfiltered Users Collection Scraping**: `aliceDb.collection("users").get()` without `.where("corpsId", "==", "geajao_cadet_corps")` -> Reject.
9. **Terminal Call State Mutation**: Updating `/calls/call_1` after `status == "ENDED"` -> Reject.
10. **Immortal Field Mutation**: Updating `createdAt` or `authorId` on `/posts/post_1` -> Reject.
11. **Value Poisoning**: Updating `likesCount` on `/posts/post_1` with a string `"many"` instead of an integer -> Reject via `isValidFeedPost(incoming())`.
12. **Future Timestamp Injection**: Writing `createdAt` far in the future (`> request.time`) -> Reject.
