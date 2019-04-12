package com.thad.mdp_9;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thad on 21/1/2019.
 */

public class GridMap extends View {

    // declarations of attributes
    // static variable is created only one in the program at the time of loading of class
    private static final String TAG = "GridMap";
    private static final int COL = 15, ROW = 20;
    private static float cellSize;      // indicating the cell size
    private static JSONObject receivedJsonObject = new JSONObject();    // for storing the current map information
    private static JSONObject mapInformation;           // for creating a dummy information and to send information to MapInformation.class
    private static JSONObject backupMapInformation;     // for saving a copy of the received map information
    private static Cell[][] cells;      // for creating cells
    private static String robotDirection = "None";      // indicate the current direction of the robot
    private static int[] startCoord = new int[]{-1, -1};       // 0: col, 1: row
    private static int[] curCoord = new int[]{-1, -1};         // 0: col, 1: row
    private static int[] oldCoord = new int[]{-1, -1};         // 0: col, 1: row
    private static int[] waypointCoord = new int[]{-1, -1};    // 0: col, 1: row
    private static ArrayList<String[]> arrowCoord = new ArrayList<>(); // storing all arrows coordinate
    private static ArrayList<int[]> obstacleCoord = new ArrayList<>(); // storing all obstacles coordinate
    private static boolean autoUpdate = false;          // false: manual mode, true: auto mode
    private static boolean mapDrawn = false;            // false: map not drawn, true: map drawn
    private static boolean canDrawRobot = false;        // false: cannot draw robot, true: can draw robot
    private static boolean setWaypointStatus = false;   // false: cannot set waypoint, true: can set waypoint
    private static boolean startCoordStatus = false;    // false: cannot set starting point, true: can set starting point
    private static boolean setObstacleStatus = false;   // false: cannot set obstacle, true: can set obstacle
    private static boolean unSetCellStatus = false;     // false: cannot unset cell, true: can unset cell
    private static boolean setExploredStatus = false;   // false: cannot check cell, true: can check cell
    private static boolean validPosition = false;       // false: robot out of range, true: robot within range
    private Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_error);   // default image for bitmap

    private Paint blackPaint = new Paint();         // for lines, etc
    private Paint obstacleColor = new Paint();      // black = obstacles position
    private Paint robotColor = new Paint();         // cyan = robot position
    private Paint endColor = new Paint();           // red = end position
    private Paint startColor = new Paint();         // green = start position
    private Paint waypointColor = new Paint();      // yellow = waypoint position
    private Paint unexploredColor = new Paint();    // gray = unexplored position
    private Paint exploredColor = new Paint();      // white = explored position
    private Paint arrowColor = new Paint();         // blue = arrow front position
    private Paint fastestPathColor = new Paint();   // magenta = fastest path position

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    // constructor of grid map
    public GridMap(Context context) {
        super(context);
        init(null);
    }

    // constructor of grid map
    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);   // for lines, etc
        obstacleColor.setColor(Color.BLACK);                // black = obstacles position
        robotColor.setColor(Color.GREEN);                   // black = obstacles position
        endColor.setColor(Color.RED);                       // red = end position
        startColor.setColor(Color.CYAN);                    // green = start position
        waypointColor.setColor(Color.YELLOW);               // yellow = waypoint position
        unexploredColor.setColor(Color.GRAY);               // gray = unexplored position
        exploredColor.setColor(Color.WHITE);                // white = explored position
        arrowColor.setColor(Color.BLACK);                   // black = arrow position
        fastestPathColor.setColor(Color.MAGENTA);           // magenta = fastest path position
    }

    // nullable allows parameter, field or method return value to be null if needed
    private void init(@Nullable AttributeSet attrs) {
        setWillNotDraw(false);
    }

    // to convert from android coordinate to screen coordinate, vice versa
    private int convertRow(int row) {
        return (20 - row);
    }

    // draw the custom view grid map
    @Override
    protected void onDraw(Canvas canvas) {
        showLog("Entering onDraw");
        super.onDraw(canvas);
        showLog("Redrawing map");

        // local variable
        ArrayList<String[]> arrowCoord = this.getArrowCoord();
        int[] curCoord = this.getCurCoord();

        // if map not drawn
        if (!this.getMapDrawn()) {
            canvas.drawColor(Color.parseColor("#D4AF37"));
            // create dummy for arrow coordinate (not sure why)
            String[] dummyArrowCoord = new String[3];
            dummyArrowCoord[0] = "1";
            dummyArrowCoord[1] = "1";
            dummyArrowCoord[2] = "dummy";
            arrowCoord.add(dummyArrowCoord);
            // create cell only when launching the application
            this.createCell();
            // set ending coordinate
            this.setEndCoord(14, 19);
            mapDrawn = true;
        }

        // draw individual cell
        this.drawIndividualCell(canvas);
        // draw grid number
        this.drawGridNumber(canvas);
        // draw robot position
        if (this.getCanDrawRobot())
            this.drawRobot(canvas, curCoord);
        // draw arrow position
        this.drawArrow(canvas, arrowCoord);

        showLog("Exiting onDraw");
    }

    // intialise cell
    private void createCell() {
        showLog("Entering cellCreate");
        cells = new Cell[COL + 1][ROW + 1];
        this.calculateDimension();
        cellSize = this.getCellSize();

        for (int x = 0; x <= COL; x++)
            for (int y = 0; y <= ROW; y++)
                cells[x][y] = new Cell(x * cellSize + (cellSize / 30), y * cellSize + (cellSize / 30), (x + 1) * cellSize, (y + 1) * cellSize, unexploredColor, "unexplored");
        showLog("Exiting createCell");
    }

    // set auto update
    public void setAutoUpdate(boolean autoUpdate) throws JSONException {
        showLog(String.valueOf(backupMapInformation));
        if (!autoUpdate)
            backupMapInformation = this.getReceivedJsonObject();
        else {
            setReceivedJsonObject(backupMapInformation);
            backupMapInformation = null;
            this.updateMapInformation();
        }
        GridMap.autoUpdate = autoUpdate;
    }

    // get auto update
    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    // get message received status
    public boolean getMapDrawn() {
        return mapDrawn;
    }

    // set valid position status
    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    // get valid position status
    public boolean getValidPosition() {
        return validPosition;
    }

    // set unset cell status
    public void setUnSetCellStatus(boolean status) {
        unSetCellStatus = status;
    }

    // get unset cell status
    public boolean getUnSetCellStatus() {
        return unSetCellStatus;
    }

    // set set obstacle status
    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    // get set obstacle status
    public boolean getSetObstacleStatus() {
        return setObstacleStatus;
    }

    // get explored cell status
    public void setExploredStatus(boolean status) {
        setExploredStatus = status;
    }

    // get set obstacle status
    public boolean getExploredStatus() {
        return setExploredStatus;
    }


    // set start coordinate status
    public void setStartCoordStatus(boolean status) {
        startCoordStatus = status;
    }

    // get start coordinate status
    private boolean getStartCoordStatus() {
        return startCoordStatus;
    }

    // set way point status
    public void setWaypointStatus(boolean status) {
        setWaypointStatus = status;
    }

    // get can draw robot boolean value
    public boolean getCanDrawRobot() {
        return canDrawRobot;
    }

    // set ending coordinates
    public void setEndCoord(int col, int row) {
        showLog("Entering setEndCoord");
        //convert to android coordinate
        row = this.convertRow(row);
        // change the color of ending coordinate
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("end");
        showLog("Exiting setEndCoord");
    }

    // set starting coordinates
    public void setStartCoord(int col, int row) {
        showLog("Entering setStartCoord");
        startCoord[0] = col;
        startCoord[1] = row;

        // if starting coordinate not set
        if (this.getStartCoordStatus())
            // convert to android coordinate
            this.setCurCoord(col, row, "right");
        showLog("Exiting setStartCoord");
    }

    // get starting coordinates (for auto/manual)
    private int[] getStartCoord() {
        return startCoord;
    }

    // set robot current coordinates
    public void setCurCoord(int col, int row, String direction) {
        showLog("Entering setCurCoord");
        curCoord[0] = col;
        curCoord[1] = row;
        this.setRobotDirection(direction);
        this.updateRobotAxis(col, row, direction);

        // convert to android coordinate
        row = this.convertRow(row);
        // change the color of robot current coordinate
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("robot");
        showLog("Exiting setCurCoord");
    }

    // for updating the text view when robot changes it's current coordinates
    private void updateRobotAxis(int col, int row, String direction) {
        // for updating the x-axis, y-axis and direction axis (for auto mode)
        TextView xAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.xAxisTextView);
        TextView yAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.yAxisTextView);
        TextView directionAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.directionAxisTextView);

        xAxisTextView.setText(String.valueOf(col));
        yAxisTextView.setText(String.valueOf(row));
        directionAxisTextView.setText(direction);
    }

    // get current coordinate
    public int[] getCurCoord() {
        // screen coordinate
        return curCoord;
    }

    // set direction of the robot
    public void setRobotDirection(String direction) {
        this.sharedPreferences();
        robotDirection = direction;
        editor.putString("direction", direction);
        editor.commit();
        this.invalidate();;
    }

    // get direction of the robot
    public String getRobotDirection() {
        return robotDirection;
    }

    // set waypoint coordinate
    private void setWaypointCoord(int col, int row) throws JSONException {
        showLog("Entering setWaypointCoord");
        waypointCoord[0] = col;
        waypointCoord[1] = row;

        // convert to android coordinate
        row = this.convertRow(row);
        cells[col][row].setType("waypoint");

        // toast is a small message displayed on the screen, similar to a popup notification that remains visible for a short time period
        MainActivity.printMessage("waypoint", waypointCoord[0], waypointCoord[1]);
        showLog("Exiting setWaypointCoord");
    }

    // get waypoint coordinate
    private int[] getWaypointCoord() {
        // screen coordinate
        return waypointCoord;
    }

    // set obstacle coordinate
    private void setObstacleCoord(int col, int row) {
        showLog("Entering setObstacleCoord");
        // screen coordinate
        int[] obstacleCoord = new int[]{col, row};
        GridMap.obstacleCoord.add(obstacleCoord);
        // convert to android coordinate
        row = this.convertRow(row);
        // change the color of obstacle coordinate
        cells[col][row].setType("obstacle");
        showLog("Exiting setObstacleCoord");
    }

    // get obstacle coordinate (screen coordinate)
    private ArrayList<int[]> getObstacleCoord() {
        return obstacleCoord;
    }

    // move robot coordinate
    public void moveRobot(String direction) {
        showLog("Entering moveRobot");
        setValidPosition(false);  // reset it to default value
        int[] curCoord = this.getCurCoord();                        // screen coordinate
        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();   // screen coordinate
        this.setOldRobotCoord(curCoord[0], curCoord[1]);            // screen coordinate
        int[] oldCoord = this.getOldRobotCoord();                   // screen coordinate
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;

        // to move robot if validPosition is true
        switch (robotDirection) {
            case "up":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "right";
                        break;
                    case "back":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "left";
                        break;
                    default:
                        robotDirection = "error up";
                        break;
                }
                break;
            case "right":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "down";
                        break;
                    case "back":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "up";
                        break;
                    default:
                        robotDirection = "error right";
                }
                break;
            case "down":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "left";
                        break;
                    case "back":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "right";
                        break;
                    default:
                        robotDirection = "error down";
                }
                break;
            case "left":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "up";
                        break;
                    case "back":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "down";
                        break;
                    default:
                        robotDirection = "error left";
                }
                break;
            default:
                robotDirection = "error moveCurCoord";
                break;
        }
        // update on current coordinate and robot direction
        if (getValidPosition())
            for (int x = curCoord[0] - 1; x <= curCoord[0] + 1; x++) {
                for (int y = curCoord[1] - 1; y <= curCoord[1] + 1; y++) {
                    for (int i = 0; i < obstacleCoord.size(); i++) {
                        if (obstacleCoord.get(i)[0] != x || obstacleCoord.get(i)[1] != y)
                            setValidPosition(true);
                        else {
                            setValidPosition(false);
                            break;
                        }
                    }
                    if (!getValidPosition())
                        break;
                }
                if (!getValidPosition())
                    break;
            }
        if (getValidPosition())
            this.setCurCoord(curCoord[0], curCoord[1], robotDirection);
        else {
            if (direction.equals("forward") || direction.equals("back"))
                robotDirection = backupDirection;
            this.setCurCoord(oldCoord[0], oldCoord[1], robotDirection);
        }
        this.invalidate();
        showLog("Exiting moveRobot");
    }

    // set old robot coordinate
    private void setOldRobotCoord(int oldCol, int oldRow) {
        showLog("Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        // convert to android coordinate
        oldRow = this.convertRow(oldRow);
        // change the color of robot current coordinate
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("explored");
        showLog("Exiting setOldRobotCoord");
    }

    // get old robot coordinate
    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    // set arrow coordinate
    private void setArrowCoordinate(int col, int row, String arrowDirection) {
        showLog("Entering setArrowCoordinate");
        // screen coordinate
        int[] obstacleCoord = new int[]{col, row};
        String[] arrowCoord = new String[3];     // 0: col, 1: row, 2: face
        arrowCoord[0] = String.valueOf(col);
        arrowCoord[1] = String.valueOf(row);
        arrowCoord[2] = arrowDirection;

        boolean update = true;

        // printing the arrow coordinate on the screen
        for (int i=0; i<this.getArrowCoord().size(); i++)
            if (this.getArrowCoord().get(i)[0].equals(arrowCoord[0]) && this.getArrowCoord().get(i)[1].equals(arrowCoord[1]) && this.getArrowCoord().get(i)[1].equals(arrowCoord[1]))
                update = false;

        if (update)
        {
            if (cells[col][row].type.equals("obstacle")) {
                this.getArrowCoord().add(arrowCoord);
                this.sharedPreferences();
                String message = "(" + String.valueOf(col - 1) + ", " + String.valueOf(row - 1) + ", " + arrowCoord[2] + ")";
                editor.putString("arrow", sharedPreferences.getString("arrow", "") + "\n " + message);
                editor.commit();
            }
        }

        // convert to android coordinate
        row = convertRow(row);
        cells[col][row].setType("arrow");
        showLog("Exiting setArrowCoordinate");
    }

    // get arrow coordinate (screen coordinate)
    private ArrayList<String[]> getArrowCoord() {
        return arrowCoord;
    }

    // draw individual cell
    private void drawIndividualCell(Canvas canvas) {
        showLog("Entering drawIndividualCell");
        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++)
                for (int i = 0; i < this.getArrowCoord().size(); i++)
                    canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);

        showLog("Exiting drawIndividualCell");
    }

    // draw grid number on grid map
    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        // draw x-axis number
        for (int x = 1; x <= COL; x++) {
            // for 2 digit number
            if (x > 10)
                canvas.drawText(Integer.toString(x - 1), cells[x][20].startX + (cellSize / 5), cells[x][20].startY + (cellSize / 3), blackPaint);
            else
                canvas.drawText(Integer.toString(x - 1), cells[x][20].startX + (cellSize / 3), cells[x][20].startY + (cellSize / 3), blackPaint);
        }
        // draw y-axis number
        for (int y = 0; y < ROW; y++) {
            // for 2 digit number
            if ((20 - (y + 1)) > 9)
                canvas.drawText(Integer.toString(20 - (y + 1)), cells[0][y].startX + (cellSize / 2), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
            else
                canvas.drawText(Integer.toString(20 - (y + 1)), cells[0][y].startX + (cellSize / 1.5f), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
        }
        showLog("Exiting drawGridNumber");
    }

    // draw robot position
    private void drawRobot(Canvas canvas, int[] curCoord) {
        showLog("Entering drawRobot");
        // convert to android coordinate
        int androidRowCoord = this.convertRow(curCoord[1]);
        // remove horizontal lines for robot
        for (int y = androidRowCoord; y <= androidRowCoord + 1; y++)
            canvas.drawLine(cells[curCoord[0] - 1][y].startX, cells[curCoord[0] - 1][y].startY - (cellSize / 30), cells[curCoord[0] + 1][y].endX, cells[curCoord[0] + 1][y].startY - (cellSize / 30), robotColor);
        // remove vertical lines for robot
        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++)
            canvas.drawLine(cells[x][androidRowCoord - 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord - 1].startY, cells[x][androidRowCoord + 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord + 1].endY, robotColor);

        // draw robot shape
        switch (this.getRobotDirection()) {
            case "up":
                // draw from bottom left to top center
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, (cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, blackPaint);
                // draw from top center to bottom right
                canvas.drawLine((cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                break;
            case "down":
                // draw from top left to bottom center
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, (cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, blackPaint);
                // draw from bottom center to top right
                canvas.drawLine((cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, blackPaint);
                break;
            case "right":
                // draw from top left to right center
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, blackPaint);
                // draw from right center to bottom left
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, blackPaint);
                break;
            case "left":
                // draw from top right to left center
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, blackPaint);
                // draw from left center to bottom right
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                break;
            default:
                Toast.makeText(this.getContext(), "Error with drawing robot (unknown direction)", Toast.LENGTH_LONG).show();
                break;
        }
        showLog("Exiting drawRobot");
    }

    // draw the arrow images on the respective coordinate
    private void drawArrow(Canvas canvas, ArrayList<String[]> arrowCoord) {
        showLog("Entering drawArrow");
        // RectF holds four float coordinates for a rectangle (left, top, right, bottom)
        RectF rect;

        for (int i = 0; i < arrowCoord.size(); i++) {
            if (!arrowCoord.get(i)[2].equals("dummy")) {
                // convert to android coordinate
                int col = Integer.parseInt(arrowCoord.get(i)[0]);
                int row = convertRow(Integer.parseInt(arrowCoord.get(i)[1]));
                rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);
                switch (arrowCoord.get(i)[2]) {
                    case "up":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_up);
                        break;
                    case "right":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_right);
                        break;
                    case "down":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_down);
                        break;
                    case "left":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_left);
                        break;
                    default:
                        break;
                }
                canvas.drawBitmap(arrowBitmap, null, rect, null);
            }
            showLog("Exiting drawArrow");
        }
    }

    // cell class
    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        String type;

        private Cell(float startX, float startY, float endX, float endY, Paint paint, String type) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
            this.type = type;
        }

        public void setType(String type) {
            this.type = type;
            switch (type) {
                case "obstacle":
                    this.paint = obstacleColor;
                    break;
                case "robot":
                    this.paint = robotColor;
                    break;
                case "end":
                    this.paint = endColor;
                    break;
                case "start":
                    this.paint = startColor;
                    break;
                case "waypoint":
                    this.paint = waypointColor;
                    break;
                case "unexplored":
                    this.paint = unexploredColor;
                    break;
                case "explored":
                    this.paint = exploredColor;
                    break;
                case "arrow":
                    this.paint = arrowColor;
                    break;
                case "fastestPath":
                    this.paint = fastestPathColor;
                    break;
                default:
                    showLog("setTtype default: " + type);
                    break;
            }
        }
    }

    // calculate dimension
    private void calculateDimension() {
        this.setCellSize(getWidth()/(COL+1));
    }

    // set cell size
    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    // get cell size
    private float getCellSize() {
        return cellSize;
    }

    // to refresh the grid map in auto mode
    public void refreshMap() {
        if (this.getAutoUpdate())
            postInvalidateDelayed(500);
    }

    // update map information on auto mode
    public void updateMapInformation() throws JSONException {
        showLog("Entering updateMapInformation");
        // current map information
        JSONObject mapInformation = this.getReceivedJsonObject();
        showLog("updateMapInformation --- mapInformation: " + mapInformation);

        JSONArray infoJsonArray;
        JSONObject infoJsonObject;
        String hexStringExplored, hexStringObstacle, exploredString, obstacleString;
        BigInteger hexBigIntegerExplored, hexBigIntegerObstacle;
        String message;

        if (mapInformation == null)
            return;

        // decoding the map information
        for(int i=0; i<mapInformation.names().length(); i++) {
            message = "updateMapInformation Default message";
            switch (mapInformation.names().getString(i)) {
                // if it contains map array
                case "map":
                    infoJsonArray = mapInformation.getJSONArray("map");
                    infoJsonObject = infoJsonArray.getJSONObject(0);

                    hexStringExplored = infoJsonObject.getString("explored");
                    hexBigIntegerExplored = new BigInteger(hexStringExplored, 16);
                    exploredString = hexBigIntegerExplored.toString(2);
                    showLog("updateMapInformation.exploredString: " + exploredString);

                    // set explored and unexplored cells
                    int x, y;
                    for (int j=0; j<exploredString.length()-4; j++) {
                        // android coordinate
                        y = 19 - (j/15);
                        x = 1 + j - ((19-y)*15);
                        if ((String.valueOf(exploredString.charAt(j+2))).equals("1") && !cells[x][y].type.equals("robot"))  //  && !cells[x][y].type.equals("arrow")
                            cells[x][y].setType("explored");
                        else if ((String.valueOf(exploredString.charAt(j+2))).equals("0") && !cells[x][y].type.equals("robot"))  // && !cells[x][y].type.equals("arrow")
                            cells[x][y].setType("unexplored");
                    }

                    int length = infoJsonObject.getInt("length");

                    hexStringObstacle = infoJsonObject.getString("obstacle");
                    showLog("updateMapInformation hexStringObstacle: " + hexStringObstacle);
                    hexBigIntegerObstacle = new BigInteger(hexStringObstacle, 16);
                    showLog("updateMapInformation hexBigIntegerObstacle: " + hexBigIntegerObstacle);
                    obstacleString = hexBigIntegerObstacle.toString(2);
                    while (obstacleString.length() < length) {
                        obstacleString = "0" + obstacleString;
                    }
                    showLog("updateMapInformation obstacleString: " + obstacleString);

                    int k = 0;
                    for (int row = ROW-1; row >= 0; row--)
                        for (int col = 1; col <= COL; col++)
                            if ((cells[col][row].type.equals("explored")||(cells[col][row].type.equals("robot"))) && k < obstacleString.length()) { // ||cells[col][row].type.equals("arrow")
                                if ((String.valueOf(obstacleString.charAt(k))).equals("1")) //  && !cells[col][row].type.equals("arrow")
                                    this.setObstacleCoord(col, 20 - row);
                                k++;
                            }

                    // set waypoint cells if it exist
                    int[] waypointCoord = this.getWaypointCoord();
                    if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                        cells[waypointCoord[0]][20-waypointCoord[1]].setType("waypoint");
                    break;
                // if it contains robot array
                case "robot":
                    if (canDrawRobot)
                        this.setOldRobotCoord(curCoord[0], curCoord[1]);
                    infoJsonArray = mapInformation.getJSONArray("robot");
                    infoJsonObject = infoJsonArray.getJSONObject(0);

                    // remove old robot color
                    for (int row = ROW-1; row >= 0; row--)
                        for (int col = 1; col <= COL; col++)
                            cells[col][row].setType("unexplored");

                    this.setStartCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    this.setCurCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"), infoJsonObject.getString("direction"));
                    canDrawRobot = true;
                    break;
                // if it contains robot array
                case "waypoint":
                    infoJsonArray = mapInformation.getJSONArray("waypoint");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    this.setWaypointCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    setWaypointStatus = true;
                    break;
                // if it contains obstacle array
                case "obstacle":
                    infoJsonArray = mapInformation.getJSONArray("obstacle");
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        this.setObstacleCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    }
                    message = "No. of Obstacle: " + String.valueOf(infoJsonArray.length());
                    break;
                // if it contains arrow array
                case "arrow":
                    infoJsonArray = mapInformation.getJSONArray("arrow");
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        if (!infoJsonObject.getString("face").equals("dummy")) {
                            this.setArrowCoordinate(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"), infoJsonObject.getString("face"));
                            message = "Arrow:  (" + String.valueOf(infoJsonObject.getInt("x")) + "," + String.valueOf(infoJsonObject.getInt("y")) + "), face: " + infoJsonObject.getString("face");
                        }
                    }
                    break;
                // if it contains move array
                case "move":
                    infoJsonArray = mapInformation.getJSONArray("move");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    if (canDrawRobot)
                        moveRobot(infoJsonObject.getString("direction"));
                    message = "moveDirection: " + infoJsonObject.getString("direction");
                    break;
                // if it contains move array
                case "status":
                    infoJsonArray = mapInformation.getJSONArray("status");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    printRobotStatus(infoJsonObject.getString("status"));
                    message = "status: " + infoJsonObject.getString("status");
                    break;
                default:
                    message = "Unintended default for JSONObject";
                    break;
            }
            /*
            if (!message.equals("updateMapInformation Default message"))
                MainActivity.receiveMessage(message);*/
        }

        showLog("Exiting updateMapInformation");
        this.invalidate();
    }

    // set map information
    public void setReceivedJsonObject(JSONObject receivedJsonObject) {
        showLog("Entered setReceivedJsonObject");
        GridMap.receivedJsonObject = receivedJsonObject;
        // to prevent screen from refreshing with old values
        backupMapInformation = receivedJsonObject;
    }

    // get received map information
    public JSONObject getReceivedJsonObject() {
        return receivedJsonObject;
    }

    // get current map information
    public JSONObject getMapInformation() {
        showLog("getCreateJsonObject() :" + getCreateJsonObject());
        return this.getCreateJsonObject();}

    // print on robot status
    public void printRobotStatus(String message) {
        TextView robotStatusTextView = ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
        robotStatusTextView.setText(message);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        showLog("Entering onTouchEvent");
        if (event.getAction() == MotionEvent.ACTION_DOWN && this.getAutoUpdate() == false) {     // new touch started
            int column = (int) (event.getX() / cellSize);
            int row = this.convertRow((int) (event.getY() / cellSize)); // convert to screen coordinate
            // for toggling the button if it is set
            ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setStartPointToggleBtn);
            ToggleButton setWaypointToggleBtn = ((Activity)this. getContext()).findViewById(R.id.setWaypointToggleBtn);

            // if start coordinate status is true
            if (startCoordStatus) {
                // remove old starting coordinates
                if (canDrawRobot) {
                    // convert to screen coordinates
                    int[] startCoord = this.getStartCoord();
                    if (startCoord[0] >= 2 && startCoord[1] >= 2) {
                        startCoord[1] = this.convertRow(startCoord[1]);
                        for (int x = startCoord[0] - 1; x <= startCoord[0] + 1; x++)
                            for (int y = startCoord[1] - 1; y <= startCoord[1] + 1; y++)
                                cells[x][y].setType("unexplored");
                    }
                }
                else
                    canDrawRobot = true;
                // set new starting coordinates
                this.setStartCoord(column, row);
                // set start coordinate status to false
                startCoordStatus = false;
                // print out the message sent to other device
                try {
                    MainActivity.printMessage("starting", column, row);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // update the axis on the screen
                updateRobotAxis(column, row, "up");
                // if the button is checked, uncheck it
                if (setStartPointToggleBtn.isChecked())
                    setStartPointToggleBtn.toggle();
                this.invalidate();
                return true;
            }
            // if waypoint coordinate status is true
            if (setWaypointStatus) {
               int[] waypointCoord = this.getWaypointCoord();
               // if waypoint coordinate is valid
               if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                   cells[waypointCoord[0]][this.convertRow(waypointCoord[1])].setType("unexplored");
                // set start coordinate status to false
                setWaypointStatus = false;
                // print out the message sent to other device
                try {
                   this.setWaypointCoord(column, row);
               } catch (JSONException e) {
                   e.printStackTrace();
               }
                // if the button is checked, uncheck it
                if (setWaypointToggleBtn.isChecked())
                   setWaypointToggleBtn.toggle();
               this.invalidate();
               return true;
            }
            // if obstacle status is true
            if (setObstacleStatus) {
                this.setObstacleCoord(column, row);
                this.invalidate();
                return true;
            }
            // if explored status is true
            if (setExploredStatus) {
                cells[column][20-row].setType("explored");
                this.invalidate();
                return true;
            }
            // if unset cell status is true
            if (unSetCellStatus) {
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                cells[column][20-row].setType("unexplored");
                for (int i=0; i<obstacleCoord.size(); i++)
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row)
                        obstacleCoord.remove(i);
                this.invalidate();
                return true;
            }
        }
        showLog("Exiting onTouchEvent");
        return false;
    }

    // toggle all button if enabled/checked, except for the clicked button
    public void toggleCheckedBtn(String buttonName) {
        ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setStartPointToggleBtn);
        ToggleButton setWaypointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setWaypointToggleBtn);
        ImageButton obstacleImageBtn = ((Activity)this.getContext()).findViewById(R.id.obstacleImageBtn);
        ImageButton exploredImageBtn = ((Activity)this.getContext()).findViewById(R.id.exploredImageBtn);
        ImageButton clearImageBtn = ((Activity)this. getContext()).findViewById(R.id.clearImageBtn);

        if (!buttonName.equals("setStartPointToggleBtn"))
            if (setStartPointToggleBtn.isChecked()) {
                this.setStartCoordStatus(false);
                setStartPointToggleBtn.toggle();
            }
        if (!buttonName.equals("setWaypointToggleBtn"))
            if (setWaypointToggleBtn.isChecked()) {
                this.setWaypointStatus(false);
                setWaypointToggleBtn.toggle();
            }
        if (!buttonName.equals("exploredImageBtn"))
            if (exploredImageBtn.isEnabled())
                this.setExploredStatus(false);
        if (!buttonName.equals("obstacleImageBtn"))
            if (obstacleImageBtn.isEnabled())
                this.setSetObstacleStatus(false);
        if (!buttonName.equals("clearImageBtn"))
            if (clearImageBtn.isEnabled())
                this.setUnSetCellStatus(false);
    }

    // create jsonobject for map information
    public JSONObject getCreateJsonObject() {
        showLog("Entering getCreateJsonObject");
        String exploredString = "11";
        String obstacleString = "";
        String hexStringObstacle = "";
        String hexStringExplored = "";
        BigInteger hexBigIntegerObstacle, hexBigIntegerExplored;
        int[] waypointCoord = this.getWaypointCoord();
        int[] curCoord = this.getCurCoord();
        String robotDirection = this.getRobotDirection();
        List<int[]> obstacleCoord = new ArrayList<>(this.getObstacleCoord());
        List<String[]> arrowCoord = new ArrayList<>(this.getArrowCoord());

        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);

        // JSONObject to contain individual JSONArray which contains another JSONObject
        // passing of map information
        JSONObject map = new JSONObject();
        for (int y=ROW-1; y>=0; y--)
            for (int x=1; x<=COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot") || cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("arrow"))
                    exploredString = exploredString + "1";
                else
                    exploredString = exploredString + "0";
        exploredString = exploredString + "11";
        showLog("exploredString: " + exploredString);

        hexBigIntegerExplored = new BigInteger(exploredString, 2);
        showLog("hexBigIntegerExplored: " + hexBigIntegerExplored);
        hexStringExplored = hexBigIntegerExplored.toString(16);
        showLog("hexStringExplored: " + hexStringExplored);

        for (int y=ROW-1; y>=0; y--)
            for (int x=1; x<=COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot"))
                    obstacleString = obstacleString + "0";
                else if (cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("arrow"))
                    obstacleString = obstacleString + "1";
        showLog("Before loop: obstacleString: " + obstacleString + ", length: " + obstacleString.length());

        while ((obstacleString.length() % 8) != 0) {
            obstacleString = obstacleString + "0";
        }

        showLog("After loop: obstacleString: " + obstacleString + ", length: " + obstacleString.length());

        if (!obstacleString.equals("")) {
            hexBigIntegerObstacle = new BigInteger(obstacleString, 2);
            showLog("hexBigIntegerObstacle: " + hexBigIntegerObstacle);
            hexStringObstacle = hexBigIntegerObstacle.toString(16);
            if (hexStringObstacle.length() % 2 != 0)
                hexStringObstacle = "0" + hexStringObstacle;
            showLog("hexStringObstacle: " + hexStringObstacle);
        }
        try {
            map.put("explored", hexStringExplored);
            map.put("length", obstacleString.length());
            if (!obstacleString.equals(""))
                map.put("obstacle", hexStringObstacle);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        JSONArray jsonMap = new JSONArray();
        jsonMap.put(map);

        // passing of waypoint coordinates
        JSONArray jsonRobot = new JSONArray();
        if (curCoord[0] >= 2 && curCoord[1] >= 2)
            try {
                JSONObject robot = new JSONObject();
                robot.put("x", curCoord[0]);
                robot.put("y", curCoord[1]);
                robot.put("direction", robotDirection);
                jsonRobot.put(robot);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        // passing of waypoint coordinates
        JSONArray jsonWaypoint = new JSONArray();
        if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
            try {
                JSONObject waypoint = new JSONObject();
                waypoint.put("x", waypointCoord[0]);
                waypoint.put("y", waypointCoord[1]);
                setWaypointStatus = true;
                jsonWaypoint.put(waypoint);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        // passing of obstacle coordinates
        JSONArray jsonObstacle = new JSONArray();
        for (int i=0; i<obstacleCoord.size(); i++)
            try {
                JSONObject obstacle = new JSONObject();
                obstacle.put("x", obstacleCoord.get(i)[0]);
                obstacle.put("y", obstacleCoord.get(i)[1]);
                jsonObstacle.put(obstacle);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        // passing of arrow coordinates
        JSONArray jsonArrow = new JSONArray();
        for (int i=0; i<arrowCoord.size(); i++) {
            try {
                JSONObject arrow = new JSONObject();
                arrow.put("x", Integer.parseInt(arrowCoord.get(i)[0]));
                arrow.put("y", Integer.parseInt(arrowCoord.get(i)[1]));
                arrow.put("face", arrowCoord.get(i)[2]);
                jsonArrow.put(arrow);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // passing of arrow coordinates
        JSONArray jsonStatus = new JSONArray();
        try {
            JSONObject status = new JSONObject();
            status.put("status", robotStatusTextView.getText().toString());
            jsonStatus.put(status);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // JSONObject to contain all the JSONArray
        mapInformation = new JSONObject();
        try {
            mapInformation.put("map", jsonMap);
            mapInformation.put("robot", jsonRobot);
            if (setWaypointStatus) {
                mapInformation.put("waypoint", jsonWaypoint);
                setWaypointStatus = false;
            }
            mapInformation.put("obstacle", jsonObstacle);
            mapInformation.put("arrow", jsonArrow);
            mapInformation.put("status", jsonStatus);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        showLog("Exiting getCreateJsonObject");
        return mapInformation;
    }

    // reset map
    public void resetMap() {
        showLog("Entering resetMap");
        // reset screen text
        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
        ToggleButton manualAutoToggleBtn = ((Activity)this.getContext()).findViewById(R.id.manualAutoToggleBtn);
        Switch phoneTiltSwitch = ((Activity)this.getContext()).findViewById(R.id.phoneTiltSwitch);
        updateRobotAxis(0, 0, "None");
        robotStatusTextView.setText("status");
        sharedPreferences();
        editor.putString("receivedText", "");
        editor.putString("sentText", "");
        editor.putString("arrow", "");
        editor.commit();

        if (manualAutoToggleBtn.isChecked())
            manualAutoToggleBtn.toggle();
        this.toggleCheckedBtn("None");

        if (phoneTiltSwitch.isChecked()) {
            phoneTiltSwitch.toggle();
            phoneTiltSwitch.setText("TILT OFF");
        }

        // reset all the values
        receivedJsonObject = null;      //new JSONObject();
        backupMapInformation = null;    //new JSONObject();
        startCoord = new int[]{-1, -1};         // 0: col, 1: row
        curCoord = new int[]{-1, -1};           // 0: col, 1: row
        oldCoord = new int[]{-1, -1};           // 0: col, 1: row
        robotDirection = "None";        // reset the robot direction
        autoUpdate = false;             // reset it to manual mode
        arrowCoord = new ArrayList<>(); // reset the arrow coordinate array list
        obstacleCoord = new ArrayList<>();  // reset the obstacles coordinate array list
        waypointCoord = new int[]{-1, -1};      // 0: col, 1: row
        mapDrawn = false;           // set map drawn to false
        canDrawRobot = false;       // set can draw robot to false
        validPosition = false;      // set valid position to false
        Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_error);   // default image for bitmap

        showLog("Exiting resetMap");
        this.invalidate();
    }

    // for activating sharedPreferences
    private void sharedPreferences() {
        // set TAG and Mode for shared preferences
        sharedPreferences = this.getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // show log message
    private void showLog(String message) {
        Log.d(TAG, message);
    }
}