package Robot;

import Map.Map;
import Map.Direction;
import Map.MapDescriptor;
import Map.MapConstants;
import Map.Cell;
import Map.ObsSurface;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import Helper.*;
import Network.NetMgr;
import Network.NetworkConstants;
import jdk.nashorn.internal.parser.JSONParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;


public class Robot {

    private static final Logger LOGGER = Logger.getLogger(Robot.class.getName());

    private boolean sim;            // true if in simulator mode, false otherwise (actual)
    private boolean findingFP;      // true if doing fastest path, false otherwise (exploration)
    private boolean reachedGoal;
    private Point pos;
    private Direction dir;
    private String status;

    private Command preMove = Command.FORWARD;

    private ArrayList<String> sensorList;
    private HashMap<String, Sensor> sensorMap;
    private HashMap<String, Integer> sensorRes;
//    private static PrintManager printer = new PrintManager();

    // for delay in sim
    private long tempStartTime, tempEndTime, tempDiff;

    // for converting map to send to android
    private MapDescriptor MDF = new MapDescriptor();

    // for image taking
    private int imageCount = 0;
    private HashSet<String> imageHashSet = new HashSet<String>();
    private HashMap<String, ObsSurface> surfaceTaken = new HashMap<String, ObsSurface>();
    
    // for alignment
    private int alignCount = 0;
    private int turnAndAlignCount = 0;
    private boolean hasTurnAndAlign = false;

    private boolean doingImage = false;


    public Robot(boolean sim, boolean findingFP, int row, int col, Direction dir) {
        this.sim = sim;
        this.findingFP = findingFP;
        this.pos = new Point(col, row);
        this.dir = dir;
        this.reachedGoal = false;  // may need to amend
        this.sensorList = new ArrayList<String>();
        this.sensorMap = new HashMap<String, Sensor>();
        this.sensorRes = new HashMap<String, Integer>();
        initSensors();
        this.status = String.format("Initialization completed.\n");
//        printer.setText(printer.getText() + this.status + "\n");
        // remember to set start position outside
    }

    @Override
    public String toString() {
        String s = String.format("Robot at %s facing %s\n", pos.toString(), dir.toString());
        return s;
    }

    // Getters and setters

    public boolean getSim() {
        return this.sim;
    }

    public void setSim(boolean sim) {
        this.sim = sim;
    }

    public boolean isFindingFP() {
        return this.findingFP;
    }

    public void setFindingFP(boolean findingFP) {
        this.findingFP = findingFP;
    }

    public Point getPos() {
        return this.pos;
    }

    public void setPos(int row, int col) {
        // to be changed when sensor is added
        this.pos = new Point(col, row);
    }

    public void setPos(Point pos) {
        // to be changed when sensor is added
        this.pos = pos;
    }


    public Direction getDir() {
        return this.dir;
    }

    public void setDir(Direction dir) {
        this.dir = dir;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isReachedGoal() {
        return this.reachedGoal;
    }

    public void setReachedGoal(boolean reachedGoal) {
        this.reachedGoal = reachedGoal;
    }

    public ArrayList<String> getSensorList() {
        return sensorList;
    }

    public HashMap<String, Sensor> getSensorMap() {
        return sensorMap;
    }

    public Sensor getSensor(String sensorId) {
        return sensorMap.get(sensorId);
    }

    public HashMap<String, Integer> getSensorRes() {
        return sensorRes;
    }

    public void setSensorRes(HashMap<String, Integer> sensorRes) {
        this.sensorRes = sensorRes;
    }

    public HashMap<String, ObsSurface> getSurfaceTaken() {
        return surfaceTaken;
    }

    /**
     * Initialization of the Sensors
     *
     * ID for the sensors: XX
     * 1st Letter: F - Front, L - Left, R - Right
     * 2nd Letter: Identifier
     * L1 is long IR, the rest is short IR
     *
     * Sensor list includes:
     * Front: 3 short range sensors
     * Right: 2 short range sensors
     * Left: 1 long range sensors
     *
     */
    private void initSensors() {
        int row = pos.y;
        int col = pos.x;

        // Front Sensors
        Sensor SF1 = new Sensor("F1", RobotConstants.SHORT_MIN, RobotConstants.SHORT_MAX, row + 1, col - 1,
                Direction.UP);
        Sensor SF2 = new Sensor("F2", RobotConstants.SHORT_MIN, RobotConstants.SHORT_MAX, row + 1, col, Direction.UP);
        Sensor SF3 = new Sensor("F3", RobotConstants.SHORT_MIN, RobotConstants.SHORT_MAX, row + 1, col + 1,
                Direction.UP);

        // RIGHT Sensor
        Sensor SR1 = new Sensor("R1", RobotConstants.SHORT_MIN, RobotConstants.SHORT_MAX, row - 1, col + 1,
                Direction.RIGHT);
        Sensor SR2 = new Sensor("R2", RobotConstants.SHORT_MIN, RobotConstants.SHORT_MAX, row + 1, col + 1,
                Direction.RIGHT);

        // LEFT Sensor
        Sensor LL1 = new Sensor("L1", RobotConstants.LONG_MIN, RobotConstants.LONG_MAX, row + 1, col - 1,
                Direction.LEFT);

        sensorList.add(SF1.getId());
        sensorList.add(SF2.getId());
        sensorList.add(SF3.getId());
        sensorList.add(SR1.getId());
        sensorList.add(SR2.getId());
        sensorList.add(LL1.getId());
        sensorMap.put(SF1.getId(), SF1);
        sensorMap.put(SF2.getId(), SF2);
        sensorMap.put(SF3.getId(), SF3);
        sensorMap.put(SR1.getId(), SR1);
        sensorMap.put(SR2.getId(), SR2);
        sensorMap.put(LL1.getId(), LL1);

        if (dir != Direction.UP) {
            rotateSensors(dir);
        }

        this.status = "Sensor initialized\n";
//        printer.setText(printer.getText() + this.status + "\n");

    }

    private void setSensorPos(int rowDiff, int colDiff) {
        int row, col;
        Sensor s;
        for (String sname: sensorList) {
            s = sensorMap.get(sname);
            s.setPos(s.getRow() + rowDiff, s.getCol() + colDiff);
        }
    }


    private void locateSensorAfterRotation(Sensor s, double angle) {
        // pos
        int newCol, newRow;
        newCol = (int) Math.round((Math.cos(angle) * (s.getCol() - pos.x) - Math.sin(angle) * (s.getRow() - pos.y) + pos.x));
        newRow = (int) Math.round((Math.sin(angle) * (s.getCol() - pos.x) - Math.cos(angle) * (s.getRow() - pos.y) + pos.y));
        s.setPos(newRow, newCol);
    }

    /**
     * Change sensor var (dir, pos) when the robot turns
     * @param turn_dir turning direction of the robot (left or right only)
     */
    private void internalRotateSensor(Direction turn_dir) {
        double angle = 0;

        // turn_dir
        switch (turn_dir) {
            case LEFT:
                angle = Math.PI / 2;
                for (String sensorId : sensorList) {
                    Sensor s = sensorMap.get(sensorId);
                    s.setSensorDir(Direction.getAntiClockwise(s.getSensorDir()));
                    locateSensorAfterRotation(s, angle);
                }
                break;
            case RIGHT:
                angle = -Math.PI / 2;
                for (String sensorId : sensorList) {
                    Sensor s = sensorMap.get(sensorId);
                    s.setSensorDir(Direction.getClockwise(s.getSensorDir()));
                    locateSensorAfterRotation(s, angle);
                }
                break;
            default:
                LOGGER.warning("No rotation done. Wrong input direction: " + turn_dir);
        }
    }


    /**
     * Change sensor var (dir, pos) when the robot turns
     * @param turn_dir turning direction of the robot (left, right, down)
     */
    private void rotateSensors(Direction turn_dir) {
        switch (turn_dir) {
            case LEFT:
                internalRotateSensor(Direction.LEFT);
                break;
            case RIGHT:
                internalRotateSensor(Direction.RIGHT);
                break;
            case DOWN:
                internalRotateSensor(Direction.RIGHT);
                internalRotateSensor(Direction.RIGHT);
                break;
            default:
                break;
        }
    }


    /**
     * Robot movement with direction (forward, backward) and steps) and Map updated.
     * @param cmd FORWARD or BACKWARD
     * @param steps number of steps moved by the robot
     * @param exploredMap current explored environment of the robot
     */
    public void move(Command cmd, int steps, Map exploredMap, int stepsPerSecond) throws InterruptedException {

        tempStartTime = System.currentTimeMillis();

        if (!sim && !findingFP) {
            // TODO to send fast forward
            // send command to Arduino
            String cmdStr = getCommand(cmd, steps);
//            LOGGER.info("Command String: " + cmdStr);
            NetMgr.getInstance().send(NetworkConstants.ARDUINO + cmdStr);
            // TODO if can delete if
            if (!findingFP) {
                alignCount += steps;
//                LOGGER.info(String.format("alignCount: %d", alignCount));
            }
        }

        int rowInc = 0, colInc = 0;

        switch(dir) {
            case UP:
                rowInc = 1;
                colInc = 0;
                break;
            case DOWN:
                rowInc = -1;
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
        }

        switch (cmd) {
            case FORWARD:
                break;
            case BACKWARD:
                rowInc *= -1;
                colInc *= -1;
                break;
            default:
                status = String.format("Invalid command: %s! No movement executed.\n", cmd.toString());
//                printer.setText(printer.getText() + status + "\n");
                LOGGER.warning(status);
                return;
        }

        int newRow = pos.y + rowInc * steps;
        int newCol = pos.x + colInc * steps;

        if(exploredMap.checkValidMove(newRow, newCol)) {

            preMove = cmd;
            status = String.format("%s for %d steps\n", cmd.toString(), steps);
            //printer.setText(printer.getText() + status + "\n" + pos.toString() + "\n");
            LOGGER.info(status);
            LOGGER.info("row = " + newRow + ", col = " + newCol);
//            logSensorInfo();

            // delay for sim
            if (sim) {
                tempEndTime = System.currentTimeMillis();
                tempDiff = RobotConstants.WAIT_TIME / stepsPerSecond * steps - (tempEndTime - tempStartTime);
                if (tempDiff > 0) {
//                System.out.println(tempDiff);
                    TimeUnit.MILLISECONDS.sleep(tempDiff);
                }
            }
            this.setPosition(newRow, newCol);
            if(!findingFP) {
                for (int i = 0; i < steps; i++) {
                    exploredMap.setPassThru(newRow - rowInc * i, newCol - colInc * i);
                }
            }
        }
    }


    /**
     * move method when cmd is about turning (TURN_LEFT, TURN RIGHT)
     * @param cmd
     */
    public void turn(Command cmd, int stepsPerSecond) throws InterruptedException {

        tempStartTime = System.currentTimeMillis();
//        if (!sim) {
        if (!sim && !findingFP) {
            // send command to Arduino
            // TODO: add turning degree
            String cmdStr = getCommand(cmd, 1);
//            LOGGER.info("Command String: " + cmdStr);
            NetMgr.getInstance().send(NetworkConstants.ARDUINO + cmdStr);
            // TODO if can delete if
            if(!findingFP) {
                alignCount++;
//                LOGGER.info(String.format("alignCount: %d", alignCount));
            }
        }
        switch(cmd) {
            case TURN_LEFT:
                dir = Direction.getAntiClockwise(dir);
                rotateSensors(Direction.LEFT);
                break;
            case TURN_RIGHT:
                dir = Direction.getClockwise(dir);
                rotateSensors(Direction.RIGHT);
                break;
            default:
                status = "Invalid command! No movement executed.\n";
//                printer.setText(printer.getText() + status + "\n");
                LOGGER.warning(status);
                return;
        }
        preMove = cmd;
        status = cmd.toString() + "\n";
        //printer.setText(printer.getText() + status + "\n" + pos.toString() + "\n");
        LOGGER.info(status);
        LOGGER.info(pos.toString());
//        logSensorInfo();

        // delay for simulator
        if (sim) {
            tempEndTime = System.currentTimeMillis();
            tempDiff = RobotConstants.WAIT_TIME / stepsPerSecond - (tempEndTime - tempStartTime);
            if (tempDiff > 0) {
                TimeUnit.MILLISECONDS.sleep(tempDiff);
            }
        }

    }


    /**
     * Set starting position, assuming direction unchanged
     * @param col
     * @param row
     * @param exploredMap
     */
    public void setStartPos(int row, int col, Map exploredMap) {
        setPosition(row, col);
        exploredMap.setAllExplored(false);
        exploredMap.setAllMoveThru(false);
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                exploredMap.getCell(r, c).setExplored(true);
                exploredMap.getCell(r, c).setMoveThru(true);
            }
        }
    }

    /**
     * Set robot position, assuming direction unchanged
     * @param col
     * @param row
     */
    public void setPosition(int row, int col) {
        int colDiff = col - pos.x;
        int rowDiff = row - pos.y;
        pos.setLocation(col, row);
        setSensorPos(rowDiff, colDiff);
    }

    public void logSensorInfo() {
        for (String sname : sensorList) {
            Sensor s = sensorMap.get(sname);
            String info = String.format("id: %s\trow: %d; col: %d\tdir: %s\n", s.getId(), s.getRow(), s.getCol(), s.getSensorDir());
            LOGGER.info(info);
        }
    }

    public Point parseStartPointJson(String jsonMsg) {
        System.out.println(jsonMsg);
        // double check to make sure that it is a start msg
        if (jsonMsg.contains(NetworkConstants.START_POINT_KEY)) {
            // parse json
            JSONObject startPointJson = new JSONObject(new JSONTokener(jsonMsg));
            Point startPoint = new Point((int) startPointJson.get("x") - 1, (int) startPointJson.get("y") - 1);
            return startPoint;
        }
        else {
            LOGGER.warning("Not a start point msg. Return null.");
            return null;
        }
    }

    public Point parseWayPointJson(String jsonMsg) {

        // double check to make sure that it is a start msg
        if (jsonMsg.contains(NetworkConstants.WAY_POINT_KEY)) {
            // parse json
            JSONObject wayPointJson = new JSONObject(new JSONTokener(jsonMsg));
            Point wayPoint = new Point((int) wayPointJson.get("x") - 1, (int) wayPointJson.get("y") - 1);
            return wayPoint;
        }
        else {
            LOGGER.warning("Not a start point msg. Return null.");
            return null;
        }
    }

    /**
     * Getting sensor result from RPI/Arduino
     * @return HashMap<SensorId, ObsBlockDis>
     */
    public HashMap<String, Integer> updateSensorRes(String msg) {
        int obsBlock;
        if (msg.charAt(0) != 'F') {
            // TODO
            // not sensor info sent from arduino
            return null;
        }
        else {
            String[] sensorStrings = msg.split("\\|");
            for (String sensorStr: sensorStrings) {
                String[] sensorInfo = sensorStr.split("\\:");
                String sensorID = sensorInfo[0];
                int grid = Integer.parseInt(sensorInfo[1]);
                if (grid >= sensorMap.get(sensorID).getMinRange() && grid <= sensorMap.get(sensorID).getMaxRange()) {
                    sensorRes.put(sensorID, grid);
                }
                else {
                    sensorRes.put(sensorID, -1);
                }
            }
            return sensorRes;
        }
    }


    public ArrayList<ObsSurface> imageRecognitionRight(Map exploredMap) {
        int rowInc = 0, colInc = 0;
        int camera_row, camera_col, temp_row, temp_col;
        int camInc = 0;

        switch (dir) {
            case UP:
                rowInc = 0;
                colInc = 1;
                break;
            case DOWN:
                rowInc = 0;
                colInc = -1;
                break;
            case LEFT:
                colInc = 0;
                rowInc = 1;
                break;
            case RIGHT:
                colInc = 0;
                rowInc = -1;
                break;
        }

        camera_row = pos.y + rowInc;
        camera_col = pos.x + colInc;

        boolean sendRPI = false, hasObsAtCamAxis = false;

        // send RPI if sensor reading within the camera range
        if ((sensorRes.get("R1") > 0 && sensorRes.get("R1") <= RobotConstants.CAMERA_MAX)
                || (sensorRes.get("R2") > 0 && sensorRes.get("R2") <= RobotConstants.CAMERA_MAX)) {
            if (!isRightHuggingWall()) {
                sendRPI = true;

            }
        }
//        }
        // else check for middle obstacles - removing else for image
//            LOGGER.info("In else");

        for (camInc = RobotConstants.CAMERA_MIN; camInc <= RobotConstants.CAMERA_MAX; camInc++) {
            temp_row = camera_row + rowInc * camInc;
            temp_col = camera_col + colInc * camInc;

            if (exploredMap.checkValidCell(temp_row, temp_col)) {
                Cell temp_cell = exploredMap.getCell(temp_row, temp_col);
                if (temp_cell.isExplored() && temp_cell.isObstacle()) {
                    // send to RPI to do image recognition
                    sendRPI = true;
                    hasObsAtCamAxis = true;
                    break;
                }
            } else {      // invalid cell
                break;
            }
        }


        // imageCount reset to 0 if preMov is turning but not the turning in turnRightAndAlign
        if (!hasTurnAndAlign && (preMove == Command.TURN_LEFT || preMove == Command.TURN_RIGHT)) {
            imageCount = 0;
        }

        ArrayList<ObsSurface> surfaceTakenList = new ArrayList<ObsSurface>();
        ObsSurface tempObsSurface;

        if (sendRPI) {
            if (imageCount == 0) {
                String to_send = String.format("I%d|%d|%s", camera_col + 1, camera_row + 1, Direction.getClockwise(dir).toString());
                if (!imageHashSet.contains(to_send)) {
                    imageHashSet.add(to_send);
                    NetMgr.getInstance().send(to_send);

                    // update surfaceTaken
                    // R1
                    tempObsSurface = addToSurfaceTaken("R1", rowInc, colInc);
                    if (tempObsSurface != null) {
                        surfaceTakenList.add(tempObsSurface);
                    }
                    // R2
                    tempObsSurface = addToSurfaceTaken("R2", rowInc, colInc);
                    if (tempObsSurface != null) {
                        surfaceTakenList.add(tempObsSurface);
                    }
                    // camera //TODO
                    if (hasObsAtCamAxis) {
                        tempObsSurface = internalAddToSurfaceTaken(camera_row, camera_col, rowInc, colInc, camInc);
                        if (tempObsSurface != null) {
                            surfaceTakenList.add(tempObsSurface);
                        }
                    }

                }
            }
//            imageCount = (imageCount + 1) % 3;
            imageCount = (imageCount + 1) % 2;

        }
        else {
            imageCount = 0;
        }
        LOGGER.info(Boolean.toString(sendRPI));
        LOGGER.info(String.format("imageCount: %d", imageCount));
        return surfaceTakenList;
    }

    public ObsSurface addToSurfaceTaken(String sensorName, int rowInc, int colInc) {
        int tempSensorRow, tempSensorCol, tempSensorReading;

        tempSensorReading = sensorRes.get(sensorName);
        if (tempSensorReading > 0 && tempSensorReading <= RobotConstants.CAMERA_MAX) {
            tempSensorRow = sensorMap.get(sensorName).getRow();
            tempSensorCol = sensorMap.get(sensorName).getCol();
            ObsSurface tempObsSurface = internalAddToSurfaceTaken(tempSensorRow, tempSensorCol, rowInc, colInc, tempSensorReading);
            return tempObsSurface;
        }
        else {
            return null;
        }

    }

    public ObsSurface internalAddToSurfaceTaken(int tempRow, int tempCol, int rowInc, int colInc, int incStep) {
        int tempObsRow, tempObsCol;
        ArrayList<ObsSurface> surfaceTakenList = new ArrayList<ObsSurface>();
        ObsSurface tempObsSurface;
        Direction tempSurface;
        tempObsRow = tempRow + rowInc * incStep;
        tempObsCol = tempCol + colInc * incStep;
        tempSurface = Direction.getAntiClockwise(dir);
        tempObsSurface = new ObsSurface(tempObsRow, tempObsCol, tempSurface);
        surfaceTaken.put(tempObsSurface.toString(), tempObsSurface);
        return tempObsSurface;
    }

    /** TODO
     * Check whether image recognition is possible (front obstacles )
     * i.e. obstacles found 2 grids in front of any front sensors
     * if yes, send to RPI
     * format: I|X|Y|RobotDirection
     */
    public void imageRecognitionFront() {
        if (sensorRes.get("F1") == 2 || sensorRes.get("F2") == 2 || sensorRes.get("F3") == 2) {
            // TODO: check using android index or algo index
            Sensor F2 = sensorMap.get("F2");
            String to_send = String.format("I%d|%d|%s", F2.getCol() + 1, F2.getRow() + 1, dir.toString());
            NetMgr.getInstance().send(to_send);
        }
    }


    /**
     * Getting sensor result for simulator
     * @param exploredMap
     * @param realMap
     * @return HashMap<SensorId, ObsBlockDis>
     */
    public HashMap<String, Integer> updateSensorRes(Map exploredMap, Map realMap) {
        int obsBlock;
        for(String sname: sensorList) {
            obsBlock = sensorMap.get(sname).detect(realMap);
            sensorRes.put(sname, obsBlock);
        }
        return sensorRes;
    }

    /**
     * Robot sensing surrounding obstacles for simulator
     * @param exploredMap
     * @param realMap
     */
    public ArrayList<ObsSurface> sense(Map exploredMap, Map realMap) {
        ArrayList<ObsSurface> surfTaken = new ArrayList<ObsSurface>();
        HashMap<String, Integer> sensorResult = completeUpdateSensorResult(exploredMap, realMap);
        updateMap(exploredMap, realMap, sensorResult);

        // send to Android
        if (!sim && !findingFP) {

            send_android(exploredMap);

            // Realignment for right
            if (alignCount > RobotConstants.CALIBRATE_AFTER) {
                // TODO: Alignment
//                align_front(exploredMap, realMap);    // unnecessary, align_front is already added when front not movable
                align_right(exploredMap, realMap);
            }

            // Realignment for front - turn right and align when it is not hugging the wall but R1 and R2 == 1 and turnAndAlignCount > CalibrationConstant
            if (isRightHuggingWall()) {
                turnAndAlignCount = 0;
            }
            else {
                turnAndAlignCount++;
            }
            if (hasTurnAndAlign) {
                hasTurnAndAlign = false;
            }

            if ((turnAndAlignCount > RobotConstants.TURN_AND_CALIBRATE) &&
                    (sensorRes.get("R1") == 1 && sensorRes.get("R2") == 1)) {

                try {
                    turnRightAndAlignMethod(exploredMap, realMap);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // TODO: Camera facing right - check whether img is needed to be detected and send RPI if needed
            surfTaken = imageRecognitionRight(exploredMap);
        }
        return surfTaken;
    }

    /** TODO: want alignment for image?
     * Update sensorRes but not the map. No alignment as well. Send image as well.
     * @param exploredMap
     * @param realMap
     */
    public void senseWithoutMapUpdateAndAlignment(Map exploredMap, Map realMap) {

        HashMap<String, Integer> sensorResult = completeUpdateSensorResult(exploredMap, realMap);
        // send to Android
        if (!sim && !findingFP) {
            send_android(exploredMap);
        }

    }


    /** TODO: want alignment for image?
     * Update sensorRes but not the map. No alignment as well. Send image as well. // alignment not updating the map
     * @param exploredMap
     * @param realMap
     */
    public ArrayList<ObsSurface> senseWithoutMapUpdate(Map exploredMap, Map realMap) {

        HashMap<String, Integer> sensorResult = completeUpdateSensorResult(exploredMap, realMap);
        ArrayList<ObsSurface> surfTaken = new ArrayList<ObsSurface>();
        // send to Android
        if (!sim && !findingFP) {
            send_android(exploredMap);

            // Realignment for right
            if (alignCount > RobotConstants.CALIBRATE_AFTER) {
                // TODO: Alignment
//                align_front(exploredMap, realMap);    // unnecessary, align_front is already added when front not movable
                align_right(exploredMap, realMap);
            }

            // Realignment for front - turn right and align when it is not hugging the wall but R1 and R2 == 1 and turnAndAlignCount > CalibrationConstant
            if (isRightHuggingWall()) {
                turnAndAlignCount = 0;
            }
            else {
                turnAndAlignCount++;
            }
            if (hasTurnAndAlign) {
                hasTurnAndAlign = false;
            }

            if ((turnAndAlignCount > RobotConstants.TURN_AND_CALIBRATE) &&
                    (sensorRes.get("R1") == 1 && sensorRes.get("R2") == 1)) {

                try {
                    turnRightAndAlignMethodWithoutMapUpdate(exploredMap, realMap);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // TODO: Camera facing right - check whether img is needed to be detected and send RPI if needed
            surfTaken = imageRecognitionRight(exploredMap);

        }
        // TODO: add alignment
        return surfTaken;
    }

    /**
     * Turn right, align front, turn left, align right
     * Condition checking is not in the method
     * @param exploredMap
     * @param realMap
     * @throws InterruptedException
     */
    public void turnRightAndAlignMethod(Map exploredMap, Map realMap) throws InterruptedException {
        turn(Command.TURN_RIGHT, RobotConstants.STEP_PER_SECOND);
        senseWithoutAlign(exploredMap, realMap);
        align_front(exploredMap, realMap);
        turn(Command.TURN_LEFT, RobotConstants.STEP_PER_SECOND);
        senseWithoutAlign(exploredMap, realMap);
        align_right(exploredMap, realMap);
//        imageRecognitionRight(exploredMap);   // try to do before alignment, if cannot, see how
        hasTurnAndAlign = true;
        turnAndAlignCount = 0;
    }

    /**
     * Turn right, align front, turn left, align right
     * Condition checking is not in the method
     * @param exploredMap
     * @param realMap
     * @throws InterruptedException
     */
    public void turnRightAndAlignMethodWithoutMapUpdate(Map exploredMap, Map realMap) throws InterruptedException {
        turn(Command.TURN_RIGHT, RobotConstants.STEP_PER_SECOND);
        senseWithoutMapUpdateAndAlignment(exploredMap, realMap);
        align_front(exploredMap, realMap);
        turn(Command.TURN_LEFT, RobotConstants.STEP_PER_SECOND);
        senseWithoutMapUpdateAndAlignment(exploredMap, realMap);
        align_right(exploredMap, realMap);
//        imageRecognitionRight(exploredMap);   // try to do before alignment, if cannot, see how
        hasTurnAndAlign = true;
        turnAndAlignCount = 0;
    }

    /**
     * Robot sensing surrounding obstacles for simulator
     * @param exploredMap
     * @param realMap
     */
    public void senseWithoutAlign(Map exploredMap, Map realMap) {
        HashMap<String, Integer> sensorResult = completeUpdateSensorResult(exploredMap, realMap);
        updateMap(exploredMap, realMap,sensorResult);

        // send to Android
        if (!sim && !findingFP) {

//            // TODO: Camera facing right - check whether img is needed to be detected and send RPI if needed
//            imageRecognitionRight(exploredMap);  // do not repeat taking

            send_android(exploredMap);

        }
    }

    public HashMap<String, Integer> completeUpdateSensorResult(Map exploredMap, Map realMap) {
        HashMap<String, Integer> sensorResult;

        if(sim) {
            sensorResult = updateSensorRes(exploredMap, realMap);
        }
        else {
            // TODO: add in case arduino send
//            String temp = NetMgr.getInstance().receive();
//            String temp2 = NetMgr.getInstance().receive();
            String msg = NetMgr.getInstance().receive();
//            while (msg.charAt(0) == 'L') {
//                LOGGER.warning("Unknow character received. Get sensor again.");
//                NetMgr.getInstance().send(NetworkConstants.ARDUINO + getCommand(Command.SEND_SENSORS, RobotConstants.MOVE_STEPS));
//                msg = NetMgr.getInstance().receive();
//
//            }
            sensorResult = updateSensorRes(msg);

            // TODO: Camera facing front - check whether img is needed to be detected and send RPI if needed
//            imageRecognitionFront();
//            try {
//                TimeUnit.MILLISECONDS.sleep(10);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
        return sensorResult;
    }

    public void updateMap(Map exploredMap, Map realMap, HashMap<String, Integer> sensorResult) {
        int obsBlock;
        int rowInc=0, colInc=0, row, col;

        if(sensorResult == null) {
            LOGGER.warning("Invalid msg. Map not updated");
            return;
        }

        for(String sname: sensorList) {
            Sensor s = sensorMap.get(sname);
            obsBlock = sensorResult.get(sname);

            // Assign the rowInc and colInc based on sensor Direction
            switch (s.getSensorDir()) {
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

            for (int j = s.getMinRange(); j <= s.getMaxRange(); j++) {

                row = s.getRow() + rowInc * j;
                col = s.getCol() + colInc * j;

                // check whether the block is valid otherwise exit (Edge of Map)
                if(exploredMap.checkValidCell(row, col)) {
                    exploredMap.getCell(row, col).setExplored(true);

                    if(j == obsBlock && !exploredMap.getCell(row, col).isMoveThru()) {
                        exploredMap.getCell(row, col).setObstacle(true);
                        exploredMap.setVirtualWall(exploredMap.getCell(row, col), true);
                        exploredMap.reinitVirtualWall();
                        break;
                    }
                    // if not in if
                    // (1) j != obsBlock && cell isMoveThru     // do not need to update
                    // (2) j == obsBlock && cell isMoveThru     // cannot be the case
                    // (3) j != obsBlock && cell !isMoveThru    // need to check
                    else if (j != obsBlock && exploredMap.getCell(row, col).isObstacle()) {      // (3)
                        exploredMap.getCell(row, col).setObstacle(false);
                        exploredMap.setVirtualWall(exploredMap.getCell(row, col), false);
                        exploredMap.reinitVirtualWall();
                    }
                }
                else  {

                    break;
                }

            }
        }
    }

    public JSONArray getRobotArray() {

        JSONArray robotArray = new JSONArray();
        JSONObject robotJson = new JSONObject()
                .put("x", pos.x + 1)
                .put("y", pos.y + 1)
                .put("direction", dir.toString().toLowerCase());
        robotArray.put(robotJson);
        return robotArray;
    }

    public JSONArray getMapArray(Map exploredMap) {
        String obstacleString = MDF.generateMDFString2(exploredMap);
        JSONArray mapArray = new JSONArray();
        JSONObject mapJson = new JSONObject()
                .put("explored", MDF.generateMDFString1(exploredMap))
                .put("obstacle", obstacleString)
                .put("length", obstacleString.length() * 4);
        mapArray.put(mapJson);
        return mapArray;
    }

    public JSONArray getStatusArray() {
        JSONArray statusArray = new JSONArray();
        JSONObject statusJson = new JSONObject()
                .put("status", status.replaceAll("\\n",""));
        statusArray.put(statusJson);
        return statusArray;
    }

    /**
     * Send the current robot position/direction and status (if uncomment) to android
     */
    public void send_android() {
        JSONObject androidJson = new JSONObject();

        androidJson.put("robot", getRobotArray());
        androidJson.put("status", getStatusArray());
        NetMgr.getInstance().send(NetworkConstants.ANDROID + androidJson.toString() + "\n");

    }

    /**
     * Send the current explored map and robot position/direciton, status (if uncomment) to android
     * @param exploredMap
     */
    public void send_android(Map exploredMap) {
        JSONObject androidJson = new JSONObject();

        androidJson.put("robot", getRobotArray());
        androidJson.put("map", getMapArray(exploredMap));
        androidJson.put("status", getStatusArray());
        NetMgr.getInstance().send(NetworkConstants.ANDROID + androidJson.toString() + "\n");

//            try {
//                TimeUnit.MILLISECONDS.sleep(10);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
    }



    public void align_front(Map exploredMap, Map realMap) { // realMap is null just to call sense

        if (sensorRes.get("F1") == 1 && sensorRes.get("F3") == 1) {
            // send align front
            String cmdStr = getCommand(Command.ALIGN_FRONT, 1);  // steps set to 0 to avoid appending to cmd
//            LOGGER.info("Command String: " + cmdStr);
            NetMgr.getInstance().send(NetworkConstants.ARDUINO + cmdStr);
//            alignCount = 0;
            status = "Aligning Front\n";
            LOGGER.info(status);
            if (doingImage) {
                senseWithoutMapUpdateAndAlignment(exploredMap, realMap);
            }
            else {
                senseWithoutAlign(exploredMap, realMap);
            }
            turnAndAlignCount = 0;
        }

    }

    public void align_right(Map exploredMap, Map realMap) { // realMap is null just to call sense
        int aligning_index = 0;
        switch(preMove) {
            case FORWARD:
                aligning_index = 1;
                break;
            case BACKWARD:
                aligning_index = 1;
                break;
            case TURN_RIGHT:
                aligning_index = 3;
                break;
            case TURN_LEFT:
                aligning_index = 2;
                break;
            default:
                LOGGER.info("Invalid preMove!! Please check!\n\n");
                aligning_index = 1;
                break;
        }
        if (sensorRes.get("R1") == 1 && sensorRes.get("R2") == 1) {
            // send align right
            String cmdStr = getCommand(Command.ALIGN_RIGHT, aligning_index);
//            LOGGER.info("Command String: " + cmdStr);
            NetMgr.getInstance().send(NetworkConstants.ARDUINO + cmdStr);
            alignCount = 0;
            status = String.format("Aligning Right: %d\n", aligning_index);
            LOGGER.info(status);
            if (doingImage) {
                senseWithoutMapUpdateAndAlignment(exploredMap, realMap);
            }
            else {
                senseWithoutAlign(exploredMap, realMap);
            }
        }

    }

    /**
     * Robot is right hugging the wall if the right sensor position is equal to
     * the lowest or highest possible row or col number
     * @return
     */
    public boolean isRightHuggingWall() {
        Point R1_pos = sensorMap.get("R1").getPos();
        Point R2_pos = sensorMap.get("R2").getPos();

        if ((R1_pos.x == 0 && R2_pos.x == 0)
                || (R1_pos.x == MapConstants.MAP_WIDTH - 1 && R2_pos.x == MapConstants.MAP_WIDTH - 1)
                || (R1_pos.y == 0 && R2_pos.y == 0)
                || (R1_pos.y == MapConstants.MAP_HEIGHT - 1 && R2_pos.y == MapConstants.MAP_HEIGHT - 1)) {
            return true;
        }
        else {
            return false;
        }

    }

    public int getAlignCount() {
        return alignCount;
    }

    public void setAlignCount(int alignCount) {
        this.alignCount = alignCount;
    }

    public int getTurnAndAlignCount() {
        return turnAndAlignCount;
    }

    public void setTurnAndAlignCount(int counter) {
        this.turnAndAlignCount = counter;
    }

    public boolean getHasTurnAndAlign() {
        return hasTurnAndAlign;
    }

    public void setHasTurnAndAlign(boolean canTurn) {
        this.hasTurnAndAlign = canTurn;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int count) {
        this.imageCount = count;
    }


    //    /**
//     * Robot sensing surrounding obstacles for actual run
//     * @param exploredMap
//     */
//    public void sense(Map exploredMap){
//        // TODO
//        // build JSON
//        // Take note of setting obstacles on and off (different from simulator)
//    }

    /**
     * Get the turn Command(s) for the robot to face the newDir
     * @param newDir Direction robot should face after the command(s) being executed
     * @return
     */
    public ArrayList<Command> getTurn(Direction newDir) {
        ArrayList<Command> commands = new ArrayList<Command>();

        if (newDir == Direction.getAntiClockwise(dir)) {
            commands.add(Command.TURN_LEFT);
        }
        else if (newDir == Direction.getClockwise(dir)) {
            commands.add(Command.TURN_RIGHT);
        }
        else if (newDir == Direction.getOpposite(dir)) {
            commands.add(Command.TURN_LEFT);
            commands.add(Command.TURN_LEFT);
        }
        return commands;
    }

    public String getCommand(Command cmd, int steps) {
        StringBuilder cmdStr = new StringBuilder();

        cmdStr.append(Command.ArduinoMove.values()[cmd.ordinal()]);
//        if (steps > 1) {
//            cmdStr.append(steps);
//        }
        cmdStr.append(steps);
        cmdStr.append('|');

        return cmdStr.toString();
    }

    public boolean isDoingImage() {
        return doingImage;
    }

    public void setDoingImage(boolean img) {
        this.doingImage = img;
    }

    public static void main(String[] args) throws InterruptedException{
        Robot robot = new Robot(true, true,1, 1, Direction.UP);
        System.out.println(robot.status);

        robot.turn(Command.TURN_RIGHT, 1);
        robot.logSensorInfo();
        LOGGER.info(robot.status);
        LOGGER.info(robot.toString());
//        printer.setText(printer.getText() + robot.status + "\n" + robot.toString() + "\n");

        robot.move(Command.FORWARD, 1, null, 1);
//        robot.logSensorInfo();
//        LOGGER.info(robot.status);
//        LOGGER.info(robot.toString());

    }

}
