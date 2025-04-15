package com.actisync.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityRecognitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            val mostProbableActivity = result?.mostProbableActivity

            val activityType = when (mostProbableActivity?.type) {
                DetectedActivity.STILL -> "STILL"
                DetectedActivity.WALKING -> "WALKING"
                DetectedActivity.RUNNING -> "RUNNING"
                DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
                else -> "UNKNOWN"
            }

            if (mostProbableActivity != null) {
                Log.d("ActivityReceiver", "User is: $activityType (Confidence: ${mostProbableActivity.confidence})")

                // Send local broadcast to MainActivity
                val updateIntent = Intent("com.actisync.app.ACTIVITY_UPDATE").setPackage(context.packageName)
                updateIntent.putExtra("activity", activityType)
                context.sendBroadcast(updateIntent)
            }
        }
    }
}
