package com.example.bluetoothedu

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.UUID

class CustomBluetoothManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) {

    private var bluetoothSocket: BluetoothSocket? = null
    private var bluetoothServerSocket: BluetoothServerSocket? = null
    private var tempSocket: BluetoothSocket? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        // Bluetooth cihazına bağlantı kurma işlemleri
        //Client cihaz server cihaza burada bağlanıyor.
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        try {
            if (bluetoothSocket==null ){
                Log.d(TAG, "connectToDevice: $device")
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                // Veri gönderme işlemi için OutputStream oluşturma
                outputStream = bluetoothSocket?.outputStream
                Log.d(TAG, "isConnect: ${bluetoothSocket?.isConnected}")
            }else{
                Log.d(TAG, "isConnect: ${bluetoothSocket?.isConnected}")
            }


        } catch (e: Exception) {
            bluetoothSocket?.close()
            bluetoothSocket=null
            Log.e(TAG, "connectToDevice: ${e.message}")
        }

    }

    @SuppressLint("MissingPermission")
    fun acceptConnection() {
        try {
            //Server Cihaz bağlantıları burada kabul ediyor.
            Log.i(TAG, "acceptConnection:...temp is connected : ..${tempSocket?.isConnected} ")
            bluetoothServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(context.applicationInfo.name, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            tempSocket = bluetoothServerSocket?.accept()
            Log.d(TAG, "Temp Socket: $tempSocket")
        } catch (e: IOException) {
            bluetoothServerSocket?.close()
            Log.e(TAG, "acceptConnection: $e", )
            e.printStackTrace()
        }
    }

    fun sendDataToClient(data: String) {
        try {
            val outputStream: OutputStream? = tempSocket?.outputStream
            Log.i(TAG, "Mesaj Giden Socket :$tempSocket ")
            outputStream?.write(data.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun receiveData(onDataReceived: (String) -> Unit) {
        try {
            val inputStream: InputStream? = tempSocket?.inputStream
            val buffer = ByteArray(1024)
            var bytesRead: Int

            while (true) {
                bytesRead = inputStream?.read(buffer) ?: -1
                if (bytesRead == -1) {
                    break // Bağlantı kapandığında döngüyü sonlandır
                }

                val receivedData = String(buffer, 0, bytesRead)
                onDataReceived.invoke(receivedData)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun sendDataToServer(data: String) {
        // Veriyi Bluetooth cihazına gönderme
        if (outputStream != null) {
            try {
                outputStream?.write(data.toByteArray())
                Log.d(TAG, "sendData: $data")
            } catch (e: IOException) {
                bluetoothSocket=null
                Log.e(TAG, "Error writing data: ${e.message}")
            }
        } else {
            Log.e(TAG, "Output stream is null. Cannot send data.")
        }


    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices() {
        //Bağlı cihazları verir
        //Mesaj Alacağı cihaza connect olur
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        //İlk sırdaki cihazı server olarak kabul eder.
        val device =pairedDevices?.first()
        Thread{
            Log.d(TAG, "Server device : ${device?.name} ")
            //connectToDevice(device!!)
            connectToDeviceRssi(device!!)
        }.start()
        Log.d(MainActivity.TAG, "Paired Devices :$pairedDevices ")
    }

    @SuppressLint("MissingPermission")
    fun connectToDeviceRssi(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun setDiscoverable() {
        //Diğer cihazlar tarafından görülmesi için
        val discoverableIntent: Intent =
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
        context.startActivity(discoverableIntent)
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
                val a=calculateDistance(rssi)
                Log.i(TAG, "RSSI: $a m")
                // RSSI değerini kullanarak istediğiniz işlemleri gerçekleştirebilirsiniz.
            } else {
                Log.e(TAG, "RSSI read failed with status $status")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startDReceiver(dReceiver: DiscoveryReceiver) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(dReceiver, intentFilter)
        bluetoothAdapter.startDiscovery()
    }

    fun startBReceiver(bReceiver: BluetoothReceiver) {
        val intentFilterB = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bReceiver, intentFilterB)
    }
    fun closeBluetoothSocket(){
        if(bluetoothSocket!=null){
            bluetoothSocket?.close()

            bluetoothSocket=null
        }

    }
    fun closeBluetoothServerSocket(){
        if (bluetoothServerSocket!=null){
            bluetoothServerSocket?.close()
            if (tempSocket!=null){
                tempSocket?.close()
                outputStream?.close()
                tempSocket=null

            }

            bluetoothSocket=null
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





    companion object {
        const val TAG = "CustomBluetoothManager"
    }
}

