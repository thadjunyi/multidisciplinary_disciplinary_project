package Algorithm;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

import Map.*;
import Robot.Robot;
import Robot.Command;
import Robot.RobotConstants;

import static java.lang.Math.*;

public class FastestPath {

    private static final Logger LOGGER = Logger.getLogger(FastestPath.class.getName());

    private boolean sim;
    private Map exploredMap;
    private Robot robot;
    private HashMap<Point, Double> costGMap;
    private HashMap<Cell, Cell> prevCellMap = new HashMap<Cell, Cell>();

    public FastestPath(Map exploredMap, Robot robot, boolean sim) {
        this.exploredMap = exploredMap;
        this.robot = robot;
        this.sim = sim;
        initCostMap();
    }

    public void initCostMap() {
        costGMap = new HashMap<Point, Double>();
        for (int row = 0; row < MapConstants.MAP_HEIGHT; row ++) {
            for (int col = 0; col < MapConstants.MAP_WIDTH; col ++) {
                Cell cell = exploredMap.getCell(row, col);
                if (cell.movableCell()) {
                    costGMap.put(cell.getPos(), 0.0);
                }
                else {
                    costGMap.put(cell.getPos(), RobotConstants.INFINITE_COST);
                }
            }
        }
    }

    public ArrayList<Cell> runAStar(Point start, Point goal, Direction initDir) {
        ArrayList<Cell> toVisit = new ArrayList<Cell>();
        ArrayList<Cell> visited = new ArrayList<Cell>();
        ArrayList<Cell> neighbours;
        double newGtemp, curGtemp;

        // init
        String status = String.format("Finding fastest path from %s to %s, initial direction: %s", start.toString(), goal.toString(), initDir.toString());
        robot.setStatus(status);
        LOGGER.info(status);
        Cell cur = exploredMap.getCell(start);
        toVisit.add(cur);
        Direction curDir = initDir;

        while(!toVisit.isEmpty()) {
            cur = getMinCostCell(toVisit, goal);
            if (prevCellMap.containsKey(cur)) {
                curDir = exploredMap.getCellDir(prevCellMap.get(cur).getPos(), cur.getPos());
            }
            visited.add(cur);
            toVisit.remove(cur);
            // Check whether the goal has been reached
            if(visited.contains(exploredMap.getCell(goal))) {
                LOGGER.info("Path found");
                return getPath(start, goal);
            }
            else {
                neighbours = exploredMap.getNeighbours(cur);
                for (Cell n: neighbours) {
                    if (visited.contains(n)) {
                        continue;
                    }
                    else {
                        newGtemp = costGMap.get(cur.getPos()) + getG(cur.getPos(), n.getPos(), curDir);
                        if (toVisit.contains(n)) {
                            curGtemp = costGMap.get(n.getPos());
                            if (newGtemp < curGtemp) {
                                costGMap.replace(n.getPos(), newGtemp);
                                prevCellMap.replace(n, cur);
                            }
                        }
                        else {
                            prevCellMap.put(n, cur);
                            costGMap.put(n.getPos(), newGtemp);
                            toVisit.add(n);
                        }
                    }
                }
            }

        }

        LOGGER.warning(String.format("Cannot find a fastest path from %s to %s, dir: %s", start.toString(), goal.toString(), initDir.toString()));
        return null;
    }

    //returns the path from the prevCell hashmap, moving backwards from goal to start
    public ArrayList<Cell> getPath(Point start, Point goal) {
        Cell cur = exploredMap.getCell(goal);
        Cell startCell = exploredMap.getCell(start);
        ArrayList<Cell> path = new ArrayList<Cell>();
        while(cur != startCell) {
            path.add(cur);
            cur = prevCellMap.get(cur);
        }
        Collections.reverse(path);
        System.out.println(path);
        return path;
    }

    /**
     * To display the fastest path found on the simulator
     * @param path
     * @param display
     */
    public void displayFastestPath(ArrayList<Cell> path, boolean display) {
        Cell temp;
        System.out.println("Path:");
        for(int i = 0; i < path.size(); i++) {
            temp = path.get(i);
            //Set the path cells to display as path on the Sim
            exploredMap.getCell(temp.getPos()).setPath(display);
            System.out.println(exploredMap.getCell(temp.getPos()).toString());

            //Output Path on console
            if(i != (path.size()-1))
                System.out.print("(" + temp.getPos().y + ", " + temp.getPos().x + ") --> ");
            else
                System.out.print("(" + temp.getPos().y + ", " + temp.getPos().x + ")");
        }
        System.out.println("\n");
    }

    //Returns the movements required to execute the path
    //TODO modify?
    public ArrayList<Command> getPathCommands(ArrayList<Cell> path) throws InterruptedException {
        Robot tempRobot = new Robot(true, true, robot.getPos().y, robot.getPos().x, robot.getDir());
        ArrayList<Command> moves = new ArrayList<Command>();

        Command move;
        Cell cell = exploredMap.getCell(tempRobot.getPos());
        Cell newCell;
        Direction cellDir;

        //Iterate through the path
        for (int i = 0; i < path.size(); i++) {
            newCell = path.get(i);
            cellDir = exploredMap.getCellDir(cell.getPos(), newCell.getPos());
            // If the TempRobot and cell direction not the same
            if (Direction.getOpposite(tempRobot.getDir()) == cellDir) {
//                // 1. use backwards
//                move = Command.BACKWARD;
                move = Command.TURN_LEFT; //first move
                tempRobot.turn(move, RobotConstants.STEP_PER_SECOND);
                moves.add(move);
                tempRobot.turn(move, RobotConstants.STEP_PER_SECOND);
                moves.add(move);
                move = Command.FORWARD; //second move

            } else if (Direction.getClockwise(tempRobot.getDir()) == cellDir) {
                move = Command.TURN_RIGHT; //first move
                tempRobot.turn(move, RobotConstants.STEP_PER_SECOND);
                moves.add(move); //second move
                move = Command.FORWARD;
            } else if (Direction.getAntiClockwise(tempRobot.getDir()) == cellDir) {
                move = Command.TURN_LEFT; //first move
                tempRobot.turn(move, RobotConstants.STEP_PER_SECOND);
                moves.add(move);
                move = Command.FORWARD; //second move
            } else {
                move = Command.FORWARD;
            }
            tempRobot.move(move, RobotConstants.MOVE_STEPS, exploredMap, RobotConstants.STEP_PER_SECOND);
            moves.add(move);
            cell = newCell;
        }
        System.out.println("Generated Moves: " + moves.toString());
        return moves;
    }

    /**
     * Get the cell with min cost from toVisit ArrayList
     * @param toVisit
     * @param goal
     * @return
     */
    private Cell getMinCostCell(ArrayList<Cell> toVisit, Point goal) {
        Cell cell = null;
        Point pos;
        double minCost = RobotConstants.INFINITE_COST;

        for (Cell cellTemp : toVisit) {
            pos = cellTemp.getPos();
            double totalCost = costGMap.get(pos) + getH(pos, goal);
            if(totalCost < minCost) {
                minCost = totalCost;
                cell = cellTemp;
            }
        }
        return cell;
    }

    /**
     * Calculate the cost from Cell A to Cell B given the direction dir
     * @param A
     * @param B
     * @param dir
     * @return  cost from A to B
     */
    private double getG(Point A, Point B, Direction dir) {
        return getMoveCost(A, B) + getTurnCost(dir, exploredMap.getCellDir(A, B));
    }

    /**
     * Calculate the heuristic from a point to the goal;
     * Heuristic - straight line distance
     *
     * @param pt
     * @param goal
     * @return heuristic from pt to goal
     */
    private double getH(Point pt, Point goal) {
        return pt.distance(goal);
    }
    //https://www.geeksforgeeks.org/a-search-algorithm/
    //using Manhattan Distance
    private double getMoveCost(Point A, Point B) {
        double steps =  abs(A.x - B.x) + abs(A.y - B.y);
        return RobotConstants.MOVE_COST * steps;
    }

    private double getTurnCost(Direction dirA, Direction dirB) {

        //Max of 2 turns in either direction, same direction will get 0
        int turns = abs(dirA.ordinal() - dirB.ordinal());

        if(turns > 2) {
            turns %= 2;
        }
        return turns * RobotConstants.TURN_COST;
    }

}
