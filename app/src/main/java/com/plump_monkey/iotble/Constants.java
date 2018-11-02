package com.plump_monkey.iotble;

public class Constants {
    public static final String TAG = "IoTBLE";
    public static final String URI = "URI";
    public static final String NO_HELP = "file:///android_res/raw/no_help.html";
    public static final String MAIN_HELP = "file:///android_res/raw/main_help.html";
    public static final String MAIN_ABOUT = "file:///android_res/raw/main_about.html";
    public static final String ACCELEROMETER_HELP = "file:///android_res/raw/accelerometer_help.html";
    public static final String ANIMALS_HELP = "file:///android_res/raw/animals_help.html";
    public static final String BUTTON_HELP = "file:///android_res/raw/button_help.html";
    public static final String DEVICE_INFORMATION_HELP = "file:///android_res/raw/device_information_help.html";
    public static final String HRM_HELP = "file:///android_res/raw/hrm_help.html";
    public static final String IO_DIGITAL_OUTPUT_HELP = "file:///android_res/raw/io_digital_output_help.html";
    public static final String LEDS_HELP = "file:///android_res/raw/leds_help.html";
    public static final String MAGNETOMETER_HELP = "file:///android_res/raw/magnetometer_help.html";
    public static final String TEMPERATURE_ALARM_HELP = "file:///android_res/raw/temperature_alarm_help.html";
    public static final String SQUIRREL_COUNTER_HELP = "file:///android_res/raw/squirrel_counter_help.html";
    public static final String MENU_HELP = "file:///android_res/raw/menu_help.html";
    public static final String CONTROLLER_HELP = "file:///android_res/raw/dual_d_pad_controller_help.html";
    public static final String UART_AVM_HELP = "file:///android_res/raw/uart_avm_help.html";
    public static final String TRIVIA_HELP = "file:///android_res/raw/trivia_help.html";

    public static final String FIND_PAIRED = "FIND PAIRED BBC MICRO:BIT(S)";
    public static final String FIND_ANY = "FIND BBC MICRO:BIT(S)";
    public static final String NO_PAIRED_FOUND = "No paired micro:bits found. Have you paired? See Help in menu";
    public static final String NONE_FOUND = "No advertising micro:bits found in range";
    public static final String FIND_HRM_MONITORS = "Find heart rate monitors";
    public static final String STOP_SCANNING = "Stop Scanning";

    public static final long GATT_OPERATION_TIME_OUT = 5000;
    public static final long CONNECTION_KEEP_ALIVE_FREQUENCY = 10000;

    public static final short MICROBIT_EVENT_VALUE_ANY = 0000;

    public static final short MICROBIT_EVENT_TYPE_TEMPERATURE_ALARM = 9000;
    public static final short MICROBIT_EVENT_TYPE_TEMPERATURE_SET_LOWER = 9001;
    public static final short MICROBIT_EVENT_TYPE_TEMPERATURE_SET_UPPER = 9002;
    public static final short MICROBIT_EVENT_VALUE_TEMPERATURE_HOT = 0001;
    public static final short MICROBIT_EVENT_VALUE_TEMPERATURE_COLD = 0002;

    public static final short MICROBIT_EVENT_TYPE_COUNTER = 9003;
    public static final short MICROBIT_EVENT_VALUE_BUTTON_A = 0000; // number of times button A has been pressed

    public static final short MICROBIT_EVENT_TYPE_TRIVIA = 1300;

    public static final String SERVICE_PRESENT_COLOUR = "#228B22";
    public static final String SERVICE_ABSENT_COLOUR = "#FF0000";

    public static final String AVM_CORRECT_RESPONSE = "GOT IT!!";

}
