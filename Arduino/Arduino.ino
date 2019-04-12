// the setup routine runs once when you press reset
void setup() {
  // initialize serial communication
  setupSerialConnection();
  setupMotorEncoder();
  setupSensorInterrupt();
  setupPID();

  while(Serial.available() > 0) {
    char clearBuffer = Serial.read();
  }

  //                       _oo0oo_
  //                      o8888888o
  //                      88" . "88
  //                      (| -_- |)
  //                      0\  =  /0
  //                    ___/`---'\___
  //                  .' \\|     |// '.
  //                 / \\|||  :  |||// \
  //                / _||||| -:- |||||- \
  //               |   | \\\  -  /// |   |
  //               | \_|  ''\---/''  |_/ |
  //               \  .-\__  '-'  ___/-. /
  //             ___'. .'  /--.--\  `. .'___
  //          ."" '<  `.___\_<|>_/___.' >' "".
  //         | | :  `- \`.;`\ _ /`;.`/ - ` : | |
  //         \  \ `_.   \_ __\ /__ _/   .-` /  /
  //     =====`-.____`.___ \_____/___.-`___.-'=====
  //                       `=---='
  //     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  
  delay(20);  
}

// the loop routine runs over and over again forever
void loop() {
  delay(2);

  // if not connected
  if (!Serial) {
    //Serial.println("Waiting for connection");
  }

  // for testing motor speed
  //testRPM(400, 400);
  
/*
  // for hardcode string
  char movement;
  String test = "O1|";  // every character need to be accompanied by a digit
  String gridMoveValueString;
  int gridMoveValueInt;
  int strLen = test.length();
  int pos = 0;
  
  // while data is available
  while (strLen > pos) {    
    // to store movement value in int
    gridMoveValueInt = 0;
    gridMoveValueString = "";
    
    // read the next character of the serial buffer
    char character = test.charAt(pos);
    pos++;
    if (character == '|')
      continue;
    if (character == '\0' || character == '\n')
      break;
    char nextChar = test.charAt(pos);
    gridMoveValueString += (char)nextChar;
    gridMoveValueInt = gridMoveValueString.toInt();
    
    switch (character) {
      // move forward 
      case 'W':
        moveForward(gridMoveValueInt * 10);
        delay(10);
        printSensorReading();
        break;
      // turn left
      case 'A':
        turnLeft(gridMoveValueInt*90);
        delay(10);
        printSensorReading();
        break;
      // move backward
      case 'S':
        moveBackwards(gridMoveValueInt * 10);
        delay(10);
        printSensorReading();
        break;
      // turn right
      case 'D':
        turnRight(gridMoveValueInt*90);
        delay(10);
        printSensorReading();
        break;
      // to get current sensor reading
      case 'K':
        printDistanceReading();
        printSensorReading();
        break;
      case 'P':
        alignRight();
        delay(10);
        printSensorReading();
        break;
      case 'O':
        alignFront();
        delay(10);
        printSensorReading();
        break;
      case 'M':
        alignRight(1);
        turnRight(90);
        alignRight(3);
        alignFront();
        turnRight(90);
        alignFront();
        turnLeft(90);
        alignRight(2);
        alignFront();
        turnLeft(90);
        alignRight(1);
        break;
      case 'N':
        alignFrontStart();
        turnRight(90);
        alignFrontStart();
        turnLeft(90);
        
        alignFrontStart();
        turnRight(90);
        alignFrontStart();
        turnLeft(90);
        
        alignFrontStart();
        alignRight(1);
        turnRight(90);
        alignFrontStart();
        turnRight(90);
        alignRight(1);
        break;
      case '|':
      case '\0':
      case '\n':
        break;     
    }
    pos++;
  }
  */
  
  //for rceiving string from rpi 
  int gridMoveValueInt;
  String gridMoveValueString;
  
  int dummy;

  // while data is available in the serial buffer
  while (Serial.available() > 0){
    dummy = Serial.peek();
    if (dummy > 90 || dummy < 49)
    {
      //Serial.println(dummy);
      dummy = Serial.read();
      continue;
    }
    
    gridMoveValueString = "";
    char character = Serial.read();

    if (character == '|')
      continue;
    if (character == '\0' || character == '\n')
      break;
    char nextChar = Serial.read();
    gridMoveValueString += nextChar;
    gridMoveValueInt = gridMoveValueString.toInt();
  
    switch (character) {
      // move forward 
      case 'W':
        moveForward(gridMoveValueInt * 10);
        delay(10);
        printSensorReading();
        break;
      // turn left
      case 'A':
        turnLeft(gridMoveValueInt*90);
        delay(10);
        printSensorReading();
        break;
      // move backward
      case 'S':
        moveBackwards(gridMoveValueInt * 10);
        delay(10);
        printSensorReading();
        break;
      // turn right
      case 'D':
        turnRight(gridMoveValueInt*90);
        delay(10);
        printSensorReading();
        break;
      // to get current sensor reading
      case 'K':
        printSensorReading();
        break;
      case 'P':
        alignRight();
        delay(10);
        printSensorReading();
        break;
      case 'O':
        alignFront();
        delay(10);
        printSensorReading();
        break;  
      case 'M':
        alignRight();
        turnRight(90);
        alignRight();
        alignFront();
        turnRight(90);
        alignFront();
        turnLeft(90);
        alignRight();
        alignFront();
        turnLeft(90);
        alignRight();
        break;
      case 'N':
        alignFrontStart();
        turnRight(90);
        alignFrontStart();
        turnLeft(90);
        
        alignFrontStart();
        turnRight(90);
        alignFrontStart();
        turnLeft(90);
        
        alignFrontStart();
        alignRight();
        turnRight(90);
        alignFrontStart();
        turnRight(90);
        alignRight();
        break;
      case '|':
      case '\0':
      case '\n':
        break;     
    }
  }
}

// serial codes
void setupSerialConnection() {
  Serial.begin(115200);
  while (!Serial);
}

void printSensorReading() {
  // print sensor reading to serial monitor
  Serial.print("XF1:");
  Serial.print((int)getFrontIR1()); // print front-left sensor distance
  Serial.print("|F2:");  
  Serial.print((int)getFrontIR2()); // print front-center sensor distance
  Serial.print("|F3:");
  Serial.print((int)getFrontIR3()); // print front-right sensor distance
  Serial.print("|R1:");
  Serial.print((int)getRightIR1()); // print right-back sensor distance
  Serial.print("|R2:");  
  Serial.print((int)getRightIR2()); // print right-front sensor distance
  Serial.print("|L1:");
  Serial.print((int)getLeftIR1());  // print left long range sensor distance
  Serial.print("|\n");
  // flush waits for transmission of outoing serial data to complete
  Serial.flush();
  delay(10);
}

void printDistanceReading() {
  // print sensor reading to serial monitor
  Serial.print("                                  F1:");
  Serial.print(readFrontSensor_1()); // print front-left sensor distance
  Serial.print("|F2:");  
  Serial.print(readFrontSensor_2()); // print front-center sensor distance
  Serial.print("|F3:");
  Serial.print(readFrontSensor_3()); // print front-right sensor distance
  Serial.print("|R1:");
  Serial.print(readRightSensor_1()); // print right-back sensor distance
  Serial.print("|R2:");  
  Serial.print(readRightSensor_2()); // print right-front sensor distance
  Serial.print("|L1:");
  Serial.print(readLeftSensor_1());  // print left long range sensor distance
  Serial.print("|\n");
  // flush waits for transmission of outoing serial data to complete
  Serial.flush();
  delay(10);
}
