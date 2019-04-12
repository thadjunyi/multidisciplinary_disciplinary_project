import numpy as np
import cv2
from picamera.array import PiRGBArray
from picamera import PiCamera
import time

upArrow = cv2.imread("template/g_upArrow.jpg", 0)
downArrow = cv2.imread("template/g_downArrow.jpg", 0)
leftArrow = cv2.imread("template/g_leftArrow.jpg", 0)
rightArrow = cv2.imread("template/g_rightArrow.jpg", 0)

#initialise camera
cap = cv2.VideoCapture(0)
camera = PiCamera()
camera.resolution = (640, 480)
camera.framerate = 30
rawCapture = PiRGBArray(camera, size=(640, 480))
time.sleep(0.1)


#frame by frame capture
for frame in camera.capture_continuous(rawCapture, format="bgr", use_video_port=True):

	rawCapture.truncate(0)
	print('scanning...')
	#print "steps outside"
	image = frame.array
	gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
	gray_dim = gray.shape[:2]
	cv2.rectangle(gray,(0, 0), (int(gray_dim[1]), int(gray_dim[0]/3)), (255, 255, 255), -1)
	#gray = gray[gray_dim[0]/3:gray_dim[0], 0:gray_dim[1]]
	found = None

	#ret,thresh = cv2.threshold(gray,127,255,cv2.THRESH_TRUNC)
	#ret,thresh = cv2.threshold(gray,127,255, 0)
	#ret,thresh = cv2.threshold(gray,127,255,cv2.THRESH_BINARY)
	thresh1 = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11,2)
	ret,thresh = cv2.threshold(thresh1,127,255, 0)
	gray,contours,h = cv2.findContours(thresh,1,2)

	#initialise min max
	min_x = min_y = 1000
	max_x = max_y = 0

	for cnt in contours:
    		approx = cv2.approxPolyDP(cnt,0.01*cv2.arcLength(cnt,True),True)
    		if len(approx)==7:
        		print "arrow"
			found = '1'
        		cv2.drawContours(gray,[cnt],-1,(255,0,0),1)
        		for a in approx:
            			x,y,w,h = cv2.boundingRect(a)
            			min_x, max_x = min(x, min_x), max(x, max_x)
            			min_y, max_y = min(y, min_y), max(y, max_y)
        		min_y -= 5
        		min_x -= 5
        		max_x += 5
        		max_y += 5
        		cv2.rectangle(gray, (min_x, min_y), (max_x, max_y), (0, 255, 0), 2)
        		cv2.imwrite('c_original.jpg', image)
			cv2.imwrite('contours.jpg', gray)
			#crop out image
			crop_img = gray[min_y:max_y, min_x:max_x]
			#crop_img = cv2.cvtColor(crop_img, cv2.COLOR_BGR2GRAY)
			crop_img = cv2.resize(crop_img,(88,88))
			cv2.imwrite('crop.jpg', crop_img)
			break
	if(found == '1'):
		break;
	rawCapture.truncate(0)


err = np.sum((crop_img.astype("float") - upArrow.astype("float")) ** 2)
err /= float(upArrow.shape[0] * crop_img.shape[1])
print err

err = np.sum((crop_img.astype("float") - downArrow.astype("float")) ** 2)
err /= float(downArrow.shape[0] * crop_img.shape[1])
print err

err = np.sum((crop_img.astype("float") - leftArrow.astype("float")) ** 2)
err /= float(leftArrow.shape[0] * crop_img.shape[1])
print err

err = np.sum((crop_img.astype("float") - rightArrow.astype("float")) ** 2)
err /= float(rightArrow.shape[0] * crop_img.shape[1])
print err