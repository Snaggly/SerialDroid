package tech.snaggle.serialdroid

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.serialport.SerialPort
import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import tech.snaggle.serialdroid.databinding.ActivityMainBinding
import java.io.File
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var handler: Handler
    private lateinit var binding: ActivityMainBinding
    private lateinit var baudSpinnerAdapter: Adapter
    private lateinit var devSpinnerAdapter: Adapter
    private var readerExecutor: ExecutorService? = null
    private var serialPort: SerialPort? = null
    private var stream: InputStream? = null
    private var devPath = ""
    private var baudRate = 115200

    private val handlerCallback = Handler.Callback { msg: Message ->
        when (msg.what) {
            1 -> {
                binding.textView.text = StringBuilder(binding.textView.text).also {
                    it.append(" \t ")
                    it.append(msg.obj as String)
                }.toString()
            }

            2 -> {
                binding.baudSpinner.visibility = View.INVISIBLE
                binding.baudTV.visibility = View.INVISIBLE
            }

            3 -> {
                binding.baudSpinner.visibility = View.VISIBLE
                binding.baudTV.visibility = View.VISIBLE
            }

            else -> {
                return@Callback false
            }
        }
        return@Callback true
    }

    private val readerRun = Runnable {
        try {
            binding.textView.text = ""

            stream = try {
                serialPort = SerialPort(devPath, baudRate)
                handler.sendMessage(handler.obtainMessage(3))
                serialPort!!.inputStream
            } catch (link: UnsatisfiedLinkError) {
                serialPort = null
                handler.sendMessage(handler.obtainMessage(2))
                File(devPath).inputStream()
            }
            stream?.let { file ->
                while (!Thread.currentThread().isInterrupted) {
                    val buffer = ByteArray(128)
                    val readBytes = file.read(buffer)
                    handler.sendMessageDelayed(handler.obtainMessage().apply {
                        what = 1
                        obj = buffer.copyOf(readBytes)
                            .joinToString(",") { byte -> "%02X".format(byte) }
                    }, 500)
                }
            }
        } catch (error: Exception) {
            error.printStackTrace()
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to read Device: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler = Handler(mainLooper, handlerCallback)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        baudSpinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            arrayListOf(
                9600,
                19200,
                28800,
                38400,
                57600,
                76800,
                115200,
                230400,
                460800,
                576000,
                921600
            )
        ).also {
            binding.baudSpinner.adapter = it
            binding.baudSpinner.setSelection(6)
        }

        val devices = arrayListOf(
            "/dev/ttyS",
            "/dev/ttyHS",
            "/dev/ttyMSM",
            "/dev/ttyHSL"
        )
        val existingDevices = ArrayList<String>()
        for (device in devices) {
            for (i in 0..9) {
                val devicePath = "${device}${i}"
                File(devicePath).apply {
                    if (exists() && canRead()) {
                        existingDevices.add(devicePath)
                    }
                }
            }
        }
        if (existingDevices.isEmpty()) {
            Toast.makeText(this, "No serial devices found!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            devSpinnerAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                existingDevices
            ).also {
                binding.devSpinner.adapter = it
            }
        }

        binding.devSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val newSelection = binding.devSpinner.selectedItem as String
                if (devPath != newSelection) {
                    devPath = newSelection
                    resumeReader()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.baudSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val newSelection = binding.baudSpinner.selectedItem as Int
                if (baudRate != newSelection) {
                    baudRate = newSelection
                    resumeReader()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stopReader()
    }

    private fun resumeReader() {
        stopReader()
        if (devPath.isEmpty()) {
            return
        }
        readerExecutor = Executors.newSingleThreadExecutor().also {
            it.execute(readerRun)
        }
    }

    private fun stopReader() {
        try {
            stream?.close()
        } catch (error: Exception) {
            Toast.makeText(this, "Failed to close stream: ${error.message}", Toast.LENGTH_LONG)
                .show()
        }
        try {
            //serialPort?.close()
        } catch (error: Exception) {
            Toast.makeText(this, "Failed to close serial port: ${error.message}", Toast.LENGTH_LONG)
                .show()
        }
        try {
            readerExecutor?.shutdownNow()
            readerExecutor = null
        } catch (error: Exception) {
            Toast.makeText(this, "Failed to shutdown executor: ${error.message}", Toast.LENGTH_LONG)
                .show()
        }
    }
}