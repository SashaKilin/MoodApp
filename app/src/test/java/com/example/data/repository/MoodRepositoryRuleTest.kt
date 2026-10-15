package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndGetProfile_authenticatedUser_succeeds() = runBlocking {
    val email = "alice_${System.currentTimeMillis()}@moodsync.test"
    val uid = signInTestUser(email)
    val repo = MoodRepository(firestore)

    val profile = UserProfile(
      userId = uid,
      displayName = "Alice",
      currentMood = "Вдохновлённое",
      moodEmoji = "✨",
      moodNote = "Делаю крутой проект"
    )

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createOrUpdateProfile(profile)
    }
    assertTrue("Save profile should succeed", saveResult.isSuccess)

    val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.getProfile(uid)
    }
    assertTrue("Get profile should succeed", fetched.isSuccess)
    val userProfile = fetched.getOrNull()
    assertNotNull(userProfile)
    assertEquals("Alice", userProfile?.displayName)
    assertEquals("Вдохновлённое", userProfile?.currentMood)
    assertEquals("✨", userProfile?.moodEmoji)
  }

  @Test
  fun updateMood_authenticatedUser_updatesStatus() = runBlocking {
    val email = "alice_update_${System.currentTimeMillis()}@moodsync.test"
    val uid = signInTestUser(email)
    val repo = MoodRepository(firestore)

    val initial = UserProfile(
      userId = uid,
      displayName = "Alice",
      currentMood = "Спокойное",
      moodEmoji = "☕"
    )
    repo.createOrUpdateProfile(initial)

    val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateMood("Продуктивное", "🔥", "В потоке!")
    }
    assertTrue("Update mood should succeed", updateResult.isSuccess)

    val updatedProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.getProfile(uid).getOrNull()
    }
    assertEquals("Продуктивное", updatedProfile?.currentMood)
    assertEquals("🔥", updatedProfile?.moodEmoji)
    assertEquals("В потоке!", updatedProfile?.moodNote)
  }

  @Test
  fun addFriendAndSendReaction_betweenUsers_succeeds() = runBlocking {
    val bobEmail = "bob_${System.currentTimeMillis()}@moodsync.test"
    val aliceEmail = "alice_${System.currentTimeMillis()}@moodsync.test"

    // 1. Setup Bob
    val bobUid = signInTestUser(bobEmail)
    val bobRepo = MoodRepository(firestore)
    bobRepo.createOrUpdateProfile(UserProfile(userId = bobUid, displayName = "Bob", currentMood = "Весёлое", moodEmoji = "🎉"))

    // 2. Setup Alice
    val aliceUid = signInTestUser(aliceEmail)
    val aliceRepo = MoodRepository(firestore)
    aliceRepo.createOrUpdateProfile(UserProfile(userId = aliceUid, displayName = "Alice", currentMood = "Рабочее", moodEmoji = "💻"))

    // Alice adds Bob as friend
    val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.addFriend(bobUid)
    }
    assertTrue("Add friend should succeed: ${addResult.exceptionOrNull()?.message ?: addResult.exceptionOrNull()}", addResult.isSuccess)

    val friendsList = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeFriends(aliceUid).first { it.isNotEmpty() }
    }
    assertEquals(1, friendsList.size)
    assertEquals(bobUid, friendsList[0].friendUserId)

    // Alice sends reaction to Bob
    val sendReactionResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.sendReaction(bobUid, "❤️", "Так держать!")
    }
    assertTrue("Send reaction should succeed", sendReactionResult.isSuccess)

    // Bob signs in and observes reaction
    signInTestUser(bobEmail)
    val bobReactions = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observeReactions(bobUid).first { it.isNotEmpty() }
    }
    assertEquals(1, bobReactions.size)
    assertEquals("❤️", bobReactions[0].emoji)
  }

  @Test
  fun unauthenticatedAccess_failsWithException() = runBlocking {
    auth.signOut()
    val repo = MoodRepository(firestore)

    var caught = false
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeFriends("any_user").first()
      }
    } catch (e: Exception) {
      caught = true
      // May be IllegalStateException from requireUserId or FirebaseFirestoreException PERMISSION_DENIED
    }
    assertTrue("Unauthenticated call must fail", caught)
  }

  private companion object {
    val ALICE_EMAIL get() = "alice_${System.nanoTime()}@moodsync.test"
    val BOB_EMAIL get() = "bob_${System.nanoTime()}@moodsync.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
