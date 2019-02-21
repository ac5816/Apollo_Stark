import paho.mqtt.client as mqtt
import subprocess
from datetime import datetime
import time
import smbus
from time import sleep
import math
import json
import RPi.GPIO as GPIO

#Settings for using buzzer
buzzPIN = 17
GPIO.setmode(GPIO.BCM)
GPIO.setup(buzzPIN,GPIO.OUT)

p = GPIO.PWM(buzzPIN, 50)
p.start(0)
delay = 0.5

#Connections of I2C
bus = smbus.SMBus(1)
#Default Threshold
threshold = 38.0

data = [0x1540]
time.sleep(1)
		
def on_message(client,userdata,message):
	global threshold
	threshold = float(message.payload.decode("utf-8")) # Gets threshold from the MQTT server sent from the app

#Instantiating MQTT client	
client = mqtt.Client()
#Mqtt connection for demo:
#client.connect("ee-estott-octo.ee.ic.ac.uk",port=1883)

#Setting up encrypted client certificate connections
client.tls_set(ca_certs="mosquitto.org.crt", certfile="client.crt",keyfile="client.key")
client.connect("test.mosquitto.org",port=8884)

while True:
	data = bus.read_i2c_block_data(0x40,0x01,2)
	#Convert data into 14bits
	cTemp = ((data[0] * 256 + (data[1] & 0xFC)) / 4)
	if (cTemp > 8191) :
		cTemp -= 16384
	cTemp = cTemp * 0.03125
	fTemp = cTemp * 1.8 + 32
	# Output data to screen
	print ("Object Temperature in Celsius : %.2f C" %cTemp)
	time.sleep(1)
	
	#Publish temperature readings
	client.publish("IC.embedded/apollostark/temperature", "{:.2f}".format(cTemp))
	
	#Subscribe to topic for threshold input from user
	client.subscribe("IC.embedded/apollostark/threshold")
	
	#Invoke function call everytime threshold is set
	client.on_message=on_message
	print(threshold) #For debugging: print on terminal
	
	
	if (cTemp >= threshold):
		p.ChangeDutyCycle(50)	#sends a 50% pulse to the microbuzzer to sound it when >threshold
		time.sleep(delay)
	else:
		p.ChangeDutyCycle(0)	#sends no pulse to the microbuzzer when <threshold

	client.loop()
	client.loop_start()
	client.loop_stop()
	
© 2019 GitHub, Inc.
