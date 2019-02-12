#!/usr/bin/python
# -*- coding: utf-8 -*-
import paho.mqtt.client as mqtt
import subprocess
import datetime
import time
import smbus  # import SMBus module of I2C
from time import sleep  # import sleep
import math
import numpy as np

bus = smbus.SMBus(0x01)  # or bus = smbus.SMBus(0) for older version boards

# TMP007 address, 0x40(64)
# Select configuration register, 0x02(02)
# ........0x1540(5440)....Continuous Conversion mode, Comparator mode

data = [0x1540]
bus.write_i2c_block_data(0x40, 0x02, data)
time.sleep(0x01)


def read_raw_data(addr):

        # Read raw 16-bit value

    high = bus.read_byte_data(Device_Address, addr)
    low = bus.read_byte_data(Device_Address, addr + 0x01)

        # concatenate higher and lower value

    value = high << 8 | low

        # to get signed value from module

    if value > 32768:
        value = value - 65536
    return value


def on_message(client, userdata, message):
    print('Received message:{} on topic'.format(message.payload,
            message.topic))


client = mqtt.Client()
client.tls_set(ca_certs='mosquitto.org.crt', certfile='client.crt',
               keyfile='client.key')
client.connect('test.mosquitto.org', port=8884)

array_c = []
array_f = []

while True:

    data = bus.read_i2c_block_data(0x40, 0x01, 0x02)

        # Convert the data to 14-bits

    cTemp = (data[0] * 256 + (data[0x01] & 0xFC)) / 4
    if cTemp > 8191:
        cTemp -= 16384
    cel = cTemp * 0.03125
    far = cTemp * 1.8 + 32
    array_c = np.append(array_c, cel)
    array_f = np.append(array_f, far)

        # Output data to screen

    #print('Object Temperature in Celsius : %.2f C' %cel)
    #print('Object Temperature in Celsius : %.2f C' %far)
    
    time.sleep(0.01)

        # sleep(1)

    if len(array_c) == 100:
        average_c = np.mean(array_c)
        average_f = np.mean(array_f)
        array_c = []
        array_f = []
        temperature = {'Celsius': average_c, 'Farenheit': average_f}
        print('Object Temperature in Celsius : %.2f C' %average_c)
        print('Object Temperature in Fahrenheit : %.2f F' %average_f)

        #client.publish('IC.embedded/apollostark/test', temperature)

        # client.on_message = on_message
        # client.subscribe("IC.embedded/apollostark/#")

        #client.loop()
        #client.loop_start()
        #client.loop_stop()

        # while True:
            # time.sleep(3)
            # client.loop_forever()

        # client.disconnect()


			
