import imgRecogMod
from picamera.array import PiRGBArray
from picamera import PiCamera
import numpy as np 
import time
import cv2

make_img_brighter = True
debug = True
imgTaken = 0

#initialise camera
camera = PiCamera()
camera.resolution = (1024, 768)
camera.framerate = 30
rawCapture = PiRGBArray(camera, size=(1024, 768))
imgTaken = 0
make_img_brighter = True
time.sleep(0.1) #let camera warm up

def takePicture(debug):
	global imgTaken
	global make_img_brighter
	imgTaken += 1
	camera.capture(rawCapture, format="bgr", use_video_port=True)
	time.sleep(0.1)
	img = rawCapture.array
	if (make_img_brighter):
		hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
		value = 80
		h,s,v = cv2.split(hsv)
		lim = 255 - value
		v[v > lim] = 255
		v[v <= lim] += value
		final_hsv = cv2.merge((h, s, v))
		img = cv2.cvtColor(final_hsv, cv2.COLOR_HSV2BGR)
	if (debug):
		cv2.imwrite("debug/" + str(imgTaken) + "raw.png", img)
	rawCapture.truncate(0)
	return img


start_time = time.time()
img = takePicture(True)
print "time taken to take picture: "+ str(time.time() - start_time) + " sec"
result = imgRecogMod.ScanArrow(True, img)
print "time taken: "+ str(time.time() - start_time) + " sec"
print "result", result