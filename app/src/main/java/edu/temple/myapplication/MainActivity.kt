package edu.temple.myapplication

import android.content.ComponentName
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private var timerBinder: TimerService.TimerBinder? = null
    private lateinit var prefs: SharedPreferences

    val timeHandler = Handler(Looper.getMainLooper()){
        findViewById<TextView>(R.id.textView).text = it.what.toString()
        true
    }

    val serviceConnection = object : ServiceConnection { //service connection
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) { //service connected
            timerBinder = service as TimerService.TimerBinder
            timerBinder?.setHandler(timeHandler) //sets handler
        }

        override fun onServiceDisconnected(name: ComponentName?) { //service disconnected
            timerBinder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("prefs", MODE_PRIVATE) //shared preferences
        bindService(Intent(this, TimerService::class.java), serviceConnection, BIND_AUTO_CREATE) //binds service

        findViewById<Button>(R.id.startButton).setOnClickListener {//start button
            val defaultTime = 100
            val time = if (prefs.getBoolean("paused", false)) prefs.getInt("remaining_time", defaultTime) else defaultTime
            timerBinder?.start(time)
        }
        findViewById<Button>(R.id.stopButton).setOnClickListener { //stop button
            timerBinder?.stop() //stops timer
        }
    }
    override fun onDestroy() { //destroys activity
        super.onDestroy()
        unbindService(serviceConnection)
    }
}
