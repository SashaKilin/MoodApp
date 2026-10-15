package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val currentMood: String = "Хорошее",
    val moodEmoji: String = "😊",
    val moodNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Friend(
    @DocumentId val id: String = "",
    val friendUserId: String = "",
    val displayName: String = "",
    @ServerTimestamp val addedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Reaction(
    @DocumentId val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val emoji: String = "❤️",
    val message: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class FriendWithMood(
    val friend: Friend,
    val moodProfile: UserProfile? = null
)

data class MoodPreset(
    val emoji: String,
    val name: String,
    val description: String
)
