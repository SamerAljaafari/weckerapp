package gms.application.weckerapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import java.util.*

/**
 * Activity that shows when an alarm triggers
 * Handles alarm display and snooze functionality
 */
class AlarmActivity : AppCompatActivity() {

    private lateinit var snoozeButton: Button
    private lateinit var stopButton: Button
    private var alarmId: Int = 0
    private val CHANNEL_ID = "AlarmChannel"
    private lateinit var alarmManager: AlarmManager
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        alarmId = intent.getIntExtra("ALARM_ID", 0)

        setupWindow()
        initComponents()
        playAlarmSound()
    }

    /**
     * Sets up the window to show over lock screen
     */
    private fun setupWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    /**
     * Initializes UI components and button listeners
     */
    private fun initComponents() {
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        snoozeButton = findViewById(R.id.snoozeButton)
        stopButton = findViewById(R.id.stopButton)

        setupListeners()
    }

    /**
     * Plays the alarm sound using MediaPlayer
     */
    private fun playAlarmSound() {
        // Implement alarm sound playback
    }

    private fun setupListeners() {
        snoozeButton.setOnClickListener {
            snoozeAlarm()
        }

        stopButton.setOnClickListener {
            stopAlarm()
        }
    }

    /**
     * Stops the alarm and closes the activity
     */
    private fun stopAlarm() {
        // Stop alarm service
        stopService(Intent(this, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        })

        finish()
    }

    /**
     * Snoozes the alarm for a set duration
     */
    private fun snoozeAlarm() {
        val snoozeMinutes = sharedPreferences.getString("snooze_time", "5")?.toIntOrNull() ?: 5

        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                pendingIntent
            )
        }

        // Show snooze notification
        showSnoozeNotification(snoozeTime)

        // Stop alarm service
        stopService(Intent(this, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        })

        finish()
    }

    private fun showSnoozeNotification(snoozeTime: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = snoozeTime

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 
            alarmId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Snooze aktiviert")
            .setContentText("Nächster Alarm: ${calendar.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", calendar.get(Calendar.MINUTE))}")
            .setSmallIcon(R.drawable.ic_alarm)  // Using ic_alarm instead of ic_snooze
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alarmId, notification)
    }
}