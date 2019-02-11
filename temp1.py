import paho.mqtt.client as mqtt
import subprocess
import datetime
import time
import smbus		#import SMBus module of I2C
from time import sleep  #import sleep
import math
import numpy as np


bus = smbus.SMBus(1) 	# or bus = smbus.SMBus(0) for older version boards

# TMP007 address, 0x40(64)
# Select configuration register, 0x02(02)
#		0x1540(5440)	Continuous Conversion mode, Comparator mode
data = [0x1540]
bus.write_i2c_block_data(0x40, 0x02, data)
time.sleep(1)

def read_raw_data(addr):
		#Read raw 16-bit value
		high = bus.read_byte_data(Device_Address, addr)
		low = bus.read_byte_data(Device_Address, addr+1)

		#concatenate higher and lower value
		value = ((high << 8) | low)

		#to get signed value from module
		if(value > 32768):
			value = value - 65536
		return value

def on_message (client,userdata,message):
		print("Received message:{} on topic".format(message.payload, message.topic))
	

client = mqtt.Client()
client.tls_set(ca_certs="mosquitto.org.crt", certfile="client.crt",keyfile="client.key")
client.connect("test.mosquitto.org",port=8884)

while True:
                cTemp = 0
                fTemp = 0

		# Convert the data to 14-bits
                for i in range(100):
                    data = bus.read_i2c_block_data(0x40, 0x01, 2)
                    dataTemp = ((data[0] * 256 + (data[1] & 0xFC)) / 4)
                    if dataTemp > 8191 :
			dataTemp -= 16384
                        cTemp = cTemp + dataTemp * 0.03125
                        fTemp = fTemp + dataTemp * 1.8 + 32
		# Output data to screen
                cTemp_avg = cTemp/100
                fTemp_avg = fTemp/100
                
                print ("Object Temperature in Celsius : %.2f C" %cTemp_avg)
                print ("Object Temperature in Farenheit : %.2f F" %fTemp_avg)
		time.sleep(1)

		
		#sleep(1)

		client.publish("IC.embedded/apollostark/test", cTemp_avg)
                client.publish("IC.embedded/apollostark/test", fTemp_avg)

		#client.on_message = on_message
		#client.subscribe("IC.embedded/apollostark/#")
		client.loop()
		client.loop_start()
		client.loop_stop()

		#while True:
			#time.sleep(3)
			#client.loop_forever()

		#client.disconnect()
		print("Disconnected!")