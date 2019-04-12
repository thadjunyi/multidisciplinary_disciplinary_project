package com.thad.mdp_9;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
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
import java.util.UUID;

// implements ReconfigureFragment.OnInputListener
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";
    // for transfering of information between activities
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Context context;
    private static boolean autoUpdate = false;

    // declaraction of variable
    private static long exploreTimer;          // for exploration timer
    private static long fastestTimer;          // for fastest timer
    private static boolean startActivityStatus = true;  // to indicate whether an intent should be started
    public String connStatus = "Disconnected";

    // for view by id
    GridMap gridMap;
    MessageBox messageBox;
    TextView connStatusTextView;
    MenuItem bluetoothMenuItem, messageMenuItem, getMapMenuItem;
    TextView exploreTimeTextView, fastestTimeTextView;
    ToggleButton exploreToggleBtn, fastestToggleBtn;
    ImageButton exploreResetImageBtn, fastestResetImageBtn;
    TextView robotStatusTextView;
    ImageButton moveForwardImageBtn, turnRightImageBtn, moveBackwardImageBtn, turnLeftImageBtn;
    Switch phoneTiltSwitch;
    Button resetMapBtn;
    ToggleButton setStartPointToggleBtn, setWaypointToggleBtn;
    TextView xAxisTextView, yAxisTextView, directionAxisTextView;
    ImageButton directionChangeImageBtn, exploredImageBtn, obstacleImageBtn, clearImageBtn;
    static TextView messageSentTextView;
    TextView messageReceivedTextView;
    ToggleButton manualAutoToggleBtn;
    Button f1Btn, f2Btn, reconfigureBtn;

    Intent intent;

    // for bluetooth
    StringBuilder message;
    BluetoothConnectionService mBluetoothConnection;
    private static UUID myUUID;
    BluetoothDevice mBTDevice;
    ProgressDialog myDialog;

    //Sensors for accelerometer
    private Sensor mSensor;
    private SensorManager mSensorManager;

    // runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnableExplore = new Runnable() {
        @Override
        public void run() {
            long millisExplore = System.currentTimeMillis() - exploreTimer;
            int secondsExplore = (int) (millisExplore / 1000);
            int minutesExplore = secondsExplore / 60;
            secondsExplore = secondsExplore % 60;

            exploreTimeTextView.setText(String.format("%02d:%02d", minutesExplore, secondsExplore));

            timerHandler.postDelayed(this, 500);
        }
    };

    Runnable timerRunnableFastest = new Runnable() {
        @Override
        public void run() {
            long millisFastest = System.currentTimeMillis() - fastestTimer;
            int secondsFastest = (int) (millisFastest / 1000);
            int minutesFastest = secondsFastest / 60;
            secondsFastest = secondsFastest % 60;

            fastestTimeTextView.setText(String.format("%02d:%02d", minutesFastest, secondsFastest));

            timerHandler.postDelayed(this, 500);
        }
    };

    // set a timer to refresh the message sent
    Runnable timedMessage = new Runnable(){
        @Override
        public void run() {
            refreshMessage();
            timerHandler.postDelayed(timedMessage, 1000);
        }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        showLog("Entering onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create a new map
        gridMap = new GridMap(this);
        // create a new message box to sync messages
        messageBox = new MessageBox();

        // default value for timer is 0
        exploreTimer = 0;
        fastestTimer = 0;

        // find all view by id
        gridMap = findViewById(R.id.mapView);
        connStatusTextView = findViewById(R.id.connStatusTextView);
        bluetoothMenuItem = findViewById(R.id.bluetoothMenuItem);
        messageMenuItem = findViewById(R.id.messageMenuItem);
        getMapMenuItem = findViewById(R.id.getMapMenuItem);
        exploreTimeTextView = findViewById(R.id.exploreTimeTextView);
        fastestTimeTextView = findViewById(R.id.fastestTimeTextView);
        exploreToggleBtn = findViewById(R.id.exploreToggleBtn);
        exploreResetImageBtn = findViewById(R.id.exploreResetImageBtn);
        fastestToggleBtn = findViewById(R.id.fastestToggleBtn);
        fastestResetImageBtn = findViewById(R.id.fastestResetImageBtn);
        robotStatusTextView = findViewById(R.id.robotStatusTextView);
        moveForwardImageBtn = findViewById(R.id.moveForwardImageBtn);
        turnRightImageBtn = findViewById(R.id.turnRightImageBtn);
        moveBackwardImageBtn = findViewById(R.id.moveBackwardImageBtn);
        turnLeftImageBtn = findViewById(R.id.turnLeftImageBtn);
        phoneTiltSwitch = findViewById(R.id.phoneTiltSwitch);
        resetMapBtn = findViewById(R.id.resetMapBtn);
        setStartPointToggleBtn = findViewById(R.id.setStartPointToggleBtn);
        setWaypointToggleBtn = findViewById(R.id.setWaypointToggleBtn);
        xAxisTextView = findViewById(R.id.xAxisTextView);
        yAxisTextView = findViewById(R.id.yAxisTextView);
        directionAxisTextView = findViewById(R.id.directionAxisTextView);
        directionChangeImageBtn = findViewById(R.id.directionChangeImageBtn);
        exploredImageBtn = findViewById(R.id.exploredImageBtn);
        obstacleImageBtn = findViewById(R.id.obstacleImageBtn);
        clearImageBtn = findViewById(R.id.clearImageBtn);
        messageSentTextView = findViewById(R.id.messageSentTextView);
        messageReceivedTextView = findViewById(R.id.messageReceivedTextView);
        manualAutoToggleBtn = findViewById(R.id.manualAutoToggleBtn);
        f1Btn = findViewById(R.id.f1Btn);
        f2Btn = findViewById(R.id.f2Btn);
        reconfigureBtn = findViewById(R.id.reconfigureBtn);

        MainActivity.context = getApplicationContext();
        this.sharedPreferences();
        // clearing text messages in shared preferences
        editor.putString("sentText", "");
        editor.putString("receivedText", "");
        editor.putString("arrow", "");
        editor.putString("direction","None");
        editor.putString("connStatus", connStatus);
        editor.commit();

        // start the timer for the message to be refreshed after every second
        timerHandler.post(timedMessage);

        // for bluetooth
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        // not used, restore state for states when tilting devices
        if (savedInstanceState != null) {
            showLog("Entering savedInstanceState");
        }

        // for declaraction of fragment manager
        final FragmentManager fragmentManager = getFragmentManager();
        final ReconfigureFragment reconfigureFragment = new ReconfigureFragment();
        final DirectionFragment directionFragment = new DirectionFragment();

        // retrieving values for F1 and F2 if it exist
        if (sharedPreferences.contains("F1")) {
            f1Btn.setContentDescription(sharedPreferences.getString("F1", ""));
            showLog("setText for f1Btn: " + f2Btn.getContentDescription().toString());
        }
        if (sharedPreferences.contains("F2")) {
            f2Btn.setContentDescription(sharedPreferences.getString("F2", ""));
            showLog("setText for f2Btn: " + f2Btn.getContentDescription().toString());
        }

        // allows scrolling of text view
        robotStatusTextView.setMovementMethod(new ScrollingMovementMethod());
        messageSentTextView.setMovementMethod(new ScrollingMovementMethod());
        messageReceivedTextView.setMovementMethod(new ScrollingMovementMethod());

        //Create Sensor Manager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //accelerometer sensor
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // when fastest toggle button clicked
        exploreToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked exploreToggleBtn");
                Button exploreToggleBtn = (Button) view;
                if (exploreToggleBtn.getText().equals("EXPLORE")) {
                    showToast("Exploration timer stop!");
                    timerHandler.removeCallbacks(timerRunnableExplore);
                }
                else if (exploreToggleBtn.getText().equals("STOP")) {
                    showToast("Exploration timer start!");
                    printMessage("XE");
                    exploreTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnableExplore, 0);
                }
                else {
                    showToast("Else statement: " + exploreToggleBtn.getText());
                }
                showLog("Exiting exploreToggleBtn");
            }
        });

        // when explore reset image button clicked
        exploreResetImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked exploreResetImageBtn");
                showToast("Reseting exploration time...");
                exploreTimeTextView.setText("00:00");
                if(exploreToggleBtn.isChecked())
                    exploreToggleBtn.toggle();
                timerHandler.removeCallbacks(timerRunnableExplore);
                showLog("Exiting exploreResetImageBtn");
            }
        });

        // when fastest toggle button clicked
        fastestToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked fastestToggleBtn");
                Button fastestToggleBtn = (Button) view;
                if (fastestToggleBtn.getText().equals("FASTEST")) {
                    showToast("Fastest timer stop!");
                    timerHandler.removeCallbacks(timerRunnableFastest);
                }
                else if (fastestToggleBtn.getText().equals("STOP")) {
                    showToast("Fastest timer start!");
                    printMessage("XF");
                    fastestTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnableFastest, 0);
                }
                else
                    showToast(fastestToggleBtn.getText().toString());
                showLog("Exiting fastestToggleBtn");
            }
        });

        // when fastest reset image button clicked
        fastestResetImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked fastestResetImageBtn");
                showToast("Reseting fastest time...");
                fastestTimeTextView.setText("00:00");
                if (fastestToggleBtn.isChecked())
                    fastestToggleBtn.toggle();
                timerHandler.removeCallbacks(timerRunnableFastest);
                showLog("Exiting fastestResetImageBtn");
            }
        });

        // when move forward image button clicked
        moveForwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveForwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("forward");
                    refreshLabel();
                    if (gridMap.getValidPosition())
                        updateStatus("moving forward");
                    else
                        updateStatus("Unable to move forward");
                    printMessage("AW1|");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting moveForwardImageBtn");
            }
        });

        // when turn right image button clicked
        turnRightImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked turnRightImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("right");
                    refreshLabel();
                    updateStatus("turning right");
                    printMessage("AD1|");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting turnRightImageBtn");
            }
        });

        // when move backaward image button clicked
        moveBackwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveBackwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("back");
                    refreshLabel();
                    if (gridMap.getValidPosition())
                        updateStatus("moving backward");
                    else
                        updateStatus("Unable to move backward");
                    printMessage("AS1|");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting moveBackwardImageBtn");
            }
        });

        // when turn left image button clicked
        turnLeftImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked turnLeftImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("left");
                    refreshLabel();
                    updateStatus("turning left");
                    printMessage("AA1|");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting turnLeftImageBtn");
            }
        });

        // when phone tilt switch button clicked
        phoneTiltSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    compoundButton.setText("TILT ON");
                }else
                {
                    compoundButton.setText("TILT OFF");
                }
                if (gridMap.getAutoUpdate()) {
                    updateStatus("Please press 'MANUAL'");
                    phoneTiltSwitch.setChecked(false);
                }
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    if(phoneTiltSwitch.isChecked()){
                        showToast("Tilt motion control: ON");
                        phoneTiltSwitch.setPressed(true);

                        //register sensor when toggled ON
                        mSensorManager.registerListener(MainActivity.this, mSensor, mSensorManager.SENSOR_DELAY_NORMAL);
                        //start a runnable that will change boolean flag to true to allow onSensorChanged code to execute every 1-2 seconds
                        sensorHandler.post(sensorDelay);
                    }else{
                        showToast("Tilt motion control: OFF");
                        showLog("unregistering Sensor Listener");
                        try {
                            //unregister when button clicked to save battery since the sensor is very power consuming.
                            mSensorManager.unregisterListener(MainActivity.this);
                        }catch(IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                        //stops the runnable loop
                        sensorHandler.removeCallbacks(sensorDelay);
                    }
                } else {
                    updateStatus("Please press 'STARTING POINT'");
                    phoneTiltSwitch.setChecked(false);
                }
            }
        });

        // when reset map button clicked
        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked resetMapBtn");
                showToast("Reseting map...");
                gridMap.resetMap();
            }
        });

        // when set starting point toggle button clicked
        setStartPointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setStartPointToggleBtn");
                if (setStartPointToggleBtn.getText().equals("STARTING POINT"))
                    showToast("Cancelled selecting starting point");
                else if (setStartPointToggleBtn.getText().equals("CANCEL") && !gridMap.getAutoUpdate()) {
                    showToast("Please select starting point");
                    gridMap.setStartCoordStatus(true);
                    gridMap.toggleCheckedBtn("setStartPointToggleBtn");
                } else
                    showToast("Please select manual mode");
                showLog("Exiting setStartPointToggleBtn");
            }
        });

        // when set waypoint toggle button clicked
        setWaypointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setWaypointToggleBtn");
                if (setWaypointToggleBtn.getText().equals("WAYPOINT"))
                    showToast("Cancelled selecting waypoint");
                else if (setWaypointToggleBtn.getText().equals("CANCEL")) {
                    showToast("Please select waypoint");
                    gridMap.setWaypointStatus(true);
                    gridMap.toggleCheckedBtn("setWaypointToggleBtn");
                }
                else
                    showToast("Please select manual mode");
                showLog("Exiting setWaypointToggleBtn");
            }
        });

        // when direction change button clicked
        directionChangeImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked directionChangeImageBtn");
                // for fragment view
                directionFragment.show(fragmentManager, "Direction Fragment");
                showLog("Exiting directionChangeImageBtn");
            }
        });

        // when explored button clicked
        exploredImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked exploredImageBtn");
                if (!gridMap.getExploredStatus()) {
                    showToast("Please check cell");
                    gridMap.setExploredStatus(true);
                    gridMap.toggleCheckedBtn("exploredImageBtn");
                }
                else if (gridMap.getExploredStatus())
                    gridMap.setSetObstacleStatus(false);
                showLog("Exiting exploredImageBtn");
            }
        });

        // when obstacle plot button clicked
        obstacleImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked obstacleImageBtn");
                if (!gridMap.getSetObstacleStatus()) {
                    showToast("Please plot obstacles");
                    gridMap.setSetObstacleStatus(true);
                    gridMap.toggleCheckedBtn("obstacleImageBtn");
                }
                else if (gridMap.getSetObstacleStatus())
                    gridMap.setSetObstacleStatus(false);
                showLog("Exiting obstacleImageBtn");
            }
        });

        // when clear button clicked
        clearImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked clearImageBtn");
                if (!gridMap.getUnSetCellStatus()) {
                    showToast("Please remove cells");
                    gridMap.setUnSetCellStatus(true);
                    gridMap.toggleCheckedBtn("clearImageBtn");
                }
                else if (gridMap.getUnSetCellStatus())
                    gridMap.setUnSetCellStatus(false);
                showLog("Exiting clearImageBtn");
            }
        });

        // when manual auto button clicked
        manualAutoToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked manualAutoToggleBtn");
                if (manualAutoToggleBtn.getText().equals("MANUAL")) {
                    try {
                        gridMap.setAutoUpdate(true);
                        autoUpdate = true;
                        gridMap.toggleCheckedBtn("None");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("AUTO mode");
                }
                else if (manualAutoToggleBtn.getText().equals("AUTO")) {
                    try {
                        gridMap.setAutoUpdate(false);
                        autoUpdate = false;
                        gridMap.toggleCheckedBtn("None");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("MANUAL mode");
                }
                showLog("Exiting manualAutoToggleBtn");
            }
        });

        // when f1 button clicked
        f1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked f1Btn");
                if (!f1Btn.getContentDescription().toString().equals("empty"))
                    printMessage(f1Btn.getContentDescription().toString());
                showLog("f1Btn value: " + f1Btn.getContentDescription().toString());
                showLog("Exiting f1Btn");
            }
        });

        // when f2 button clicked
        f2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked f2Btn");
                if (!f2Btn.getContentDescription().toString().equals("empty"))
                    printMessage(f2Btn.getContentDescription().toString());
                showLog("f2Btn value: " + f2Btn.getContentDescription().toString());
                showLog("Exiting f2Btn");
            }
        });

        // when reconfigure button clicked
        reconfigureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked reconfigureBtn");
                // for fragment view
                reconfigureFragment.show(fragmentManager, "Reconfigure Fragment");
                showLog("Exiting reconfigureBtn");
            }
        });

        // for toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ImageButton backBtn = findViewById(R.id.backBtn);

        // return to home main menu
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory( Intent.CATEGORY_HOME );
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
            }
        });

        //Progress dialog to show when the bluetooth is disconnected
        myDialog = new ProgressDialog(MainActivity.this);
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    // for refreshing all the label in the screen
    private void refreshLabel() {
        xAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[0]));
        yAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[1]));
        directionAxisTextView.setText(sharedPreferences.getString("direction",""));
    }

    // for refreshing the message sent and received after a certain time
    public void refreshMessage() {
        // get received text from main activity
        messageReceivedTextView.setText(sharedPreferences.getString("receivedText", ""));
        //messageSentTextView.setText(sharedPreferences.getString("sentText", ""));
        messageSentTextView.setText(sharedPreferences.getString("arrow", ""));
        directionAxisTextView.setText(sharedPreferences.getString("direction",""));
        connStatusTextView.setText(sharedPreferences.getString("connStatus", ""));
        // send a request for update map every 1 second
        /*
        if (autoUpdate && BluetoothConnectionService.BluetoothConnectionStatus == true) {
            String message = "XS";
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }
        */
    }

    // for refreshing the direction of the robot
    public void refreshDirection(String direction) {
        gridMap.setRobotDirection(direction);
        printMessage("Direction is set to " + direction);
    }

    // for updating the displaying for robot status
    private void updateStatus(String message) {
        //robotStatusTextView.setText(message);
    }

    // print on message received
    public static void receiveMessage(String message) {
        showLog("Entering receiveMessage");
        sharedPreferences();
        editor.putString("receivedText", sharedPreferences.getString("receivedText", "") + "\n " + message);
        editor.commit();
        showLog("Exiting receiveMessage");
    }

    // print message on message sent
    public static void printMessage(String name, int x, int y) throws JSONException {
        showLog("Entering printMessage");
        sharedPreferences();

        JSONObject jsonObject = new JSONObject();
        String message;

        switch(name) {
            case "starting":
            case "waypoint":
                jsonObject.put(name, name);
                jsonObject.put("x", x);
                jsonObject.put("y", y);
                message = name + " (" + x + "," + y + ")";
                break;
            default:
                message = "Unexpected default for printMessage: " + name;
                break;
        }
        editor.putString("sentText", sharedPreferences.getString("sentText", "") + "\n " + message);
        editor.commit();
        printMessage("X" + String.valueOf(jsonObject));
        /*
        if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }*/
        showLog("Exiting printMessage");
    }
    public static void printMessage(String message) {
        showLog("Entering printMessage");
        sharedPreferences();

        if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }
        showLog(message);
        editor.putString("sentText", sharedPreferences.getString("sentText", "") + "\n " + message);
        editor.commit();
        showLog("Exiting printMessage");
    }

    // for closing keyboard (not used)
    // type closeKeyboard();
    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        // if view is focused
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.example_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.sharedPreferences();
        switch(item.getItemId()) {
            case R.id.bluetoothMenuItem:
                //currentActivity = false;
                showToast("Entering Bluetooth Configuration...");
                intent = new Intent(MainActivity.this, BluetoothPopUp.class);
                startActivityStatus = false;
                startActivityForResult(intent, 1);
                break;
            case R.id.messageMenuItem:
                showToast("Message Box selected");
                intent = new Intent(MainActivity.this, MessageBox.class);
                editor.putString("receivedText", messageReceivedTextView.getText().toString());
                break;
            case R.id.getMapMenuItem:
                showToast("Get Map Information selected");
                intent = new Intent(MainActivity.this, MapInformation.class);
                break;
            case R.id.menuMenuItem:
                startActivityStatus = false;
                break;
            case R.id.saveMapMenuItem:
                showToast("Map saved");
                showLog("saveMapMenuItem: " + String.valueOf(gridMap.getMapInformation()));
                editor.putString("mapSaved", String.valueOf(gridMap.getMapInformation()));
                startActivityStatus = false;
                break;
            case R.id.loadMapMenuItem:
                if(sharedPreferences.contains("mapSaved")) {
                    try {
                        showLog("loadMapMenuItem: " + sharedPreferences.getString("mapSaved", ""));
                        gridMap.setReceivedJsonObject(new JSONObject(sharedPreferences.getString("mapSaved", "")));
                        gridMap.updateMapInformation();
                        showToast("Map loaded");
                        showLog("loadMapMenuItem try success");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showLog("loadMapMenuItem try fail");
                    }
                }
                else
                    showToast("No map found");
                startActivityStatus = false;
                break;

            default:
                showToast("onOptionsItemSelected has reached default");
                return false;
        }
        // pass information to activity
        editor.putString("mapJsonObject", String.valueOf(gridMap.getCreateJsonObject()));
        editor.putString("connStatus", connStatusTextView.getText().toString());
        editor.commit();
        if (startActivityStatus)
            startActivity(intent);
        startActivityStatus = true;
        return super.onOptionsItemSelected(item);
    }

    // for activating sharedPreferences
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    public static void sharedPreferences() {
        // set TAG and Mode for shared preferences
        sharedPreferences = MainActivity.getSharedPreferences(MainActivity.context);
        editor = sharedPreferences.edit();
    }

    // show toast message
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // show log message
    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    // for bluetooth
    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences();

            if(status.equals("connected")){
                //When the device reconnects, this broadcast will be called again to enter CONNECTED if statement
                //must dismiss the previous dialog that is waiting for connection if not it will block the execution
                try {
                    myDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Log.d(TAG, "mBroadcastReceiver5: Device now connected to "+mDevice.getName());
                Toast.makeText(MainActivity.this, "Device now connected to "+mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected to " + mDevice.getName());
                connStatusTextView.setText("Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Log.d(TAG, "mBroadcastReceiver5: Disconnected from "+mDevice.getName());
                Toast.makeText(MainActivity.this, "Disconnected from "+mDevice.getName(), Toast.LENGTH_LONG).show();
                //start accept thread and wait on the SAME device again
                mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
                mBluetoothConnection.startAcceptThread();

                // For displaying disconnected for all page
                editor.putString("connStatus", "Disconnected");
                TextView connStatusTextView = findViewById(R.id.connStatusTextView);
                connStatusTextView.setText("Disconnected");

                myDialog.show();
            }
            editor.commit();
        }
    };

    // for receiving from bluetooth
    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
            showLog("receivedMessage: message --- " + message);
            try {
                if (message.length() > 7 && message.substring(2,6).equals("grid")) {
                    String resultString = "";
                    String amdString = message.substring(11,message.length()-2);
                    showLog("amdString: " + amdString);
                    BigInteger hexBigIntegerExplored = new BigInteger(amdString, 16);
                    String exploredString = hexBigIntegerExplored.toString(2);

                    while (exploredString.length() < 300)
                        exploredString = "0" + exploredString;

                    for (int i=0; i<exploredString.length(); i=i+15) {
                        int j=0;
                        String subString = "";
                        while (j<15) {
                            subString = subString + exploredString.charAt(j+i);
                            j++;
                        }
                        resultString = subString + resultString;
                    }
                    hexBigIntegerExplored = new BigInteger(resultString, 2);
                    resultString = hexBigIntegerExplored.toString(16);

                    JSONObject amdObject = new JSONObject();
                    amdObject.put("explored", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
                    amdObject.put("length", amdString.length()*4);
                    amdObject.put("obstacle", resultString);
                    JSONArray amdArray = new JSONArray();
                    amdArray.put(amdObject);
                    JSONObject amdMessage = new JSONObject();
                    amdMessage.put("map", amdArray);
                    message = String.valueOf(amdMessage);
                    showLog("Executed for AMD message, message: " + message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (gridMap.getAutoUpdate()) {
                try {
                    gridMap.setReceivedJsonObject(new JSONObject(message));
                    gridMap.updateMapInformation();
                    showLog("messageReceiver: try decode successful");
                } catch (JSONException e) {
                    showLog("messageReceiver: try decode unsuccessful");
                }
            }
            sharedPreferences();
            String receivedText = sharedPreferences.getString("receivedText", "") + "\n " + message;
            editor.putString("receivedText", receivedText);
            editor.commit();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 1:
                if(resultCode == Activity.RESULT_OK){
                    mBTDevice = (BluetoothDevice) data.getExtras().getParcelable("mBTDevice");
                    myUUID = (UUID) data.getSerializableExtra("myUUID");
                }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
            //unregister sensor in case not turned off.
            mSensorManager.unregisterListener(this);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    //unregister bluetooth connection status broadcast when the activity switches
    @Override
    protected void onPause(){
        super.onPause();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    //register bluetooth connection status broadcast when the activity resumes
    @Override
    protected void onResume(){
        super.onResume();
        try{
            //Broadcasts when bluetooth state changes (connected, disconnected etc) custom receiver
            IntentFilter filter2 = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    // for saving states when changing activity
    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putString(TAG, "onSaveInstanceState");
        showLog("Exiting onSaveInstanceState");
    }

    //ACCELEROMETER SENSOR
    Handler sensorHandler = new Handler();
    boolean sensorFlag= false;

    private final Runnable sensorDelay = new Runnable() {
        @Override
        public void run() {
            //sets flag to true to execute the codes in onSensorChanged.
            sensorFlag = true;
            //calls sensorDelay again to execute 1 seconds later
            sensorHandler.postDelayed(this,1000); //1 seconds
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        showLog("SensorChanged X: "+x);
        showLog("SensorChanged Y: "+y);
        showLog("SensorChanged Z: "+z);

        if(sensorFlag) {
            //x,y,z values are based on how easy it is to move the wrist for e.g. tilting device forward is easier so y<-2
            if (y < -2) {
                //move forward
                showLog("Sensor Move Forward Detected");
                gridMap.moveRobot("forward");
                refreshLabel();
                printMessage("AW1|");
            } else if (y > 2) {
                //move backward
                showLog("Sensor Move Backward Detected");
                gridMap.moveRobot("back");
                refreshLabel();
                printMessage("AS1|");

            } else if (x > 2) {
                //move left
                showLog("Sensor Move Left Detected");
                gridMap.moveRobot("left");
                refreshLabel();
                printMessage("AA1|");

            } else if (x < -2) {
                //move right
                showLog("Sensor Move Right Detected");
                gridMap.moveRobot("right");
                refreshLabel();
                printMessage("AD1|");
            }
        }
        //set flag back to false so that it wont execute the code above until 1-2 seconds later
        sensorFlag = false;
    }
    //must declare this method or will have error. not using this method so its blank
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
