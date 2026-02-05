package com.joshuagranick.btautopin;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.nio.charset.StandardCharsets;

/**
 * Headless Bluetooth pairing receiver for devices without SystemUI pairing dialogs.
 *
 * This component is intended for automotive / embedded Android builds
 * where user interaction is unavailable or undesired.
 *
 * REQUIREMENTS:
 * - Must be installed as a priv-app (/system/priv-app)
 * - Must be signed with the platform key
 * - Requires android.permission.BLUETOOTH_PRIVILEGED
 *
 * NOTE:
 * On modern Android versions, Bluetooth pairing APIs are restricted
 * to privileged system code. This receiver will not function correctly
 * as a normal user-installed APK.
 */
public class PairingReceiver extends BroadcastReceiver {

    // Constants not in public SDK but present at runtime
    private static final int PAIRING_VARIANT_CONSENT = 3;
    private static final int PAIRING_VARIANT_PIN_16_DIGITS = 4;

    private static final String LOG_TAG = "BTAutoPIN";

    // Default legacy PIN used by many automotive head units
    private static final String DEFAULT_PIN = "0000";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction())) {
            return;
        }

        BluetoothDevice device =
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (device == null) {
            Log.e(LOG_TAG, "PAIRING_REQUEST received with no device");
            return;
        }

        int variant = intent.getIntExtra(
                BluetoothDevice.EXTRA_PAIRING_VARIANT,
                BluetoothDevice.ERROR);

        Log.i(LOG_TAG, "Pairing request from " + device.getAddress());
        Log.i(LOG_TAG, "Pairing variant: " + variant);

        logDeviceClass(device);

        try {
            handlePairingVariant(context, device, variant);
            Log.i(LOG_TAG, "Pairing handling completed");

        } catch (SecurityException se) {
            Log.e(LOG_TAG,
                    "SecurityException: missing BLUETOOTH_PRIVILEGED permission", se);

        } catch (Exception e) {
            Log.e(LOG_TAG, "Unexpected error during pairing", e);
        }
    }

    private void handlePairingVariant(
            Context context,
            BluetoothDevice device,
            int variant) throws Exception {

        switch (variant) {

            case BluetoothDevice.PAIRING_VARIANT_PIN:
            case PAIRING_VARIANT_PIN_16_DIGITS:
                injectPin(device, getPin(context));
                break;

            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
            case PAIRING_VARIANT_CONSENT:
                confirmPairing(device);
                break;

            default:
                Log.w(LOG_TAG, "Unsupported or unknown pairing variant: " + variant);
                break;
        }
    }

    private void injectPin(BluetoothDevice device, String pin) throws Exception {
        Log.i(LOG_TAG, "Injecting PIN for device");

        byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
        device.setPin(pinBytes);
        device.setPairingConfirmation(true);
    }

    private void confirmPairing(BluetoothDevice device) {
        Log.i(LOG_TAG, "Confirming pairing without PIN");
        device.setPairingConfirmation(true);
    }

    private String getPin(Context context) {
        // Placeholder for future configurability
        // OEMs may replace this with a secure setting or resource
        return DEFAULT_PIN;
    }

    private void logDeviceClass(BluetoothDevice device) {
        BluetoothClass btClass = device.getBluetoothClass();
        if (btClass == null) {
            Log.i(LOG_TAG, "Bluetooth class: unknown");
            return;
        }

        int majorClass = btClass.getMajorDeviceClass();
        Log.i(LOG_TAG, "Bluetooth major device class: " + majorClass);
    }
}
