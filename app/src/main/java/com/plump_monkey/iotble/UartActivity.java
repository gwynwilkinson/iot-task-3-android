package com.plump_monkey.iotble;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.plump_monkey.iotble.bluetooth.BleAdapterService;
import com.plump_monkey.iotble.bluetooth.ConnectionStatusListener;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class UartActivity extends AppCompatActivity implements ConnectionStatusListener {

    private BleAdapterService bluetooth_le_adapter;

    private boolean exiting=false;
    private boolean indications_on=false;
    private int guess_count=0;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(Constants.TAG, "onServiceConnected");
            bluetooth_le_adapter = ((BleAdapterService.LocalBinder) service).getService();
            bluetooth_le_adapter.setActivityHandler(mMessageHandler);

            if (bluetooth_le_adapter.setIndicationsState(Utility.normaliseUUID(BleAdapterService.UARTSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.UART_TX_CHARACTERISTIC_UUID), true)) {
                showMsg(Utility.htmlColorGreen("UART TX indications ON"));
            } else {
                showMsg(Utility.htmlColorRed("Failed to set UART TX indications ON"));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetooth_le_adapter = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_uart);
        getSupportActionBar().setTitle(R.string.screen_title_UART);

        // read intent data
        final Intent intent = getIntent();
        MicroBit.getInstance().setConnection_status_listener(this);

        // connect to the Bluetooth smart service
        Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // Set up a listener for the edit text done button
        EditText text = (EditText) UartActivity.this.findViewById(R.id.uartPin);
        text.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    sendPIN();
                    return true;
                }
                return false;            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(Constants.TAG, "onDestroy");
        super.onDestroy();
        if (indications_on) {
            exiting = true;
            bluetooth_le_adapter.setIndicationsState(Utility.normaliseUUID(BleAdapterService.UARTSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.UART_TX_CHARACTERISTIC_UUID), false);
        }
        try {
            // may already have unbound. No API to check state so....
            unbindService(mServiceConnection);
        } catch (Exception e) {
        }
    }

    public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed");
        if (MicroBit.getInstance().isMicrobit_connected() && indications_on) {
            exiting = true;
            bluetooth_le_adapter.setIndicationsState(Utility.normaliseUUID(BleAdapterService.UARTSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.UART_TX_CHARACTERISTIC_UUID), false);
        }
        exiting=true;
        if (!MicroBit.getInstance().isMicrobit_connected()) {
            try {
                // may already have unbound. No API to check state so....
                unbindService(mServiceConnection);
            } catch (Exception e) {
            }
        }
        finish();
        exiting=true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_uart, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.menu_uart_avm_help) {
            Intent intent = new Intent(UartActivity.this, HelpActivity.class);
            intent.putExtra(Constants.URI, Constants.UART_AVM_HELP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Service message handler
    private Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Bundle bundle;
            String service_uuid = "";
            String characteristic_uuid = "";
            String descriptor_uuid = "";
            byte[] b = null;
            TextView value_text = null;

            switch (msg.what) {
                case BleAdapterService.GATT_CHARACTERISTIC_WRITTEN:
                    Log.d(Constants.TAG, "Handler received characteristic written result");
                    bundle = msg.getData();
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                    Log.d(Constants.TAG, "characteristic " + characteristic_uuid + " of service " + service_uuid + " written OK");
                    showMsg(Utility.htmlColorGreen("Ready"));
                    break;
                case BleAdapterService.GATT_DESCRIPTOR_WRITTEN:
                    Log.d(Constants.TAG, "Handler received descriptor written result");
                    bundle = msg.getData();
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                    descriptor_uuid = bundle.getString(BleAdapterService.PARCEL_DESCRIPTOR_UUID);
                    Log.d(Constants.TAG, "descriptor " + descriptor_uuid + " of characteristic " + characteristic_uuid + " of service " + service_uuid + " written OK");
                    if (!exiting) {
                        showMsg(Utility.htmlColorGreen("UART TX indications ON"));
                        indications_on=true;
                    } else {
                        showMsg(Utility.htmlColorGreen("UART TX indications OFF"));
                        indications_on=false;
                        finish();
                    }
                    break;

                case BleAdapterService.NOTIFICATION_OR_INDICATION_RECEIVED:
                    bundle = msg.getData();
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                    b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                    Log.d(Constants.TAG, "Value=" + Utility.byteArrayAsHexString(b));
                    if (characteristic_uuid.equalsIgnoreCase((Utility.normaliseUUID(BleAdapterService.UART_TX_CHARACTERISTIC_UUID)))) {
                        String ascii="";
                        Log.d(Constants.TAG, "UART TX received");
                        try {
                            ascii = new String(b,"US-ASCII");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            showMsg(Utility.htmlColorGreen("Could not convert TX data to ASCII"));
                            return;
                        }
                        Log.d(Constants.TAG, "micro:bit: " + ascii);
                        showAlert( "Microbit Response", ascii);
                    }
                    break;
                case BleAdapterService.MESSAGE:
                    bundle = msg.getData();
                    String text = bundle.getString(BleAdapterService.PARCEL_TEXT);
                    showMsg(Utility.htmlColorRed(text));
            }
        }
    };

    private void showMsg(final String msg) {
        Log.d(Constants.TAG, msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) UartActivity.this.findViewById(R.id.message)).setText(Html.fromHtml(msg));
            }
        });
    }

    private void showAlert(String headerMsg, String response) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(headerMsg);
        builder.setMessage(response);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    @Override
    public void connectionStatusChanged(boolean connected) {
        if (connected) {
            showMsg(Utility.htmlColorGreen("Connected"));
        } else {
            showMsg(Utility.htmlColorRed("Disconnected"));
        }
    }

    @Override
    public void serviceDiscoveryStatusChanged(boolean new_state) {
    }


    public void onSendPIN(View view) {
        Log.d(Constants.TAG, "onSendPIN");
        sendPIN();
    }

    // Handler for the "Send PIN" Button
    private void sendPIN() {
        EditText text = (EditText) UartActivity.this.findViewById(R.id.uartPin);
        Log.d(Constants.TAG, "onSendPIN: " + text.getText().toString());
        try {
            // Obtain the string of the pin from the EditText box
            String inputPIN = text.getText().toString();

            // Hard code the salt. This must be the same on the MicroBit
            String salt = "ThisIsMySaltThereAreManyLikeIt";

            // Hash the PIN and Salt to create the dpk
            byte[] dpk = sha1Hash(inputPIN + salt);

            Log.d(Constants.TAG, "DPK Created:" + Utility.byteArrayAsHexString(dpk) + " - Length: " + dpk.length);

            // The returned dpk is 20 bytes long (SHA-1 block length)
            // But the key for AES-128-ECB is 16 bytes. Truncate the string
            // Storage for the encrypted text
            dpk = Arrays.copyOf(dpk, 16);

            Log.d(Constants.TAG, "Truncated DPK:" + Utility.byteArrayAsHexString(dpk) + " - Length: " + dpk.length);

            byte[] encrypted = null;

            // Setup the Secret Key
            SecretKeySpec secretKeySpec = new SecretKeySpec(dpk, "AES");

            // Get the Cipher instance with AES-128-ECB and PKCS5 padding as the parameters
            try {
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");

                try {
                    // Initialise the Cipher with the key
                    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
                    try {

                        String textToEncrypt = "IoT-";

                        textToEncrypt = textToEncrypt + inputPIN;

                        // Encrypt the protocol text
                        encrypted = cipher.doFinal(textToEncrypt.getBytes());

                        // Debug logs to show the output
                        Log.d(Constants.TAG, "Cipher Generated:" + Utility.byteArrayAsHexString(encrypted) + " - Length: " + encrypted.length);
                        Log.d(Constants.TAG, "Key used:" + Utility.byteArrayAsHexString(dpk) + " - Length: " + dpk.length);

                    }  catch (BadPaddingException e) {
                        e.printStackTrace();
                        showAlert("Error", "BadPaddingException");

                    } catch (IllegalBlockSizeException e) {
                        e.printStackTrace();
                        showAlert("Error", "IllegalBlockSizeException");
                    }
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                    showAlert("Error", "Invalid key");
                }
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                e.printStackTrace();
                showAlert("Error", "Unable to initialise cipher");
            }

            // Split the encrypted text and send over BLE.
            String encryptedString =  Utility.byteArrayAsHexString(encrypted);

                // Only do this if we had some text
                if(!TextUtils.isEmpty(encryptedString)) {

                for (int i = 0, j = 0; i < encryptedString.length(); i = i + 20, j++) {
                    String splitText = encryptedString.substring(i, Math.min(i + 20, encryptedString.length()));

                    byte[] ascii_bytes = splitText.getBytes("US-ASCII");
                    Log.d(Constants.TAG, "ASCII bytes: 0x" + Utility.byteArrayAsHexString(ascii_bytes) + " - Length: " + ascii_bytes.length);
                    bluetooth_le_adapter.writeCharacteristic(Utility.normaliseUUID(BleAdapterService.UARTSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.UART_RX_CHARACTERISTIC_UUID), ascii_bytes);

                    // Add a delay between sending the chunks.
                    SystemClock.sleep(400);
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            showMsg("Unable to convert text to ASCII bytes");
        }
    }

    public  byte[] sha1Hash (String inputString) {
        try {
            // Create SHA-1 Hash
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try {
                byte[] bytes = inputString.getBytes("UTF-8");

                digest.update(bytes, 0, bytes.length);
                bytes = digest.digest();

                Log.d(Constants.TAG, "SHA-1 hash of " + inputString + " == " + Utility.byteArrayAsHexString(bytes) + " - Length: " + bytes.length);

                return(bytes);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
}