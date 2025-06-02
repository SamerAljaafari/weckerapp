package gms.application.weckerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.*

/**
 * RecyclerView adapter for displaying alarms in a list
 * Handles alarm display, toggle, and deletion
 */
class AlarmAdapter(
    private var alarms: MutableList<Alarm>,
    private val onAlarmToggled: (Alarm, Boolean) -> Unit,
    private val onAlarmDeleted: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    /**
     * ViewHolder for individual alarm items
     */
    class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.timeText)
        val daysText: TextView = view.findViewById(R.id.daysText)
        val alarmSwitch: SwitchMaterial = view.findViewById(R.id.alarmSwitch)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        
        holder.timeText.text = String.format("%02d:%02d", alarm.hour, alarm.minute)
        holder.daysText.text = if (alarm.days.isEmpty()) "Einmalig" else getDaysText(alarm.days)
        holder.alarmSwitch.isChecked = alarm.isEnabled
        
        holder.alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            onAlarmToggled(alarm, isChecked)
        }
        
        holder.deleteButton.setOnClickListener {
            onAlarmDeleted(alarm)
        }
    }

    override fun getItemCount() = alarms.size

    fun updateAlarms(newAlarms: List<Alarm>) {
        alarms.clear()
        alarms.addAll(newAlarms)
        notifyDataSetChanged()
    }

    /**
     * Converts day numbers to short text (Mo, Di, etc.)
     */
    private fun getDaysText(days: Set<Int>): String {
        val dayNames = days.sorted().map { dayNumber ->
            when (dayNumber) {
                1 -> "Mo"
                2 -> "Di"
                3 -> "Mi"
                4 -> "Do"
                5 -> "Fr"
                6 -> "Sa"
                7 -> "So"
                else -> ""
            }
        }
        return dayNames.joinToString(" ")
    }
} 