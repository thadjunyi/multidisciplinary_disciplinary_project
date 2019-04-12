import numpy as np
import cv2
upArrow = cv2.imread("upArrow.png", 0)
downArrow = cv2.imread("downArrow.png", 0)
leftArrow = cv2.imread("leftArrow.png", 0)
rightArrow = cv2.imread("rightArrow.png", 0)

#thresh1 = cv2.adaptiveThreshold(upArrow, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11,2)
ret,thresh = cv2.threshold(upArrow,127,255, cv2.THRESH_BINARY_INV)
upArrow,contours,h = cv2.findContours(thresh,1,2)
cv2.imwrite('g_upArrow.jpg', upArrow)

#thresh1 = cv2.adaptiveThreshold(downArrow, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11,2)
ret,thresh = cv2.threshold(downArrow,127,255, cv2.THRESH_BINARY_INV)
downArrow,contours,h = cv2.findContours(thresh,1,2)
cv2.imwrite('g_downArrow.jpg', downArrow)

#thresh1 = cv2.adaptiveThreshold(leftArrow, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11,2)
ret,thresh = cv2.threshold(leftArrow,127,255, cv2.THRESH_BINARY_INV)
leftArrow,contours,h = cv2.findContours(thresh,1,2)
cv2.imwrite('g_leftArrow.jpg', leftArrow)

#thresh1 = cv2.adaptiveThreshold(rightArrow, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11,2)
ret,thresh = cv2.threshold(rightArrow,127,255, cv2.THRESH_BINARY_INV)
rightArrow,contours,h = cv2.findContours(thresh,1,2)
cv2.imwrite('g_rightArrow.jpg', rightArrow)