package Robot;

import java.awt.*;

import Map.*;

public class Sensor {

    private String id;
    private int minRange;
    private int maxRange;

    private Point pos;
    private Direction sensorDir;

    public Sensor(String id, int minRange, int maxRange, int sensorPosRow, int sensorPosCol, Direction sensorDir) {
        this.id = id;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.pos = new Point(sensorPosCol, sensorPosRow);
        this.sensorDir = sensorDir;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMinRange() {
        return minRange;
    }

    public void setMinRange(int minRange) {
        this.minRange = minRange;
    }

    public int getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(int maxRange) {
        this.maxRange = maxRange;
    }

    public Point getPos() {
        return pos;
    }

    public int getRow() {
        return pos.y;
    }

    public int getCol() {
        return pos.x;
    }

    public void setPos(int row, int col) {
        this.pos.setLocation(col, row);
    }

    public Direction getSensorDir() {
        return sensorDir;
    }

    public void setSensorDir(Direction sensorDir) {
        this.sensorDir = sensorDir;
    }

    @Override
    public String toString() {
        String s = String.format("Sensor %s at %s facing %s\n", id, pos.toString(), sensorDir.toString());
        return s;
    }

    public int detect(Map map) {

        for (int cur = minRange; cur <= maxRange; cur++) {

            switch (sensorDir) {
                case UP:
//                    if (pos.y + cur > MapConstants.MAP_HEIGHT - 1)
//                        return -1;
                    if (pos.y + cur == MapConstants.MAP_HEIGHT)
                        return cur;
                    else if (map.getCell(pos.y + cur, pos.x).isObstacle())
                        return cur;
                    break;
                case RIGHT:
//                    if (pos.x + cur > MapConstants.MAP_WIDTH - 1)
//                        return -1;
                    if (pos.x + cur == MapConstants.MAP_WIDTH)
                        return cur;
                    else if (map.getCell(pos.y, pos.x + cur).isObstacle())
                        return cur;
                    break;
                case DOWN:
//                    if (pos.y - cur < 0)
//                        return -1;
                    if (pos.y - cur == -1)
                        return cur;
                    else if (map.getCell(pos.y - cur, pos.x).isObstacle())
                        return cur;
                    break;
                case LEFT:
//                    if (pos.x - cur < 0)
//                        return -1;
                    if (pos.x - cur == -1)
                        return cur;
                    else if (map.getCell(pos.y, pos.x - cur).isObstacle())
                        return cur;
                    break;
            }
        }
        return -1;
    }
}
