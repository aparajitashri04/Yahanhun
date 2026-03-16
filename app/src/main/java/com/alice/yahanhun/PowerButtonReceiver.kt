package com.alice.yahanhun.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

class PowerButtonReceiver(
    private val onTriplePress: () -> Unit
) : BroadcastReceiver() {

    private var pressCount = 0
    private val handler = Handler(Looper.getMainLooper())

    // Reset count if 3 presses don't happen within 1.5 seconds
    private val resetRunnable = Runnable { pressCount = 0 }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_SCREEN_OFF) return

        handler.removeCallbacks(resetRunnable)
        pressCount++

        if (pressCount >= 3) {
            pressCount = 0
            handler.removeCallbacks(resetRunnable)
            onTriplePress()
        } else {
            handler.postDelayed(resetRunnable, 1500)
        }
    }
}