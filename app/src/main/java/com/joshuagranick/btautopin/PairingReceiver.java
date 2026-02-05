package com.joshuagranick.btautopin;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PairingReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "BTAutoPIN";
    private static final String DEFAULT_PIN = "0000";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction())) {
            return;
        }

        BluetoothDevice device =
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (device == null) {
            Log.e(LOG_TAG, "No device in pairing request");
            return;
        }

        try {
            Log.i(LOG_TAG, "Pairing request from " + device.getAddress());

            device.setPin(DEFAULT_PIN.getBytes("UTF-8"));
            device.setPairingConfirmation(true);

            Log.i(LOG_TAG, "PIN sent successfully");

            abortBroadcast();

        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to inject PIN", e);
        }
    }
}
