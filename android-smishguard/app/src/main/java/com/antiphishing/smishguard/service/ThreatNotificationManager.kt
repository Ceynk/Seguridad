package com.antiphishing.smishguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.antiphishing.smishguard.R
import com.antiphishing.smishguard.domain.model.AnalysisResult
import com.antiphishing.smishguard.domain.model.CallAction
import com.antiphishing.smishguard.domain.model.CallAnalysisResult
import com.antiphishing.smishguard.ui.MainActivity

object ThreatNotificationManager {

    private const val CHANNEL_THREATS = "smishguard_threats_channel"
    private const val CHANNEL_CALL_THREATS = "smishguard_calls_channel"
    private const val NOTIFICATION_ID_BASE = 1000
    private const val CALL_NOTIFICATION_ID_BASE = 2000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val smsChannelName = context.getString(R.string.channel_threats_name)
            val smsChannelDesc = context.getString(R.string.channel_threats_desc)
            val smsChannel = NotificationChannel(CHANNEL_THREATS, smsChannelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = smsChannelDesc
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(smsChannel)

            val callChannelName = "Alertas de Llamadas Maliciosas"
            val callChannelDesc = "Notificaciones de llamadas fraudulentas, Wangiri o spam bloqueadas por SmishGuard"
            val callChannel = NotificationChannel(CHANNEL_CALL_THREATS, callChannelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = callChannelDesc
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(callChannel)
        }
    }

    fun showThreatAlert(context: Context, sender: String, analysis: AnalysisResult) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "quarantine")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val factorSummary = analysis.factors.take(2).joinToString("\n") { "- ${it.category}" }
        val contentText = "Riesgo ${analysis.threatScore}%: SMS interceptado y puesto en cuarentena."

        val builder = NotificationCompat.Builder(context, CHANNEL_THREATS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("SmishGuard: Amenaza Neutralizada ($sender)")
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$contentText\n\nMotivos detectados:\n$factorSummary")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notificationId, builder.build())
    }

    fun showCallThreatAlert(context: Context, phoneNumber: String, analysis: CallAnalysisResult) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "dashboard")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isBlocked = analysis.action == CallAction.BLOCK
        val title = if (isBlocked) {
            "SmishGuard: Llamada Maliciosa Bloqueada"
        } else {
            "SmishGuard: Alerta de Llamada Sospechosa"
        }

        val factorSummary = analysis.factors.joinToString("\n") { "- ${it.category}" }
        val originInfo = analysis.countryOrOrigin?.let { "Origen: $it\n" } ?: ""
        val contentText = "Número $phoneNumber | Riesgo ${analysis.threatScore}%"

        val builder = NotificationCompat.Builder(context, CHANNEL_CALL_THREATS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$contentText\n${originInfo}Estado: ${if (isBlocked) "Bloqueada y rechazada" else "Alerta preventiva"}\n\nFactores:\n$factorSummary")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = CALL_NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notificationId, builder.build())
    }
}
