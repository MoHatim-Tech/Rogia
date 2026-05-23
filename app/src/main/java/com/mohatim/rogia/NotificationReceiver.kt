package com.mohatim.rogia

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Random

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Show notification
        showDhikrNotification(context)

        // Reschedule in 10 minutes (10 * 60 * 1000 ms)
        scheduleNextNotification(context)
    }

    private fun showDhikrNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "dhikr_notifications"

        // Create Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "أذكار وتنبيهات الصلاة على النبي",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات تلقائية مريحة كل 10 دقائق للتذكير بالذكر والصلاة على النبي ﷺ"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // List of peaceful remembrances
        val remembrances = listOf(
            "صلّوا على من ينادي يوم القيامة أمتي أمتي.. اللهم صلِ وسلم على نبينا محمد ﷺ",
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            "لا حَوْلَ وَلا قُوَّةَ إِلا باللَّهِ العلي العظيم",
            "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير",
            "استغفر الله العظيم الذي لا إله إلا هو الحي القيوم وأتوب إليه",
            "اللهم إنك عفو كريم تحب العفو فاعف عني",
            "اللهم صلِ وسلم وزد وبارك على نبينا وحبيبنا محمد ﷺ",
            "لا إله إلا أنت سبحانك إني كنت من الظالمين"
        )

        val randomText = remembrances[Random().nextInt(remembrances.size)]

        // Intent to open app when clicking notification
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System default fallback safe icon
            .setContentTitle("تذكير إيماني بالذكر")
            .setContentText(randomText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(randomText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }

    companion object {
        fun scheduleNextNotification(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                202,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val interval = 10 * 60 * 1000L // 10 minutes in milliseconds
            val triggerTime = System.currentTimeMillis() + interval

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun cancelNotifications(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                202,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarmManager.cancel(pendingIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
