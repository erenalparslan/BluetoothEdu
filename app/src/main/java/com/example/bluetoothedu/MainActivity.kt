package com.example.bluetoothedu

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BlendMode
import android.icu.lang.UProperty.NAME
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetoothedu.databinding.ActivityMainBinding
import viewBinding
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bReceiver: BluetoothReceiver
    private lateinit var dReceiver: DiscoveryReceiver
    private lateinit var bManager: CustomBluetoothManager
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null



    private val binding by viewBinding(ActivityMainBinding::inflate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        dReceiver= DiscoveryReceiver(binding.receiverProgress)
        bReceiver= BluetoothReceiver()
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        bManager = CustomBluetoothManager(this,bluetoothAdapter)

        checkBluetoothPermission()

        if (checkLocationPermission()) {

        } else {
            requestLocationPermission()
        }

        if (bluetoothAdapter == null) {
            // Bluetooth cihazı desteklenmiyorsa uygulamadan çık
            Toast.makeText(this, "Bluetooth Desteklenmiyor !!", Toast.LENGTH_LONG).show()
        }

        if (bluetoothAdapter?.isEnabled == true) {
            val isBleSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

            if (isBleSupported) {
                Log.d("ScanEren", "isBleSupp : $isBleSupported")
                bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
                //startBluetoothScan()
                // Cihaz BLE özelliklerini destekliyor
                // Diğer BLE işlemlerini gerçekleştirin
            } else {
                Log.d("ScanEren", "isBleSuppElse : $isBleSupported")
                // Cihaz BLE özelliklerini desteklemiyor
            }
        } else {
            // Bluetooth kapalı, uygun hata işlemlerini gerçekleştirin
        }
        binding.changeStatus.setOnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                //Kapalı ise açıyor ve receiver a ekliyor
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(intent)

            }else{
                bManager.closeBluetoothSocket()
                bManager.closeBluetoothSocket()
                bluetoothAdapter.disable()
            }
        }

        binding.startBReceiver.setOnClickListener {

            startBluetoothScan()
        }

        binding.startDReceiver.setOnClickListener {
            binding.receiverProgress.visibility=View.VISIBLE
            bManager.startDReceiver(dReceiver)
        }

        
        binding.setDiscoverable.setOnClickListener {
            bManager.setDiscoverable()
        }

     
        binding.getPairedDevice.setOnClickListener {
            bManager.getPairedDevices()
        }


        binding.sendMessage.setOnClickListener {
            bManager?.sendDataToServer("Hello, SERVER !!!")
        }

        binding.acceptConnection.setOnClickListener {
            Thread {
                bManager?.acceptConnection()
            }.start()

        }

        binding?.sendToClient?.setOnClickListener {
            bManager.startBeaconAdvertising()
        }
        binding.receiverMessage.setOnClickListener {
            Thread {
                bManager?.receiveData { data ->
                    onDataReceived(data)
                }
            }.start()
        }


    }



    @SuppressLint("MissingPermission")
    private fun startBluetoothScan() {
        bluetoothLeScanner?.startScan(scanCallback)
    }


    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device: BluetoothDevice = result.device
            val rssi: Int = result.rssi
            var x=calculateDistance(rssi)
            Log.d("ScanEren", "Device :${device?.name} ---->$x ")
            // Use RSSI value to estimate distance or perform other actions
            // ...
        }

        override fun onScanFailed(errorCode: Int) {
            // Handle scan failure
            Log.d("ScanEren", "onScanFailed: $errorCode ")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            gatt: BluetoothGatt?,
            status: Int,
            newState: Int
        ) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.")
                // Connection established, now you can request RSSI
                gatt?.readRemoteRssi()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "RSSI: $rssi dBm")
                // RSSI değerini kullanarak istediğiniz işlemleri gerçekleştirebilirsiniz.
            } else {
                Log.e(TAG, "RSSI read failed with status $status")
            }
        }
    }

    private fun onDataReceived(data: String) {
        runOnUiThread {
            // Alınan veriyi işle
            Log.i(TAG, "Received data: $data")
            Toast.makeText(this,"$data",Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateDistance(rssi: Int): Double {
        val txPower = -59.0 // Ölçüm yapılan yerdeki sabit bir değer
        val ratio = rssi * 1.0 / txPower
        if (ratio < 1.0) {
            return Math.pow(ratio, 10.0)
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111
        }
    }

    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }


    private fun checkLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkBluetoothPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED-> {
                // Bluetooth izinleri zaten verilmiş
                Log.d("Eren", "Bluetooth izinleri zaten var.")
            }

            shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH)
                    || shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_ADMIN)|| shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)-> {
                // İzin reddedildi ancak kullanıcıdan gerekçe istenebilir
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Log.d("Eren", "Bluetooth izinlerini iste.")
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION

                        ),
                        101
                    )
                }
            }

            else -> {
                // İzin verilmemiş, kullanıcıdan direkt olarak izin iste
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Log.d("Eren", "Bluetooth izinlerini direkt olarak iste.")
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                        ),
                        101
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(bReceiver)
        unregisterReceiver(dReceiver)
        bManager.closeBluetoothSocket()
        bManager.closeBluetoothServerSocket()
    }

    companion object {
        const val TAG = "CustomBluetoothManager"
    }
}









