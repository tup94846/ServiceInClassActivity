package edu.temple.myapplication

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log

class TimerService : Service() {

    private var isRunning = false
    private var paused = false
    private var remainingTime = 100

    private var timerHandler: Handler? = null
    private lateinit var t: TimerThread

    private lateinit var prefs: SharedPreferences

    inner class TimerBinder : Binder() {
        fun isRunning() = this@TimerService.isRunning
        fun isPaused() = this@TimerService.paused

        fun start(startValue: Int) {
            remainingTime = if (paused) prefs.getInt("remaining_time", startValue) else startValue
            if (!isRunning) {
                if (::t.isInitialized) t.interrupt()
                this@TimerService.start(remainingTime)
            }
        }

        fun setHandler(handler: Handler) {
            timerHandler = handler
        }

        fun stop() {
            if (::t.isInitialized) {
                t.interrupt()
                clearSavedState()
            }
        }

        fun pause() {
            if (::t.isInitialized) {
                paused = true
                isRunning = false
                saveState(remainingTime)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("TimerPrefs", MODE_PRIVATE)
        remainingTime = prefs.getInt("remaining_time", 100)
        paused = prefs.getBoolean("paused", false)
    }

    override fun onBind(intent: Intent): IBinder = TimerBinder()

    private fun start(startValue: Int) {
        t = TimerThread(startValue)
        t.start()
    }

    private fun saveState(time: Int) {
        prefs.edit().apply {
            putInt("remaining_time", time)
            putBoolean("paused", true)
            apply()
        }
    }

    private fun clearSavedState() {
        prefs.edit().clear().apply()
    }

    inner class TimerThread(private var time: Int) : Thread() {
        override fun run() {
            isRunning = true
            paused = false
            try {
                for (i in time downTo 1) {
                    if (paused) break
                    remainingTime = i
                    prefs.edit().putInt("remaining_time", i).apply()
                    timerHandler?.sendEmptyMessage(i)
                    sleep(1000)
                }
                if (!paused) {
                    isRunning = false
                    clearSavedState()
                }
            } catch (e: InterruptedException) {
                isRunning = false
                paused = false
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (::t.isInitialized) t.interrupt()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::t.isInitialized) t.interrupt()
    }
}
