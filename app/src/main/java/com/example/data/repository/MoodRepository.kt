package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Friend
import com.example.data.model.Reaction
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MoodRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Profile & Mood ---

    suspend fun getProfile(userId: String): Result<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                Result.success(snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
            } else {
                Result.success(null)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    suspend fun createOrUpdateProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to profile.displayName.ifBlank { "Пользователь" },
                    "currentMood" to profile.currentMood.ifBlank { "Хорошее" },
                    "moodEmoji" to profile.moodEmoji.ifBlank { "😊" },
                    "moodNote" to profile.moodNote,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "displayName" to profile.displayName.ifBlank { "Пользователь" },
                    "currentMood" to profile.currentMood.ifBlank { "Хорошее" },
                    "moodEmoji" to profile.moodEmoji.ifBlank { "😊" },
                    "moodNote" to profile.moodNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateMood(currentMood: String, moodEmoji: String, moodNote: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val updates = mapOf(
                "currentMood" to currentMood,
                "moodEmoji" to moodEmoji,
                "moodNote" to moodNote,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Friends ---

    fun observeFriends(userId: String): Flow<List<Friend>> {
        val friendsRef = db.collection("users").document(userId).collection("friends")
        return friendsRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(Friend::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, friendsRef.path)
                throw error
            }
    }

    suspend fun addFriend(friendUserId: String): Result<Friend> {
        val uid = requireUserId()
        require(friendUserId.isNotBlank()) { "ID друга не может быть пустым" }
        require(friendUserId != uid) { "Вы не можете добавить себя в друзья" }

        // Fetch friend's profile to check existence and get their display name
        val friendDocRef = db.collection("users").document(friendUserId)
        return try {
            val friendSnapshot = friendDocRef.get().await()
            if (!friendSnapshot.exists()) {
                return Result.failure(IllegalArgumentException("Пользователь с таким ID не найден"))
            }

            val friendProfile = friendSnapshot.toObject(UserProfile::class.java)
            val friendName = friendProfile?.displayName?.ifBlank { "Друг" } ?: "Друг"

            val friendSubDoc = db.collection("users").document(uid).collection("friends").document(friendUserId)
            val friendData = mapOf(
                "friendUserId" to friendUserId,
                "displayName" to friendName,
                "addedAt" to FieldValue.serverTimestamp()
            )
            friendSubDoc.set(friendData).await()

            val createdFriend = Friend(
                id = friendUserId,
                friendUserId = friendUserId,
                displayName = friendName
            )
            Result.success(createdFriend)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, friendDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendUserId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("friends").document(friendUserId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Reactions / Nudges ---

    fun observeReactions(userId: String): Flow<List<Reaction>> {
        val reactionsRef = db.collection("users").document(userId).collection("reactions")
        return reactionsRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(Reaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, reactionsRef.path)
                throw error
            }
    }

    suspend fun sendReaction(
        targetUserId: String,
        emoji: String,
        message: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val senderName = auth.currentUser?.displayName?.ifBlank { "Друг" } ?: "Друг"
        val reactionId = "rx_${System.currentTimeMillis()}_$uid"
        val docRef = db.collection("users").document(targetUserId).collection("reactions").document(reactionId)

        return try {
            val payload = mapOf(
                "fromUserId" to uid,
                "fromUserName" to senderName,
                "emoji" to emoji,
                "message" to message,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteReaction(reactionId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("reactions").document(reactionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
