package Map;

import javafx.scene.paint.Color;

public class MapConstants {

    // Public Map Variables
    public static final short CELL_CM = 10;
    public static final short MAP_HEIGHT = 20;
    public static final short MAP_WIDTH = 15;
    public static final short GOALZONE_ROW = MAP_HEIGHT - 2;
    public static final short GOALZONE_COL = MAP_WIDTH - 2;
    public static final short STARTZONE_ROW = 1;
    public static final short STARTZONE_COL = 1;

    //Graphic Constants
    public static final Color SZ_COLOR = Color.GREEN;	//Start Zone Color
    public static final Color GZ_COLOR = Color.RED;	//Goal Zone Color
    public static final Color UE_COLOR = Color.BURLYWOOD;	//Unexplored Color
   	public static final Color EX_COLOR = Color.WHITE;	//Explored Color
   	public static final Color OB_COLOR = Color.BLACK;	//Obstacle Color
   	public static final Color CW_COLOR = Color.WHITESMOKE;	//Cell Border Color
   	public static final Color WP_COLOR = Color.LIGHTSKYBLUE;	// WayPoint Color
   	public static final Color THRU_COLOR = Color.LIGHTBLUE;
    public static final Color PH_COLOR  = Color.PINK; //Path Color


    public static final int MAP_CELL_SZ = 25;			//Size of the Cells on the Map (Pixels)
    public static final int MAP_OFFSET = 25;

}
