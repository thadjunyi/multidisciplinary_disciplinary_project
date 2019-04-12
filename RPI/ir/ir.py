import numpy as np
import cv2

subject_img = cv2.imread('2.jpg', 0)
upArrow = cv2.imread("template/upArrow.png", 0)
downArrow = cv2.imread("template/downArrow.png", 0)
leftArrow = cv2.imread("template/leftArrow.png", 0)
rightArrow = cv2.imread("template/rightArrow.png", 0)

ret,thresh = cv2.threshold(subject_img,127,255,1)

subject_img,contours,h = cv2.findContours(thresh,1,2)
#initialise min max
min_x = min_y = 1000
max_x = max_y = 0

for cnt in contours:
    approx = cv2.approxPolyDP(cnt,0.01*cv2.arcLength(cnt,True),True)
    print 'approx'
    print approx
    if len(approx)==7:
        print "arrow"
	cv2.drawContours(subject_img,[cnt],-1,(0,255,0),3)
        cv2.drawContours(subject_img,[approx],-1,(0,0,255),3)
        for a in approx:
            x,y,w,h = cv2.boundingRect(a)
            min_x, max_x = min(x, min_x), max(x, max_x)
            min_y, max_y = min(y, min_y), max(y, max_y)
        min_y -= 5
        min_x -= 5
        max_x += 5
        max_y += 5
        cv2.rectangle(subject_img, (min_x, min_y), (max_x, max_y), (0, 255, 0), 2)
        break
cv2.imwrite('contours.jpg', subject_img)
#crop out image
crop_img = subject_img[min_y:max_y, min_x:max_x]
#crop_img = cv2.cvtColor(crop_img, cv2.COLOR_BGR2GRAY)
crop_img = cv2.resize(crop_img,(88,88))
cv2.imwrite('crop.jpg', crop_img)




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