package Map;

public enum Direction {

    // Anti-clockwise
    UP, LEFT, DOWN, RIGHT;

    /**
     * Get the anti-clockwise direction of robot's current direction
     * @param curDirection
     * @return
     */
    public static Direction getAntiClockwise(Direction curDirection) {
        return values()[(curDirection.ordinal() + 1) % values().length];
    }

    /**
     * et the clockwise direction of robot's current direction
     * @param curDirection
     * @return
     */
    public static Direction getClockwise(Direction curDirection) {
        return values()[(curDirection.ordinal() + values().length - 1) % values().length];
    }

    public static Direction getOpposite(Direction curDirection) {
        return values()[(curDirection.ordinal() + 2) % values().length];
    }

}
