package com.thad.mdp_9;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * Created by Thad on 21/1/2019.
 */

public class GridView extends View {

    // declarations of attributes
    // static variable is created only one in the program at the time of loading of class
    private static final String TAG = "GridView";
    private static final int COL = 15, ROW = 20;
    public static float cellSize;
    public static JSONObject mapJsonObject;
    public static Cell[][] cells;
    private ArrayList<String[]> arrowCoord = new ArrayList<>();
    Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_error);   // default image for bitmap

    private String exploredString = "";
    private String obstacleString = "";
    public boolean plotObstacle = false;
    private boolean mapDrawn = false;

    private Paint blackPaint = new Paint();         // for lines, etc
    private Paint whitePaint = new Paint();         // for obstacles numbering
    private Paint obstacleColor = new Paint();      // black = obstacles position
    private Paint waypointColor = new Paint();      // yellow = waypoint position
    private Paint unexploredColor = new Paint();    // gray = unexplored position
    private Paint exploredColor = new Paint();      // white = explored position
    private Paint arrowColor = new Paint();         // blue = arrow front position
    private Paint fastestPathColor = new Paint();   // magenta = fastest path position

    public GridView(Context context) {
        super(context);
        init(null);
    }

    // constructor of grid map
    public GridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);   // for lines, etc
        whitePaint.setColor(Color.WHITE);                   // for obstacles and arrow numbering
        obstacleColor.setColor(Color.BLACK);                // black = obstacle position
        waypointColor.setColor(Color.YELLOW);               // yellow = waypoint position
        unexploredColor.setColor(Color.LTGRAY);             // ltgray = unexplored position
        exploredColor.setColor(Color.WHITE);                // white = explored position
        arrowColor.setColor(Color.BLACK);                   // blue = arrow position
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

        // if map not drawn
        if (!mapDrawn) {
            canvas.drawColor(Color.WHITE);
            // create dummy for arrow coordinate (not sure why)
            String[] dummyArrowCoord = new String[]{};
            this.getArrowCoord().add(dummyArrowCoord);
            // create cell
            this.createCell();
            mapDrawn = true;
        }

        // try catch for setting map for received jsonobject
        try {
            this.setMap(mapJsonObject);
            showLog("setMap try success");
        } catch (JSONException e) {
            e.printStackTrace();
            showLog("setMap try fail");
        }

        // draw horizontal line for each row
        this.drawHorizontalLines(canvas);
        // draw vertical line for each column
        this.drawVerticalLines(canvas);
        // draw grid number
        this.drawGridNumber(canvas);

        // plot the obstacle cell
        if (plotObstacle)
            this.plotObstacle();

        // draw individual cell
        this.drawIndividualCell(canvas);

        // draw number for explored and unexplored cell
        if (!plotObstacle)
            this.drawNumber(canvas);

        // draw number for obstacle and no obstacle cell
        if (plotObstacle) {
            this.drawObstacle(canvas);
            // draw arrow position
            this.drawArrow(canvas, arrowCoord);
        }

        showLog("Exiting onDraw");
    }

    // intialise cell
    public void createCell() {
        showLog("Entering cellCreate");
        cells = new Cell[COL + 1][ROW + 1];
        calculateDimension();

        for (int x = 0; x <= COL; x++)
            for (int y = 0; y <= ROW; y++)
                cells[x][y] = new Cell(x * cellSize + (cellSize / 30), y * cellSize + (cellSize / 30), (x + 1) * cellSize, (y + 1) * cellSize, unexploredColor, "unexplored");
        showLog("Exiting createCell");
    }

    // set map with received jsonobject
    private void setMap(JSONObject mapJsonObject) throws JSONException {
        showLog("Entering setMap");
        this.mapJsonObject = mapJsonObject;
        JSONArray infoJsonArray;
        JSONObject infoJsonObject;
        String hexStringExplored, hexStringObstacle;
        BigInteger hexBigIntegerExplored, hexBigIntegerObstacle;
        String message = "No message received"; // default message

        // iterating through the jsonObject to extract the names for their respective function
        for (int i = 0; i < mapJsonObject.names().length(); i++) {
            switch (mapJsonObject.names().getString(i)) {
                // if it contains map array
                case "map":
                    infoJsonArray = mapJsonObject.getJSONArray("map");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    hexStringExplored = infoJsonObject.getString("explored");
                    hexBigIntegerExplored = new BigInteger(hexStringExplored, 16);
                    exploredString = hexBigIntegerExplored.toString(2);

                    // set explored and unexplored cells
                    int x, y;
                    for (int j=0; j<exploredString.length()-4; j++) {
                        // android coordinate
                        y = 19 - (j/15);
                        x = 1 + j - ((19-y)*15);
                        if ((String.valueOf(exploredString.charAt(j+2))).equals("1"))
                            cells[x][y].setType("explored");
                        else
                            cells[x][y].setType("unexplored");
                    }

                    int length = infoJsonObject.getInt("length");

                    hexStringObstacle = infoJsonObject.getString("obstacle");
                    showLog("hexStringObstacle: " + hexStringObstacle);
                    hexBigIntegerObstacle = new BigInteger(hexStringObstacle, 16);
                    obstacleString = hexBigIntegerObstacle.toString(2);
                    while (obstacleString.length() < length) {
                        obstacleString = "0" + obstacleString;
                    }
                    message = "Explored map:  " + exploredString + "\n" + "Obstacle map:  " + obstacleString;
                    break;
                // if it contains waypoint array
                case "waypoint":
                    infoJsonArray = mapJsonObject.getJSONArray("waypoint");
                    // print waypoint coordinates to the respective cells
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    cells[infoJsonObject.getInt("x")][20 - infoJsonObject.getInt("y")].setType("waypoint");
                    message = "Waypoint:  " + String.valueOf(infoJsonObject.getInt("x")) + String.valueOf(infoJsonObject.getInt("y"));
                    break;
                // if it contains arrow array
                case "arrow":
                    infoJsonArray = mapJsonObject.getJSONArray("arrow");
                    // print arrow coordinates to the respective cells
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        if (!infoJsonObject.getString("face").equals("dummy")) {
                            this.setArrowCoordinate(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"), infoJsonObject.getString("face"));
                            message = "Arrow:  " + String.valueOf(infoJsonObject.getInt("x")) + String.valueOf(infoJsonObject.getInt("y")) + infoJsonObject.getString("face");
                        }
                    }
                    break;
                default:
                    message = "default for JSONObject: " + mapJsonObject.names().getString(i);
                    break;
            }
            showLog(message);
        }
        showLog("Exiting setMap");
    }

    // set arrow coordinate
    public void setArrowCoordinate(int col, int row, String arrowDirection) {
        showLog("Entering setArrowCoordinate");
        // screen coordinate
        String[] arrowCoord = new String[3];     // 0: col, 1: row, 2: face
        arrowCoord[0] = String.valueOf(col);
        arrowCoord[1] = String.valueOf(row);
        arrowCoord[2] = arrowDirection;
        this.getArrowCoord().add(arrowCoord);

        showLog("Exiting setArrowCoordinate");
    }

    // get arrow coordinate (screen coordinate)
    private ArrayList<String[]> getArrowCoord() {
        return this.arrowCoord;
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

    // draw horizontal line for each row
    private void drawHorizontalLines(Canvas canvas) {
        for (int y = 0; y <= ROW; y++)
            canvas.drawLine(cells[1][y].startX, cells[1][y].startY - (cellSize / 30), cells[15][y].endX, cells[15][y].startY - (cellSize / 30), blackPaint);
    }

    // draw vertical line for each column
    private void drawVerticalLines(Canvas canvas) {
        for (int x = 0; x <= COL; x++)
            canvas.drawLine(cells[x][0].startX - (cellSize / 30) + cellSize, cells[x][0].startY - (cellSize / 30), cells[x][0].startX - (cellSize / 30) + cellSize, cells[x][19].endY + (cellSize / 30), blackPaint);
    }

    // draw grid number on grid map
    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        // draw x-axis number
        for (int x = 1; x <= COL; x++) {
            // for 2 digit number
            if (x > 9)
                canvas.drawText(Integer.toString(x), cells[x][20].startX + (cellSize / 4.5f), cells[x][20].startY + (cellSize / 2), blackPaint);
            else
                canvas.drawText(Integer.toString(x), cells[x][20].startX + (cellSize / 3), cells[x][20].startY + (cellSize / 2), blackPaint);
        }
        // draw y-axis number
        for (int y = 0; y < ROW; y++) {
            // for 2 digit number
            if ((20 - y) > 9)
                canvas.drawText(Integer.toString(20 - y), cells[0][y].startX + (cellSize / 2), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
            else
                canvas.drawText(Integer.toString(20 - y), cells[0][y].startX + (cellSize / 1.5f), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
        }
        showLog("Exiting drawGridNumber");
    }

    // draw the arrow images on the respective coordinate
    private void drawArrow(Canvas canvas, ArrayList<String[]> arrowCoord) {
        showLog("Entering drawArrow");
        // RectF holds four float coordinates for a rectangle (left, top, right, bottom)
        RectF rect;

        if (arrowCoord.size() == 0) {
            return;
        }

        for (int i = 1; i < arrowCoord.size(); i++) {
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

    // draw number from cell type
    private void drawNumber(Canvas canvas) {
        showLog("Entering drawNumber");
        for(int x=1; x<=COL; x++)
            for(int y=0; y<ROW; y++)
                switch (cells[x][y].type) {
                    case "unexplored":
                        showLog("unexplored x: " + x + ", y: " + y);
                        canvas.drawText(Integer.toString(0), cells[x][y].startX + (cellSize / 1.3f), cells[x][y].startY + (cellSize / 1.2f), blackPaint);
                        break;
                    case "explored":
                        showLog("explored x: " + x + ", y: " + y);
                        canvas.drawText(Integer.toString(1), cells[x][y].startX + (cellSize / 1.3f), cells[x][y].startY + (cellSize / 1.2f), blackPaint);
                        break;
                    default:
                        showLog("Unexpected default for draw number: " + cells[x][y].type);
                        break;
                }
        showLog("Exiting drawNumber");
    }

    // cell class
    public class Cell {
        float startX, startY, endX, endY;
        String type;
        Paint paint;

        public Cell(float startX, float startY, float endX, float endY, Paint paint, String type) {
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

    // draw obstacle on map
    private void drawObstacle(Canvas canvas) {
        showLog("Entering drawObstacle");
        for(int x=1; x<=COL; x++)
            for(int y=0; y<ROW; y++)
                switch (cells[x][y].type) {
                    case "explored":
                        canvas.drawText(Integer.toString(0), cells[x][y].startX + (cellSize / 1.3f), cells[x][y].startY + (cellSize / 1.2f), blackPaint);
                        break;
                    case "obstacle":
                    case "arrow":
                        canvas.drawText(Integer.toString(1), cells[x][y].startX + (cellSize / 1.3f), cells[x][y].startY + (cellSize / 1.2f), whitePaint);
                        break;
                    default:
                        showLog("Unexpected default for draw obstacle: " + cells[x][y].type);
                        break;
                }
        showLog("Exiting drawObstacle");
    }

    // for ploting of obstacle
    public void plotObstacle() {
        showLog("Entering plotObstacle");
        int k = 0;
        for (int row = ROW-1; row >= 0; row--)
            for (int col = 1; col <= COL; col++) {
                if (cells[col][row].type.equals("explored")) {
                    if ((String.valueOf(obstacleString.charAt(k))).equals("1"))
                        cells[col][row].setType("obstacle");
                    k++;
                }
            }
        showLog("Exiting plotObstacle");
    }

    // calculate dimension
    private void calculateDimension() {
        this.setCellSize(getWidth() / (COL + 1));
    }

    // set cell size
    private void setCellSize(float cellSize) {
        this.cellSize = cellSize;
    }

    // get cell size
    private float getCellSize() {
        return this.cellSize;
    }

    // show log message
    private void showLog(String message) {
        Log.d(TAG, message);
    }
}