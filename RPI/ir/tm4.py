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
#up_img = cv2.imread("template/g_upArrow.jpg")
#cnt = contours_up[0]
#cv2.drawContours(up_img, [cnt], 0, (0,0,255), 3)
#cv2.imwrite("up_contour_image.png", up_img)

print "cnt area:"
print cv2.minAreaRect(contours_up[0])
print cv2.contourArea(contours_down[0])
print cv2.contourArea(contours_left[0])
print cv2.contourArea(contours_right[0])
templates_cnt= (contours_up[0],contours_down[0],contours_left[0],contours_right[0])
templates=(up, down, left, right)
templatesName=("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow")
templatesSize=(up.shape[:2], down.shape[:2], left.shape[:2], right.shape[:2])
#(tH, tW) = template.shape[:2]
threshold = 0.03
match_template_threshold = 0.34
found = 0

#frame by frame capture
for frame in camera.capture_continuous(rawCapture, format="bgr", use_video_port=True):
	global steps
	if found:
		break
	if steps != True:
		rawCapture.truncate(0)
		continue;

	image = frame.array
	gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
	#gray = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11,2)
	ret,gray = cv2.threshold(gray,127,255,cv2.THRESH_BINARY_INV)
	#gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
	gray_dim = gray.shape[:2]
	cv2.rectangle(gray,(0, 0), (int(gray_dim[1]), int(gray_dim[0]/3)), (255, 255, 255), -1)
	#gray = gray[gray_dim[0]/3:gray_dim[0], 0:gray_dim[1]]
				
	#multi-scaling
	for width in (640, 560, 480, 360):
		resized = imutils.resize(gray, width = width)
		r = gray.shape[1] / float(resized.shape[1])
		print width
		edged = cv2.Canny(resized, 0, 100)
		cv2.imwrite("edged.jpg", edged)

		#do contour matching (finding white object from black background)
		ret,thresh = cv2.threshold(resized,127,255, cv2.THRESH_BINARY_INV)
		cnt_img, contours, hierachy = cv2.findContours(thresh,1, cv2.CHAIN_APPROX_SIMPLE)

		cv2.imwrite("tm_contour_pc_vision.png", thresh)

		lowest_ret = 1
		lowest_template_index = 0
		lowest_match_contour = []
		
		for cnt in contours:
			template_index = 0;
			for tmp in templates_cnt:
				ret = cv2.matchShapes(cnt, tmp, 1, 0.0)
				#print("index, ret", template_index, ret)
				if lowest_ret > ret:
					lowest_ret = ret
					lowest_template_index= template_index
					lowest_match_contour = cnt
					print("  lower: index: ", template_index, ret)
				template_index+=1
		#cv2.drawContours(tmp_thresh, lowest_match_contour, -1, (0,0,255), 3)
		#cv2.imwrite("tm_contour_pc_vision_w_cnt.png", tmp_thresh)
		#img_cnt = image
		#cv2.drawContours(img_cnt, lowest_match_contour, -1, (0,0,255), 3)
		#cv2.imwrite("tm_contour_img.png", image)
		print("template index, ret: ", lowest_template_index, lowest_ret)
		
		if (lowest_ret < threshold):
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
			crop_img = thresh[min_y:max_y, min_x:max_x]
			canny_crop_img = cv2.Canny(crop_img, 0, 100)
			cv2.imwrite("Match_crop.png", canny_crop_img)
			i =0
			highest_max_val=0
			best_template_index=0
			for temp in templates:
				try:
					result = cv2.matchTemplate(canny_crop_img, temp, cv2.TM_CCOEFF_NORMED)
					(_, maxVal, _, maxLoc) = cv2.minMaxLoc(result)
					cv2.imwrite("Match_crop_"+templatesName[i]+".png", temp)
					print maxVal
					if maxVal > highest_max_val and maxVal > match_template_threshold:
						found =1
						highest_max_val = maxVal
						best_template_index = i
					i += 1
				except:
					print "error in template match"

			if found:
        			cv2.rectangle(image, (int(min_x*r), int(min_y*r)), (int(max_x*r), int(max_y*r)), (0, 0, 255), 2)
				cv2.putText(image, templatesName[best_template_index], (int(min_x*r), int(min_y*r)-15), cv2.FONT_HERSHEY_SIMPLEX, 1, (0,0,255), 2, cv2.LINE_AA)
				cv2.imwrite("Match.png", image)
				cv2.imwrite("gray.png", gray)
				break
		
		print "scanning..."	
		key = cv2.waitKey(1) & 0xFF
		rawCapture.truncate(0)
			 
		# clear the stream in preparation for the next frame
				
			 
		# if the `q` key was pressed, break from the loop
		if key == ord("q"):
			rawCapture.truncate(0)
			break

	rawCapture.truncate(0)