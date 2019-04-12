#include <SharpIR2.h> //for long-range
#include <SharpIR.h>  //for short-range
#include <RunningMedian.h>
//short range - 10-80cm (OPTIMAL 10-30CM)
//long range - 20-150cm (OPTIMAL 20 -60CM)
//our short range sensors work best between 10-30 cm. 
//30cm onwards there'll be larger discrepancies (discrepancies must be within 10%)

const int MAX_SMALL_SENSOR = 80;    // max distance for small sensor is 80cm
const int MAX_BIG_SENSOR = 150;     // max distance for big sensor is 150cm
const int NUM_SAMPLES_MEDIAN = 19;  // getting a maximum sample of 15

double frontIR1_Value = 0, frontIR2_Value = 0, frontIR3_Value = 0;
double rightIR1_Value = 0, rightIR2_Value = 0, leftIR1_Value = 0;

//declaration of our IR sensors. "GP2Y0A21YK0F" is just the model number of the SharpIR sensor
//A0-A5 == Arduino pin 1-6

SharpIR2 leftIR_1(A4, 20150);  //LEFT 
SharpIR2 frontIR_1(A0, 1080);
SharpIR2 frontIR_2(A5, 1080);
SharpIR2 frontIR_3(A1, 1080);
SharpIR2 rightIR_1(A3, 1080);
SharpIR2 rightIR_2(A2, 1080);

void setupSensorInterrupt() {
  //  ADCSRA &= ~(bit (ADPS0) | bit (ADPS1) | bit (ADPS2)); // clear prescaler bits
  //  ADCSRA |= bit (ADPS0) | bit (ADPS2);// 32  prescaler
  //  ADCSRA |= bit (ADPS2); // 16  prescaler
  //    MsTimer2::set(35, readSensors);
  //    MsTimer2::start();
}

// read and return the median of (5*11) front left sensor values in grid distance
int getFrontIR1() {
  double median = readFrontSensor_1();
  return (shortGrid(median, 4.02, 16.55, 30.68)); 
}

// read and return the median of (5*11) front center sensor values in grid distance
int getFrontIR2() {
  double median = readFrontSensor_2();
  return (shortGrid(median, 3.25, 13.80, 22.60)); 
}

// read and return the median of (5*11) front right sensor values in grid distance
int getFrontIR3() {
  double median = readFrontSensor_3();
  return (shortGrid(median, 3.10, 14.55, 22.00));  
}

// read and return the median of (5*11) right back sensor values in grid distance
int getRightIR1() {
  double median = readRightSensor_1();
  return (shortGrid(median, 4.90, 16.50, 26.40));
}

// read and return the median of (5*11) right front sensor values in grid distance
int getRightIR2() {
  double median = readRightSensor_2();
  return (shortGrid(median, 4.95, 19.10, 45.20));
}

// read and return the median of (5*11) left front sensor values in grid distance
int getLeftIR1() {
  double median = readLeftSensor_1();
  return (longGrid(median));
}

// read and return the median of (55) front left sensor values in cm
double readFrontSensor_1() {
  RunningMedian frontIR1_Median = RunningMedian(NUM_SAMPLES_MEDIAN);
  for (int n = 0; n < NUM_SAMPLES_MEDIAN; n++) {
    double irDistance = frontIR_1.distance() - (10.62);   
    //reference point at 3x3 grid boundary (30cmx30cm) is 0cm
    
    frontIR1_Median.add(irDistance);    // add in the array  
    if (frontIR1_Median.getCount() == NUM_SAMPLES_MEDIAN) {
      if (frontIR1_Median.getHighest() - frontIR1_Median.getLowest() > 15)
        return -10;
      
      frontIR1_Value = frontIR1_Median.getMedian();
    }
  }
  return frontIR1_Value;
}

// read and return the median of (55) front center sensor values in cm
double readFrontSensor_2() {
  RunningMedian frontIR2_Median = RunningMedian(NUM_SAMPLES_MEDIAN);
  for (int n = 0; n < NUM_SAMPLES_MEDIAN; n++) {
    double irDistance = frontIR_2.distance() - (10.40);
    //reference point at 3x3 grid boundary (30cmx30cm) is 0cm
    
    frontIR2_Median.add(irDistance);    // add in the array  
    if (frontIR2_Median.getCount() == NUM_SAMPLES_MEDIAN) {
      if (frontIR2_Median.getHighest() - frontIR2_Median.getLowest() > 15)
        return -10;
      frontIR2_Value = frontIR2_Median.getMedian();
    }
  }
  return frontIR2_Value;
}

// read and return the median of (55) front right sensor values in cm
double readFrontSensor_3() {
  RunningMedian frontIR3_Median = RunningMedian(NUM_SAMPLES_MEDIAN);
  for (int n = 0; n < NUM_SAMPLES_MEDIAN; n++) {
    double irDistance = frontIR_3.distance() - (10.97);
    //reference point at 3x3 grid boundary (30cmx30cm) is 0cm
    
    frontIR3_Median.add(irDistance);    // add in the array  
    if (frontIR3_Median.getCount() == NUM_SAMPLES_MEDIAN) {
      if (frontIR3_Median.getHighest() - frontIR3_Median.getLowest() > 15)
        return -10;
      frontIR3_Value = frontIR3_Median.getMedian();
    }
  }
  return frontIR3_Value;
}

// read and return the median of (55) right back sensor values in cm
double readRightSensor_1() {
  RunningMedian rightIR1_Median = RunningMedian(NUM_SAMPLES_MEDIAN);
  for (int n = 0; n < NUM_SAMPLES_MEDIAN; n++) {
    double irDistance = rightIR_1.distance() - (10.59+0.44-0.21);
    //reference point at 3x3 grid boundary (30cmx30cm) is 0cm
    
    rightIR1_Median.add(irDistance);    // add in the array 
    if (rightIR1_Median.getCount() == NUM_SAMPLES_MEDIAN) {
      if (rightIR1_Median.getHighest() - rightIR1_Median.getLowest() > 30)
        return -10;
      rightIR1_Value = rightIR1_Median.getMedian();
    }
  }
  return rightIR1_Value;
}

// read and return the median of (55) right front sensor values in cm
double readRightSensor_2() {
  RunningMedian rightIR2_Median = RunningMedian(NUM_SAMPLES_MEDIAN);
  for (int n = 0; n < NUM_SAMPLES_MEDIAN; n++) {
    double irDistance = rightIR_2.distance() - (10.94-0.06-0.09);
    //reference point at 3x3 grid boundary (30cmx30cm) is 0cm

    rightIR2_Median.add(irDistance);
    if (rightIR2_Median.getCount() == NUM_SAMPLES_MEDIAN) {
      if (rightIR2_Median.getHighest() - rightIR2_Median.getLowest() > 40)
        return -10;
      rightIR2_Value = rightIR2_Median.getMedian();
    }
  }
  return rightIR2_Value;
}

// read and return the median of (55) left sensor values in cm
double readLeftSensor_1() {
  RunningMedian leftIR_1_Median = RunningMedian(NUM_SAMPLES_MEDIAN);
  for (int n = 0; n < NUM_SAMPLES_MEDIAN; n++) {
    double irDistance = leftIR_1.distance() + 0.1;
    //reference point at 3x3 grid boundary (30cmx30cm) is 0cm
    
    leftIR_1_Median.add(irDistance);    // add in the array  
    if (leftIR_1_Median.getCount() == NUM_SAMPLES_MEDIAN) {
      if (leftIR_1_Median.getHighest() - leftIR_1_Median.getLowest() > 30)
        return -10;
      leftIR1_Value = leftIR_1_Median.getMedian();
    }
  }
  return leftIR1_Value;
}

// determine which grid it belongs for short sensor
int shortGrid(double distance, double offset1, double offset2,  double offset3) {
  if (distance == -10)
    return -1;
  else if (distance <= offset1)
    return 1;
  else if (distance <= offset2)
    return 2;
  else if (distance <= offset3)
    return 3;
  else
    return -1;
}

// determine which grid it belongs for long sensor
int longGrid(double distance) {
  if (distance == -10)
    return -1;
  else if (distance <= 21.60)
    return 1;
  else if (distance <= 28.00)
    return 2;
  else if (distance <= 38.20)
    return 3;
  else if (distance <= 46.00)
    return 4;
  else if (distance <= 50.00)
    return 5;
  else if (distance <= 66)
    return -1;
  else if (distance <= 70)
    return -1;
  else
    return -1;
  //return distance;
}
