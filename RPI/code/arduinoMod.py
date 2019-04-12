#!/usr/bin/python
# -*- coding: utf-8 -*-
import serial
import time
from colors import *


class arduinoComm(object):

	def __init__(self):
		self.port = '/dev/ttyACM0'
		self.baurd_rate = 115200
		self.arduino_connected = False

	def connect_arduino(self):
		print 'Waiting for Arduino serial connection..'
		while True:
			retry = False
			try:
				self.ser = serial.Serial(self.port, self.baurd_rate)
				cprint(BOLD + GREEN, 'Connected to Arduino!')
				self.arduino_connected = True
				retry = False
			except Exception, e:
				#print 'ARDUINO-Conn Error: %s' % str(e)
				retry = True
			if (not retry):
				break
			#print 'Retrying Arduino connection..'
			time.sleep(1)

	def arduino_is_connected(self):
		return self.arduino_connected
			
	def disconnect_arduino(self):
		if self.ser:
			self.ser.close()
			self.arduino_connected = False

	def read_from_arduino(self):
		try:
			self.ser.flush()
			get_data = self.ser.readline()
			print 'Transmission from Arduino: %s' % get_data.rstrip()
			return get_data
		except Exception, e:
			print 'ARDUINO-Recv Error: %s' % str(e)
			if ('Input/output error' in str(e)):
				self.disconnect_arduino()
				cprint(BOLD + RED, 'Assuming Arduino disconnected, trying to reconnect..')
				self.connect_arduino()

	def write_to_arduino(self, message):
		try:
			if (not self.arduino_connected):
				cprint(BOLD + RED, 'Arduino not connected! Unable to transmit.')
				return
			message = message.encode('utf-8')
			self.ser.write(message)
			#print 'Transmitted to Arduino: %s' % message
		except Exception, e:

			print 'ARDUINO-Send Error: %s' % str(e)
