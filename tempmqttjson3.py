import paho.mqtt.client as mqtt
import subprocess
from datetime import datetime
import time
import smbus
from time import sleep
import math
import json
import RPi.GPIO as GPIO

buzzPIN = 17
GPIO.setmode(GPIO.BCM)
GPIO.setup(buzzPIN,GPIO.OUT)

p = GPIO.PWM(buzzPIN, 50)
p.start(0)
delay = 0.5
bus = smbus.SMBus(1)
threshold = 40.0

data = [0x1540]
time.sleep(1)
		
def on_message(client,userdata,message):
	global threshold
	threshold = float(message.payload.decode("utf-8"))
	#print("Received message:{} on topic".format(message.payload,message.topic))
	

	
client = mqtt.Client()
#client.tls_set(ca_certs="mosquitto.org.crt", certfile="client.crt",keyfile="client.key")
client.connect("ee-estott-octo.ee.ic.ac.uk",port=1883)

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
	
	client.publish("IC.embedded/apollostark/temperature", "{:.2f}".format(cTemp))
	client.subscribe("IC.embedded/apollostark/threshold")
	client.on_message=on_message
	print(threshold)
	
	if (cTemp >= threshold):
		p.ChangeDutyCycle(50)
		time.sleep(delay)
	else:
		p.ChangeDutyCycle(0)

	client.loop()
	client.loop_start()
	client.loop_stop()
	
	




	
	
	
