#include <MsTimer2.h>
#include <EnableInterrupt.h>
#include <DualVNH5019MotorShield.h>
#include <PID_v1.h>

const int LEFT_PULSE = 3;           // M1 - LEFT motor pin number on Arduino board
const int RIGHT_PULSE = 11;         // M2 - RIGHT motor pin number on Arduino board
const int MOVE_FAST_SPEED = 395;    //if target distance more than 30cm 
const int MOVE_MAX_SPEED = 350;     //if target distance more than 60cm

const int TURN_MAX_SPEED = 350;     //change this value to calibrate turning. If the rotation overshoots, decrease the speed 
const int ROTATE_MAX_SPEED = 380;   //used in rotateLeft() and rotateRight()

int TURN_TICKS_L = 689;       //change this left encoder ticks value to calibrate left turn 
int TURN_TICKS_R = 692;       //change this right encoder ticks value to calibrate right turn 
int TURN_TICKS_L_180 = 1500;
int TURN_TICKS_R_180 = 1493;

//TICKS[0] for general cm -> ticks calibration. 
//TICKS[1-9] with specific distance (by grids) e.g. distance=5, TICKS[5] 
//Grids           1     2     3     4     5     6     7     8    9  
int TICKS[9] = {475, 1075, 1672, 2290, 2890, 3495, 4095, 4685, 5295};  // for movement of each grid
const int LEFTTICK[14] = {20, 25, 30, 35, 40, 360, 50, 55, 489, 65, 70, 75, 80, 85};
const int RIGHTTICK[14] = {20, 25, 30, 35, 40, 313, 50, 55, 450, 65, 70, 75, 80, 85};
const double kp =  8, ki = 5, kd = 0.00;

// for PID
double tick_R = 0;
double tick_L = 0;
double speed_O = 0;

//motor declaration as 'md' e.g. to set motor speed ==> md.setSpeeds(Motor1, Motor2)
DualVNH5019MotorShield md;  //our motor is Pololu Dual VNH5019 Motor Driver Shield 
PID myPID(&tick_R, &speed_O, &tick_L, kp, ki, kd, REVERSE);

//--------------------------Motor Codes-------------------------------
void setupMotorEncoder() {
  md.init();
  pinMode(LEFT_PULSE, INPUT);
  pinMode(RIGHT_PULSE, INPUT);
  enableInterrupt(LEFT_PULSE, leftMotorTime, CHANGE); //Enables interrupt on a left motor (M1) - enable interrupt basically enables the interrupt flag and enables Interrupt service routines to run
  enableInterrupt(RIGHT_PULSE, rightMotorTime, CHANGE); //Enables interrupt on a left motor (M1)
}

// not used
void stopMotorEncoder() {
  disableInterrupt(LEFT_PULSE);
  disableInterrupt(RIGHT_PULSE);
}

void setupPID() {
  myPID.SetMode(AUTOMATIC);
  myPID.SetOutputLimits(-400, 400);   // change this value for PID calibration. This is the maximum speed PID sets to
  myPID.SetSampleTime(5);
}

// when forward command is received, taking in the parameter of how many cm it should move
void moveForward(int distancee) {
  initializeTick();   // set all tick to 0
  initializeMotor_Start();  // set motor and brake to 0
  int distance = cmToTicks(distancee); // convert grid movement to tick value
  double currentSpeed = 0;
  boolean initialStatus = true;

  if (distancee == 10) {    // if number of tick to move < 60, move at a slower speed of 200
    currentSpeed = MOVE_MAX_SPEED;
  } else {                // if number of tick to move >= 60, move at the max speed of 350
    currentSpeed = MOVE_FAST_SPEED;
  }

  //error checking feedback in a while loop
  while (tick_R <= distance || tick_L <= distance) {
    if (distancee == 10)
    {
      if (myPID.Compute()) {
        if (initialStatus) {
          //md.setSpeeds(0, currentSpeed - speed_O);
          md.setSpeeds(currentSpeed + speed_O, 0);
          delay(5);
          initialStatus = false;
        }
        md.setSpeeds(currentSpeed + speed_O, currentSpeed - speed_O);
      }
    }
    else    // for distancee >= 20
    {
      if (myPID.Compute()) {
        if (initialStatus) {
          //md.setSpeeds(0, currentSpeed - speed_O);
          md.setSpeeds(currentSpeed + 2*speed_O, 0);
          delay(7);         
          initialStatus = false;
        }
        md.setSpeeds(currentSpeed + 2*speed_O, currentSpeed);
      }
    }
  }
  if (distancee == 10)
  {/*
    initializeMotor2_End();  //brakes the motor
    initializeTick();   // set all tick to 0
    initializeMotor_Start();  // set motor and brake to 0
    while (tick_R < 3) { // -15
        if (myPID.Compute())
        {
          md.setSpeeds(0, currentSpeed - speed_O);
        }
      }*/
      initializeMotor2_End();   //brakes the motor
  }
  else
  {/*
    initializeMotorFront_End();  //brakes the motor
    initializeTick();   // set all tick to 0
    initializeMotor_Start();  // set motor and brake to 0
    while (tick_R < 5) { // -15
        if (myPID.Compute())
        {
          md.setSpeeds(currentSpeed + speed_O, 0);
        }
      }*/
    initializeMotor3_End();   //brakes the motor
  }
}

// when backward command is received, taking in the parameter of how many cm it should move
void moveBackwards(int distance) {
  initializeTick();
  initializeMotor_Start();
  distance = cmToTicks(distance);
  double currentSpeed = MOVE_MAX_SPEED;
  boolean initialStatus = true;
   
  //error checking feedback in a while loop
  while (tick_R <= distance || tick_L <= distance) {
    if (myPID.Compute()) {
      if (initialStatus) {
        //md.setSpeeds(-(currentSpeed + speed_O), 0);
        //delay(5);
        initialStatus = false;
      }
      md.setSpeeds(-(currentSpeed + speed_O), -(currentSpeed - speed_O));
    }
  }
  initializeMotorBack_End();  //brakes the motor
}

// when left command is received, taking in the parameter of how much angle it should rotate anticlockwise
void turnLeft(int angle) {
  int i=0;    // for loop iterator
  double currentSpeed = TURN_MAX_SPEED;

  if (angle >= 90) {
    for (i = 0; i<angle; i+=90) {
      initializeTick();
      initializeMotor_Start();
      while (tick_R < TURN_TICKS_L || tick_L < TURN_TICKS_L) { // -15
        if (myPID.Compute())
          md.setSpeeds(-(currentSpeed + speed_O), currentSpeed - speed_O);
      }
      initializeMotorTurn_End();   //brakes the motor
    }
  }
  if (angle - i > 0) {
    turnLeftDeg(angle-i);
  }
  initializeMotorTurn_End();   //brakes the motor
}


// will turn <90 degree
void turnLeftDeg(int angle) {
  int index = (angle-20)/5; // index is the index no. of the LEFTTICKS array of size 14
  int tick;
  if (index <= 14)
    tick = LEFTTICK[index];
    
  initializeMotor_Start();
  double currentSpeed = TURN_MAX_SPEED;
  initializeTick();
  while (tick_R < tick || tick_L < tick) {
    if (myPID.Compute())
      md.setSpeeds(-(currentSpeed + speed_O), currentSpeed - speed_O);
  }
}

// when right command is received, taking in the parameter of how much angle it should rotate clockwise
void turnRight(int angle) {
  int i=0;    // for loop iterator
  double currentSpeed = TURN_MAX_SPEED;

  if (angle >= 90) {
    for (i=0; i<angle; i=i+90) {
      initializeTick();
      initializeMotor_Start();
      while (tick_R < (TURN_TICKS_R) || tick_L < (TURN_TICKS_R)) { // -16
        if (myPID.Compute())
          md.setSpeeds((currentSpeed + speed_O), -(currentSpeed - speed_O));
      }
    }
  }
  if (angle - i > 0) {
    turnRightDeg(angle-i);
  }
  initializeMotorTurn_End();   //brakes the motor
}

// will turn <90 degree
void turnRightDeg(int angle) {
  int index = (angle-20)/5; // index is the index no. of the LEFTTICKS array of size 14
  int tick;
  if (index <= 14)
    tick = RIGHTTICK[index];

  initializeMotor_Start();
  double currentSpeed = TURN_MAX_SPEED;
  initializeTick();
  while (tick_R < tick || tick_L < tick) {
    if (myPID.Compute())
      md.setSpeeds((currentSpeed + speed_O), -(currentSpeed - speed_O));
  }
}

//for enableInterrupt() function, to add the tick for counting 
void leftMotorTime() {
  tick_L++;
}

//for enableInterrupt() function, to add the tick for counting
void rightMotorTime() {
  tick_R++;
}

// to reset/initialize the ticks and speed for PID
void initializeTick() {
  tick_R = 0;
  tick_L = 0;
  speed_O = 0;
}

// to reset/initialize the motor speed and brakes for PID
void initializeMotor_Start() {
  md.setSpeeds(0, 0);
  md.setBrakes(0, 0);
}

// brakes when moving forward (please revise) - thad
void initializeMotorFront_End() {
  md.setSpeeds(0, 0);
  //md.setBrakes(400, 400);

  for (int i = 200; i <400; i+=50) {
    md.setBrakes(i*1.01, i);
    delay(10);
  }
  
  delay(50);
}

// brakes when moving forward (please revise) - thad
void initializeMotor_End() {
  md.setSpeeds(0, 0);
  //md.setBrakes(400, 400);
  
  for (int i = 200; i <400; i+=50) {
    md.setBrakes(i, i);
    delay(10);
  }
  
  delay(50);
}

// brakes when moving forward (please revise) - thad
void initializeMotor2_End() {
  md.setSpeeds(0, 0);
  //md.setBrakes(400, 400);
  
  for (int i = 200; i <400; i+=50) {
    md.setBrakes(i*1.05, i);
    delay(20);
  }
  
  delay(50);
}

// brakes when moving forward (please revise) - thad
void initializeMotor3_End() {
  md.setSpeeds(0, 0);
  md.setBrakes(400, 400);

  delay(50);
}

// brakes when moving backward (please revise) - thad
void initializeMotorBack_End() {
  md.setSpeeds(0, 0);
  //md.setBrakes(400, 400);
  
  for (int i = 200; i <400; i+=50) {
    md.setBrakes(i*1.07, i);
    delay(10);
  }
  
  delay(50);
}

// brakes when turning left/right (please revise) - thad
void initializeMotorTurn_End() {
  md.setSpeeds(0, 0);
  //md.setBrakes(400, 400);
  
  for (int i = 200; i <400; i+=50) {
    md.setBrakes(i, i);
    delay(6);
  }
  delay(50);
}

// converting distance (cm) to ticks
int cmToTicks(int cm) {
  int dist = (cm / 10) - 1; //dist is the index no. of the TICKS array of size 10
  if (dist < 10)
    return TICKS[dist]; //TICKS[10] = {545, 1155, 1760, 2380, 2985, 3615, 4195, 4775, 5370};
  return 0;
}

/*
// printing movement message to android
void printMessage(String directionString, int value) {
  if (directionString.equals("forward") || directionString.equals("back")) {
    Serial.print("B{\"status\":[{\"status\":\"moving ");
    Serial.print(directionString);
    Serial.print(" by: ");
  } else if (directionString.equals("left") || directionString.equals("right")) {
    Serial.print("B{\"status\":[{\"status\":\"turning ");
    Serial.print(directionString);
    Serial.print(" by angle of: ");
  } else
    return;
  Serial.print(value);
  Serial.print("\"}]}");
  Serial.print("\n");
}*/

//use this function to check RPM of the motors
void testRPM(int M1Speed, int M2Speed){
  md.setSpeeds(M1Speed, M2Speed);  //setSpeeds(Motor1, Motor2)
  delay(1000);
  Serial.println(tick_R/562.25 * 60 );
  Serial.println(tick_L/562.25 * 60);
  tick_R = 0;
  tick_L = 0;
}

//to avoid 1x1 obstacle
void avoid(){

  while(1){
    moveForward(1*10);
    
    int frontIR1 = (int)getFrontIR1();
    int frontIR2 = (int)getFrontIR2();
    int frontIR3 = (int)getFrontIR3();
    int rightIR1 = (int)getRightIR1();
    int rightIR2 = (int)getRightIR2();
    int leftIR1 = (int)getLeftIR1();

    int flag = 0;
        
    if(frontIR2 == 1){  //Obstacle is in front of Front Center sensor
      Serial.println("Obstacle Detected by Front Center Sensor");
      delay(500);
      //turnRight(90);
      turnLeft(90);
      delay(500);
      moveForward(2*10);
      delay(500);
      //turnLeft(90);
      turnRight(90);
      delay(500);
      moveForward(4*10);
      delay(500);
      //turnLeft(90);
      turnRight(90);
      delay(500);
      moveForward(2*10);
      delay(500);
      //turnRight(90);
      turnLeft(90);
    }
    else if(frontIR1 == 1 && frontIR2 != 1){ //Obstacle is in front of Front Left sensor
      Serial.println("Obstacle Detected by Front Left Sensor");
      delay(500);
      turnRight(90);
      delay(500);
      moveForward(1*10);
      delay(500);
      turnLeft(90);
      delay(500);
      moveForward(4*10);
      delay(500);
      turnLeft(90);
      delay(500);
      moveForward(1*10);
      delay(500);
      turnRight(90);
    }
    else if(frontIR3 == 1 && frontIR2 != 1){ //Obstacle is in front of Front Right sensor
      Serial.println("Obstacle Detected by Front Right Sensor");
      delay(500);
      turnRight(90);
      delay(500);
      moveForward(3*10);
      delay(500);
      turnLeft(90);
      delay(500);
      moveForward(4*10);
      delay(500);
      turnLeft(90);
      delay(50000);
      moveForward(3*10);
      delay(5000);
      turnRight(90);
    }
    delay(500);
  }  
}

//to avoid 1x1 obstacle diagonally
void avoidDiagonal(){

  while(1){
    moveForward(1*10);
    
    int frontIR1 = (int)getFrontIR1();
    int frontIR2 = (int)getFrontIR2();
    int frontIR3 = (int)getFrontIR3();
    int rightIR1 = (int)getRightIR1();
    int rightIR2 = (int)getRightIR2();
    int leftIR1 = (int)getLeftIR1();

    int flag = 0;
        
    if(frontIR2 == 2){  //Obstacle is in front of Front Center sensor
      Serial.println("Obstacle Detected by Front Center Sensor");
      delay(500);
      turnLeft(45);
      delay(500);
      moveForwardTick(2546);
      delay(500);
      turnRight(45);
      turnRight(45);
      delay(500);
      moveForwardTick(2600);
      delay(500);
      turnLeft(45);
    }
    else if(frontIR1 == 1 && frontIR2 != 1){ //Obstacle is in front of Front Left sensor
      Serial.println("Obstacle Detected by Front Left Sensor");
      delay(500);
      turnRight(45);
      delay(500);
      moveForwardTick(580);
      delay(500);
      turnLeft(45);
      delay(500);
      moveForwardTick(580);
      delay(500);
      turnLeft(45);
      delay(500);
      moveForwardTick(580);
      delay(500);
      turnRight(45);
    }
    else if(frontIR3 == 1 && frontIR2 != 1){ //Obstacle is in front of Front Right sensor
      Serial.println("Obstacle Detected by Front Right Sensor");
      delay(500);
      turnRight(45);
      delay(500);
      moveForwardTick(580);
      delay(500);
      turnLeft(45);
      delay(500);
      moveForwardTick(580);
      delay(500);
      turnLeft(45);
      delay(50000);
      moveForwardTick(580);
      delay(5000);
      turnRight(45);
    }
    delay(500);
  }  
}

// for moving diagonal
void moveForwardTick(int distance) {
  initializeTick();   // set all tick to 0
  initializeMotor_Start();  // set motor and brake to 0
  double currentSpeed = MOVE_MAX_SPEED;
  
  //error checking feedback in a while loop
  while (tick_R <= distance || tick_L <= distance) {
    if (myPID.Compute()) {
      md.setSpeeds(currentSpeed + speed_O, currentSpeed - speed_O);
    }
  }
  initializeMotor_End();  //brakes the motor
}

void alignRight() {
  int count = 0;

  double diff = (readRightSensor_2()+0.18) - (readRightSensor_1()-0.09); // Sensor2, adding allow it to move closer to the wall
  /*while (true)
    {
      diff = (readRightSensor_2()) - (readRightSensor_1());
      Serial.print(readRightSensor_2());
      Serial.print(",");
      Serial.print(readRightSensor_1()-0.09);
      Serial.print(",");
      Serial.print(diff);
      Serial.print("\n");
    }*/
  //double diff = readRightSensor_2() - readRightSensor_1();
  
  while (abs(diff) > 0.2 && abs(diff) < 4)
  {
    if (count >= 6)
      break;
    rotateRight(abs(diff*8), abs(diff)/diff*-1);
    diff = (readRightSensor_2()+0.18) - (readRightSensor_1()-0.09);
    count++;
  }
}

void rotateRight(int distance, int direct) {
  initializeTick();
  initializeMotor_Start();
  double currentSpeed = ROTATE_MAX_SPEED;

  while (tick_R < distance) {
    if (myPID.Compute())
      md.setSpeeds(0, direct*(currentSpeed - speed_O));
  }
  initializeMotor_End();
}

void rotateLeft(int distance, int direct) {
  initializeTick();
  initializeMotor_Start();

  double currentSpeed = ROTATE_MAX_SPEED;
  while (tick_L < distance) {
    if (myPID.Compute())
      md.setSpeeds(direct*(currentSpeed - speed_O), 0);
  }
  initializeMotor_End();
}

// for getting the number of tick to move a certain grid
void forwardCalibration(int maxGrid) {
  double desiredDistanceSensor1 = -2.09;
  double desiredDistanceSensor3 = -1.89;
  for (int grid = 1; grid < maxGrid+1; grid++)
  {
    moveForward(grid * 10);
    int ticksL = TICKS[grid-1];
    int ticksR = TICKS[grid-1];
    Serial.print("Tick before moving ");
    Serial.print(grid);
    Serial.print(" grid: ");
    Serial.print(ticksL);
    Serial.print(", ");
    Serial.print(ticksR);
    Serial.print("\n");
  
    double diffLeft = readFrontSensor_1() - desiredDistanceSensor1;
    double diffRight = readFrontSensor_3() - desiredDistanceSensor3;

    while ((abs(diffLeft) >= 0.2 && abs(diffLeft) < 4)|| (abs(diffRight) >= 0.2 && abs(diffRight) < 4))
    {
      if (diffLeft < 0 || diffRight < 0)
      {
        if (abs(diffLeft) < abs(diffRight))
        {
          ticksR -= (int)(abs(diffRight*12));
          rotateRight(abs(diffRight*12), abs(diffRight)/diffRight*1);
        }
        else
        {
          ticksL -= (int)(abs(diffLeft*12));
          rotateLeft(abs(diffLeft*12), abs(diffLeft)/diffLeft*1);
        }
      }
      else
      {
        if (abs(diffLeft) >= 0.2)
        {
          if (abs(diffLeft)/diffLeft == 1)
            ticksL += (int)(abs(diffLeft*12));
          else if (abs(diffLeft)/diffLeft == -1)
            ticksL -= (int)(abs(diffLeft*12));
          rotateLeft(abs(diffLeft*12), abs(diffLeft)/diffLeft*1);
        }
        else if (abs(diffRight) >= 0.2)
        {
          if (abs(diffRight)/diffRight == 1)
            ticksR += (int)(abs(diffRight*12));
          else if (abs(diffLeft)/diffRight == -1)
            ticksR -= (int)(abs(diffRight*12));
          rotateRight(abs(diffRight*12), abs(diffRight)/diffRight*1);
        }
      }
      
      diffLeft = readFrontSensor_1() - desiredDistanceSensor1;
      diffRight = readFrontSensor_3() - desiredDistanceSensor3;
    }
    Serial.print("Tick after moving ");
    Serial.print(grid);
    Serial.print(" grid: ");
    Serial.print(ticksL);
    Serial.print(", ");
    Serial.print(ticksR);
    Serial.print("\n");

    alignRight();
  }
}

void adjustTick(int distance, int direct, bool left) {
  initializeTick();
  initializeMotor_Start();
  double currentSpeed = ROTATE_MAX_SPEED;
  if (left)
  {
    if (direct == -1)
      TURN_TICKS_L -= distance;
    else
      TURN_TICKS_L += distance;
  }
  else
  {
    if (direct == -1)
      TURN_TICKS_R += distance;
    else
      TURN_TICKS_R -= distance;
  }
  /*Serial.print("After add: ");
  Serial.print(TURN_TICKS_L);
  Serial.print(", ");
  Serial.print(TURN_TICKS_R);
  Serial.print("\n");*/
  while (tick_R < distance) {
    if (myPID.Compute())
      md.setSpeeds(0, direct*(currentSpeed - speed_O));
  }
  initializeMotor_End();
}

void rotateBoth(int distanceLeft, int distanceRight, int direct) {
  initializeTick();
  initializeMotor_Start();
  double currentSpeed = MOVE_MAX_SPEED;
  while (tick_R < distanceRight || tick_L < distanceLeft) {
    if (myPID.Compute())
      md.setSpeeds(direct*(currentSpeed + speed_O), direct*(currentSpeed - speed_O));
  }
  initializeMotor_End();
}

void alignFront() {
  delay(2);
  
  /*while (true)
  {
    Serial.print(readFrontSensor_1() + 0.30); // +0.64
    Serial.print(",");
    Serial.print(readFrontSensor_3() + 0.52); // +0.85
    Serial.print("\n");
    double desiredDistanceSensor1 = -0.64;  // - 0.8 more
    double desiredDistanceSensor3 = -0.85;  // - 0.8 more
    double diffLeft = readFrontSensor_1() - desiredDistanceSensor1;
    double diffRight = readFrontSensor_3() - desiredDistanceSensor3;
  }*/
  
  int count = 0;
  double desiredDistanceSensor1 = -0.45;  // minus more means nearer to wall
  double desiredDistanceSensor3 = -0.67;  // plus more means further from wall
  
  double diffLeft = readFrontSensor_1() - desiredDistanceSensor1;
  double diffRight = readFrontSensor_3() - desiredDistanceSensor3;

  while ((abs(diffLeft) > 0.2 && abs(diffLeft) < 20)|| (abs(diffRight) > 0.2 && abs(diffRight) < 20))
  {   
    if (abs(diffLeft) >= 0.2)
    {
      rotateLeft(abs(diffLeft*8), abs(diffLeft)/diffLeft*1);
    }
    if (abs(diffRight) >= 0.2)
    {
      rotateRight(abs(diffRight*8), abs(diffRight)/diffRight*1);
    }
    diffLeft = readFrontSensor_1() - desiredDistanceSensor1;
    diffRight = readFrontSensor_3() - desiredDistanceSensor3;

    count++;
    if (count >= 8)
      break;
  }
}

void alignFrontStart() {
  delay(2);
  /*
  while (true)
  {
    Serial.print(readFrontSensor_1());
    Serial.print(",");
    Serial.print(readFrontSensor_3());
    Serial.print("\n");
  }*/
  double desiredDistanceSensor1 = -0.45;  // - 0.5 more
  double desiredDistanceSensor3 = -0.67;  // - 0.5 more
  double diffLeft = readFrontSensor_1() - desiredDistanceSensor1;
  double diffRight = readFrontSensor_3() - desiredDistanceSensor3;

  while ((abs(diffLeft) > 0.2 && abs(diffLeft) < 20)|| (abs(diffRight) > 0.2 && abs(diffRight) < 20))
  {   
    if (abs(diffLeft) >= 0.2)
    {
      rotateLeft(abs(diffLeft*8), abs(diffLeft)/diffLeft*1);
    }
    if (abs(diffRight) >= 0.2)
    {
      rotateRight(abs(diffRight*8), abs(diffRight)/diffRight*1);
    }
    diffLeft = readFrontSensor_1() - desiredDistanceSensor1;
    diffRight = readFrontSensor_3() - desiredDistanceSensor3;
  }
}
