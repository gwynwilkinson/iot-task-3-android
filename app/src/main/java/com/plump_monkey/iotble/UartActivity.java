package com.plump_monkey.iotble;

import android.annotation.SuppressLint;
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

/*************************************************************************************
 * Class        :   UartActivity
 *
 * Description  :   Activity to handle the BTLE UART communication to the MicroBit
 *
 *************************************************************************************/
public class UartActivity extends AppCompatActivity implements ConnectionStatusListener {

    private BleAdapterService bluetooth_le_adapter;

    private boolean exiting=false;
    private boolean indications_on=false;
    int ledServiceValue = 0;
    int rgbLedServiceValue = 0;
    int fanServiceValue = 0;
    int buzzerServiceValue = 0;
    String perSessionSalt = new String();

    // Activity onCrate Handler
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_uart);
        getSupportActionBar().setTitle(R.string.screen_title_UART);

        // Read intent data
        final Intent intent = getIntent();
        MicroBit.getInstance().setConnection_status_listener(this);

        // Connect to the Bluetooth smart service
        Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // Set up a listener for the PIN code edit text field
        EditText text = (EditText) UartActivity.this.findViewById(R.id.uartPin);
        text.setOnKeyListener(new View.OnKeyListener() {
            @Override
            // If someone presses enter on the keypad after sending the PIN, send the command
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    sendCommand();
                    return true;
                }
                return false;            }
        });

        // Setup the handler for the "Select Service" button.
        final Button serviceDataButton = findViewById(R.id.serviceActionButton);
        serviceDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(Constants.TAG, "Service Data Button Pressed");

                // We need to know which service is selected to dynamically
                // display the correct menu. Obtain a reference to the radio
                // buttons so we can determine which one is checked.
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

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        // Handler for BT Service Connection indication
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            // Display in the debug log that we have connected the BTLE
            Log.d(Constants.TAG, "onServiceConnected");

            // Get the BTLE adpater
            bluetooth_le_adapter = ((BleAdapterService.LocalBinder) service).getService();
            bluetooth_le_adapter.setActivityHandler(mMessageHandler);

            if (bluetooth_le_adapter.setIndicationsState(Utility.normaliseUUID(BleAdapterService.UARTSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.UART_TX_CHARACTERISTIC_UUID), true)) {
                showMsg(Utility.htmlColorGreen("UART TX indications ON"));
            } else {
                showMsg(Utility.htmlColorRed("Failed to set UART TX indications ON"));
            }

            // When the MicroBit first connects over BTLE, generate and send a per session Salt.
            // The PIN and Salt used for the initial handshake are hardcoded.
            // Format up the protocol message with the header, protocol version, request bit, Salt
            // Service request, and the Salt. Finish the string with the CRC.
            String protocolString = new String();

            // Set the header, Protocol Version and the Request bit.
            protocolString = "IoT" + Integer.toString(Constants.PROTOCOL_VERSION) + Integer.toString(Constants.REQUEST);

            // Generate 5 random characters for the salt from the character set.
            String randomLetters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!£$%^&*()_+";
            Random r = new Random();

            // Set the new salt in the perSessionSalt variable
            for (int i = 0; i < 5; i++) {
                perSessionSalt += randomLetters.charAt(r.nextInt(randomLetters.length()));
            }

            // Set the protocol message service identification to show we are sending a Salt, and add the salt to the string.
            protocolString = protocolString + Integer.toString(Constants.SERVICE_PER_SESSION_SALT) + perSessionSalt;

            // Generate the Random number bits and add it to the protocol string.
            protocolString += String.format("%02x",r.nextInt(0xff));

            // Add the CRC
            String crc = Integer.toHexString(ccitt_crc(protocolString,13));
            Log.d(Constants.TAG, "Calculated CRC value = " + crc);

            protocolString = protocolString + crc;

            // Display to the debug log the unencoded protocol string.
            Log.d(Constants.TAG, "New Salt Protocol String == " + protocolString);

            // Hard Code the PIN used for the first message. this must be the same on the MicroBit.
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

    // Handler for the radio buttons.
    // When a button is pressed, it clears any other radio
    // buttons and resets the text on the Service Selection button
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
    @SuppressLint("HandlerLeak")
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

                    // Received notification from the Micro:bit
                    // Will need to parse the message and format an alert text box
                    // to display the message to the user.

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

                        String crc = String.format("%02x", ccitt_crc(ascii, 13));
                        Log.d(Constants.TAG, "CRC = " + crc);

                        String serviceID = ascii.substring(5,6);
                        String responseCode = ascii.substring(10,11);
                        String status;
                        String service;
                        String message = "";

                        // Verify IoT header and CRC match
                        if(ascii.substring(0,3).equals("IoT")) {
                            if((ascii.substring(13,15).equals(crc))) {
                                // Message parsed ok. Verify Response Flag. Micro:bit to
                                // Android currently only supports response notifications.
                                if(ascii.substring(3,4).equals("1")) {
                                    if(responseCode.equals("0")) {
                                        status = "Failed";
                                    } else {
                                        status = "Success";
                                    }

                                    switch (ascii.substring(5,6) ) {
                                        case "1":
                                            service = "LED ";
                                            break;
                                        case "2":
                                            service = "Buzzer ";
                                            break;
                                        case "3":
                                            service = "Fan ";
                                            break;
                                        case "4":
                                            service = "RGB LED ";
                                            break;
                                        case "8":
                                            service = "PIN Code ";
                                            status = "Incorrect";
                                            break;
                                        default:
                                            service = "Unknown ";
                                            break;
                                    }
                                    message = service + status;

                                    Log.d(Constants.TAG, message);
                                }
                            }
                        }
                        showAlert( "Microbit Response", message);
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

    // Handler for the Send Command button
    public void onSendCommand(View view) {
        Log.d(Constants.TAG, "onSendCommand");
        sendCommand();
    }

    /*****************************************************************************
     *  Method:         sendCommand
     *
     *  Description:    Method called when the Send Command button is pressed.
     *                  Extracts the PIN from the Edit Text box and validates it.
     *                  Formats the Protocol Message as per the definition.
     *                  Service ID and Data depend on the UI controls which are set.
     *                  Calls the encryption and sendMessage methods with the formatted
     *                  message.
     *
     *****************************************************************************/
    // Format and send the message to Microbit
    private void sendCommand() {
        // Find the PIN Number Edit Text box and dump the Pin to the debug log.
        EditText pinButton = (EditText) UartActivity.this.findViewById(R.id.uartPin);
        Log.d(Constants.TAG, "onSendCommand: " + pinButton.getText().toString());

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

        // Check that a valid PIN has been entered. Otherwise fire an error and return
        if (pinButton.getText().toString().matches("")) {
            Log.d(Constants.TAG, "No PIN entered");

            // No Service Action has been selected. Display an error and quit
            Toast toast = Toast.makeText(this, "Enter a PIN", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        } else if (pinButton.getText().toString().length() != 3) {
            Log.d(Constants.TAG, "Invalid PIN entered");

            // No Service Action has been selected. Display an error and quit
            Toast toast = Toast.makeText(this, "Enter a 3 digit PIN", Toast.LENGTH_LONG);
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
// | Description |   Header  | Prot | Req | Svc |    Service Data     |  Redund |   CRC   |
// |             |           | Ver  | ACK | ID  |                     |   Info  |         |
//  ---------------------------------------------------------------------------------------
// | Contents    | I   O   T |   1  | 0/1 | 0-F |   0 - FFFFFF        | Random  |  0-FF   |
//  ---------------------------------------------------------------------------------------

        // Set the header, Protocol Version and the Request bit.
        protocolString = "IoT" + Integer.toString(Constants.PROTOCOL_VERSION) + Integer.toString(Constants.REQUEST);

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

        String crc = String.format("%02x", ccitt_crc(protocolString, 13));
        Log.d(Constants.TAG, "Calculated CRC value = " + crc);

        // Add the CRC
        protocolString = protocolString + crc;

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

                Toast toast = Toast.makeText(this, "Sending Command", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER,0, 0);
                toast.show();

                sendMessage(encryptedString);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            showMsg("Unable to convert text to ASCII bytes");
        }
    }

    /*****************************************************************************
     *
     * Method:          encryptString
     *
     * Description:     The parameters to the method are the Protocol Message,
     *                  the input PIN, and the salt.
     *
     *                  A DPK is generated as a SHA-1 Hash of the combination of the
     *                  PIN + Salt. This is truncated to 16 bytes as the key for AES
     *                  is only 16 bytes, but the SHA-1 Hash is 20 bytes.
     *
     *                  Sets up an instance of a AES-128-ECB Cipher and encrypts
     *                  the protocol string with the DPK.
     *
     *****************************************************************************/
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

                    String textToEncrypt;

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

    /*******************************************************************************
     *
     *  Method:             sendMessage
     *
     *  Description:        Receives the encrypted ASCII string as a parameter and splits
     *                      the data into 20 byte chunks. Uses the BLE UART service to send
     *                      the data chunks.
     *
     *******************************************************************************/
    // Function to send the message.
    private void sendMessage(String encryptedString) throws UnsupportedEncodingException {
        for (int i = 0, j = 0; i < encryptedString.length(); i = i + 20, j++) {
            String splitText = encryptedString.substring(i, Math.min(i + 20, encryptedString.length()));

            byte[] ascii_bytes = splitText.getBytes("US-ASCII");
            Log.d(Constants.TAG, "ASCII bytes: 0x" + Utility.byteArrayAsHexString(ascii_bytes) + " - Length: " + ascii_bytes.length);
            bluetooth_le_adapter.writeCharacteristic(Utility.normaliseUUID(BleAdapterService.UARTSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.UART_RX_CHARACTERISTIC_UUID), ascii_bytes);

            // Add a delay between sending the chunks.
            SystemClock.sleep(400);
        }
    }

    /*******************************************************************************
     *
     *  Method:             sha1Hash
     *
     *  Description:        Generate a SHA-1 hash of the given input string.
     *
     *******************************************************************************/
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

    /*******************************************************************************
     *
     *  Method:             ccitt_crc
     *
     *  Description:        Generate a 1 byte CCITT-CRC for a given message.
     *
     *******************************************************************************/
    static int crc_table[] = {
            0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5,
            0x60c6, 0x70e7, 0x8108, 0x9129, 0xa14a, 0xb16b,
            0xc18c, 0xd1ad, 0xe1ce, 0xf1ef, 0x1231, 0x0210,
            0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6,
            0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c,
            0xf3ff, 0xe3de, 0x2462, 0x3443, 0x0420, 0x1401,
            0x64e6, 0x74c7, 0x44a4, 0x5485, 0xa56a, 0xb54b,
            0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
            0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6,
            0x5695, 0x46b4, 0xb75b, 0xa77a, 0x9719, 0x8738,
            0xf7df, 0xe7fe, 0xd79d, 0xc7bc, 0x48c4, 0x58e5,
            0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823,
            0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969,
            0xa90a, 0xb92b, 0x5af5, 0x4ad4, 0x7ab7, 0x6a96,
            0x1a71, 0x0a50, 0x3a33, 0x2a12, 0xdbfd, 0xcbdc,
            0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
            0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03,
            0x0c60, 0x1c41, 0xedae, 0xfd8f, 0xcdec, 0xddcd,
            0xad2a, 0xbd0b, 0x8d68, 0x9d49, 0x7e97, 0x6eb6,
            0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70,
            0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a,
            0x9f59, 0x8f78, 0x9188, 0x81a9, 0xb1ca, 0xa1eb,
            0xd10c, 0xc12d, 0xf14e, 0xe16f, 0x1080, 0x00a1,
            0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
            0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c,
            0xe37f, 0xf35e, 0x02b1, 0x1290, 0x22f3, 0x32d2,
            0x4235, 0x5214, 0x6277, 0x7256, 0xb5ea, 0xa5cb,
            0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d,
            0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447,
            0x5424, 0x4405, 0xa7db, 0xb7fa, 0x8799, 0x97b8,
            0xe75f, 0xf77e, 0xc71d, 0xd73c, 0x26d3, 0x36f2,
            0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
            0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9,
            0xb98a, 0xa9ab, 0x5844, 0x4865, 0x7806, 0x6827,
            0x18c0, 0x08e1, 0x3882, 0x28a3, 0xcb7d, 0xdb5c,
            0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a,
            0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0,
            0x2ab3, 0x3a92, 0xfd2e, 0xed0f, 0xdd6c, 0xcd4d,
            0xbdaa, 0xad8b, 0x9de8, 0x8dc9, 0x7c26, 0x6c07,
            0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
            0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba,
            0x8fd9, 0x9ff8, 0x6e17, 0x7e36, 0x4e55, 0x5e74,
            0x2e93, 0x3eb2, 0x0ed1, 0x1ef0 };

    int ccitt_crc(String protocolString, int length)
    {
        int count;
        int crc = 0;
        int temp;

        for (count = 0; count < length; ++count)
        {
            temp = (protocolString.charAt(count) ^ (crc >> 8)) & 0xff;
            crc = crc_table[temp] ^ (crc << 8);
        }
        return ((crc%256) & 0xff);

    }
}

