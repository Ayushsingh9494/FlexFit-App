package com.example.flexfit.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.mutableIntStateOf
import java.time.LocalDate

class StepCounterManager(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val stepPreferences = appContext.getSharedPreferences("fluxfit_steps", Context.MODE_PRIVATE)

    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE)
                as SensorManager

    private val stepSensor =
        sensorManager.getDefaultSensor(
            Sensor.TYPE_STEP_COUNTER
        )

    val steps = mutableIntStateOf(0)
    val isSensorAvailable: Boolean
        get() = stepSensor != null

    private var onStepsChanged: ((Int) -> Unit)? = null

    fun setOnStepsChanged(listener: ((Int) -> Unit)?) {
        onStepsChanged = listener
    }

    fun startListening() {

        stepSensor?.let {

            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {

        event?.let {
            val today = LocalDate.now().toString()
            val rawSteps = it.values[0].toInt()
            val baselineKey = "baseline_$today"
            val baseline = stepPreferences.getInt(baselineKey, -1)
            if (baseline == -1 || rawSteps < baseline) {
                stepPreferences.edit().putInt(baselineKey, rawSteps).apply()
                steps.intValue = 0
            } else {
                steps.intValue = (rawSteps - baseline).coerceAtLeast(0)
            }
            onStepsChanged?.invoke(steps.intValue)
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {

    }
}
