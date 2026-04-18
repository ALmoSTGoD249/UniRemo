package com.soumyadeep.uniremote

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private var ir: ConsumerIrManager? = null
    private var vibrator: Vibrator? = null
    private lateinit var logView: TextView

    // --- Confirmed NEC Protocol Settings ---
    private val FREQ = 38000              // 38 kHz carrier
    private val LEADER_MARK = 9000        // 9 ms
    private val LEADER_SPACE = 4500       // 4.5 ms
    private val MARK = 560                // 0.56 ms ON
    private val SPACE_ZERO = 560          // 0 bit = 560 µs OFF
    private val SPACE_ONE = 1690          // 1 bit = 1.69 ms OFF

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ir = getSystemService(CONSUMER_IR_SERVICE) as? ConsumerIrManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        logView = findViewById(R.id.txtLog)

        if (ir == null || !ir!!.hasIrEmitter()) {
            Toast.makeText(this, "No IR emitter found on this device.", Toast.LENGTH_LONG).show()
            return
        }

        // ✅ Your working IR codes (MSB-first)
        val buttonMap = mapOf(
            R.id.btnPowerOn to 0x00F7C03F,
            R.id.btnPowerOff to 0x00F740BF,
            R.id.btnBrightnessUp to 0x00F700FF,
            R.id.btnBrightnessDown to 0x00F7807F,
            R.id.btnRed to 0x00F720DF,
            R.id.btnGreen to 0x00F7A05F,
            R.id.btnBlue to 0x00F7609F,
            R.id.btnWhite to 0x00F7E01F,
            R.id.btnOrange to 0x00F710EF,
            R.id.btnLightGreen to 0x00F7906F,
            R.id.btnLightBlue to 0x00F750AF,
            R.id.btnFlash to 0x00F7D02F,
            R.id.btnLightRed to 0x00F730CF,
            R.id.btnTeal to 0x00F7B04F,
            R.id.btnPurple to 0x00F7708F,
            R.id.btnStrobe to 0x00F7F00F,
            R.id.btnOrange2 to 0x00F708F7,
            R.id.btnCyan to 0x00F78877,
            R.id.btnViolet to 0x00F748B7,
            R.id.btnFade to 0x00F7C837,
            R.id.btnYellow to 0x00F728D7,
            R.id.btnDeepBlue to 0x00F7A857,
            R.id.btnPink to 0x00F76897,
            R.id.btnSmooth to 0x00F7E817
        )

        // Initialize all buttons
        for ((id, cmd) in buttonMap) {
            val btn = findViewById<MaterialButton?>(id)
            if (btn != null) {
                btn.setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    vibrator?.let { v ->
                        if (v.hasVibrator())
                            v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                    sendNEC32_MSB_38k(cmd)
                }
            }
        }
    }

    /**
     * Send a 32-bit NEC command (MSB-first) at 38 kHz
     */
    private fun sendNEC32_MSB_38k(data: Int) {
        try {
            val pattern = buildNEC32_MSB(data)
            ir?.transmit(FREQ, pattern)
            val msg = "Sent 0x${data.toString(16).uppercase()} (${pattern.size} pulses @38kHz)"
            log(msg)
        } catch (e: Exception) {
            e.printStackTrace()
            log("⚠️ IR error: ${e.message}")
        }
    }

    /**
     * Build NEC 32-bit pattern (MSB-first)
     */
    private fun buildNEC32_MSB(data: Int): IntArray {
        val pattern = ArrayList<Int>()
        pattern.add(LEADER_MARK)
        pattern.add(LEADER_SPACE)

        for (i in 31 downTo 0) {  // MSB-first
            pattern.add(MARK)
            pattern.add(if (((data shr i) and 1) == 1) SPACE_ONE else SPACE_ZERO)
        }

        // Stop pulse
        pattern.add(MARK)
        return pattern.toIntArray()
    }

    private fun log(msg: String) {
        Log.d("IR_DEBUG", msg)
        runOnUiThread { logView.text = msg }
    }
}
