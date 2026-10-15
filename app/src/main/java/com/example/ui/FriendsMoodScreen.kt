package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FriendWithMood
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FriendsMoodScreen(
    friends: List<FriendWithMood>,
    accountUid: String,
    onAddFriendClicked: () -> Unit,
    onSendReaction: (friendUid: String, emoji: String, message: String) -> Unit,
    onRemoveFriend: (friendUid: String) -> Unit,
    onCopyMyId: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("Все") }
    var selectedFriendForDetail by remember { mutableStateOf<FriendWithMood?>(null) }
    var friendToRemove by remember { mutableStateOf<FriendWithMood?>(null) }

    // Filter logic
    val filteredFriends by remember(friends, searchQuery, selectedFilterCategory) {
        derivedStateOf {
            friends.filter { item ->
                val name = item.moodProfile?.displayName ?: item.friend.displayName
                val mood = item.moodProfile?.currentMood ?: ""
                val note = item.moodProfile?.moodNote ?: ""
                val matchesSearch = searchQuery.isBlank() ||
                        name.contains(searchQuery, ignoreCase = true) ||
                        mood.contains(searchQuery, ignoreCase = true) ||
                        note.contains(searchQuery, ignoreCase = true) ||
                        item.friend.friendUserId.contains(searchQuery, ignoreCase = true)

                val matchesCategory = when (selectedFilterCategory) {
                    "Все" -> true
                    "Радость" -> item.moodProfile?.moodEmoji in listOf("😊", "🎉", "🥳", "✨")
                    "Фокус" -> item.moodProfile?.moodEmoji in listOf("🔥", "⚡", "💪")
                    "Релакс" -> item.moodProfile?.moodEmoji in listOf("☕", "🏖️", "🍕")
                    "Поддержка" -> item.moodProfile?.moodEmoji in listOf("🌧️", "😴", "🤔")
                    else -> true
                }

                matchesSearch && matchesCategory
            }
        }
    }

    // Top mood summary stats
    val dominantMood = remember(friends) {
        friends.mapNotNull { it.moodProfile?.currentMood }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }?.key
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Друзья и настроения",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Лента статусов близких в реальном времени",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAddFriendClicked,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("add_friend_header_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Добавить", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("friends_search_input"),
            placeholder = { Text("Поиск по имени, статусу или заметке...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Очистить"
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        val categories = listOf("Все", "Радость", "Фокус", "Релакс", "Поддержка")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = selectedFilterCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilterCategory = cat },
                    label = { Text(cat) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Community Mood Banner
        if (friends.isNotEmpty() && dominantMood != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "✨", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "В вашем кругу чаще всего: $dominantMood",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Friends list or Empty State
        if (friends.isEmpty()) {
            EmptyFriendsCard(
                onAddFriendClicked = onAddFriendClicked,
                onCopyMyId = onCopyMyId,
                accountUid = accountUid
            )
        } else if (filteredFriends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔍", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Друзья по запросу не найдены",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredFriends, key = { it.friend.friendUserId }) { item ->
                    FriendMoodCard(
                        friendWithMood = item,
                        onSendQuickReaction = { emoji ->
                            onSendReaction(item.friend.friendUserId, emoji, "")
                        },
                        onOpenDetail = { selectedFriendForDetail = item },
                        onRemove = { friendToRemove = item }
                    )
                }
            }
        }
    }

    // Detail Dialog
    selectedFriendForDetail?.let { friendDetail ->
        FriendDetailDialog(
            friendWithMood = friendDetail,
            onDismiss = { selectedFriendForDetail = null },
            onSendReactionWithMessage = { emoji, message ->
                onSendReaction(friendDetail.friend.friendUserId, emoji, message)
                selectedFriendForDetail = null
            }
        )
    }

    // Remove Confirmation Dialog
    friendToRemove?.let { target ->
        AlertDialog(
            onDismissRequest = { friendToRemove = null },
            title = { Text("Удалить из друзей?") },
            text = { Text("Вы перестанете видеть статус настроения «${target.friend.displayName}». Вы всегда сможете добавить друга заново по его ID.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveFriend(target.friend.friendUserId)
                        friendToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { friendToRemove = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun FriendMoodCard(
    friendWithMood: FriendWithMood,
    onSendQuickReaction: (String) -> Unit,
    onOpenDetail: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mood = friendWithMood.moodProfile
    val friendName = mood?.displayName?.ifBlank { friendWithMood.friend.displayName } ?: friendWithMood.friend.displayName
    val lastUpdated = mood?.updatedAt ?: mood?.createdAt
    val relativeTime = formatRelativeTime(lastUpdated)
    val isRecentlyActive = isRecent(lastUpdated)

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() }
            .testTag("friend_mood_card_${friendWithMood.friend.friendUserId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Avatar + Name + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Mood Emoji Badge
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 3.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = mood?.moodEmoji ?: "💬",
                                fontSize = 28.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = friendName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isRecentlyActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }
                        }

                        Text(
                            text = mood?.currentMood ?: "Настроение не указано",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Действия"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Подробнее") },
                            onClick = {
                                showMenu = false
                                onOpenDetail()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить друга", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onRemove()
                            }
                        )
                    }
                }
            }

            // Note quote bubble (if present)
            if (!mood?.moodNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "«${mood?.moodNote}»",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Last Updated Time Pill
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Обновлено: $relativeTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Cheer/Reaction Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickEmojis = listOf("❤️", "☕", "👏", "🔥", "🫂", "✨")
                    quickEmojis.forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onSendQuickReaction(emoji) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFriendsCard(
    onAddFriendClicked: () -> Unit,
    onCopyMyId: () -> Unit,
    accountUid: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🤝", fontSize = 42.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Делитесь настроением с друзьями",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Добавьте друзей по их Account ID, чтобы видеть, как они себя чувствуют прямо сейчас, и радовать их тёплыми реакциями!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // My Account ID block for easy sharing
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyMyId() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ваш Account ID для друзей:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = accountUid,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onCopyMyId) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Скопировать ID",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddFriendClicked,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_add_friend_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить друга по ID")
            }
        }
    }
}

@Composable
private fun FriendDetailDialog(
    friendWithMood: FriendWithMood,
    onDismiss: () -> Unit,
    onSendReactionWithMessage: (emoji: String, message: String) -> Unit
) {
    val context = LocalContext.current
    val mood = friendWithMood.moodProfile
    val friendName = mood?.displayName ?: friendWithMood.friend.displayName
    var selectedReactionEmoji by remember { mutableStateOf("❤️") }
    var customMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = mood?.moodEmoji ?: "💬", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = friendName, fontWeight = FontWeight.Bold)
                    Text(
                        text = mood?.currentMood ?: "Настроение не указано",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column {
                if (!mood?.moodNote.isNullOrBlank()) {
                    Text(text = "Заметка друга:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "«${mood?.moodNote}»",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                val lastUpdated = mood?.updatedAt ?: mood?.createdAt
                Text(
                    text = "Время обновления: ${formatDetailedTime(lastUpdated)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Отправить знак внимания:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val emojis = listOf("❤️", "☕", "👏", "🔥", "🫂", "✨", "🍕", "🏖️")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    emojis.forEach { emoji ->
                        val isSel = selectedReactionEmoji == emoji
                        Surface(
                            shape = CircleShape,
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { selectedReactionEmoji = emoji }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { if (it.length <= 100) customMessage = it },
                    placeholder = { Text("Тёплое сообщение (необязательно)") },
                    supportingText = { Text("${customMessage.length}/100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendReactionWithMessage(selectedReactionEmoji, customMessage) }
            ) {
                Text("Отправить $selectedReactionEmoji")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

// -------------------------------------------------------------
// Time formatting helpers
// -------------------------------------------------------------

private fun formatRelativeTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "только что"
    val date = timestamp.toDate()
    val now = Date()
    val diffSeconds = (now.time - date.time) / 1000
    return when {
        diffSeconds < 60 -> "только что"
        diffSeconds < 3600 -> "${diffSeconds / 60} мин. назад"
        diffSeconds < 86400 -> "${diffSeconds / 3600} ч. назад"
        else -> SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(date)
    }
}

private fun formatDetailedTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "только что"
    return SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("ru")).format(timestamp.toDate())
}

private fun isRecent(timestamp: Timestamp?): Boolean {
    if (timestamp == null) return false
    val diffMillis = System.currentTimeMillis() - timestamp.toDate().time
    return diffMillis < 3600_000L // within 1 hour
}
