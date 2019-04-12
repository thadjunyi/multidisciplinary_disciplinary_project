# Python program to illustrate  
# multiscaling contour matching with template matching 
import cv2 
import numpy as np 
import imutils
import argparse
import sys
import time
import os

imgTaken = 0
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
templatesName=("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow")
templatesSize=(up.shape[:2], down.shape[:2], left.shape[:2], right.shape[:2])


############################################################################
#threshold settings

rectangle_top_masking_height = 300
rectangle_btm_masking_height = 100
ret_threshold = 0.3
match_template_threshold = 0.2
contour_area_threshold = 4000

############################################################################
imgTaken = 0
#called by main to take pic and scan for arrows
def ScanArrow(debug, image):
	found = False
	global imgTaken
	imgTaken += 1
	gray_source = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
	ret,gray = cv2.threshold(gray_source,127,255,cv2.THRESH_BINARY_INV)
	gray_dim = gray.shape[:2]
	cv2.rectangle(gray,(0, 0), (int(gray_dim[1]), int(rectangle_top_masking_height)), (255, 255, 255), -1) #Mask out top portion
	cv2.rectangle(gray,(0, int(gray_dim[0])-rectangle_btm_masking_height), (int(gray_dim[1]), int(gray_dim[0])), (255, 255, 255), -1) #Mask out btm portion
	#multi-scaling
	for width in (1024, 640, 560, 480):
		resized = imutils.resize(gray, width = width)
		r = gray.shape[1] / float(resized.shape[1])
		p("Img_"+ str(imgTaken), debug)
		p(width, debug)
		edged = cv2.Canny(resized, 36,53)
		saveImg(debug, "tm6_canny.jpg", edged)

		#do contour matching (finding white object from black background)
		ret,thresh = cv2.threshold(resized,127,255, cv2.THRESH_BINARY_INV)
		cnt_img, contours, hierachy = cv2.findContours(thresh,1, cv2.CHAIN_APPROX_SIMPLE)

		saveImg(debug, "tm6_contour_pc_vision.png", thresh)

		lowest_ret = 1
		lowest_template_index = 0
		lowest_match_contour = []
		lowest_area = 0
		#find the highest matched contour
		for cnt in contours:
			area = cv2.contourArea(cnt)
			if(area > contour_area_threshold):
				p("match area threshold" + str(area), debug)
				template_index = 0
				ret = cv2.matchShapes(cnt, templates_cnt[0], 1, 0.0)
				if (lowest_ret > ret):
					lowest_ret = ret
					lowest_template_index= template_index
					lowest_match_contour = cnt
					lowest_area = area
					p("  lower: index: " + str(template_index) + "," + str(ret), debug)
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
				if(crop_img.size == 0):
					print("cropped img is empty")
					p("cropped img is empty", debug)
					continue
				saveImg(debug, "tm6_Matched_crop.png", crop_img)
				i =0
				highest_max_val=0
				best_template_index=0
			
				result = cv2.matchTemplate(crop_img, up, cv2.TM_CCOEFF_NORMED)
				(_, maxVal, _, maxLoc) = cv2.minMaxLoc(result)
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
				saveImg(debug, "tm6_Match.png", image)
				p("width of arrow: "+ str((max_x-min_x)*r), debug)
				#1 grid: 140 - 223
				#2 grid: 110 - 140
				#3 grid: 86 - 110
				p("top: "+ str(max_y*r), debug)
				p("btm: "+ str(min_y*r), debug)
				pos = calculatePos(max_x, r, width)
				if(pos==3):
					return "0|0|0"
				grid=calculateGrid((max_x-min_x)*r)
				if(grid==-1):
					return "0|0|0"
				return "1|" + str(pos) + "|"+str(grid)
	image = drawlines(image, width, r)
	saveImg(debug, "tm6_raw_capture.png", image)
	#status position distance
	return "0|0|0"

def drawlines(image, width, r):
	#draw
	segment = int(width*r / 3)
	cv2.line(image, (segment+16, 650), (segment+10 +88, 550), (0,225,0), 2) #left
	cv2.line(image, (segment*2 -47, 650), (segment*2 - 27 -90, 550), (0,225,0), 2) #right
	cv2.line(image, (segment+10 +88, 550), (segment*2 - 27 -90, 550), (0,225,0), 2) #LR join
	#cv2.line(image, (int(width/2*r)-31, int(768*r)), (int(width/2*r)-31, 0), (0,255,0), 2)
	cv2.rectangle(image, (segment, 0), (segment*2, int(768*r)), (0, 0, 255), 2)
	return image

def p(str, debug):
	if(debug):
		print(str)

def saveImg(debug, filename, img):
	global imgTaken
	if (debug):
		cv2.imwrite("debug/"+ str(imgTaken)  +filename, img)

def calculateGrid(width):
	print("width:" + str(width))
	if width >= 140 and width <220:
		return 1
	elif width >= 105 and width < 140:
		return 2
	elif width >= 86 and width <105:
		return 3
	else:
		return -1

def calculatePos(max_x, r, width):
	return int(max_x*r/int(width / 3))
