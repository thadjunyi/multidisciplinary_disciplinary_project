package Map;

import java.io.*;
import java.util.logging.*;

public class MapDescriptor {

    private static final Logger LOGGER = Logger.getLogger(MapDescriptor.class.getName());

    private String hexMapStr1;
    private String hexMapStr2;
    private String filename;

    /**
     * Construct Map descriptor for when there is no input real Map text file
     */
    public MapDescriptor() {
        hexMapStr1 = "";
        hexMapStr2 = "";
        filename = "";
    }

    /**
     * Construct Map descriptor with given real Map text file
     */
    public MapDescriptor(String filename) throws IOException {
        setHexMapStr(filename);
    }

    public String getHexMapStr1() {
        return hexMapStr1;
    }

    public String getHexMapStr2() {
        return hexMapStr2;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setHexMapStr(String filename) throws IOException {
        this.filename = filename;

        FileReader file = new FileReader(filename);
        BufferedReader buf = new BufferedReader(file);

        hexMapStr1 = buf.readLine();
        hexMapStr2 = buf.readLine();

        buf.close();
    }

    /**
     * Right pad "0" to the binary string so that its length is in multiple of 8 (as required)
     * @param biStr
     * @return
     */
    private String rightPadTo8(String biStr) {
        int check = biStr.length() % 8;
        if (check != 0) {
            int to_pad = 8 - check;
            LOGGER.log(Level.FINER, "Length of binary string not divisible by 8.");
            LOGGER.log(Level.FINER, "Length of string: {0}, Right Padding: {1}", new Object[]{biStr.length(), to_pad});
            StringBuilder padding = new StringBuilder();
            for (int i = 0; i < to_pad; i++) {
                padding.append('0');
            }
            biStr += padding.toString();
        }
        return biStr;
    }

    /**
     * Right pad "0" to the binary string so that its length is in multiple of 4
     * @param biStr
     * @return
     */
    private String rightPadTo4(String biStr) {
        int check = biStr.length() % 4;
        if (check != 0) {
            int to_pad = 4 - check;
            LOGGER.log(Level.FINER, "Length of binary string not divisible by 4.");
            LOGGER.log(Level.FINER, "Length of string: {0}, Right Padding: {1}", new Object[]{biStr.length(), to_pad});
            StringBuilder padding = new StringBuilder();
            for (int i = 0; i < to_pad; i++) {
                padding.append('0');
            }
            biStr += padding.toString();
        }
        return biStr;
    }

    /**
     * Left pad "0" to the binary string until its length is multiple of 4 (one hex = 4 bits)
     * @param biStr
     * @return
     */
    private String leftPadTo4(String biStr) {
        int check = biStr.length() % 4;
        if (check != 0) {
            int to_pad = 4 - check;
            LOGGER.log(Level.FINEST, "Length of binary string not divisible by 4.");
            LOGGER.log(Level.FINEST, "Length of string: {0}, Left Padding: {1}", new Object[]{biStr.length(), to_pad});
            StringBuilder padding = new StringBuilder();
            for (int i = 0; i < to_pad; i++) {
                padding.append('0');
            }
            biStr = padding.toString() + biStr;
        }
        return biStr;
    }

    /**
     * Convert 4-bit binary to 1-bit hex
     * @param biStr 8-bit binary string
     * @return 2-bit hex string
     */
    private String biToHex(String biStr) {
        int dec = Integer.parseInt(biStr, 2);
        String hexStr = Integer.toHexString(dec);
//        // pad left 0 if length is 1
//        if (hexStr.length() < 2) {
//            hexStr = "0" + hexStr;
//        }
        return hexStr;
    }

    /**
     * Convert the entire hex string to binary string
     * @param hexStr
     * @return
     */
    private String hexToBi(String hexStr) {
        String biStr = "";
        String tempBiStr = "";
        int tempDec;
        for (int i = 0; i < hexStr.length(); i++) {
            tempDec = Integer.parseInt(Character.toString(hexStr.charAt(i)), 16);
            tempBiStr = Integer.toBinaryString(tempDec);
            biStr += leftPadTo4(tempBiStr);
        }
        return biStr;
    }

    public String generateMDFString1(Map map) {
        StringBuilder MDFcreator1 = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        temp.append("11");
        for (int r = 0; r < MapConstants.MAP_HEIGHT; r++) {
            for (int c = 0; c < MapConstants.MAP_WIDTH; c++) {
                temp.append(map.getCell(r, c).isExplored() ? '1':'0');
                // convert to hex every 8 bits to avoid overflow
                if(temp.length() == 4) {
                    MDFcreator1.append(biToHex(temp.toString()));
                    temp.setLength(0);
                }
            }
        }
        // last byte
        temp.append("11");
        MDFcreator1.append(biToHex(temp.toString()));

        return MDFcreator1.toString();
    }

    public String generateMDFString2(Map map) {
        StringBuilder MDFcreator2 = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        for (int r = 0; r < MapConstants.MAP_HEIGHT; r++) {
            for (int c = 0; c < MapConstants.MAP_WIDTH; c++) {
                if (map.getCell(r, c).isExplored()) {
                    temp.append(map.getCell(r, c).isObstacle() ? '1' : '0');
                    if (temp.length() == 4) {
                        MDFcreator2.append(biToHex(temp.toString()));
                        temp.setLength(0);
                    }
                }
            }
        }

        // last byte
        if(temp.length() % 4 != 0) {
            // right pad to 4 // previously 8
            String tempBiStr = rightPadTo4(temp.toString());
            MDFcreator2.append(biToHex(tempBiStr));
        }

        return MDFcreator2.toString();

    }

    /**
     * Load the explored arena in the Map
     * @param MDFstr1
     * @param map initialized empty Map
     */
    private void loadMDFString1(String MDFstr1, Map map) {
        String expStr = hexToBi(MDFstr1);
        int index = 2;
        for (int r = 0; r < MapConstants.MAP_HEIGHT; r++) {
            for (int c = 0; c < MapConstants.MAP_WIDTH; c++) {
                if (expStr.charAt(index) == '1') {
                    map.getCell(r, c).setExplored(true);
                }
                index++;
            }
        }
    }

    public void loadMDFString2(String MDFstr2, Map map) {
        String obsStr = hexToBi(MDFstr2);
        int index = 0;
        for (int r = 0; r < MapConstants.MAP_HEIGHT; r++) {
            for (int c = 0; c < MapConstants.MAP_WIDTH; c++) {
                Cell cell = map.getCell(r, c);
                if (cell.isExplored()) {
                    if (obsStr.charAt(index) == '1') {
                        cell.setObstacle(true);
                        // create virtual wall
                        map.setVirtualWall(cell, true);
                    }
                    index++;
                }
            }
        }
    }


    /**
     * Load real Map for simulator
     * @param map initialized empty
     */
    public void loadRealMap(Map map) {
        if(filename == "") {
            LOGGER.warning("No MDF found! Map not loaded!\n");
        }
        else {
            loadMDFString1(this.hexMapStr1, map);
            loadMDFString2(this.hexMapStr2, map);
        }
    }

    public void loadRealMap(Map map, String filename) {
        this.filename = filename;
        try {
            setHexMapStr(filename);
        } catch (IOException e) {
            LOGGER.warning("IOException");
            e.printStackTrace();
        }
        loadMDFString1(this.hexMapStr1, map);
        loadMDFString2(this.hexMapStr2, map);
    }

    public void saveRealMap(Map map, String filename) {
        try {

            FileWriter file = new FileWriter(filename);

            BufferedWriter buf = new BufferedWriter(file);
            String mapDes = generateMDFString1(map);
            buf.write(mapDes);
            buf.newLine();

            mapDes = generateMDFString2(map);
            buf.write(mapDes);
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // MDF Testing
    public static void main(String[] args) {
        Map m = new Map();
        String str1 = "ffc07f80ffe1ffc3ffc7fff1fe03fc03f807f80ffe1ffc3ff87ff007e00fc01f803f003e007f";
        String str2 = "0303000000007c07c0400001000000007c0000000000";

        MapDescriptor mdfCoverter = new MapDescriptor();
        mdfCoverter.loadMDFString1(str1, m);
        mdfCoverter.loadMDFString2(str2, m);

        String str1_test = mdfCoverter.generateMDFString1(m);
        String str2_test = mdfCoverter.generateMDFString2(m);

        LOGGER.info(str1);
        LOGGER.info(str1_test);
        LOGGER.info(str2);
        LOGGER.info(str2_test);


    }

}
