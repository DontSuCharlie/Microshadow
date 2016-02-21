#!/usr/bin/python
import numpy as np
import cv2
import time

#opening text file to write to
coordinatesFile = open("coordinates.txt", "w+", 0)
#loading classifiers
while True:
     coordinatesFile = open("coordinates.txt", "w+", 0)
     time.sleep(0.1);#sleeps for 100ms
     face_cascade = cv2.CascadeClassifier('C:\Users\drago\Downloads\Binaries\opencv\sources\data\haarcascades\haarcascade_frontalface_default.xml')
     eye_cascade = cv2.CascadeClassifier('C:\Users\drago\Downloads\Binaries\opencv\sources\data\haarcascades\haarcascade_eye.xml')
     #reading file
     img = cv2.imread('C:/Users/drago/Github/Microshadow/frame.png')
     if img is None:
          continue
     gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
     faces = face_cascade.detectMultiScale(gray, 1.3, 3)
     for (x,y,w,h) in faces:
          rect = cv2.rectangle(img,(x,y),(x+w,y+h),(0,0,255),2)
          coordinatesFile.write(str(x+w/2))
          coordinatesFile.write(" ")
          coordinatesFile.write(str(y+h/2))
          coordinatesFile.write("\n")
          roi_gray = gray[y:y+h, x:x+w]
          roi_color = img[y:y+h, x:x+w]
          #eyes = eye_cascade.detectMultiScale(roi_gray)
          #for (ex,ey,ew,eh) in eyes:
          #     cv2.rectangle(roi_color,(ex,ey),(ex+ew,ey+eh),(0,255,0),2)
     cv2.imshow('img',img)
     cv2.waitKey(0)
     cv2.destroyAllWindows()          
     coordinatesFile.close();