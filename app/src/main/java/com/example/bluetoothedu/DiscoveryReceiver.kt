package com.example.bluetoothedu

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout

class DiscoveryReceiver(val progressBar: ProgressBar) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(p0: Context?, p1: Intent?) {
        val action: String? = p1?.action
        when (action) {
            BluetoothDevice.ACTION_FOUND -> {

                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device: BluetoothDevice? =
                    p1?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device?.name != null) {
                    val deviceName = device.name
                    Log.d(TAG, "---------------->>>Device Name: $deviceName")

                    if (deviceName == "Octomall") {
                        //Bulduğu ilk cihazla eşleşmek ister
                        Log.e(TAG, "Bond Device: $deviceName")
                        Toast.makeText(
                            p0,
                            "Octomall cihazına bağlantı isteği gönderildi  ",
                            Toast.LENGTH_LONG
                        ).show()
                        device.createBond()
                    }
                }
                if (device?.name != null) {
                    when (device?.bondState) {

                        BluetoothDevice.BOND_NONE -> {
                            Log.i(TAG, "None: ${device.name} ")
                        }

                        BluetoothDevice.BOND_BONDED -> {
                            Log.e(TAG, "Bonded: ${device.name} ")
                        }

                        BluetoothDevice.BOND_BONDING -> {
                            Log.d(TAG, "Bonding: ${device.name} ")
                        }
                    }
                }


            }

            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                progressBar.visibility = View.GONE
                Log.i(TAG, "ACTION_DISCOVERY_FINISHED: ")
            }

            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                Log.i(TAG, "ACTION_DISCOVERY_STARTED: ")
            }

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                Log.i(TAG, "ACTION_STATE_CHANGED: ")
            }
        }

    }

    companion object {
        const val TAG = "DiscoveryReceiver"
    }
}