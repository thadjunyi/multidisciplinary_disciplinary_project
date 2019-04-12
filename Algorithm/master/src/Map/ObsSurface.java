package Map;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;

public class ObsSurface {

    private Point pos;
    private Direction surface;

    public ObsSurface(Point pos, Direction surface) {
        this.pos = pos;
        this.surface = surface;
    }


    public ObsSurface(int row, int col, Direction surface) {
        this.pos = new Point(col, row);
        this.surface = surface;
    }

    @Override
    public String toString() {
        return String.format("%d|%d|%s", this.pos.y, this.pos.x, this.surface.toString());   // row|col|surface
    }

    public Point getPos() {
        return pos;
    }

    public int getRow() {
        return this.pos.y;
    }

    public int getCol() {
        return this.pos.x;
    }

    public Direction getSurface() {
        return surface;
    }

    public void setPos(Point pt) {
        this.pos = pt;
    }

    public void setSurface(Direction dir) {
        this.surface = dir;
    }

    public void setPos(int row, int col) {
        this.pos = new Point(col, row);
    }
}
