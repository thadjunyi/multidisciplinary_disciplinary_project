package Map;

import java.awt.Point;

public class Cell {
    // Position Variables
    private Point pos;

    // Exploration Booleans
    private boolean explored;
    private boolean obstacle;
    private boolean virtualWall;
    private boolean wayPoint;
    private boolean moveThru;
    private boolean path;

    // Constructor
    public Cell(Point pos) {
        this.pos = pos;
        this.explored = false;  // initially all cells are unexplored
    }

    // Getters and Setters
    public Point getPos() {
        return pos;
    }

    public void setPos(){
        this.pos = pos;
    }

    public boolean isExplored() {
        return explored;
    }

    public void setExplored(boolean explored) {
        this.explored = explored;
    }

    public boolean isObstacle() {
        return obstacle;
    }

    public void setObstacle(boolean obstacle) {
        this.obstacle = obstacle;
    }

    public boolean isVirtualWall() {
        return virtualWall;
    }

    public void setVirtualWall(boolean virtualWall) {
        this.virtualWall = virtualWall;
    }

    public boolean isWayPoint() {
        return wayPoint;
    }

    public boolean setWayPoint(boolean isWayPoint) {
        if(!obstacle && explored && !virtualWall) {
            this.wayPoint = isWayPoint;
            return true;
        }
        return false;
    }

    public boolean isMoveThru() {
        return moveThru;
    }

    public void setMoveThru(boolean moveThru) {
        this.moveThru = moveThru;
    }

    public boolean isPath() {
        return path;
    }

    public void setPath(boolean path) {
        this.path = path;
    }

    // Cell is movable is it has been explored and it is not obstacle or virtual wall
    public boolean movableCell() {
        return explored && !obstacle && !virtualWall;
    }

    @Override
    public String toString() {
        return "Cell [pos=" + pos + ", explored=" + explored + ", obstacle=" + obstacle + ", virtualWall=" + virtualWall
                + ", isWayPoint=" + wayPoint + ", moveThru=" + moveThru + ", path=" + path + "]";
    }
}
