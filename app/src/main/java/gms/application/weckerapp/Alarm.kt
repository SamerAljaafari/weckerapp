package gms.application.weckerapp

/**
 * Data class representing an alarm
 * id: unique identifier
 * hour: hour of the alarm (24h format)
 * minute: minute of the alarm
 * isEnabled: whether the alarm is active
 * days: set of days for recurring alarms (1=Monday, 7=Sunday)
 */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val days: Set<Int> = emptySet() // 1 = Monday, 7 = Sunday
) 