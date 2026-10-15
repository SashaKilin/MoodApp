package com.example.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FriendWithMood
import com.example.data.model.UserProfile
import com.example.widget.FriendsMoodWidgetProvider
import com.example.widget.MyMoodWidgetProvider

@Composable
fun WidgetsHubScreen(
    userProfile: UserProfile?,
    friends: List<FriendWithMood>,
    onQuickChangeMood: (emoji: String, name: String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Banner
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Виджеты на рабочий стол",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Следите за настроением без открытия приложения",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Widget 1: My Mood Widget
        Text(
            text = "Виджет 1: Моё настроение",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Отображает ваш статус и позволяет менять настроение в 1 клик прямо с домашнего экрана",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Live Preview of My Mood Widget
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("widget_my_mood_preview"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MoodSync",
                            color = Color(0xFF818CF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " • Моё настроение",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "Виджет 3×2",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = userProfile?.moodEmoji ?: "😊",
                        fontSize = 42.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = userProfile?.currentMood ?: "Хорошее",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (!userProfile?.moodNote.isNullOrBlank()) "«${userProfile?.moodNote}»" else "Быстрая смена в 1 нажатие",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick buttons bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val presets = listOf(
                        Triple("😊", "Радостное", "happy"),
                        Triple("🔥", "Продуктивное", "fire"),
                        Triple("☕", "Спокойное", "coffee"),
                        Triple("🎉", "Праздничное", "party"),
                        Triple("😴", "Уставшее", "sleep")
                    )

                    presets.forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable {
                                    onQuickChangeMood(preset.first, preset.second)
                                    onShowSnackbar("Настроение изменено на ${preset.second} ${preset.first}")
                                },
                            shape = CircleShape,
                            color = Color(0xFF334155)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = preset.first, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pin Button for Widget 1
        PinWidgetButton(
            context = context,
            providerClass = MyMoodWidgetProvider::class.java,
            buttonText = "Добавить виджет настроения на экран",
            onShowSnackbar = onShowSnackbar
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Widget 2: Friends Mood Widget
        Text(
            text = "Виджет 2: Настроения друзей",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Показывает статусы ваших близких прямо на экране вашего телефона",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Live Preview of Friends Widget
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("widget_friends_mood_preview"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MoodSync",
                            color = Color(0xFF818CF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " • Друзья",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "${friends.size} в сети",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (friends.isEmpty()) {
                    Text(
                        text = "Добавьте друзей во вкладке «Друзья», чтобы их статусы отображались здесь!",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    friends.take(3).forEach { item ->
                        val mood = item.moodProfile
                        val name = mood?.displayName ?: item.friend.displayName

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mood?.moodEmoji ?: "💬",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = mood?.currentMood ?: "Хорошее",
                                    color = Color(0xFF818CF8),
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "сейчас",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pin Button for Widget 2
        PinWidgetButton(
            context = context,
            providerClass = FriendsMoodWidgetProvider::class.java,
            buttonText = "Добавить виджет друзей на экран",
            onShowSnackbar = onShowSnackbar
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Instructions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Как добавить виджет вручную:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                InstructionStep(1, "Перейдите на главный экран телефона")
                InstructionStep(2, "Зажмите пальцем пустое место на экране на 2 секунды")
                InstructionStep(3, "В появившемся меню выберите «Виджеты» (Widgets)")
                InstructionStep(4, "Найдите «MoodSync» в списке и перетащите виджет на экран")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun PinWidgetButton(
    context: Context,
    providerClass: Class<*>,
    buttonText: String,
    onShowSnackbar: (String) -> Unit
) {
    Button(
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, providerClass)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    appWidgetManager.requestPinAppWidget(componentName, null, null)
                    onShowSnackbar("Запрос на добавление виджета отправлен на экран")
                } else {
                    onShowSnackbar("Зажмите пустое место на рабочем столе и выберите Виджеты -> MoodSync")
                }
            } else {
                onShowSnackbar("Зажмите пустое место на рабочем столе и выберите Виджеты -> MoodSync")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(imageVector = Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(buttonText)
    }
}

@Composable
private fun InstructionStep(stepNumber: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
