import paho.mqtt.client as mqtt
import subprocess
import datetime
import time

'''
    Find Heading by using HMC5883L interface with Raspberry Pi using Python
	http://www.electronicwings.com
'''
import smbus		#import SMBus module of I2C
from time import sleep  #import sleep
import math

#some MPU6050 Registers and their Address
Register_A     = 0              #Address of Configuration register A
Register_B     = 0x01           #Address of configuration register B
Register_mode  = 0x02           #Address of mode register

X_axis_H    = 0x03              #Address of X-axis MSB data register
Z_axis_H    = 0x05              #Address of Z-axis MSB data register
Y_axis_H    = 0x07              #Address of Y-axis MSB data register
declination = -0.296706            #define declination angle of location where measurement going to be done
pi          = 3.14159265359     #define pi value


def Magnetometer_Init():
		#write to Configuration Register A
		bus.write_byte_data(Device_Address, Register_A, 0x70)

		#Write to Configuration Register B for gain
		bus.write_byte_data(Device_Address, Register_B, 0xa0)

		#Write to mode Register for selecting mode
		bus.write_byte_data(Device_Address, Register_mode, 0)
	
	

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


bus = smbus.SMBus(1) 	# or bus = smbus.SMBus(0) for older version boards
Device_Address = 0x1e   # HMC5883L magnetometer device address

Magnetometer_Init()     # initialize HMC5883L magnetometer 

print (" Reading Heading Angle")

def on_message (client,userdata,message):
		print("Received message:{} on topic".format(message.payload, message.topic))
	

client = mqtt.Client()
client.tls_set(ca_certs="mosquitto.org.crt", certfile="client.crt",keyfile="client.key")
client.connect("test.mosquitto.org",port=8884)

while True:
		#Read Accelerometer raw value
		x = read_raw_data(X_axis_H)
		z = read_raw_data(Z_axis_H)
		y = read_raw_data(Y_axis_H)

		heading = math.atan2(y, x) + declination
        
		#Due to declination check for >360 degree
		if(heading > 2*pi):
				heading = heading - 2*pi

		#check for sign
		if(heading < 0):
				heading = heading + 2*pi

		#convert into angle
		heading_angle = int(heading * 180/pi)

		print ("Heading Angle = %d°" %heading_angle)
		sleep(1)
		
		

		client.publish("IC.embedded/apollostark/test", heading_angle)

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
		




