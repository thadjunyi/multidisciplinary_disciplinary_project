package Robot;

import Map.Direction;

public class RobotData {
    private int row;
    private int col;
    private Direction dir;

    public RobotData(int row, int col, Direction dir) {
        this.row = row;
        this.col = col;
        this.dir = dir;
    }
}
