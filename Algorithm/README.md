# MDP Algo Group 9 

Algorithm code for NTU CZ3004 Multi-disciplinary Design Project.


Branches:

* master
  * exploration with full calibration and image recognition.
  * select tasks in GUI:
    * "Exploration" for right-wall-hugging exploration with image taking
    (but do not go out and take image after fully explored) -- Week 9 Leaderboard
    * "Fastest Path" for performing fastest path with explored map
    * "Image Rec" for exhausted exploration with the robot going out from Start Zone 
    to capture all accessible surfaces after fully explored the maze. -- Week 10 Leaderboard
* image_disabled
  * comment out imageRecognitionRight function to avoid sending image rec command to RPI 
  for faster timing (diff ~ 10+ sec as RPI can transmit msg faster without image rec)
* reduced_alignment
  * on top of image_disabled, remove turnRightAndAlign with obstacle blocks 
  for faster timing (diff ~ 10+ sec)
  * minor changes in RWH reverse case
  * Week 11 Leaderboard
* min_alignment
  * on top of reduced_alignment, remove turnRightAndAlign for RWH right and front not movable case
  for faster timing (diff ~ 10+ sec)