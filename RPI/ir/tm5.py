# Python program to illustrate  
# multiscaling in contour matching with template matching 
import cv2 
import numpy as np 
import imutils
from picamera.array import PiRGBArray
from picamera import PiCamera
import argparse
import sys
import time
import os
                
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
ret,upt = cv2.threshold(up,127,255, cv2.THRESH_BINARY_INV)
up = cv2.Canny(up, 50, 200)

down = cv2.imread("template/g_downArrow.jpg")
down = cv2.cvtColor(down, cv2.COLOR_BGR2GRAY)
ret,downt = cv2.threshold(down,127,255, cv2.THRESH_BINARY_INV)
down = cv2.Canny(down, 50, 200)

left = cv2.imread("template/g_leftArrow.jpg")
left = cv2.cvtColor(left, cv2.COLOR_BGR2GRAY)
ret,leftt = cv2.threshold(left,127,255, cv2.THRESH_BINARY_INV)
left = cv2.Canny(left, 50, 200)

right = cv2.imread("template/g_rightArrow.jpg")
right = cv2.cvtColor(right, cv2.COLOR_BGR2GRAY)
ret,rightt = cv2.threshold(right,127,255, cv2.THRESH_BINARY_INV)
right = cv2.Canny(right, 50, 200)

cnt_Upimg, contours_up, hierachy_up = cv2.findContours(upt,cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
cnt_downimg, contours_down, hierachy_down = cv2.findContours(downt,cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
cnt_Leftimg, contours_left, hierachy_left = cv2.findContours(leftt,cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
cnt_Rightimg, contours_right, hierachy_right = cv2.findContours(rightt,cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

templates_cnt= (contours_up[0],contours_down[0],contours_left[0],contours_right[0])
#templates=(up, down, left, right)
templates=(up)
templatesName=("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow")
templatesSize=(up.shape[:2], down.shape[:2], left.shape[:2], right.shape[:2])
#(tH, tW) = template.shape[:2]

ret_threshold = 0.3
match_template_threshold = 0.2
contour_area_threshold = 1500

global steps

#take a pic
def takePicture():
	try:
		os.remove("C:/Desktop/ir/match_crop.png")
	except: pass
	camera.resolution = (1024, 768)
	#camera.resolution = (640, 480)
	camera.capture('raw_capture.png')
	img = cv2.imread('raw_capture.png')
	hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
	value = 80
	h,s,v = cv2.split(hsv)
	lim=255 - value
	v[v>lim] =255
	v[v <= lim] += value
	final_hsv = cv2.merge((h,s,v))
	img2 = cv2.cvtColor(final_hsv, cv2.COLOR_HSV2BGR)
	cv2.imwrite("raw_brighter.png", img2)
	return img

#called by main to take pic and scan for arrows
def ScanArrow(debug):
	found = False
	image = takePicture()
	gray_source = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
	ret,gray = cv2.threshold(gray_source,127,255,cv2.THRESH_BINARY_INV)
	gray_dim = gray.shape[:2]
	#cv2.rectangle(gray,(0, 0), (int(gray_dim[1]), int(gray_dim[0]/3)), (255, 255, 255), -1) #Mask out top portion

	#multi-scaling
	for width in (1024, 640, 560, 480):
		resized = imutils.resize(gray, width = width)
		r = gray.shape[1] / float(resized.shape[1])
		p(width, debug)
		edged = cv2.Canny(resized, 36,53)
		cv2.imwrite("canny.jpg", edged)

		#do contour matching (finding white object from black background)
		ret,thresh = cv2.threshold(resized,127,255, cv2.THRESH_BINARY_INV)
		cnt_img, contours, hierachy = cv2.findContours(thresh,1, cv2.CHAIN_APPROX_SIMPLE)

		cv2.imwrite("tm_contour_pc_vision.png", thresh)

		lowest_ret = 1
		lowest_template_index = 0
		lowest_match_contour = []
		lowest_area = 0
		#find the highest matched contour
		for cnt in contours:
			area = cv2.contourArea(cnt)
			if(area > contour_area_threshold):
				p("match area threshold" + str(area), debug)
				#print "match area threshold", area
				template_index = 0
				ret = cv2.matchShapes(cnt, templates_cnt[0], 1, 0.0)
				if (lowest_ret > ret):
					lowest_ret = ret
					lowest_template_index= template_index
					lowest_match_contour = cnt
					lowest_area = area
					p("  lower: index: " + str(template_index) + "," + str(ret), debug)
					#print("  lower: index: ", template_index, ret)
				template_index+=1

		#print("template index, ret, area: ", lowest_template_index, lowest_ret, lowest_area)
		p("  template index, ret: " + str(lowest_template_index) + "," + str(lowest_ret), debug)
		if (lowest_ret < ret_threshold):
			p( "ret threshold met", debug)
			p("area:" + str(lowest_area), debug)
			#Draw rect around best match contour
			min_x = min_y = 1000
			max_x = max_y = 0
			for a in lowest_match_contour:
				x,y,w,h = cv2.boundingRect(a)
            			min_x, max_x = min(x, min_x), max(x, max_x)
            			min_y, max_y = min(y, min_y), max(y, max_y)
        		min_y -= 5
        		min_x -= 5
       			max_x += 5
        		max_y += 5

			#crop matched area and do template matching
			try:
				p("cropped size: "+ str(max_x-min_x) + "x" + str(max_y - min_y), debug)
				crop_img = edged[min_y:max_y, min_x:max_x]
				#canny_crop_img = cv2.Canny(crop_img, 50, 200)
				cv2.imwrite("Match_crop.png", crop_img)
				i =0
				highest_max_val=0
				best_template_index=0
			
				result = cv2.matchTemplate(crop_img, up, cv2.TM_CCOEFF_NORMED)
				cv2.imwrite("Match_crop_temp.png", up)
				(_, maxVal, _, maxLoc) = cv2.minMaxLoc(result)
				#cv2.imwrite("Match_crop_"+templatesName[i]+".png", temp)
				p("max value: "+ str(maxVal), debug)
				if maxVal > highest_max_val and maxVal > match_template_threshold:
					found =True
					highest_max_val = maxVal
					best_template_index = i
				i += 1
			except Exception, e:
				p( "error in template match:" + str(e), debug)

			if found:
        			cv2.rectangle(image, (int(min_x*r), int(min_y*r)), (int(max_x*r), int(max_y*r)), (0, 0, 255), 2)
				cv2.putText(image, templatesName[best_template_index], (int(min_x*r), int(min_y*r)-15), cv2.FONT_HERSHEY_SIMPLEX, 1, (0,0,255), 2, cv2.LINE_AA)
				image = drawlines(image, width, r)
				cv2.imwrite("Match.png", image)
				cv2.imwrite("gray.png", gray)
				p("width of arrow: "+ str((max_x-min_x)*r), debug)
				#1 grid: 133-156
				#2 grid: 92-118
				#3 grid: 75-77
				p("top: "+ str(max_y*r), debug)
				p("btm: "+ str(min_y*r), debug)
				pos = int(max_x*r/int(width*r / 3))
				return "1|" + str(pos) + "|"+str(calculateGrid((max_x-min_x)*r))
	image = drawlines(image, width, r)
	cv2.imwrite("raw_capture.png", image)
	#status position distance
	return "0|0|0"

def drawlines(image, width, r):
	#draw
	segment = int(width*r / 3)
	cv2.line(image, (segment+16, 650), (segment+10 +88, 550), (0,225,0), 2)
	cv2.line(image, (segment*2 -47, 650), (segment*2 - 27 -90, 550), (0,225,0), 2)
	cv2.line(image, (segment +88, 550), (segment*2 -90, 550), (0,225,0), 2)
	cv2.line(image, (int(width/2*r)-31, int(768*r)), (int(width/2*r)-31, 0), (0,255,0), 2)
	cv2.rectangle(image, (segment, 0), (segment*2, int(768*r)), (0, 0, 255), 2)
	return image

def p(str, debug):
	if(debug):
		print(str)

def calculateGrid(width):
	if width >= 140 and width <223:
		return 1
	elif width >= 110 and width < 140:
		return 2
	elif width >= 86 and width <110:
		return 3
	else:
		return -1