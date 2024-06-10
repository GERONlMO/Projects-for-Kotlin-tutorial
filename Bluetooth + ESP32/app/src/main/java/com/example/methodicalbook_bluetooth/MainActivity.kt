package com.example.methodicalbook_bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val deviceAddress = "XX:XX:XX:XX:XX:XX"  // Замените на MAC-адрес вашего ESP32
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val REQUEST_CODE_PERMISSIONS = 1

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSIONS)
        } else {
            setupChat()
        }
    }

    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setupChat()
            } else {
                // Handle the case where the user denies permissions
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupChat() {
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val chatView = findViewById<TextView>(R.id.chatView)

        sendButton.setOnClickListener {
            val message = messageInput.text.toString()
            sendMessage(message)
            chatView.append("\nSent: $message")
        }

        CoroutineScope(Dispatchers.IO).launch {
            setupConnection()
            receiveMessages(chatView)
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupConnection() {
        if (!checkBluetoothPermissions()) {
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSIONS)
            return
        }

        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        bluetoothSocket = device?.createRfcommSocketToServiceRecord(uuid)
        bluetoothAdapter?.cancelDiscovery()
        try {
            bluetoothSocket?.connect()
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
        } catch (e: IOException) {
            e.printStackTrace()
            try {
                bluetoothSocket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun sendMessage(message: String) {
        if (!checkBluetoothPermissions()) {
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSIONS)
            return
        }

        if (outputStream != null) {
            try {
                outputStream?.write(message.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun receiveMessages(chatView: TextView) {
        if (!checkBluetoothPermissions()) {
            withContext(Dispatchers.Main) {
                ActivityCompat.requestPermissions(this@MainActivity, requiredPermissions, REQUEST_CODE_PERMISSIONS)
            }
            return
        }

        val buffer = ByteArray(1024)
        var bytes: Int
        while (true) {
            try {
                bytes = inputStream?.read(buffer) ?: break
                val message = String(buffer, 0, bytes)
                withContext(Dispatchers.Main) {
                    chatView.append("\nReceived: $message")
                }
            } catch (e: IOException) {
                break
            }
        }
    }
}