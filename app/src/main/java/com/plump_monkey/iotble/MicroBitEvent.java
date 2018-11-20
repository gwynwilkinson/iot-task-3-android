package com.plump_monkey.iotble;

/*
 * Author: Martin Woolley
 * Twitter: @bluetooth_mdw
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

public class MicroBitEvent {

    private short event_type;
    private short event_value;

    public MicroBitEvent(short event_type, short event_value) {
        this.event_type = event_type;
        this.event_value = event_value;
    }

    public MicroBitEvent(byte [] event_bytes) {
        byte[] event_type_bytes = new byte[2];
        byte[] event_value_bytes = new byte[2];
        System.arraycopy(event_bytes, 0, event_type_bytes, 0, 2);
        System.arraycopy(event_bytes, 2, event_value_bytes, 0, 2);
        event_type = Utility.shortFromLittleEndianBytes(event_type_bytes);
        event_value = Utility.shortFromLittleEndianBytes(event_value_bytes);
    }

    public short getEvent_type() {
        return event_type;
    }

    public void setEvent_type(short event_type) {
        this.event_type = event_type;
    }

    public short getEvent_value() {
        return event_value;
    }

    public void setEvent_value(short event_value) {
        this.event_value = event_value;
    }

    public byte [] getEventBytesForBle() {
        byte[] event_type_bytes = new byte[2];
        byte[] event_value_bytes = new byte[2];
        byte[] event_bytes = new byte[4];
        event_type_bytes = Utility.leBytesFromShort(event_type);
        event_value_bytes = Utility.leBytesFromShort(event_value);
        System.arraycopy(event_type_bytes, 0, event_bytes, 0, 2);
        System.arraycopy(event_value_bytes, 0, event_bytes, 2, 2);
        return event_bytes;
    }
}
