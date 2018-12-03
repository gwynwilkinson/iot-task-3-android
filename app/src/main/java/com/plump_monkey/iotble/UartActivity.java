package com.plump_monkey.iotble;

import android.content.ComponentName;
import android.content.DialogInterface;
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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.plump_monkey.iotble.bluetooth.BleAdapterService;
import com.plump_monkey.iotble.bluetooth.ConnectionStatusListener;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class UartActivity extends AppCompatActivity implements ConnectionStatusListener {

    private BleAdapterService bluetooth_le_adapter;

    private boolean exiting=false;
    private boolean indications_on=false;
    int ledServiceValue = 0;
    int rgbLedServiceValue = 0;
    int fanServiceValue = 0;
    int buzzerServiceValue = 0;
    String perSessionSalt = new String();

    // Handler for BT Service Connection indication
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

            // Generate and send a per session Salt. Hardcode the PIN and Salt used for the
            // initial handshake.
            String protocolString = new String();

            // Set the header, Protocol Version and the Request bit.
            protocolString = "IoT" + Integer.toString(Constants.PROTOCOL_VERSION) + Integer.toString(Constants.REQUEST);

            // Generate 5 random characters
            String randomLetters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!£$%^&*()_+";
            Random r = new Random();

            // Set the new salt in the perSessionSalt variable
            for (int i = 0; i < 5; i++) {
                perSessionSalt += randomLetters.charAt(r.nextInt(randomLetters.length()));
            }

            // Set the service to show we are sending a Salt, and add the salt to the string.
            protocolString = protocolString + Integer.toString(Constants.SERVICE_SALT) + perSessionSalt;

            // Generate the Random number bits and add it to the protocol string.
            protocolString += String.format("%02x",r.nextInt(0xff));

            // TODO - CRC
            // Add the CRC
            protocolString += String.format("%02x",0xff);

            Log.d(Constants.TAG, "New Salt Protocol String == " + protocolString);

            // Hard Code the PIN used for the first message
            String PIN = "123";

            // Hard code the salt. This must be the same on the MicroBit
            String initialSalt = "ThisIsMySaltThereAreManyLikeIt";

            // Now try and encrypt the protocol message
            try {
                byte[] encrypted = encryptString(protocolString, PIN, initialSalt);

                // Split the encrypted text and send over BLE.
                String encryptedString =  Utility.byteArrayAsHexString(encrypted);

                // Only do this if we had some text
                if(!TextUtils.isEmpty(encryptedString)) {
                    sendMessage(encryptedString);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                showMsg("Unable to convert text to ASCII bytes");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetooth_le_adapter = null;
        }
    };

    // Activity onCrate Handler
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

        final Button serviceDataButton = findViewById(R.id.serviceActionButton);

        serviceDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(Constants.TAG, "Service Data Button Pressed");

                RadioButton ledButton = findViewById(R.id.ledRadioButton);
                RadioButton rgbButton = findViewById(R.id.rgbRadioButton);
                RadioButton buzzerButton = findViewById(R.id.buzzerRadioButton);
                RadioButton fanButton = findViewById(R.id.fanRadioButton);

                // See which button is selected
                if(ledButton.isChecked()) {
                    // LED button is selected. Build the options list
                    final CharSequence[] ledItems = {"Off", "On", "SoS"};
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(UartActivity.this);
                    builder.setTitle("LED Options");
                    builder.setSingleChoiceItems(ledItems, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(Constants.TAG, "LED Service Data Selected:- " + which);
                            // Save the selected value
                            ledServiceValue = which;
                            if(which == Constants.SERVICE_LED_OFF) {
                                serviceDataButton.setText(R.string.off);
                            } else if(which == Constants.SERVICE_LED_ON){
                                serviceDataButton.setText(R.string.on);
                            } else if(which == Constants.SERVICE_LED_SOS){
                                serviceDataButton.setText(R.string.sos);
                            }
                            dialog.dismiss();
                        }
                    });
                    builder.show();
                } else if ( rgbButton.isChecked() ) {
                    // RGB button is selected. Build the options list

                    final CharSequence[] rgbLedItems = {"Off", "Red", "Blue", "Green", "Magenta", "Yellow", "Cyan","White","Party"};

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(UartActivity.this);
                    builder.setTitle("RGB LED Options");
                    builder.setSingleChoiceItems(rgbLedItems, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(Constants.TAG, "RGB LED Service Data Selected:- " + which);
                            // Save the selected value
                            rgbLedServiceValue = which;

                            if(which == Constants.SERVICE_RGB_OFF) {
                                serviceDataButton.setText(R.string.off);
                            } else if(which == Constants.SERVICE_RGB_RED){
                                serviceDataButton.setText(R.string.red);
                            } else if(which == Constants.SERVICE_RGB_BLUE){
                                serviceDataButton.setText(R.string.blue);
                            } else if(which == Constants.SERVICE_RGB_GREEN){
                                serviceDataButton.setText(R.string.green);
                            } else if(which == Constants.SERVICE_RGB_MAGENTA){
                                serviceDataButton.setText(R.string.magenta);
                            } else if(which == Constants.SERVICE_RGB_YELLOW){
                                serviceDataButton.setText(R.string.yellow);
                            } else if(which == Constants.SERVICE_RGB_CYAN){
                                serviceDataButton.setText(R.string.cyan);
                            } else if(which == Constants.SERVICE_RGB_WHITE){
                                serviceDataButton.setText(R.string.white);
                            } else if(which == Constants.SERVICE_RGB_PARTY) {
                                serviceDataButton.setText(R.string.party);
                            }
                            dialog.dismiss();
                        }
                    });
                    builder.show();
                } else if ( buzzerButton.isChecked() ) {
                    // Buzzer button is selected. Build the options list

                    final CharSequence[] buzzerItems = {"Off","Basic","Siren","Fanfare"};

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(UartActivity.this);
                    builder.setTitle("Buzzer Options");
                    builder.setSingleChoiceItems(buzzerItems, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(Constants.TAG, "Buzzer Service Data Selected:- " + which);
                            // Save the selected value
                            buzzerServiceValue = which;

                            if (which == Constants.SERVICE_BUZZER_OFF) {
                                serviceDataButton.setText(R.string.off);
                            } else if (which == Constants.SERVICE_BUZZER_BASIC) {
                                serviceDataButton.setText(R.string.basic);
                            } else if (which == Constants.SERVICE_BUZZER_SIREN) {
                                serviceDataButton.setText(R.string.siren);
                            } else if (which == Constants.SERVICE_BUZZER_FANFARE) {
                                serviceDataButton.setText(R.string.fanfare);
                            }
                            dialog.dismiss();
                        }
                    });
                    builder.show();
                } else if ( fanButton.isChecked() ) {
                    // Fan button is selected. Build the options list

                    final CharSequence[] buzzerItems = {"Off","Slow","Medium","Fast"};

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(UartActivity.this);
                    builder.setTitle("Fan Options");
                    builder.setSingleChoiceItems(buzzerItems, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(Constants.TAG, "Fan Service Data Selected:- " + which);
                            // Save the selected value
                            fanServiceValue = which;

                            if (which == Constants.SERVICE_FAN_OFF) {
                                serviceDataButton.setText(R.string.off);
                            } else if (which == Constants.SERVICE_FAN_SLOW) {
                                serviceDataButton.setText(R.string.slow);
                            } else if (which == Constants.SERVICE_FAN_MED) {
                                serviceDataButton.setText(R.string.medium);
                            } else if (which == Constants.SERVICE_FAN_FAST) {
                                serviceDataButton.setText(R.string.fast);
                            }
                            dialog.dismiss();
                        }
                    });
                    builder.show();
                }
            }
        });
    }

    // Handler for the radio buttons.
    // Clears any other radio buttons and resets the text on
    // the Service Selection button
    public void onRadioButtonClicked(View view) {

        Button serviceDataButton = findViewById(R.id.serviceActionButton);

        // Button Changed. Clear the values.
        ledServiceValue = 0;
        rgbLedServiceValue = 0;
        fanServiceValue = 0;
        buzzerServiceValue = 0;

        // Reset the text on the Service Data Button to indicate we need
        // to select a new service
        serviceDataButton.setText(R.string.select_service);

        // Clear any other radio buttons
        switch (view.getId()) {

            case R.id.ledRadioButton:
            case R.id.fanRadioButton:
                Log.d(Constants.TAG, "Group 1 pressed");
                RadioGroup group2 = findViewById(R.id.radioGroup2);
                group2.clearCheck();
                break;

            default:
                Log.d(Constants.TAG, "Group 2 pressed");
                RadioGroup group1 = findViewById(R.id.radioGroup1);
                group1.clearCheck();
                break;
        }
    }

    // Handle the onDestroy for the activity
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

    // Activity back button handler
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

    // Menu Options
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

    // Bluetooth Service message handler
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

    // Display BT service messages
    private void showMsg(final String msg) {
        Log.d(Constants.TAG, msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) UartActivity.this.findViewById(R.id.message)).setText(Html.fromHtml(msg));
            }
        });
    }

    @Override
    public void serviceDiscoveryStatusChanged(boolean new_state) {
    }

    @Override
    public void connectionStatusChanged(boolean connected) {
        if (connected) {
            showMsg(Utility.htmlColorGreen("Connected"));
        } else {
            showMsg(Utility.htmlColorRed("Disconnected"));
        }
    }

    /******************************************************************************************
     *
     * Main functions to handle the IoT message interaction
     *
     ******************************************************************************************/

    // Display an Alert box to the user with the supplied message
    private void showAlert(String headerMsg, String response) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(headerMsg);
        builder.setMessage(response);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    // Handler for the Send PIN button
    public void onSendPIN(View view) {
        Log.d(Constants.TAG, "onSendPIN");
        sendPIN();
    }

    // Format and send the message to Microbit
    private void sendPIN() {

        // Find the PIN Number Edit Text box and dump the Pin to the debug log.
        EditText pinButton = (EditText) UartActivity.this.findViewById(R.id.uartPin);
        Log.d(Constants.TAG, "onSendPIN: " + pinButton.getText().toString());

        // Check that there has been a service action selected
        Button serviceActionButton = findViewById(R.id.serviceActionButton);
        if (serviceActionButton.getText().toString().matches("Select Service")) {
            Log.d(Constants.TAG, "No action selected");

            // No Service Action has been selected. Display an error and quit
            Toast toast = Toast.makeText(this, "Select a service action", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        // Check that a PIN has been entered. Otherwise fire an error and return
        if (pinButton.getText().toString().matches("")) {
            Log.d(Constants.TAG, "No PIN entered");

            // No Service Action has been selected. Display an error and quit
            Toast toast = Toast.makeText(this, "Enter a PIN", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        // Create the protocol message
        String protocolString = new String();

        RadioButton ledButton = findViewById(R.id.ledRadioButton);
        RadioButton rgbButton = findViewById(R.id.rgbRadioButton);
        RadioButton buzzerButton = findViewById(R.id.buzzerRadioButton);
        RadioButton fanButton = findViewById(R.id.fanRadioButton);

        // Protocol Message definition
//  ---------------------------------------------------------------------------------------
// | Byte        | 0 | 1 | 2 |  3  |   4  | 5   | 6 | 7 | 8 |  9 | 10 | 11 | 12 | 13 | 14 |
//  ---------------------------------------------------------------------------------------
// | Description |   Header  | Prot | Req | Svc |    Service Data          |  Redund | CRC |
// |             |           | Ver  | ACK | ID  |                          |   Info  |     |
//  ---------------------------------------------------------------------------------------
// | Contents    | I   O   T |   1  | 0/1 | 0-FF|   0 - FFFFFFFF          |  Random  |0-FF |
//  ----------------------------------------------------------------------------------------

        // Set the header, Protocol Version and the Request bit.
        protocolString = "IoT" + Integer.toString(Constants.PROTOCOL_VERSION) + Integer.toString(Constants.REQUEST);

        String serviceActionText = serviceActionButton.getText().toString();
        // See which button is selected
        if (ledButton.isChecked()) {
            // LED button is selected. Set the Service ID and the Data
            protocolString = protocolString + Integer.toString(Constants.SERVICE_LED) +
                    String.format("%05x", ledServiceValue);
        } else if (rgbButton.isChecked() ) {
            // RGB button is selected. Set the Service ID and the Data
            protocolString = protocolString + Integer.toString(Constants.SERVICE_RGB_LED) +
                    String.format("%05x", rgbLedServiceValue);
        } else if ( buzzerButton.isChecked() ) {
            // Buzzer button is selected. Set the Service ID and the Data
            protocolString = protocolString + Integer.toString(Constants.SERVICE_BUZZER) +
                    String.format("%05x", buzzerServiceValue);
        } else if ( fanButton.isChecked() ) {
            // Fan button is selected. Set the Service ID and the Data
            protocolString = protocolString + Integer.toString(Constants.SERVICE_FAN) +
                    String.format("%05x", fanServiceValue);
        }

        // Add the random number (0x0000-0xFFFF)
        Random rand = new Random();
        int n = rand.nextInt(0xff);
        protocolString = protocolString + String.format("%02x",n);

        // TODO - CRC
        // Add the CRC
        protocolString = protocolString + String.format("%02x",0xff);

        Log.d(Constants.TAG, "Final Protocol String is - " + protocolString + " Length - "+ protocolString.length());

        // Obtain the string of the pin from the EditText box
        String inputPIN = pinButton.getText().toString();

        // Now try and encrypt the protocol message
        try {
            byte[] encrypted = encryptString(protocolString, inputPIN, perSessionSalt);

            // Split the encrypted text and send over BLE.
            String encryptedString =  Utility.byteArrayAsHexString(encrypted);

            // Only do this if we had some text
            if(!TextUtils.isEmpty(encryptedString)) {

                sendMessage(encryptedString);

            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            showMsg("Unable to convert text to ASCII bytes");
        }
    }

    private byte[] encryptString(String protocolString, String inputPIN, String salt) {
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

                    textToEncrypt = protocolString;

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
        return encrypted;
    }

    // Function to send the message.
    private void sendMessage(String encryptedString) throws UnsupportedEncodingException {
        Toast toast = Toast.makeText(this, "Sending Command", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0, 0);
        toast.show();

        for (int i = 0, j = 0; i < encryptedString.length(); i = i + 20, j++) {
            String splitText = encryptedString.substring(i, Math.min(i + 20, encryptedString.length()));

            byte[] ascii_bytes = splitText.getBytes("US-ASCII");
            Log.d(Constants.TAG, "ASCII bytes: 0x" + Utility.byteArrayAsHexString(ascii_bytes) + " - Length: " + ascii_bytes.length);
            bluetooth_le_adapter.writeCharacteristic(Utility.normaliseUUID(BleAdapterService.UARTSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.UART_RX_CHARACTERISTIC_UUID), ascii_bytes);

            // Add a delay between sending the chunks.
            SystemClock.sleep(400);
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