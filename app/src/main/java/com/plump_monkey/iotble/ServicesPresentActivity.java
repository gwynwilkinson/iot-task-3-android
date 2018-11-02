package com.plump_monkey.iotble;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.plump_monkey.iotble.bluetooth.BleAdapterService;

public class ServicesPresentActivity extends AppCompatActivity {

    public static final int START_SERVICES_PRESENT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services_present);
        TextView presence_generic_access = (TextView) this.findViewById(R.id.service_generic_access_presence);
        TextView presence_generic_attribute = (TextView) this.findViewById(R.id.service_generic_attribute_presence);
        TextView presence_device_information = (TextView) this.findViewById(R.id.service_device_information_presence);
        TextView presence_dfu_control = (TextView) this.findViewById(R.id.service_dfu_control_presence);
        TextView presence_accelerometer = (TextView) this.findViewById(R.id.service_accelerometer_presence);
        TextView presence_magnetometer = (TextView) this.findViewById(R.id.service_magnetometer_presence);
        TextView presence_button = (TextView) this.findViewById(R.id.service_button_presence);
        TextView presence_io_pin = (TextView) this.findViewById(R.id.service_io_pin_presence);
        TextView presence_led = (TextView) this.findViewById(R.id.service_led_presence);
        TextView presence_event = (TextView) this.findViewById(R.id.service_event_presence);
        TextView presence_temperature = (TextView) this.findViewById(R.id.service_temperature_presence);
        TextView presence_uart = (TextView) this.findViewById(R.id.service_uart_presence);

        indicatePresence(presence_generic_access,MicroBit.getInstance().hasService(BleAdapterService.GENERICACCESS_SERVICE_UUID));
        indicatePresence(presence_generic_attribute,MicroBit.getInstance().hasService(BleAdapterService.GENERICATTRIBUTE_SERVICE_UUID));
        indicatePresence(presence_device_information,MicroBit.getInstance().hasService(BleAdapterService.DEVICEINFORMATION_SERVICE_UUID));
        indicatePresence(presence_dfu_control,MicroBit.getInstance().hasService(BleAdapterService.DFUCONTROLSERVICE_SERVICE_UUID));
        indicatePresence(presence_accelerometer,MicroBit.getInstance().hasService(BleAdapterService.ACCELEROMETERSERVICE_SERVICE_UUID));
        indicatePresence(presence_magnetometer,MicroBit.getInstance().hasService(BleAdapterService.MAGNETOMETERSERVICE_SERVICE_UUID));
        indicatePresence(presence_button,MicroBit.getInstance().hasService(BleAdapterService.BUTTONSERVICE_SERVICE_UUID));
        indicatePresence(presence_io_pin,MicroBit.getInstance().hasService(BleAdapterService.IOPINSERVICE_SERVICE_UUID));
        indicatePresence(presence_led,MicroBit.getInstance().hasService(BleAdapterService.LEDSERVICE_SERVICE_UUID));
        indicatePresence(presence_event,MicroBit.getInstance().hasService(BleAdapterService.EVENTSERVICE_SERVICE_UUID));
        indicatePresence(presence_temperature,MicroBit.getInstance().hasService(BleAdapterService.TEMPERATURESERVICE_SERVICE_UUID));
        indicatePresence(presence_uart,MicroBit.getInstance().hasService(BleAdapterService.UARTSERVICE_SERVICE_UUID));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Back");
    }

    private void indicatePresence(TextView tv , boolean present) {
        Log.d(Constants.TAG, "indicatePresence: "+tv.getText().toString()+" "+present);
        if (present) {
            tv.setTextColor(Color.parseColor(Constants.SERVICE_PRESENT_COLOUR));
        } else {
            tv.setTextColor(Color.parseColor(Constants.SERVICE_ABSENT_COLOUR));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(Constants.TAG, "ServicesPresentActivity onOptionsItemSelected");
        switch (item.getItemId()) {
            case android.R.id.home:
                this.setResult(RESULT_OK);
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
