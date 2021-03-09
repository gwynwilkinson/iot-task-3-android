# IoT - Task 3 Group Assignment - Android Code

## Group Members

- Jonathan White (ID: 93003016)
- Gwyn Wilkinson (ID: 09028091)


## Description

<center>![image](/uploads/301b3ff6ada6cd74dead2339fae8a1ca/image.png)</center>

This assignment implements a simple authentication system that provides pin protected device access and encrypted data communication between an Android phone and a Micro:bit, using Bluetooth LE.

The sending device will be a custom Android app which will utilise the BLE UART service to send encrypted commands to the Micro:bit using a protocol message structure.

The Micro:bit will receive the encrypted text and then prompt the user for a pin code input. The pin code and the per-session salt will be hashed using SHA-1 and used as the decryption key for the received message.

## Code Locations

Android Client code repository:- https://github.com/gwynwilkinson/iot-task-3-android

Micro:bit code repository:- https://github.com/gwynwilkinson/iot-task-3

## How to Build

Using Android Studio 3.2, clone the project from git hub and import into Android Studio. Use 'Run->Run app' to install the apk onto the connected Android device.

## Installing the apk

Alternatively, the APK can be manually installed if the device is set to allow installation from unapproved sources in the security settings. Download the APK from the APK directory in the respository and install

## Android Code Description

The Android application is derived from the example Micro:bit BLE Android application by Martin Woolley (https://github.com/microbit-foundation/microbit-blue ). The original application showcases the Micro:bit Bluetooth capabilities with various displays such as the Accelerometer and Temperature sensors and an Animal-Vegetable-Mineral game which uses the UART service. 
The original application provides Open-Source Bluetooth Low Energy device discovery and connection libraries which have been reused for the new custom application. The irrelevant parts of the original application have been removed, and the UartActivity class has been heavily modified to support the new functionality

The bulk of the changes were made to app\src\main\java\com\plump_monkey\iotble\UartActivity.java

The process flow of this activity is:

![image](/uploads/9e60f64f1f4c0ed39f43a846c1ab4ef5/image.png)

