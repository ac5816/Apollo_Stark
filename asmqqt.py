import paho.mqtt.client as mqtt
import subprocess
import datetime
import time

client = mqtt.Client()
client.tls_set(ca_certs="mosquitto.org.crt", certfile="client.crt",keyfile="client.key")
client.connect("test.mosquitto.org",port=8884)

client.publish("IC.embedded/apollostark/test", "test_msg")

def on_message (client,userdata,message):
	print("Received message:{} on topic".format(message.payload, message.topic))
	

client.on_message = on_message
client.subscribe("IC.embedded/apollostark/#")
client.loop()
client.loop_start()

while True:
	time.sleep(3)
	#client.loop_forever()

client.disconnect()
print("Disconnected!")


