package com.example.bluetoothedu

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothReceiver:BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        val action=p1?.action
        if (action==BluetoothAdapter.ACTION_STATE_CHANGED){
            when(p1.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR)){

                BluetoothAdapter.STATE_ON->{
                    Log.i(TAG, "STATE_ON: ")
                }
                BluetoothAdapter.STATE_OFF->{
                    Log.i(TAG, "STATE_OFF: ")
                }
                BluetoothAdapter.STATE_TURNING_ON->{
                    Log.i(TAG, "STATE_TURNING_ON: ")
                }
                BluetoothAdapter.STATE_TURNING_OFF->{
                    Log.i(TAG, "STATE_TURNING_OFF: ")
                }
                BluetoothAdapter.STATE_CONNECTED->{
                    Log.i(TAG, "STATE_CONNECTED: ")
                }
                BluetoothAdapter.STATE_DISCONNECTED->{
                    Log.i(TAG, "STATE_DISCONNECTED: ")
                }
                BluetoothAdapter.STATE_DISCONNECTING->{
                    Log.i(TAG, "STATE_DISCONNECTING: ")
                }
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE->{
                    Log.i(TAG, "SCAN_MODE_CONNECTABLE_DISCOVERABLE: ")
                }
            }
        }
    }

    companion object{
        const val TAG= "BluetoothReceiver"
    }

}
