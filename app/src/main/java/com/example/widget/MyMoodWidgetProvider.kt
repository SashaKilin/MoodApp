package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "MyMoodWidget"
const val ACTION_SET_MOOD = "com.example.widget.ACTION_SET_MOOD"
const val EXTRA_EMOJI = "extra_emoji"
const val EXTRA_TITLE = "extra_title"
const val EXTRA_NOTE = "extra_note"

class MyMoodWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SET_MOOD) {
            val emoji = intent.getStringExtra(EXTRA_EMOJI) ?: "😊"
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "Хорошее"
            val note = intent.getStringExtra(EXTRA_NOTE) ?: ""

            val auth = Firebase.auth
            val currentUser = auth.currentUser
            if (currentUser != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dbId = context.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        val updates = mapOf(
                            "currentMood" to title,
                            "moodEmoji" to emoji,
                            "moodNote" to note,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("users").document(currentUser.uid).update(updates).await()
                        updateAllWidgets(context)
                        FriendsMoodWidgetProvider.updateFriendsWidgets(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update mood from widget", e)
                    }
                }
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MyMoodWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_my_mood)

            // Tap root opens app
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_my_mood_root, openAppPendingIntent)

            // Setup Quick Buttons
            setupMoodButton(context, views, R.id.btn_widget_mood_happy, "😊", "Радостное", "Отличное настроение!")
            setupMoodButton(context, views, R.id.btn_widget_mood_fire, "🔥", "Продуктивное", "В потоке фокуса!")
            setupMoodButton(context, views, R.id.btn_widget_mood_coffee, "☕", "Спокойное", "Пью кофе, отдыхаю")
            setupMoodButton(context, views, R.id.btn_widget_mood_party, "🎉", "Праздничное", "Время праздновать!")
            setupMoodButton(context, views, R.id.btn_widget_mood_sleep, "😴", "Уставшее", "Нужен отдых и сон")

            // Load latest profile from Firestore asynchronously
            val user = Firebase.auth.currentUser
            if (user != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dbId = context.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        val snapshot = db.collection("users").document(user.uid).get().await()
                        if (snapshot.exists()) {
                            val emoji = snapshot.getString("moodEmoji") ?: "😊"
                            val title = snapshot.getString("currentMood") ?: "Хорошее"
                            val note = snapshot.getString("moodNote") ?: ""
                            val timestamp = snapshot.getTimestamp("updatedAt") ?: snapshot.getTimestamp("createdAt")

                            views.setTextViewText(R.id.tv_widget_my_emoji, emoji)
                            views.setTextViewText(R.id.tv_widget_my_title, title)
                            views.setTextViewText(
                                R.id.tv_widget_my_note,
                                if (note.isNotBlank()) "«$note»" else "Быстрая смена в 1 нажатие"
                            )
                            views.setTextViewText(
                                R.id.tv_widget_my_time,
                                if (timestamp != null) SimpleDateFormat("HH:mm", Locale("ru")).format(timestamp.toDate()) else "только что"
                            )
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching profile for widget", e)
                    }
                }
            } else {
                views.setTextViewText(R.id.tv_widget_my_title, "Войдите в MoodSync")
                views.setTextViewText(R.id.tv_widget_my_note, "Нажмите, чтобы открыть приложение")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setupMoodButton(
            context: Context,
            views: RemoteViews,
            buttonId: Int,
            emoji: String,
            title: String,
            note: String
        ) {
            val intent = Intent(context, MyMoodWidgetProvider::class.java).apply {
                action = ACTION_SET_MOOD
                putExtra(EXTRA_EMOJI, emoji)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_NOTE, note)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                buttonId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(buttonId, pendingIntent)
        }
    }
}
