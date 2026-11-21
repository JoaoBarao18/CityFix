package pt.isec.amov.a2024136885.cityfix.domain.sensor


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

internal class SensorImpactoManager(
    context: Context,
    private val onImpacto:()-> Unit
): SensorEventListener{
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val acelometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var estavel = false

    fun start() {
        val sensor = acelometro ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop(){
        sensorManager.unregisterListener(this)
        estavel = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val eixoZ = event.values[2]
        val outintervalo = eixoZ > 14f || eixoZ < 6f
        val novoImpacto = estavel && outintervalo
        estavel = !outintervalo

        if (novoImpacto) {
            onImpacto()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}