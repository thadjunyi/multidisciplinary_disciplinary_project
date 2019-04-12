import sys
import time
import Queue
import threading
import json

from arduinoMod import *
from bluetoothMod import *
from pcMod import *
import cv2
import imgRecogMod
from Queue import Queue
from colors import *

from picamera.array import PiRGBArray
from picamera import PiCamera
import numpy as np

class Main(threading.Thread):
	def __init__(self):
		threading.Thread.__init__(self)

		self.debug = False
		
		#self.arrowQueue = Queue()
		
		#PiCamera initialization
		#self.camera = PiCamera()
		#self.camera.resolution = (1024, 768)
		#self.camera.framerate = 30
		#self.rawCapture = PiRGBArray(self.camera, size=(1024, 768))
		#self.imgTaken = 0
		#self.make_img_brighter = True
		
		#initialize all the thread with each component class file
		self.pc_thread = pcComm()
		self.arduino_thread = arduinoComm()
		self.bluetooth_thread = bluetoothComm()

		#Call each thread connection
		#self.pc_thread.init_pc_comm()
		#self.arduino_thread.connect_arduino()
		#self.bluetooth_thread.init_bluetooth_comm()
		
		pcInitThread = threading.Thread(target = self.pc_thread.init_pc_comm, name = "pc_init_thread")
		arduinoInitThread = threading.Thread(target = self.arduino_thread.connect_arduino, name = "ar_init_thread")
		bluetoothInitThread = threading.Thread(target = self.bluetooth_thread.init_bluetooth_comm, name = "bt_init_thread")
		pcInitThread.daemon = True
		arduinoInitThread.daemon = True
		bluetoothInitThread.daemon = True
		pcInitThread.start()
		arduinoInitThread.start()
		bluetoothInitThread.start()
		time.sleep(1)
		
		while not (self.pc_thread.pc_is_connected() and self.arduino_thread.arduino_is_connected() and self.bluetooth_thread.bt_is_connected):
			time.sleep(0.1)
	
	def takePicture(self, debug):
		self.imgTaken += 1
		self.camera.capture(self.rawCapture, format="bgr", use_video_port=True)
		time.sleep(0.1)
		img = self.rawCapture.array
		if (self.make_img_brighter):
			hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
			value = 80
			h,s,v = cv2.split(hsv)
			lim = 255 - value
			v[v > lim] = 255
			v[v <= lim] += value
			final_hsv = cv2.merge((h, s, v))
			img = cv2.cvtColor(final_hsv, cv2.COLOR_HSV2BGR)
		if (debug):
			cv2.imwrite("debug/" + str(self.imgTaken) + "raw.png", img)
		self.rawCapture.truncate(0)
		return img
	
	def arrowRecognition(self):
		try:
			while True:
				if not (self.arrowQueue.empty()):
					thisJob = self.arrowQueue.get()
					print("ArrowRecog: Processing job X=" + str(thisJob[0]) + ", Y=" + str(thisJob[1]) + ", Face=" + str(thisJob[2]))
					detect = imgRecogMod.ScanArrow(self.debug, thisJob[3])
					print("ArrowRecog: Detection results %s" % detect)
					detectSplit = detect.split('|')
					detectSplit = [int(i) for i in detectSplit]
					if (detectSplit[0] == 1):
						direction = self.determineArrowDirection(thisJob[2])
						position = self.determineArrowPosition(thisJob[0], thisJob[1], thisJob[2], detectSplit[1], detectSplit[2])
						cprint(BOLD + GREEN, "Arrow at: X=" + str(position[0]) + ", Y=" + str(position[1]) + ", direction=" + direction)
						detectResult = self.parse_json(position[0], position[1], direction.lower())
						self.writeBluetooth(detectResult + "\r\n")
				time.sleep(0.1)
		except Exception as e:
			print("main/arrowRecog Error: %s" % str(e))
	
	def parse_json(self, x, y, direction):
		data = {"x": x, "y": y, "face": direction}
		to_send = {"arrow": [data]}
		return json.dumps(to_send)
	
	def determineArrowDirection(self, face):
		if (face == 0):
			return 'DOWN'
		elif (face == 1):
			return 'UP'
		elif (face == 2):
			return 'RIGHT'
		elif (face == 3):
			return 'LEFT'
		return 'UNKNOWN'
		
	def determineArrowPosition(self, robotX, robotY, robotFace, scanSection, scanGridDistance):
		if (robotFace == 0):
			if (scanSection == 0):
				return [robotX-1, robotY+scanGridDistance]
			elif (scanSection == 1):
				return [robotX, robotY+scanGridDistance]
			elif (scanSection == 2):
				return [robotX+1, robotY+scanGridDistance]
		elif (robotFace == 1):
			if (scanSection == 0):
				return [robotX+1, robotY-scanGridDistance]
			elif (scanSection == 1):
				return [robotX, robotY-scanGridDistance]
			elif (scanSection == 2):
				return [robotX-1, robotY-scanGridDistance]
		elif (robotFace == 2):
			if (scanSection == 0):
				return [robotX-scanGridDistance, robotY-1]
			elif (scanSection == 1):
				return [robotX-scanGridDistance, robotY]
			elif (scanSection == 2):
				return [robotX-scanGridDistance, robotY+1]
		elif (robotFace == 3):
			if (scanSection == 0):
				return [robotX+scanGridDistance, robotY+1]
			elif (scanSection == 1):
				return [robotX+scanGridDistance, robotY]
			elif (scanSection == 2):
				return [robotX+scanGridDistance, robotY-1]
		return [0, 0]
	
	# create functions for PC
	def writePC(self, messageToPC):
		self.pc_thread.write_to_PC(messageToPC)
		print("Data transmitted to PC: %s \r\n" % messageToPC.rstrip())
		
	def processMsg(self, readPCMessage):
		if (readPCMessage is None):
			return
		readPCMessage = readPCMessage.lstrip()
		if (len(readPCMessage) == 0):
			return
		if(readPCMessage[0].upper() == 'B'):
			print("Data passed from PC to Bluetooth: %s" % readPCMessage[1:].rstrip())
			self.writeBluetooth(readPCMessage[1:])
		elif(readPCMessage[0].upper() == 'A'):
			print("Data passed from PC to Arduino: %s" % readPCMessage[1:].rstrip())
			self.writeArduino(readPCMessage[1:] + "\r\n")
		elif(readPCMessage[0].upper() == 'I'):
			img = self.takePicture(self.debug)
			msgSplit = readPCMessage[1:].split('|')
			if (msgSplit[2].upper().rstrip() == 'UP'):
				robotFace = 0
			elif (msgSplit[2].upper().rstrip() == 'DOWN'):
				robotFace = 1
			elif (msgSplit[2].upper().rstrip() == 'LEFT'):
				robotFace = 2
			elif (msgSplit[2].upper().rstrip() == 'RIGHT'):
				robotFace = 3
				
			newCmd = [int(msgSplit[0]), int(msgSplit[1]), robotFace, img]
			self.arrowQueue.put(newCmd)
			print("Scanning for arrow at: X=" + str(newCmd[0]) + ", Y=" + str(newCmd[1]) + ", Face=" + msgSplit[2].upper().rstrip())
		else:
			print("Incorrect header from PC (expecting 'B' for Android, 'A' for Arduino or 'I' for Image Recog.): [%s]" % readPCMessage[0])

	def readPC(self):
		try:
			while True:
				readPCMessage = self.pc_thread.read_from_PC()
				if (readPCMessage is None):
					continue
				readPCMessage = readPCMessage.split('\n')
				for msg in readPCMessage:
					self.processMsg(msg)
		except Exception as e:
			print("main/PC-Recv Error: %s" % str(e))


	def writeBluetooth(self, messageToBT):
		self.bluetooth_thread.write_to_bluetooth(messageToBT)
		print("Data transmitted to Bluetooth: %s" % messageToBT.rstrip())

	def readBluetooth(self):
		while True:
			retry = False
			try:
				while True:
					readBTMessage = self.bluetooth_thread.read_from_bluetooth()
					if (readBTMessage is None):
						continue
					readBTMessage = readBTMessage.lstrip()
					if (len(readBTMessage) == 0):
						continue
					if (readBTMessage[0].upper() == 'X'):
						#time.sleep(0.1)
						print("Data passed from Bluetooth to PC: %s" % readBTMessage[1:].rstrip())
						self.writePC(readBTMessage[1:]+ "\r\n")

					elif (readBTMessage[0].upper() == 'A'):
						#time.sleep(0.1)
						print("Data passed from Bluetooth to Arduino: %s" % readBTMessage[1:].rstrip())
						self.writeArduino(readBTMessage[1:]+"\r\n")
					#elif(readBTMessage[0].upper() == 'I'):
					#	self.checkArrow = 2
					#	print("Data flag set for image recognition!")
					else:
						print("Incorrect header from Bluetooth (expecting 'X' for PC, 'A' for Arduino): [%s]" % readBTMessage[0])
			except Exception as e:
				print("main/BT-Recv Error: %s" % str(e))
				retry = True
			if (not retry):
				break
			self.bluetooth_thread.disconnect_bluetooth()
			print 'Re-establishing bluetooh connection..'
			self.bluetooth_thread.init_bluetooth_comm()

	def writeArduino(self, messageToAr):
		self.arduino_thread.write_to_arduino(messageToAr)
		print("Data transmitted to Arduino: %s \r\n" % messageToAr.rstrip())

	def readArduino(self):
		try:
			while True:
				readArduinoMsg = self.arduino_thread.read_from_arduino()
				if (readArduinoMsg is None):
					continue
				readArduinoMsg = readArduinoMsg.lstrip()
				if (len(readArduinoMsg) == 0):
					continue
				if (readArduinoMsg[0].upper() == 'X'):
					print("Data passed from Arduino to PC: %s" % readArduinoMsg[1:].rstrip())
					self.writePC(readArduinoMsg[1:] + "\r\n")
				elif (readArduinoMsg[0].upper() == 'B'):
					print("Data passed from Arduino to Bluetooth: %s" % readArduinoMsg[1:].rstrip())
					self.writeBluetooth(readArduinoMsg[1:])
		except socket.error as e:
			print("Arduino disconnected!") 


	def initialize_threads(self):
		self.readPCThread = threading.Thread(target = self.readPC, name = "pc_read_thread")
		self.readArduinoThread = threading.Thread(target = self.readArduino, name = "ar_read_thread")
		self.readBTThread = threading.Thread(target = self.readBluetooth, name = "bt_read_thread")
		#self.arrowRecogThread = threading.Thread(target = self.arrowRecognition, name = "arrow_recog_thread")
		
		self.readPCThread.daemon = True
		self.readArduinoThread.daemon = True
		self.readBTThread.daemon = True
		#self.arrowRecogThread.daemon = True
		print ("All daemon threads initialized successfully!")

		self.readPCThread.start()
		self.readArduinoThread.start()
		self.readBTThread.start()
		#self.arrowRecogThread.start()
		print ("All daemon threads started successfully!")

	def close_all_sockets(self):
		pc_thread.close_all_sockets()
		arduino_thread.close_all_sockets()
		bluetooth_thread.close_all_sockets()
		print ("All threads killed!")

	def keep_main_alive(self):
		while(1):
			if not (self.readPCThread.is_alive()):
				cprint(BOLD + RED, 'Fatal: PC thread is not running!')
			if not (self.readArduinoThread.is_alive()):
				cprint(BOLD + RED, 'Fatal: Arduino thread is not running!')
			if not (self.readBTThread.is_alive()):
				cprint(BOLD + RED, 'Fatal: Bluetooth thread is not running!')
			#if not (self.arrowRecogThread.is_alive()):
			#	cprint(BOLD + RED, 'Fatal: ArrowRecog thread is not running!')
			#	self.arrowRecogThread = threading.Thread(target = self.arrowRecognition, name = "arrow_recog_thread")
			#	self.arrowRecogThread.daemon = True
			#	self.arrowRecogThread.start()
			#	cprint(BOLD + BLUE, 'Resolution: ArrowRecog thread has been restarted.')
			time.sleep(1)


if __name__ == "__main__":
	mainThread = Main()
	mainThread.initialize_threads()
	mainThread.keep_main_alive()
	mainThread.close_all_sockets()
