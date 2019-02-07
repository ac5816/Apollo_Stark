import RPi.GPIO as GPIO
import time

servoPIN = 17
GPIO.setmode(GPIO.BCM)
GPIO.setup(servoPIN, GPIO.OUT)

p = GPIO.PWM(servoPIN, 50) # GPIO 17 for PWM with 50Hz
p.start(2.5) # Initialization
delay = 0.5
try:
	while True:
		p.ChangeDutyCycle(5)
		time.sleep(delay)
		#p.ChangeDutyCycle(0)
		#time.sleep(delay)
		p.ChangeDutyCycle(25)
		time.sleep(delay)
		#p.ChangeDutyCycle(0)
		#time.sleep(delay)
		p.ChangeDutyCycle(50)
		time.sleep(delay)
		#p.ChangeDutyCycle(0)
		#time.sleep(delay)
except KeyboardInterrupt:
	p.stop()
	GPIO.cleanup()