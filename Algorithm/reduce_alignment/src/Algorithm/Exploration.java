package Algorithm;

import Map.Map;
import Map.Cell;
import Map.Direction;
import Map.MapConstants;
import Map.ObsSurface;
import Network.NetMgr;
import Network.NetworkConstants;
import Robot.Robot;
import Robot.Command;
import Robot.RobotConstants;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import Helper.*;

public class Exploration {

    private static final Logger LOGGER = Logger.getLogger(Exploration.class.getName());

    private Map exploredMap;
    private Map realMap;
    private Robot robot;
    private double coverageLimit;
    private int timeLimit;
    private int stepPerSecond;
    private boolean sim;
    private double areaExplored;
    private long startTime;
    private long endTime;
    private Point start;

    // for image
    HashMap<String, ObsSurface> notYetTaken;

    private int right_move = 0;     // checking for four consecutive right + forward move

//    private boolean firstMove = false;  // for aligning right when it is firstMove
    public Exploration(Map exploredMap, Map realMap, Robot robot, double coverageLimit, int timeLimit, int stepPerSecond,
                       boolean sim) {
        this.exploredMap = exploredMap;
        this.realMap = realMap;
        this.robot = robot;
        this.coverageLimit = coverageLimit;
        this.timeLimit = timeLimit;
        this.stepPerSecond = stepPerSecond;
        this.sim = sim;
    }

    public Map getExploredMap() {
        return exploredMap;
    }

    public void setExploredMap(Map exploredMap) {
        this.exploredMap = exploredMap;
    }

    public double getCoverageLimit() {
        return coverageLimit;
    }

    public void setCoverageLimit(double coverageLimit) {
        this.coverageLimit = coverageLimit;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }


    public void imageExploration(Point start) throws InterruptedException {
        long imageStartTime = System.currentTimeMillis();
        int exp_timing = explorationAllRightWallHug(start);
        HashMap<String, ObsSurface> allPossibleSurfaces;

        // if fastest than previous leaderboard timing -- return to stop (do not go out)
        if (exp_timing < RobotConstants.BEST_EXP_TIMING) {
            return;
        }
        else {
            robot.setDoingImage(true);
            // algo for image
            notYetTaken = getUntakenSurfaces();
            if (notYetTaken.size() == 0) {
                return;
            }
            // calibrate and let the robot face up
            calibrate_at_start_before_going_out();
            // get all untaken surfaces
            System.out.println("DEBUG " + notYetTaken);
            while (notYetTaken.size() > 0) {
                imageLoop();
                // TODO
            }
            goToPoint(start);
        }

    }

    private void imageLoop() throws InterruptedException {
        boolean doingImage = true;
        ArrayList<ObsSurface> surfTaken;
        ObsSurface nearestObstacle;
        Cell nearestCell;
        boolean success;
        nearestObstacle = exploredMap.nearestObsSurface(robot.getPos(), notYetTaken);
        System.out.println("DEBUG nearestObstacle " + nearestObstacle.toString());
        nearestCell = exploredMap.nearestMovable(nearestObstacle);
        System.out.println("DEBUG nearestCell is null:" + (nearestCell == null));

        if (nearestCell != null) {
            System.out.println("DEBUG nearestCell " + nearestCell.toString());

            // go to nearest cell
            success = goToPointForImage(nearestCell.getPos(), nearestObstacle);
            if (success) {
                System.out.println("DEBUG cell pos " + nearestCell.getPos().toString());
                do {
                    robot.setImageCount(0);
                    surfTaken = robot.imageRecognitionRight(exploredMap);
                    updateNotYetTaken(surfTaken);
                    rightWallHug(doingImage);
                    // TODO
                    System.out.println("DEBUG robot pos " + robot.getPos().toString());
                } while (!robot.getPos().equals(nearestCell.getPos()) && !robot.isRightHuggingWall());
            }
            else {
                System.out.println("DEBUG in inner else");
                removeFromNotYetTaken(nearestObstacle);
            }

        }
        else {
            System.out.println("DEBUG in outer else");
            removeFromNotYetTaken(nearestObstacle);
            System.out.println("DEBUG after removing in outer else");
        }
    }

    private void updateNotYetTaken(ArrayList<ObsSurface> surfTaken) {
        for (ObsSurface obsSurface : surfTaken) {
            if (notYetTaken.containsKey(obsSurface.toString())) {
                notYetTaken.remove(obsSurface.toString());
                LOGGER.info("Remove from not yet taken: " + obsSurface);
            }
        }
    }

    private void removeFromNotYetTaken(ObsSurface obsSurface) {
        notYetTaken.remove(obsSurface.toString());
        LOGGER.info("Remove from not yet taken: " + obsSurface.toString());

    }

    private boolean goToPointForImage(Point loc, ObsSurface obsSurface) throws InterruptedException {
        ArrayList<ObsSurface> surfTaken = new ArrayList<ObsSurface>();
        robot.setStatus("Go to point: " + loc.toString());
        LOGGER.info(robot.getStatus());
        ArrayList<Command> commands = new ArrayList<Command>();
        ArrayList<Cell> path = new ArrayList<Cell>();
        FastestPath fp = new FastestPath(exploredMap, robot, sim);
        path = fp.runAStar(robot.getPos(), loc, robot.getDir());
        if (path == null) {
            return false;
        }

        fp.displayFastestPath(path, true);
        commands = fp.getPathCommands(path);
        System.out.println("Exploration Fastest Commands: "+commands);

        for (Command c : commands) {
            System.out.println("Command: "+c);
            if ((c == Command.FORWARD) && !movable(robot.getDir())) {
                System.out.println("Not Executing Forward Not Movable");
                // TODO
                goToPointForImage(loc, obsSurface);
                break;
            } else{
                if(((c == Command.TURN_LEFT && !movable(Direction.getAntiClockwise(robot.getDir())))||
                        (c == Command.TURN_RIGHT && !movable(Direction.getClockwise(robot.getDir())))) && commands.indexOf(c) == commands.size()-1)
                    goToPointForImage(loc, obsSurface);
                if (c == Command.TURN_LEFT || c == Command.TURN_RIGHT){
                    robot.turn(c, stepPerSecond);
                }
                else {
                    robot.move(c, RobotConstants.MOVE_STEPS, exploredMap, stepPerSecond);
                }

                surfTaken = robot.senseWithoutMapUpdate(exploredMap, realMap);
                updateNotYetTaken(surfTaken);

            }
        }


        // Orient the robot to make its right side hug the wall
        // if right movable

        Direction desiredDir = Direction.getClockwise(obsSurface.getSurface());
        if (desiredDir == robot.getDir()) {
            return true;
        }
        else if (desiredDir == Direction.getClockwise(robot.getDir())) {
            robot.turn(Command.TURN_RIGHT, stepPerSecond);
            surfTaken = robot.senseWithoutMapUpdate(exploredMap, realMap);
            updateNotYetTaken(surfTaken);
        }
        else if (desiredDir == Direction.getAntiClockwise(robot.getDir())) {
            robot.turn(Command.TURN_LEFT, stepPerSecond);
            surfTaken = robot.senseWithoutMapUpdate(exploredMap, realMap);
            updateNotYetTaken(surfTaken);
        }
        // opposite
        else {
            robot.turn(Command.TURN_LEFT, stepPerSecond);
            surfTaken = robot.senseWithoutMapUpdate(exploredMap, realMap);
            updateNotYetTaken(surfTaken);
            robot.turn(Command.TURN_LEFT, stepPerSecond);
            surfTaken = robot.senseWithoutMapUpdate(exploredMap, realMap);
            updateNotYetTaken(surfTaken);
        }

        return true;
    }

    private HashMap<String, ObsSurface> getUntakenSurfaces() {
        HashMap<String, ObsSurface> notYetTaken;

        // get all surfaces possilbe
        notYetTaken = getAllObsSurfaces();
        for (String tempObsSurfaceStr : robot.getSurfaceTaken().keySet()) {
            if (!notYetTaken.containsKey(tempObsSurfaceStr)) {
                LOGGER.warning("Surface taken not in all possible surfaces. Please check. \n\n\n");
            }
            else {
                notYetTaken.remove(tempObsSurfaceStr);
            }
        }

        return notYetTaken;
    }

    private HashMap<String, ObsSurface> getAllObsSurfaces() {
        // TODO
        Cell tempCell;
        Cell temp;
        ObsSurface tempObsSurface;
        HashMap<Direction, Cell> tempNeighbours;
        HashMap<String, ObsSurface> allPossibleSurfaces = new HashMap<String, ObsSurface>();
        for (int row = 0; row < MapConstants.MAP_HEIGHT; row++) {
            for (int col = 0; col < MapConstants.MAP_WIDTH; col++) {
                tempCell = exploredMap.getCell(row, col);

                if (tempCell.isObstacle()) {
                    // check neighbouring
                    tempNeighbours = exploredMap.getNeighboursMap(tempCell);

                    for (Direction neighbourDir: tempNeighbours.keySet()) {
                        temp = tempNeighbours.get(neighbourDir);

                        if (!temp.isObstacle()) {
                            tempObsSurface = new ObsSurface(tempCell.getPos(), neighbourDir);
                            allPossibleSurfaces.put(tempObsSurface.toString(), tempObsSurface);
                        }
                    }
                }

            }
        }
        System.out.println();
        return allPossibleSurfaces;
    }

    private void calibrate_at_start_before_going_out() throws InterruptedException {
        String calibrationCmd = robot.getCommand(Command.INITIAL_CALIBERATE, 1);    // steps 1 for consistency
        NetMgr.getInstance().send(NetworkConstants.ARDUINO + calibrationCmd);

        // Orient the robot on laptop to face lap as after caliberation, it will face up
        // need to turn after setFindingFP(true) as it will not send command to arduino
        robot.setFindingFP(true);
        robot.turn(Command.TURN_RIGHT, RobotConstants.STEP_PER_SECOND);
        robot.turn(Command.TURN_RIGHT, RobotConstants.STEP_PER_SECOND);
        robot.setFindingFP(false);
    }


    //TODO clean this
    public int explorationAllRightWallHug(Point start) throws InterruptedException {
        boolean doingImage = false;
        areaExplored = exploredMap.getExploredPercentage();
        startTime = System.currentTimeMillis();
        endTime = startTime + timeLimit;
        double prevArea = exploredMap.getExploredPercentage();
        int moves = 1;
        int checkingStep = RobotConstants.CHECKSTEPS;
        this.start = start;
//        this.firstMove = true;

        // Loop to explore the map
        outer:
        do {
            prevArea = areaExplored;
            if(areaExplored >= 100)
                break;
            try {
                rightWallHug(doingImage);

            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            areaExplored = exploredMap.getExploredPercentage();
            if (prevArea == areaExplored)
                moves++;
            else
                moves=1;

            LOGGER.info(Double.toString(areaExplored));
            if (moves % checkingStep == 0 || right_move > 3 || (robot.getPos().distance(start)==0 && areaExplored < 100.00)) {      // prevent from keep turning right and forward
//            if (moves % checkingStep == 0 || robot.getPos().distance(start)==0) {     // original
//            if (moves % checkingStep == 0) {
                do{
                    if (robot.getPos().equals(start)) {
                        goToPoint(start);
                        calibrate_at_start_before_going_out();
                    }
                    prevArea = areaExplored;
                    if(!goToUnexplored())
                        break outer;
                    areaExplored = exploredMap.getExploredPercentage();
                }while(prevArea == areaExplored);
                moves = 1;
                checkingStep = RobotConstants.CHECKSTEPS;
            }
        } while (areaExplored < coverageLimit && System.currentTimeMillis() < endTime);
        if (sim) {  // for actual run, just let the timer run
            Main.SimulatorNew.displayTimer.stop();
        }
        moves = 0;
        while (!robot.getPos().equals(start) && moves < 18) {
            rightWallHug(doingImage);
            moves++;
        }
        robot.setImageCount(0);
        robot.imageRecognitionRight(exploredMap);
        goToPoint(start);   // orient the robot
        endTime = System.currentTimeMillis();
        int seconds = (int)((endTime - startTime)/1000%60);
        int minutes = (int)((endTime - startTime)/1000/60);
        int total_in_seconds = (int)((endTime - startTime)/1000);
        System.out.println("Total Time: "+total_in_seconds+" seconds");
        System.out.println("Total Time: "+minutes+"mins "+seconds+"seconds");
        return total_in_seconds;
    }

    //TODO clean this
    public int exploration(Point start) throws InterruptedException {
        areaExplored = exploredMap.getExploredPercentage();
        startTime = System.currentTimeMillis();
        endTime = startTime + timeLimit;
        double prevArea = exploredMap.getExploredPercentage();
        int moves = 1;
        int checkingStep = RobotConstants.CHECKSTEPS;
        this.start = start;
//        this.firstMove = true;

        // Loop to explore the map
        outer:
        do {
            prevArea = areaExplored;
            if(areaExplored >= 100)
                break;
            try {
                System.out.println("DEBUG");
                rightWallHug(false);

            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            areaExplored = exploredMap.getExploredPercentage();
            if (prevArea == areaExplored)
                moves++;
            else
                moves=1;

            LOGGER.info(Double.toString(areaExplored));
//            LOGGER.info(Integer.toString(moves));

//            // for week 8 only, do not go out again if returning to start and areaExplored > target percentage
//            // disable after week 9
//            if (robot.getPos().distance(start) == 0 && areaExplored > RobotConstants.TARGETED_COVERAGE) {
//                break outer;
//            }

            if (moves % checkingStep == 0 || right_move > 3 || (robot.getPos().distance(start)==0 && areaExplored < 100.00)) {      // prevent from keep turning right and forward
//            if (moves % checkingStep == 0 || robot.getPos().distance(start)==0) {     // original
//            if (moves % checkingStep == 0) {
                do{
                    if (robot.getPos().equals(start)) {
                        goToPoint(start);
                        if (!sim) {
                            robot.turnRightAndAlignMethodWithoutMapUpdate(exploredMap, realMap);
                            robot.align_front(exploredMap, realMap);
                            robot.align_right(exploredMap, realMap);
                        }
                    }
                    prevArea = areaExplored;
                    if(!goToUnexplored())
                        break outer;
                    areaExplored = exploredMap.getExploredPercentage();
                }while(prevArea == areaExplored);
                moves = 1;
                checkingStep = RobotConstants.CHECKSTEPS;
            }
        } while (areaExplored < coverageLimit && System.currentTimeMillis() < endTime);
        if (sim) {  // for actual run, just let the timer run
            Main.SimulatorNew.displayTimer.stop();
        }
        goToPoint(start);
        endTime = System.currentTimeMillis();
        int seconds = (int)((endTime - startTime)/1000%60);
        int minutes = (int)((endTime - startTime)/1000/60);
        int total_in_seconds = (int)((endTime - startTime)/1000);
        System.out.println("Total Time: "+total_in_seconds+" seconds");
        System.out.println("Total Time: "+minutes+"mins "+seconds+"seconds");
        return total_in_seconds;
    }


    /**
     * Go to the nearest unexplored cell
     * @return true there is an unexplored cell and function executed, false if unexplored cell not found or no path to the nearest unexplored cell
     */
    public boolean goToUnexplored() throws InterruptedException {
        robot.setStatus("Go to nearest unexplored\n");
        LOGGER.info(robot.getStatus());

        // Pause for half a second
//        if(sim) {
//            TimeUnit.MILLISECONDS.sleep(500);
//        }

        Cell nearestUnexp = exploredMap.nearestUnexplored(robot.getPos());
        LOGGER.info("Nearest unexplored: " + nearestUnexp);
        Cell nearestExp = exploredMap.nearestExplored(nearestUnexp.getPos(), robot.getPos());
        LOGGER.info("Nearest explored: " + nearestExp);
        if (nearestExp == null) {
            LOGGER.info("No nearest unexplored found.");
            return false;
        }
        else {
            robot.setStatus("Go to nearest explored " + nearestExp.getPos().toString() + "\n");
            LOGGER.info("Go to " + nearestExp.toString());
            return goToPoint(nearestExp.getPos());
        }
    }


    /**
     * Basic right wall hugging algo
     */
    public void rightWallHug(boolean doingImage) throws InterruptedException {
        HashMap<String, Integer> sensorRes;
        Direction robotDir = robot.getDir();
        ArrayList<ObsSurface> surfTaken;
//
//        if (sim) {
//            TimeUnit.MILLISECONDS.sleep(RobotConstants.WAIT_TIME / stepPerSecond);
//        }
        
        // if right movable
        if (movable(Direction.getClockwise(robotDir))) {
//            LOGGER.info("DEBUG: In right movable");

            // check front alignment
            if (!sim) {
                robot.align_front(exploredMap, realMap);
            }

            robot.turn(Command.TURN_RIGHT, stepPerSecond);
            if (doingImage) {
                surfTaken = robot.senseWithoutMapUpdate(exploredMap, realMap);
                updateNotYetTaken(surfTaken);
            }
            else {
                robot.sense(exploredMap, realMap);
            }

            // if firstMove, align right
//            if (firstMove) {
//                LOGGER.info("First Move, align right.");
//                robot.align_right(exploredMap, realMap);
//                firstMove = false;
//            }


            moveForward(RobotConstants.MOVE_STEPS, stepPerSecond, doingImage);
            right_move++;
        }

        // else if front movable
        else if (movable(robotDir)) {
//            LOGGER.info("DEBUG: In front movable");
//            // if firstMove, align right
//            if (firstMove) {
//                LOGGER.info("First Move, align right.");
//                robot.align_right(exploredMap, realMap);
//                firstMove = false;
//            }

            robot.move(Command.FORWARD, RobotConstants.MOVE_STEPS, exploredMap, stepPerSecond);
            if (doingImage) {
                surfTaken = robot.senseWithoutMapUpdate(exploredMap, realMap);
                updateNotYetTaken(surfTaken);
            }
            else {
                robot.sense(exploredMap, realMap);
            }
            right_move = 0;

        }

        // else if left movable
        else if (movable(Direction.getAntiClockwise(robotDir))) {
//            LOGGER.info("DEBUG: In right movable");

            // try to turn right, align front, turn left, align front and right if possible before and after turning left
//            LOGGER.info("Right and front not movable, try to align.");

            turnRightAndAlignBeforeTurnLeft(doingImage);

            alignAndImageRecBeforeLeftTurn(doingImage);


            robot.turn(Command.TURN_LEFT, stepPerSecond);
            if (doingImage) {
                surfTaken = robot.senseWithoutMapUpdate(exploredMap, realMap);
                updateNotYetTaken(surfTaken);
            }
            else {
                robot.sense(exploredMap, realMap);
            }

            if (!sim) {
                robot.align_right(exploredMap, realMap);
            }

            moveForward(RobotConstants.MOVE_STEPS, stepPerSecond, doingImage);
            right_move = 0;

        }

        // else move backwards
        else {
//            LOGGER.info("DEBUG: In else");

            // Option1. Turn left twice with alignment
            // if R1 and R2 == 1, turn right and align first
            turnRightAndAlignBeforeTurnLeft(doingImage);

            alignAndImageRecBeforeLeftTurn(doingImage);

            robot.turn(Command.TURN_LEFT, stepPerSecond);
            if (doingImage) {
                surfTaken = robot.senseWithoutMapUpdate(exploredMap, realMap);
                updateNotYetTaken(surfTaken);
            }
            else {
                robot.sense(exploredMap, realMap);
            }

            alignAndImageRecBeforeLeftTurn(doingImage);


            robot.turn(Command.TURN_LEFT, stepPerSecond);
            if (doingImage) {
                surfTaken = robot.senseWithoutMapUpdate(exploredMap, realMap);
                updateNotYetTaken(surfTaken);
            }
            else {
                robot.sense(exploredMap, realMap);
            }
            if (!sim) {
                robot.align_right(exploredMap, realMap);
            }

//            // Option2. Move backwards
//            Boolean firstBackward = true;
//            do {
//                right_move = 0;
//
//                // try to align front and right if possible before moving backwards for the first time
//                if (firstBackward) {
//                    LOGGER.info("Before moving backwards, try to align");
//                    robot.align_front(exploredMap, realMap);
//                    robot.align_right(exploredMap, realMap);
//                    firstBackward = false;
//                }
//
//                robot.move(Command.BACKWARD, RobotConstants.MOVE_STEPS, exploredMap, stepPerSecond);
//                robot.align_right(exploredMap, realMap);
//                robot.sense(exploredMap, realMap);
//
////                if (sim) {
////                    TimeUnit.MILLISECONDS.sleep(RobotConstants.WAIT_TIME / stepPerSecond);
////                }
//
//            } while (!movable(Direction.getAntiClockwise(robotDir)) && !movable(Direction.getClockwise(robotDir)));
//
//            // turn left if possible
//            if (movable(Direction.getAntiClockwise(robotDir))) {
//                robot.turn(Command.TURN_LEFT, stepPerSecond);
//                robot.sense(exploredMap, realMap);
//                moveForward(RobotConstants.MOVE_STEPS, stepPerSecond);
//                right_move = 0;
//            }
//
//            // else turn left twice
//            else {
//                robot.turn(Command.TURN_LEFT, stepPerSecond);
//                robot.sense(exploredMap, realMap);
//
//                robot.align_front(exploredMap, realMap);
//
//                robot.turn(Command.TURN_LEFT, stepPerSecond);
//                robot.sense(exploredMap, realMap);
//
//                robot.align_right(exploredMap, realMap);
//                // then restart, dont move forward
////                moveForward(RobotConstants.MOVE_STEPS, stepPerSecond);
//                right_move = 0;
//            }
        }

    }

    /**
     * Turn right, align front, turn left, align front and right if possible before and after turning left
     * Avoid turning twice with turnAndAlignCount in Robot class
     * @throws InterruptedException
     */
    private void turnRightAndAlignBeforeTurnLeft(boolean doingImage) throws InterruptedException {
        if ((robot.getSensorRes().get("R1") == 1 && robot.getSensorRes().get("R2") == 1) &&
                (!robot.getHasTurnAndAlign()) &&
                (!sim)) {
            if (doingImage) {
                robot.turnRightAndAlignMethodWithoutMapUpdate(exploredMap, realMap);
            }
            else {
                robot.turnRightAndAlignMethod(exploredMap, realMap);
            }
        }
        else if (robot.getHasTurnAndAlign()) {
            robot.setHasTurnAndAlign(false);
        }
    }

    /**
     * Align front, align right and do image recognition before turning left
     */
    private void alignAndImageRecBeforeLeftTurn(boolean doingImage) {
        if (!sim) {
            robot.align_front(exploredMap, realMap);
            robot.align_right(exploredMap, realMap);
            // before turn left, take image just in case
            robot.setImageCount(0);
            ArrayList<ObsSurface> surfTaken = robot.imageRecognitionRight(exploredMap);
            if (doingImage) {
                updateNotYetTaken(surfTaken);
            }
        }
    }

    /**
     * Move forward if movable
     * @param steps
     */
    private void moveForward(int steps, int stepPerSecond, boolean doingImage) throws InterruptedException {
        if (movable(robot.getDir())) {       // for actual, double check in case of previous sensing error

//            if (sim) {
//                TimeUnit.MILLISECONDS.sleep(RobotConstants.WAIT_TIME / stepPerSecond);
//            }

            robot.move(Command.FORWARD, steps, exploredMap, stepPerSecond);
            if (doingImage) {
                ArrayList<ObsSurface> surfTaken = robot.senseWithoutMapUpdate(exploredMap, realMap);
                updateNotYetTaken(surfTaken);
            }
            else {
                robot.sense(exploredMap, realMap);
            }
        }
    }

    /**
     * Check whether the next cell along the dir is movable
     * @param dir direction relative to the robot
     * @return true if movable, false otherwise
     */
    public boolean movable(Direction dir) {

        int rowInc = 0, colInc = 0;

        switch (dir) {
            case UP:
                rowInc = 1;
                colInc = 0;
                break;

            case LEFT:
                rowInc = 0;
                colInc = -1;
                break;

            case RIGHT:
                rowInc = 0;
                colInc = 1;
                break;

            case DOWN:
                rowInc = -1;
                colInc = 0;
                break;
        }
//        LOGGER.info(String.format("DEBUG: checking movable row: %d, col: %d", robot.getPos().y + rowInc, robot.getPos().x + colInc));
        return exploredMap.checkValidMove(robot.getPos().y + rowInc, robot.getPos().x + colInc);
    }

    // TODO clean this
    public boolean goToPoint(Point loc) throws InterruptedException {
        robot.setStatus("Go to point: " + loc.toString());
        LOGGER.info(robot.getStatus());
        // TODO: now ignore robot already at start
        if (robot.getPos().equals(start) && loc.equals(start)) {
            while (robot.getDir() != Direction.DOWN) {
                robot.turn(Command.TURN_LEFT, stepPerSecond);
                System.out.println(robot.getDir());
                if (sim) {
                    robot.sense(exploredMap, realMap);
                }
                else {
                    NetMgr.getInstance().receive();
                }

            }
            return true;


            // old
//            while (robot.getDir() != Direction.UP) {
////                if (sim) {
////                    try {
////                        TimeUnit.MILLISECONDS.sleep(RobotConstants.WAIT_TIME / stepPerSecond);
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
////                }
//                robot.sense(exploredMap, realMap);
//                robot.turn(Command.TURN_RIGHT, stepPerSecond);
//                // hx: to be changed to turn right  / left
//            }
//            return false;
        }

        ArrayList<Command> commands = new ArrayList<Command>();
        ArrayList<Cell> path = new ArrayList<Cell>();
        FastestPath fp = new FastestPath(exploredMap, robot, sim);
        path = fp.runAStar(robot.getPos(), loc, robot.getDir());
        if (path == null)
            return false;
        fp.displayFastestPath(path, true);
        commands = fp.getPathCommands(path);
        System.out.println("Exploration Fastest Commands: "+commands);

        //Not moving back to start single moves

        // TODO: temporarily disable
//        if (true) {
        if (!loc.equals(start)) {
            for (Command c : commands) {
                System.out.println("Command: "+c);
                if ((c == Command.FORWARD) && !movable(robot.getDir())) {
                    System.out.println("Not Executing Forward Not Movable");
                    // TODO
                    goToPoint(loc);
                    break;
                } else{
                    if(((c == Command.TURN_LEFT && !movable(Direction.getAntiClockwise(robot.getDir())))||
                            (c == Command.TURN_RIGHT && !movable(Direction.getClockwise(robot.getDir())))) && commands.indexOf(c) == commands.size()-1)
                        continue;
                    if (c == Command.TURN_LEFT || c == Command.TURN_RIGHT){
                        alignAndImageRecBeforeLeftTurn(false);
                        robot.turn(c, stepPerSecond);
                    }
                    else {
                        robot.move(c, RobotConstants.MOVE_STEPS, exploredMap, stepPerSecond);
                    }

                    robot.sense(exploredMap, realMap);

                }
//                if (sim) {
//                    try {
//                        TimeUnit.MILLISECONDS.sleep(RobotConstants.WAIT_TIME / stepPerSecond);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
            }

            //If Robot Gets Lost When Moving to unexplored area Move it Back to a wall
            if(!loc.equals(start) && exploredMap.getExploredPercentage() < 100 && movable(Direction.getClockwise(robot.getDir()))) {
                robot.setStatus("Lost. Finding the nearest virtual wall.");
                LOGGER.info(robot.getStatus());

                //Get direction of the nearest virtual wall
                Direction dir = nearestVirtualWall(robot.getPos());
                System.out.println(dir);

                //If not at a virtual wall
                if (movable(dir))
                {
                    System.out.println("ininin");
                    //Orient the robot to face the wall
                    while(dir != robot.getDir()) {
                        //Check the difference in the direction enum
                        if(dir.ordinal() - robot.getDir().ordinal()==1)
                            robot.turn(Command.TURN_LEFT, stepPerSecond);
                        else
                            robot.turn(Command.TURN_RIGHT, stepPerSecond);
                    }
                    //Move Towards the wall till unable to move
                    while(movable(robot.getDir())) {
                        robot.move(Command.FORWARD, RobotConstants.MOVE_STEPS, exploredMap, stepPerSecond);
//                        if (sim) {
//                            try {
//                                TimeUnit.MILLISECONDS.sleep(RobotConstants.WAIT_TIME / stepPerSecond);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
                        robot.sense(exploredMap, realMap);
                    }
                }
                //Orient the robot to make its right side hug the wall
                while(Direction.getAntiClockwise(dir) != robot.getDir()) {
                    robot.turn(Command.TURN_LEFT, stepPerSecond);
//                    if (sim) {
//                        try {
//                            TimeUnit.MILLISECONDS.sleep(RobotConstants.WAIT_TIME / stepPerSecond);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
                    robot.sense(exploredMap, realMap);
                }

            }
        }

//        /* TODO: temporarily disable
        //Moving back to Start multiple moves
        else {
            int moves = 0;
            Command c = null;
            for (int i = 0; i < commands.size(); i++) {
                c = commands.get(i);
//                if (sim) {
//                    try {
//                        TimeUnit.MILLISECONDS.sleep(RobotConstants.WAIT_TIME / stepPerSecond);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                }

//                if ((c == Command.FORWARD) && !movable(robot.getDir())) {
                // checking sensorRes reading instead since only sensorRes is updated
                if ((c == Command.FORWARD) &&
                        (robot.getSensorRes().get("F1") == 1 ||
                                robot.getSensorRes().get("F2") == 1 ||
                                robot.getSensorRes().get("F3") == 1)
                    ) {

                // System.out.println("moves "+moves);
                    System.out.println("Not Executing Forward Not Movable");
                    // update map (sensorRes is updated)
                    robot.updateMap(exploredMap, realMap, robot.getSensorRes());
                    goToPoint(loc);
                    break;
                } else {
                    if (c == Command.FORWARD && moves < 1) {
//                    if (c == Command.FORWARD && moves < RobotConstants.MAX_MOVE) {
                        moves++;
                        // If last command
                        if (i == (commands.size() - 1)) {
                            robot.move(c, moves, exploredMap, stepPerSecond);
                            robot.senseWithoutMapUpdateAndAlignment(exploredMap, realMap);
                        }
                    } else {
                        if (moves > 0) {
                            robot.move(Command.FORWARD, moves, exploredMap, stepPerSecond);
                            robot.senseWithoutMapUpdateAndAlignment(exploredMap, realMap);


                        }
                        if (c == Command.TURN_RIGHT || c == Command.TURN_LEFT) {
                            robot.turn(c, stepPerSecond);
                        } else {
                            robot.move(c, RobotConstants.MOVE_STEPS, exploredMap, stepPerSecond);
                        }
                        robot.senseWithoutMapUpdateAndAlignment(exploredMap, realMap);
                        moves = 0;
                    }
                }
            }
        }
//        */

        //TODO: temp code
        if (loc.equals(start)) {
            // Orient robot to face UP
            if (loc.equals(start)) {

                // TODO: Changed - orient the robot to face down for caliberation
//                while (robot.getDir() != Direction.UP) {
                while (robot.getDir() != Direction.DOWN) {
                    // TODO: check - arduino dont want
                    // align if possible
//                    if (!sim) {
//                        robot.align_front(exploredMap, realMap);
//                        robot.align_right(exploredMap, realMap);
//                    }

//                    robot.turn(Command.TURN_RIGHT, stepPerSecond);
                    robot.turn(Command.TURN_LEFT, stepPerSecond);
//                    if (sim) {
//                        try {
//                            TimeUnit.MILLISECONDS.sleep(RobotConstants.WAIT_TIME / stepPerSecond);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }

                    System.out.println(robot.getDir());

                    // since it is alr at start, do not update sensor reading just incase the position is wrong and phantom blocks
                    if (sim) {
                        robot.sense(exploredMap, realMap);
                    }
                    else {
                        NetMgr.getInstance().receive();
                    }
                    // actual to be added later
//                    if(!sim && !movable(robot.getDir())) {
//                        NetMgr.getInstance().send("Alg|Ard|"+Command.ALIGN_FRONT.ordinal()+"|0");
//                        NetMgr.getInstance().receive();
//                        if(!movable(Direction.getPrevious(robot.getDir()))) {
//                            NetMgr.getInstance().send("Alg|Ard|"+Command.ALIGN_RIGHT+"|0");
//                            NetMgr.getInstance().receive();
//                        }
//                    }
                }
            }
        }

        return true;
    }

    // TODO: clean this
    //Returns the direction to the nearest virtual wall
    public Direction nearestVirtualWall(Point pos) {
        int rowInc, colInc, lowest = 1000, lowestIter = 0, curDist = 0;
        //Distance to wall Evaluation order: right, up, left, down
        Direction dir = Direction.RIGHT;
        //Evaluate the distance to nearest virtualwall
        System.out.println("Nearest Wall");
        for (int i=0; i<4; i++) {
            rowInc = (int)Math.sin(Math.PI/2*i);
            colInc = (int)Math.cos(Math.PI/2*i);
            curDist = 0;
            for (int j = 1; j < MapConstants.MAP_HEIGHT; j++) {
                if(exploredMap.checkValidCell(pos.y+rowInc*j, pos.x+colInc*j)) {
                    //Keep Looping till reached a virtual wall
                    if(exploredMap.clearForRobot(pos.y+rowInc*j, pos.x+colInc*j))
                        curDist++;
                    else
                        break;
                }
                //Reached the end of the wall
                else
                    break;
            }
            System.out.println("Direction: "+i+" "+curDist);
            //Evaluate the distance to previous lowest
            if (curDist<lowest)
            {
                lowest = curDist;
                lowestIter = i;
            }
        }
        System.out.println("Direction "+dir);
        //Choose the direction based on the result
        for (int c=0; c<lowestIter; c++)
        {
            dir = Direction.getAntiClockwise(dir);
        }

        return dir;
    }

}
