package gms.application.weckerapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager

class AlarmService : Service() {

    private val mediaPlayers = mutableMapOf<Int, MediaPlayer>()
    private var wakeLock: PowerManager.WakeLock? = null
    private val CHANNEL_ID = "AlarmChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", 0) ?: 0
        startForeground(alarmId, createNotification(alarmId))
        startAlarmSound(alarmId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(alarmId: Int): Notification {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm klingelt")
            .setContentText("Tippen zum Öffnen")
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmService::WakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutes
        }
    }

    private fun startAlarmSound(alarmId: Int) {
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayers[alarmId] = MediaPlayer.create(this, alarmSound).apply {
            isLooping = true
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllAlarms()
        wakeLock?.release()
    }

    private fun stopAllAlarms() {
        mediaPlayers.values.forEach { mediaPlayer ->
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        mediaPlayers.clear()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopAllAlarms()
        stopSelf()
    }

    override fun stopService(name: Intent?): Boolean {
        val alarmId = name?.getIntExtra("ALARM_ID", 0) ?: 0
        mediaPlayers[alarmId]?.let { mediaPlayer ->
            mediaPlayer.stop()
            mediaPlayer.release()
            mediaPlayers.remove(alarmId)
        }
        
        if (mediaPlayers.isEmpty()) {
            stopSelf()
        }
        
        return super.stopService(name)
    }
}