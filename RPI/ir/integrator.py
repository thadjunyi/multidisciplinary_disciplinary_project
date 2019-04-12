import sys
import time
import Queue
import threading
from pc_comm import *
from bt_comm import *
from sr_comm import *
from picamera.array import PiRGBArray
from picamera import PiCamera
import time
import cv2
import imutils
import numpy as np
import argparse


class Main(threading.Thread):
##        global direction_msg
##        direction_msg = ""
	global steps
	steps = True

	def __init__(self):
		threading.Thread.__init__(self)

		self.pc_thread = PcAPI() #create an instance of this class
		self.bt_thread = AndroidAPI()
		self.sr_thread = SerialAPI()

		# Initialize the connections
		self.pc_thread.init_pc_comm()
		self.bt_thread.connect_bluetooth()
		self.sr_thread.connect_serial()
		time.sleep(2)							# wait for 2 secs before starting


	# PC Functions
	def writePC(self, msg_to_pc):					# Write to PC. Invoke write_to_PC()
		self.pc_thread.write_to_PC(msg_to_pc)
		print "WritePC: Sent to PC: %s" % msg_to_pc

	def readPC(self):								# Read from PC. Invoke read_from_PC() and send data according to header
		print "Inside readPC"
		while True:
			read_pc_msg = self.pc_thread.read_from_PC()
			read_pc_array = read_pc_msg.split(';')
			i = len(read_pc_array)

			for x in range(i):
				#counter = counter + 1
				# Check header for destination and strip out first char
				if read_pc_array[x] is not "":
					if(read_pc_array[x][0].lower() == 'c'):		# send to android & robot
						self.writeBT(read_pc_array[x][1:])		# strip the header
						self.writeSR(read_pc_array[x][1:])		# strip the header
						print "value received from PC: %s" % read_pc_array[x][1:]
							
					elif(read_pc_array[x][0].lower() == 'a'):		# send to android
						self.writeBT(read_pc_array[x][1:])		# strip the header
						print "value received from PC: %s" % read_pc_array[x][1:]

					elif(read_pc_array[x][0].lower() == 'r'):	# send to robot
						self.writeSR(read_pc_array[x][1:])		# strip the header
						print "value received from PC: %s" % read_pc_array[x][1:]
							
					elif(read_pc_array[x][0].lower() == 'x'):
					# send to rpi
						global steps
						steps = True		# strip the header
						print "value received from RPI: %s" % read_pc_array[x][1:]

					else:
						print "incorrect header received from PC: [%s]" % read_pc_array[x][0]
						time.sleep(1)
                                                

				
	# Android/BT functions
	def writeBT(self, msg_to_bt):					# Write to BT. Invoke write_to_bt()
		self.bt_thread.write_to_bt(msg_to_bt)
		print "Value sent to Android: %s" % msg_to_bt

	def readBT(self):								#Read from BT. Invoke read_from_bt() and send data to PC
		print "Inside readBT"
		while True:
			read_bt_msg = self.bt_thread.read_from_bt()

			# Check header and send data to PC
			if(read_bt_msg[0].lower() == 'b'):		# Send to PC & robot
				self.writePC(read_bt_msg[1:])		# Strip the header
				self.writeSR(read_bt_msg[1:])		# Strip the header
				print "Value received from Android: %s" % read_bt_msg[1:]
			elif(read_bt_msg[0].lower() == 'p'):		# Send to PC
				self.writePC(read_bt_msg[1:])		# Strip the header 
				print "Value received from Android: %s" % read_bt_msg[1:]

	#### this case can be commented out ####
			# elif(read_bt_msg[0].lower() == 'h'):	# send to robot
			# 	self.writeSR(read_bt_msg[1:])		# strip the header
			# 	print "value received from BT: %s" % read_bt_msg[1:]

			else:
				print "incorrect header received from BT: [%s]" % read_bt_msg[0]
				time.sleep(1)

	# Serial Comm functions
	def writeSR(self, msg_to_sr):					# Write to robot. Invoke write_to_serial()
		self.sr_thread.write_to_serial(msg_to_sr)
		print "Value sent to arduino: %s" % msg_to_sr

	def readSR(self):								# Read from robot. Invoke read_from_serial() and send data to PC
		print "Inside readSR"
		while True:
			read_sr_msg = self.sr_thread.read_from_serial()

			# Write straight to PC without any checking
			self.writePC(read_sr_msg)
			print "value received from arduino: %s" % read_sr_msg	
			# time.sleep(1)
	
	#image_recogition
	def arrow_recog(self):
                
		#initialise camera
		cap = cv2.VideoCapture(0)
		camera = PiCamera()
		camera.resolution = (640, 480)
		camera.framerate = 30
		rawCapture = PiRGBArray(camera, size=(640, 480))
		time.sleep(0.1)

		#pre-process for template
		template = cv2.imread("template.jpg")
		template = cv2.cvtColor(template, cv2.COLOR_BGR2GRAY)
		template = cv2.Canny(template, 50, 200)
		(tH, tW) = template.shape[:2]
		threshold = 0.3

		#frame by frame capture
		for frame in camera.capture_continuous(rawCapture, format="bgr", use_video_port=True):
			global steps
	
			if steps != True:
					#print "steps inside"
					rawCapture.truncate(0)
					continue;

                        #print "steps outside"
			image = frame.array
			gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
			found = None
				
			#multi-scaling
			for width in (640, 560, 480, 360):
				resized = imutils.resize(image, width = width)
				r = gray.shape[1] / float(resized.shape[1])

				edged = cv2.Canny(resized, 50, 200)
				result = cv2.matchTemplate(edged, template, cv2.TM_CCOEFF_NORMED)
				(_, maxVal, _, maxLoc) = cv2.minMaxLoc(result)

				if found is None or maxVal > found[0]:
					found = (maxVal, maxLoc, r)

				if found[0] > threshold:               
					steps = False
										
					#write to PC
					self.writePC("pArrow")
					rawCapture.truncate(0)
					break
					
				key = cv2.waitKey(1) & 0xFF
				rawCapture.truncate(0)
			 
				# clear the stream in preparation for the next frame
				
			 
				# if the `q` key was pressed, break from the loop
				if key == ord("q"):
					rawCapture.truncate(0)
					break

			rawCapture.truncate(0)

#### Remember to comment this out and use direct communication with PC


	# 		# Check header and send data to PC
	# 		if(read_sr_msg[0].lower() == 'p'):	# send to PC
	# 			self.writePC(read_sr_msg[1:])	# strip the header
	# 			print "value written to PC from SR: %s" % read_sr_msg[1:]

	# ##### this can be commented out ####
	# 		elif(read_bt_msg[0].lower() == 'a'):	# send to BT
	# 			self.writeBT(read_sr_msg[1:])		# strip the header
	# 			print "value written to BT from SR: %s" % read_sr_msg[1:]

	# 		else:
	# 			print "incorrect header received from SR: [%s]" % read_sr_msg[0]
	# 			time.sleep(1)

		
	def initialize_threads(self):

		# PC read and write thread
		readpc = threading.Thread(target = self.readPC, name = "pc_read_thread")
		# print "created readpc"
		writepc = threading.Thread(target = self.writePC, args = ("",), name = "pc_write_thread")
		# print "created writepc"

##		# Bluetooth (BT) read and write thread
		readbt = threading.Thread(target = self.readBT, name = "bt_read_thread")
##		# print "created readbt"
		writebt = threading.Thread(target = self.writeBT, args = ("",), name = "bt_write_thread")
##		# print "created writebt"

		# Robot (SR) read and write thread
		readsr = threading.Thread(target = self.readSR, name = "sr_read_thread")
		# print "created readsr"
		writesr = threading.Thread(target = self.writeSR, args = ("",), name = "sr_write_thread")
		# print "created writesr"
		
		startvid = threading.Thread(target = self.arrow_recog, name = "start_vid")

		# Set threads as daemons
		readpc.daemon = True
		writepc.daemon = True
##
		readbt.daemon = True
		writebt.daemon = True

		readsr.daemon = True
		writesr.daemon = True

		startvid.daemon = True 

		print "All threads initialized successfully"


		# Start Threads
		readpc.start()
		writepc.start()

		readbt.start()
		writebt.start()

		readsr.start()
		writesr.start()
		
		startvid.start()
	
		# print "Starting rt and wt threads"


	def close_all_sockets(self):					# Close all sockets
		pc_thread.close_all_pc_sockets()
		bt_thread.close_all_bt_sockets()
		sr_thread.close_all_sr_sockets()
		
		print "end threads"

	def keep_main_alive(self):						# Allows for a Ctrl+C kill while keeping the main() alive
		while True:
			time.sleep(1) 


if __name__ == "__main__":
	test = Main()
	test.initialize_threads()
	test.keep_main_alive()
	test.close_all_sockets()
