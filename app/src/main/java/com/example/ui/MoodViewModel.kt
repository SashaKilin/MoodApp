package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Friend
import com.example.data.model.FriendWithMood
import com.example.data.model.MoodPreset
import com.example.data.model.Reaction
import com.example.data.model.UserProfile
import com.example.data.repository.MoodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "MoodViewModel"

val MOOD_PRESETS = listOf(
    MoodPreset("😊", "Радостное", "Полон энергии, делюсь улыбкой"),
    MoodPreset("🔥", "Продуктивное", "В потоке фокуса и задач"),
    MoodPreset("☕", "Спокойное", "Пью кофе, наслаждаюсь моментом"),
    MoodPreset("🎉", "Праздничное", "Отличное настроение, время праздновать!"),
    MoodPreset("😴", "Уставшее", "Нужен отдых и перезагрузка"),
    MoodPreset("🤔", "Задумчивое", "В размышлениях о будущем"),
    MoodPreset("🥳", "Весёлое", "Готов к общению и приключениям"),
    MoodPreset("❤️", "Влюблённое", "Теплота и любовь в сердце"),
    MoodPreset("🌧️", "Грустное", "Небольшая хандра, нужна поддержка"),
    MoodPreset("⚡", "Заряженное", "Максимум мотивации!")
)

val REACTION_PRESETS = listOf(
    "❤️" to "Обнимаю",
    "☕" to "Угощаю кофе",
    "👏" to "Горжусь тобой!",
    "🔥" to "Ты огонь!",
    "🫂" to "Я рядом",
    "✨" to "Лучи добра"
)

class MoodViewModel(
    private val repository: MoodRepository,
    val currentUserId: String
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // 1. Live observation of authenticated user's profile
    val userProfile: StateFlow<UserProfile?> = repository.observeProfile(currentUserId)
        .catch { error ->
            Log.e(TAG, "Error observing profile", error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    // 2. Live observation of friends list
    private val friendsFlow = repository.observeFriends(currentUserId)
        .catch { error ->
            Log.e(TAG, "Error observing friends", error)
            emit(emptyList())
        }

    // 3. Combined flow: friends + their live moods
    @OptIn(ExperimentalCoroutinesApi::class)
    val friendsWithMood: StateFlow<List<FriendWithMood>> = friendsFlow
        .flatMapLatest { friends ->
            if (friends.isEmpty()) {
                flowOf(emptyList())
            } else {
                val profileFlows = friends.map { friend ->
                    repository.observeProfile(friend.friendUserId)
                        .catch { emit(null) }
                }
                combine(profileFlows) { profiles ->
                    friends.mapIndexed { index, friend ->
                        FriendWithMood(
                            friend = friend,
                            moodProfile = profiles.getOrNull(index)
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // 4. Live observation of received reactions / cheers
    val reactions: StateFlow<List<Reaction>> = repository.observeReactions(currentUserId)
        .catch { error ->
            Log.e(TAG, "Error observing reactions", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        // Initialize profile document if it doesn't exist
        viewModelScope.launch {
            val result = repository.getProfile(currentUserId)
            if (result.isSuccess && result.getOrNull() == null) {
                repository.createOrUpdateProfile(
                    UserProfile(
                        userId = currentUserId,
                        displayName = "Я",
                        currentMood = "Хорошее",
                        moodEmoji = "😊",
                        moodNote = ""
                    )
                )
            }
        }
    }

    fun syncUserGoogleProfile(displayName: String?) {
        viewModelScope.launch {
            val name = displayName?.ifBlank { "Пользователь" } ?: "Пользователь"
            val current = userProfile.value
            if (current == null || current.displayName.isBlank() || current.displayName == "Я") {
                repository.createOrUpdateProfile(
                    UserProfile(
                        userId = currentUserId,
                        displayName = name,
                        currentMood = current?.currentMood ?: "Хорошее",
                        moodEmoji = current?.moodEmoji ?: "😊",
                        moodNote = current?.moodNote ?: ""
                    )
                )
            }
        }
    }

    fun updateMood(emoji: String, moodName: String, note: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateMood(
                currentMood = moodName.ifBlank { "Хорошее" },
                moodEmoji = emoji.ifBlank { "😊" },
                moodNote = note.trim()
            )
            _isBusy.value = false
            if (result.isSuccess) {
                _snackbarMessage.emit("Настроение успешно обновлено! $emoji")
            } else {
                _snackbarMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "Не удалось обновить настроение")
            }
        }
    }

    fun addFriend(friendId: String, onComplete: (Boolean) -> Unit) {
        val trimmed = friendId.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _snackbarMessage.emit("Пожалуйста, введите ID друга") }
            return
        }
        if (trimmed == currentUserId) {
            viewModelScope.launch { _snackbarMessage.emit("Вы не можете добавить свой собственный ID") }
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addFriend(trimmed)
            _isBusy.value = false
            if (result.isSuccess) {
                val friend = result.getOrThrow()
                _snackbarMessage.emit("Друг «${friend.displayName}» успешно добавлен!")
                onComplete(true)
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Пользователь не найден"
                _snackbarMessage.emit(err)
                onComplete(false)
            }
        }
    }

    fun removeFriend(friendUserId: String) {
        viewModelScope.launch {
            val result = repository.removeFriend(friendUserId)
            if (result.isSuccess) {
                _snackbarMessage.emit("Друг удалён из списка")
            } else {
                _snackbarMessage.emit("Не удалось удалить друга")
            }
        }
    }

    fun sendReaction(targetUserId: String, emoji: String, message: String) {
        viewModelScope.launch {
            val result = repository.sendReaction(targetUserId, emoji, message)
            if (result.isSuccess) {
                _snackbarMessage.emit("Реакция $emoji отправлена другу!")
            } else {
                _snackbarMessage.emit("Не удалось отправить реакцию")
            }
        }
    }

    fun deleteReaction(reactionId: String) {
        viewModelScope.launch {
            repository.deleteReaction(reactionId)
        }
    }
}
