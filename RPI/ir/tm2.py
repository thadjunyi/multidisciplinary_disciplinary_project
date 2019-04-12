# Python program to illustrate  
# multiscaling in template matching 
import cv2 
import numpy as np 
import imutils
from picamera.array import PiRGBArray
from picamera import PiCamera
import argparse
import sys
import time

#image_recogition
global steps
steps = True
                
#initialise camera
cap = cv2.VideoCapture(0)
camera = PiCamera()
camera.resolution = (640, 480)
camera.framerate = 30
rawCapture = PiRGBArray(camera, size=(640, 480))
time.sleep(0.1)

#pre-process for template
up = cv2.imread("template/g_upArrow.jpg")
up = cv2.cvtColor(up, cv2.COLOR_BGR2GRAY)
up = cv2.Canny(up, 50, 200)

down = cv2.imread("template/g_downArrow.jpg")
down = cv2.cvtColor(down, cv2.COLOR_BGR2GRAY)
down = cv2.Canny(down, 50, 200)

left = cv2.imread("template/g_leftArrow.jpg")
left = cv2.cvtColor(left, cv2.COLOR_BGR2GRAY)
left = cv2.Canny(left, 50, 200)

right = cv2.imread("template/g_rightArrow.jpg")
right = cv2.cvtColor(right, cv2.COLOR_BGR2GRAY)
right = cv2.Canny(right, 50, 200)

templates=(up, down, left, right)
templatesName=("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow")
templatesSize=(up.shape[:2], down.shape[:2], left.shape[:2], right.shape[:2])
#(tH, tW) = template.shape[:2]
threshold = 0.31

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
	#gray = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11,2)
	ret,gray = cv2.threshold(gray,127,255,cv2.THRESH_BINARY_INV)
	#gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
	gray_dim = gray.shape[:2]
	cv2.rectangle(gray,(0, 0), (int(gray_dim[1]), int(gray_dim[0]/3)), (255, 255, 255), -1)
	#gray = gray[gray_dim[0]/3:gray_dim[0], 0:gray_dim[1]]
	found = None
				
	#multi-scaling
	for width in (640, 560, 480, 360):
		resized = imutils.resize(gray, width = width)
		r = gray.shape[1] / float(resized.shape[1])
		print width
		edged = cv2.Canny(resized, 50, 200)
		highestMaxVal =0 
		templateIndex =0
		counter =0
		tempResult=(0, (0,0), 0)
		#find the template above threshold
		for template in templates:
			result = cv2.matchTemplate(edged, templates[counter], cv2.TM_CCOEFF_NORMED)
			(_, maxVal, _, maxLoc) = cv2.minMaxLoc(result)
			print templatesName[counter]+ ': ' + str(maxVal)
			if maxVal > highestMaxVal and maxVal > threshold:
				highestMaxVal = maxVal
				tempResult = (maxVal, maxLoc, r)
				templateIndex = counter
			counter+=1

		if found is None or highestMaxVal > 0:
			found = tempResult
			if tempResult[0] > threshold:               
				steps = False
				print "matched"
				print tempResult[0]
				print tempResult[1]
				maxLoc = tempResult[1]
				(tH, tW) = templatesSize[templateIndex]
				cv2.putText(image, templatesName[templateIndex], (maxLoc[0]-3, maxLoc[1]-3), cv2.FONT_HERSHEY_SIMPLEX, 1, (0,255,0), 1, cv2.LINE_AA)
				cv2.rectangle(image,(int(maxLoc[0]*r), int(maxLoc[1]*r)), (int((maxLoc[0]+tW)*r), int((maxLoc[1]+tH)*r)), (0, 255, 0), 1)
				cv2.imwrite("Match.jpg", image)
				cv2.imwrite("gray.jpg", gray)
				print tW
				print tH
				print r
				#cv2.imshow("Match.jpg", image)
				#cv2.waitKey(0)
				break				
						
			#write to PC
			print "scanning..."
			rawCapture.truncate(0)
			#break
					
		key = cv2.waitKey(1) & 0xFF
		rawCapture.truncate(0)
			 
		# clear the stream in preparation for the next frame
				
			 
		# if the `q` key was pressed, break from the loop
		if key == ord("q"):
			rawCapture.truncate(0)
			break

	rawCapture.truncate(0)