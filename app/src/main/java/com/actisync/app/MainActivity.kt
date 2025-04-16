package com.actisync.app
//testing

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.actisync.app.databinding.ActivityMainBinding
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var activityRecognitionClient: ActivityRecognitionClient

    private val activityUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val activity = intent?.getStringExtra("activity") ?: return
            updateActivityUI(activity)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userName = "Anurag"
        val currentTime = SimpleDateFormat("EEEE, MMM dd yyyy - hh:mm a", Locale.getDefault()).format(Date())
        binding.textGreeting.text = "Hello $userName!\nToday is $currentTime"

        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 101)
        } else {
            startActivityRecognition()
        }
    }

    private fun startActivityRecognition() {
        activityRecognitionClient = ActivityRecognition.getClient(this)
        val intent = Intent(this, ActivityRecognitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        activityRecognitionClient.requestActivityUpdates(2000L, pendingIntent)
            .addOnSuccessListener {
                Log.d("MainActivity", "Activity updates registered.")
            }
            .addOnFailureListener {
                Log.e("MainActivity", "Failed to register activity updates", it)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun updateActivityUI(activity: String) {
        binding.textActivity.text = "Current activity: $activity"

        val imageRes = when (activity) {
            "WALKING" -> R.drawable.ic_walking
            "RUNNING" -> R.drawable.ic_running
            "IN_VEHICLE" -> R.drawable.ic_vehicle
            "STILL" -> R.drawable.ic_still
            else -> R.drawable.ic_unknown
        }

        binding.imageActivity.setImageResource(imageRes)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permissions", "ACTIVITY_RECOGNITION permission granted")
            startActivityRecognition()
        } else {
            Log.w("Permissions", "ACTIVITY_RECOGNITION permission denied")
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.actisync.app.ACTIVITY_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(activityUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(activityUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(activityUpdateReceiver)
    }
}
