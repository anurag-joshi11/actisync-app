package com.actisync.app

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.actisync.app.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var lastActivity: String? = null
    private var lastActivityStartTime: Long = 0L

    private val magnitudeWindow = LinkedList<Float>()
    private val windowSize = 20

    private var currentStableActivity: String? = null
    private var activityConsistencyCounter = 0
    private val stabilityThreshold = 3

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userName = "Majid"
        val currentTime = SimpleDateFormat("EEEE, MMM dd yyyy - hh:mm a", Locale.getDefault()).format(Date())
        binding.textGreeting.text = "Hello $userName!\nToday is $currentTime"

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        updateActivityUI("STILL")
        lastActivity = "STILL"
        currentStableActivity = "STILL"
        lastActivityStartTime = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val rawMagnitude = sqrt(x * x + y * y + z * z)
            val netMagnitude = abs(rawMagnitude - SensorManager.GRAVITY_EARTH)

            magnitudeWindow.add(netMagnitude)
            if (magnitudeWindow.size > windowSize) {
                magnitudeWindow.removeFirst()
            }

            val avg = magnitudeWindow.average()
            val stdDev = sqrt(magnitudeWindow.map { (it - avg) * (it - avg) }.average())

            Log.d("Sensor", "avg=%.2f, stdDev=%.2f".format(avg, stdDev))

            val predictedActivity = when {
                avg < 1.3 -> "STILL"
                avg in 1.3..4.5 && stdDev >= 0.5 -> "WALKING"
                avg in 1.3..6.5 && stdDev < 0.5 -> "IN_VEHICLE"
                avg > 6.5 && stdDev < 0.4 -> "IN_VEHICLE"
                avg > 6.5 && stdDev >= 0.4 -> "RUNNING"
                else -> "UNKNOWN"
            }

            if (predictedActivity == currentStableActivity) {
                activityConsistencyCounter++
                if (activityConsistencyCounter >= stabilityThreshold && predictedActivity != lastActivity) {
                    val duration = System.currentTimeMillis() - lastActivityStartTime
                    if (lastActivity != null && duration > 3000) {
                        showDurationToast()
                    }

                    updateActivityUI(predictedActivity)
                    lastActivity = predictedActivity
                    lastActivityStartTime = System.currentTimeMillis()
                }
            } else {
                currentStableActivity = predictedActivity
                activityConsistencyCounter = 1
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateActivityUI(activity: String) {
        binding.textActivity.text = "Current activity: $activity"

        val icon = when (activity) {
            "WALKING" -> R.drawable.ic_walking
            "RUNNING" -> R.drawable.ic_running
            "STILL" -> R.drawable.ic_still
            "IN_VEHICLE" -> R.drawable.ic_vehicle
            else -> R.drawable.ic_unknown
        }

        binding.imageActivity.setImageResource(icon)
        Log.d("Sensor", "Updated activity UI to: $activity")
    }

    private fun showDurationToast() {
        val duration = System.currentTimeMillis() - lastActivityStartTime
        val seconds = (duration / 1000) % 60
        val minutes = (duration / 1000) / 60

        val pastTense = when (lastActivity) {
            "WALKING" -> "walked"
            "RUNNING" -> "ran"
            "STILL" -> "been still"
            "IN_VEHICLE" -> "been in a vehicle"
            else -> "done something"
        }

        val message = "You have just $pastTense for $minutes min, $seconds sec"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.d("Sensor", "Toast: $message")
    }
}
