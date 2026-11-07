package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ItemStatus
import kotlinx.coroutines.flow.first
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_ID = "vencimentos_channel"
    private const val NOTIFICATION_ID = 8001
    private const val ALARM_REQUEST_CODE = 8002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Lembretes de Vencimento"
            val descriptionText = "Notificações diárias às 08:00 sobre contas e recebimentos do dia ou vencidos"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminder(context: Context) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DAILY_CHECK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (_: Exception) {
            // Inexact repeating handles scheduling smoothly
        }
    }

    suspend fun checkAndShowDueNotification(context: Context, isTest: Boolean = false) {
        createNotificationChannel(context)
        val db = AppDatabase.getInstance(context)
        val loans = db.loanDao().getAllLoans().first()
        val receivables = db.receivableDao().getAllReceivables().first()
        val payables = db.payableDao().getAllPayables().first()

        val startOfToday = DateUtils.getStartOfDay()
        val endOfToday = DateUtils.getEndOfDay()

        var dueTodayCount = 0
        var overdueCount = 0

        loans.forEach { item ->
            if (item.status != ItemStatus.PAID && !item.hasNoDueDate && item.dueDate != null) {
                if (item.dueDate < startOfToday) overdueCount++
                else if (item.dueDate <= endOfToday) dueTodayCount++
            }
        }

        receivables.forEach { item ->
            if (item.status != ItemStatus.PAID && !item.hasNoDueDate && item.dueDate != null) {
                if (item.dueDate < startOfToday) overdueCount++
                else if (item.dueDate <= endOfToday) dueTodayCount++
            }
        }

        payables.forEach { item ->
            if (item.status != ItemStatus.PAID && !item.hasNoDueDate && item.dueDate != null) {
                if (item.dueDate < startOfToday) overdueCount++
                else if (item.dueDate <= endOfToday) dueTodayCount++
            }
        }

        if (dueTodayCount > 0 || overdueCount > 0 || isTest) {
            val contentTitle = if (isTest && dueTodayCount == 0 && overdueCount == 0) {
                "Lembrete Diário (08:00) - Teste"
            } else {
                "Lembrete de Vencimentos - Controle Financeiro"
            }

            val contentText = when {
                dueTodayCount > 0 && overdueCount > 0 ->
                    "Atenção: $dueTodayCount lançamento(s) vencem hoje e $overdueCount em atraso!"
                dueTodayCount > 0 ->
                    "Você tem $dueTodayCount lançamento(s) vencendo hoje!"
                overdueCount > 0 ->
                    "Você tem $overdueCount lançamento(s) em atraso precisando de atenção."
                else ->
                    "Tudo em dia! Notificação programada diariamente para as 08:00."
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
            } catch (_: SecurityException) {
                // If notification permission not granted
            }
        }
    }

    fun checkAndNotifyAssistedBackup(context: Context, isTest: Boolean = false) {
        createNotificationChannel(context)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            8003,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isTest) "Lembrete de Backup (Teste)" else "Lembrete de Segurança: Backup"
        val message = "Mantenha seus dados financeiros seguros exportando uma cópia criptografada em Configurações."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(8003, builder.build())
        } catch (_: SecurityException) {
            // If notification permission not granted
        }
    }
}
