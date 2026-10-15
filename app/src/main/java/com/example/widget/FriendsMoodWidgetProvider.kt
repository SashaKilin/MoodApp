package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "FriendsMoodWidget"

class FriendsMoodWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateFriendsWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FriendsMoodWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_friends_mood)

            // Tap root opens app
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 100, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_friends_root, openAppPendingIntent)

            val user = Firebase.auth.currentUser
            if (user != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dbId = context.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)

                        // Fetch user's friends subcollection
                        val friendsSnapshot = db.collection("users").document(user.uid)
                            .collection("friends").limit(3).get().await()

                        if (friendsSnapshot.isEmpty) {
                            views.setViewVisibility(R.id.slot_friend_1, View.GONE)
                            views.setViewVisibility(R.id.slot_friend_2, View.GONE)
                            views.setViewVisibility(R.id.slot_friend_3, View.GONE)
                            views.setViewVisibility(R.id.tv_friends_empty, View.VISIBLE)
                            views.setTextViewText(R.id.tv_friends_empty, "Добавьте друзей в приложении MoodSync!")
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                            return@launch
                        }

                        views.setViewVisibility(R.id.tv_friends_empty, View.GONE)

                        val slotIds = listOf(
                            Triple(R.id.slot_friend_1, R.id.tv_friend_1_emoji, Pair(R.id.tv_friend_1_name, R.id.tv_friend_1_mood)),
                            Triple(R.id.slot_friend_2, R.id.tv_friend_2_emoji, Pair(R.id.tv_friend_2_name, R.id.tv_friend_2_mood)),
                            Triple(R.id.slot_friend_3, R.id.tv_friend_3_emoji, Pair(R.id.tv_friend_3_name, R.id.tv_friend_3_mood))
                        )
                        val timeIds = listOf(R.id.tv_friend_1_time, R.id.tv_friend_2_time, R.id.tv_friend_3_time)

                        val docs = friendsSnapshot.documents
                        for (i in 0 until 3) {
                            val slot = slotIds[i]
                            val timeId = timeIds[i]

                            if (i < docs.size) {
                                val friendDoc = docs[i]
                                val friendUid = friendDoc.getString("friendUserId") ?: friendDoc.id
                                val friendDefaultName = friendDoc.getString("displayName") ?: "Друг"

                                // Fetch friend's public profile mood
                                val friendProfileSnap = db.collection("users").document(friendUid).get().await()
                                val emoji = friendProfileSnap.getString("moodEmoji") ?: "😊"
                                val moodTitle = friendProfileSnap.getString("currentMood") ?: "Хорошее"
                                val displayName = friendProfileSnap.getString("displayName")?.ifBlank { friendDefaultName } ?: friendDefaultName
                                val timestamp = friendProfileSnap.getTimestamp("updatedAt") ?: friendProfileSnap.getTimestamp("createdAt")

                                views.setViewVisibility(slot.first, View.VISIBLE)
                                views.setTextViewText(slot.second, emoji)
                                views.setTextViewText(slot.third.first, displayName)
                                views.setTextViewText(slot.third.second, moodTitle)

                                val timeStr = if (timestamp != null) {
                                    val diffMin = (System.currentTimeMillis() - timestamp.toDate().time) / 60000
                                    when {
                                        diffMin < 60 -> "${diffMin}м"
                                        diffMin < 1440 -> "${diffMin / 60}ч"
                                        else -> SimpleDateFormat("d MMM", Locale("ru")).format(timestamp.toDate())
                                    }
                                } else "сейчас"
                                views.setTextViewText(timeId, timeStr)
                            } else {
                                views.setViewVisibility(slot.first, View.GONE)
                            }
                        }

                        views.setTextViewText(R.id.tv_widget_friends_count, "${docs.size} в сети")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching friends for widget", e)
                    }
                }
            } else {
                views.setViewVisibility(R.id.slot_friend_1, View.GONE)
                views.setViewVisibility(R.id.slot_friend_2, View.GONE)
                views.setViewVisibility(R.id.slot_friend_3, View.GONE)
                views.setViewVisibility(R.id.tv_friends_empty, View.VISIBLE)
                views.setTextViewText(R.id.tv_friends_empty, "Войдите в MoodSync")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
