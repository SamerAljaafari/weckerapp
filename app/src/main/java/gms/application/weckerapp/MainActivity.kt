/**
 * WeckerApp - A Modern Android Alarm Clock Application
 * 
 * Features:
 * - Multiple alarm support with individual controls
 * - Material Design UI with consistent blue theme (#156bb7)
 * - 24-hour time format
 * - Persistent alarm storage using SharedPreferences
 * - Snooze functionality with customizable duration
 * - Support for both one-time and recurring alarms
 * - Visual feedback through Toast messages
 * - Sorted alarm list by time
 * - Individual alarm enable/disable toggles
 * - Delete functionality for each alarm
 * 
 * @author Same Aljaafari
 */
package gms.application.weckerapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.util.*

class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var timePicker: TimePicker
    private lateinit var setAlarmButton: Button
    private lateinit var settingsButton: Button
    private lateinit var alarmManager: AlarmManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var alarmsList: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private val alarms = mutableListOf<Alarm>()

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
    }

    /**
     * Initializes the activity, sets up UI components, and loads saved alarms
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupListeners()

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        loadAlarms()
        checkNotificationPermission()
    }

    /**
     * Initializes all view components and sets 24-hour time format
     */
    private fun initViews() {
        timePicker = findViewById(R.id.timePicker)
        setAlarmButton = findViewById(R.id.setAlarmButton)
        settingsButton = findViewById(R.id.settingsButton)
        topAppBar = findViewById(R.id.topAppBar)

        timePicker.setIs24HourView(true)
    }

    /**
     * Sets up the RecyclerView with custom adapter for displaying alarms
     */
    private fun setupRecyclerView() {
        alarmsList = findViewById(R.id.alarmsList)
        alarmsList.layoutManager = LinearLayoutManager(this)
        alarmAdapter = AlarmAdapter(
            alarms,
            onAlarmToggled = { alarm, isEnabled ->
                toggleAlarm(alarm, isEnabled)
            },
            onAlarmDeleted = { alarm ->
                deleteAlarm(alarm)
            }
        )
        alarmsList.adapter = alarmAdapter
    }

    /**
     * Sets up click listeners for buttons
     */
    private fun setupListeners() {
        setAlarmButton.setOnClickListener {
            setAlarm()
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, PreferenceActivity::class.java))
        }
    }

    /**
     * Gets the current hour from TimePicker with backwards compatibility
     */
    private fun getTimePickerHour(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.hour
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentHour
        }
    }

    /**
     * Gets the current minute from TimePicker with backwards compatibility
     */
    private fun getTimePickerMinute(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.minute
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentMinute
        }
    }

    /**
     * Creates and schedules a new alarm based on the selected time
     */
    private fun setAlarm() {
        val hour = getTimePickerHour()
        val minute = getTimePickerMinute()
        val newAlarm = Alarm(
            id = System.currentTimeMillis().toInt(),
            hour = hour,
            minute = minute
        )
        
        scheduleAlarm(newAlarm)
        alarms.add(newAlarm)
        alarms.sortBy { it.hour * 60 + it.minute }
        alarmAdapter.notifyDataSetChanged()
        saveAlarms()
        
        Toast.makeText(this, "Alarm für ${String.format("%02d:%02d", hour, minute)} gesetzt", Toast.LENGTH_SHORT).show()
    }

    /**
     * Schedules an alarm using AlarmManager with exact timing
     * Handles different Android versions for exact alarm scheduling
     */
    private fun scheduleAlarm(alarm: Alarm) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
        }

        val currentTime = System.currentTimeMillis()
        val scheduledTime = calendar.timeInMillis

        Toast.makeText(
            this,
            "Scheduling alarm for: ${alarm.hour}:${String.format("%02d", alarm.minute)}" +
            "${if (scheduledTime <= currentTime) " (tomorrow)" else ""}",
            Toast.LENGTH_LONG
        ).show()

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * Toggles an alarm's enabled state and updates the UI
     * Shows a confirmation message when an alarm is disabled
     */
    private fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {
        val index = alarms.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            alarms[index] = updatedAlarm
            if (isEnabled) {
                scheduleAlarm(updatedAlarm)
            } else {
                cancelAlarmById(updatedAlarm.id)
                Toast.makeText(
                    this,
                    "Alarm für ${String.format("%02d:%02d", alarm.hour, alarm.minute)} deaktiviert",
                    Toast.LENGTH_SHORT
                ).show()
            }
            saveAlarms()
        }
    }

    /**
     * Deletes an alarm and removes it from the system
     */
    private fun deleteAlarm(alarm: Alarm) {
        cancelAlarmById(alarm.id)
        alarms.removeAll { it.id == alarm.id }
        alarmAdapter.notifyDataSetChanged()
        saveAlarms()
        Toast.makeText(this, "Alarm gelöscht", Toast.LENGTH_SHORT).show()
    }

    /**
     * Cancels a scheduled alarm by its ID
     */
    private fun cancelAlarmById(alarmId: Int) {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Saves all alarms to SharedPreferences in a custom format
     * Format: "id,hour,minute,isEnabled,day1,day2,...|id2,hour2,minute2,isEnabled2,..."
     */
    private fun saveAlarms() {
        val alarmsJson = alarms.joinToString("|") { alarm ->
            "${alarm.id},${alarm.hour},${alarm.minute},${alarm.isEnabled},${alarm.days.joinToString(",")}"
        }
        sharedPreferences.edit().putString("saved_alarms", alarmsJson).apply()
    }

    /**
     * Loads saved alarms from SharedPreferences and schedules enabled ones
     * Handles corrupted data gracefully
     */
    private fun loadAlarms() {
        val alarmsJson = sharedPreferences.getString("saved_alarms", "") ?: ""
        if (alarmsJson.isNotEmpty()) {
            alarms.clear()
            alarmsJson.split("|").forEach { alarmStr ->
                try {
                    val parts = alarmStr.split(",")
                    if (parts.size >= 4) {
                        val alarm = Alarm(
                            id = parts[0].trim().toInt(),
                            hour = parts[1].trim().toInt(),
                            minute = parts[2].trim().toInt(),
                            isEnabled = parts[3].trim().toBoolean(),
                            days = if (parts.size > 4) {
                                try {
                                    parts.subList(4, parts.size)
                                        .map { it.trim().toInt() }
                                        .toSet()
                                } catch (e: NumberFormatException) {
                                    emptySet()
                                }
                            } else emptySet()
                        )
                        alarms.add(alarm)
                        if (alarm.isEnabled) {
                            scheduleAlarm(alarm)
                        }
                    }
                } catch (e: Exception) {
                    // Skip malformed alarm entries
                    e.printStackTrace()
                }
            }
            alarms.sortBy { it.hour * 60 + it.minute }
            alarmAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Handles permission results for notification access
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Checks and requests notification permission for Android 13 and above
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
}