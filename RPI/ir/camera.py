
import cv2 
import numpy as np 
import imutils
from picamera.array import PiRGBArray
from picamera import PiCamera
import picamera
import argparse
import sys
import time

with picamera.PiCamera() as camera:
	camera.resolution=(1024, 768)
	camera.start_preview(fullscreen =False, window=(100, 100, 256, 192))
	time.sleep(2)
	camera.preview.window=(200,200, 256, 192)
	time.sleep(2)
	camera.preview.window=(0,0, 512, 384)



