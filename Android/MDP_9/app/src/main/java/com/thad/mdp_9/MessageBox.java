package com.thad.mdp_9;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;

public class MessageBox extends AppCompatActivity {
    private final String TAG = "MessageBox";
    Intent intent;
    private String receivedText = "";
    private String sentText = "";
    private String connStatus;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    ProgressDialog myDialog;
    BluetoothConnectionService mBluetoothConnection;

    // declaration and finding view by id
    TextView messageBoxReceivedTextView;
    TextView messageBoxSentTextView;
    EditText typeBoxEditText;
    Button sendTextBtn;
    Button clearTextBtn;
    TextView connStatusTextView;

    // responsible for maintaining the bluetooth connection, sending the data, and receiving incoming data through input and output streams respectively
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        showLog("Entering onCreateView");
        super.onCreate(savedInstanceState);

        connStatus = "Disconnected";
        setContentView(R.layout.activity_message_box);
        intent = getIntent();

       // set TAG and Mode for shared preferences
        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        // get received text from main activity
        if (sharedPreferences.contains("receivedText"))
            receivedText = sharedPreferences.getString("receivedText", "");
        if (sharedPreferences.contains("sentText"))
            sentText = sharedPreferences.getString("sentText", "");

        // declaration and finding view by id
        messageBoxReceivedTextView = findViewById(R.id.messageBoxReceivedTextView);
        messageBoxSentTextView = findViewById(R.id.messageBoxSentTextView);
        typeBoxEditText = findViewById(R.id.typeBoxEditText);
        sendTextBtn = findViewById(R.id.sendTextBtn);
        clearTextBtn = findViewById(R.id.clearTextBtn);

        // allows scrolling of text view
        messageBoxReceivedTextView.setMovementMethod(new ScrollingMovementMethod());
        messageBoxSentTextView.setMovementMethod(new ScrollingMovementMethod());

        messageBoxReceivedTextView.setText(receivedText);
        messageBoxSentTextView.setText(sentText);

        // for bluetooth
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));
        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);

        // when send button clicked
        sendTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked sendTextBtn");
                sentText = " " + typeBoxEditText.getText().toString();

                sharedPreferences = getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("sentText", sentText);
                editor.commit();
                messageBoxSentTextView.setText(sharedPreferences.getString("sentText", ""));
                typeBoxEditText.setText(" ");

                // if connection exist
                if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
                    byte[] bytes = sentText.getBytes(Charset.defaultCharset());
                    BluetoothConnectionService.write(bytes);
                }
                showLog("Exiting sendTextBtn");
            }
        });

        clearTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked clearTextBtn");
                messageBoxSentTextView.setText("");
                showLog("Exiting clearTextBtn");
            }
        });

        // for toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ImageButton backBtn = findViewById(R.id.backBtn);
        TextView connStatusTextView = findViewById(R.id.connStatusTextView);

        if (sharedPreferences.contains("connStatus"))
            connStatus = sharedPreferences.getString("connStatus", "");
        connStatusTextView.setText(connStatus);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor = sharedPreferences.edit();
                editor.putString("receivedText", messageBoxReceivedTextView.getText().toString());
                editor.putString("sentText",  messageBoxSentTextView.getText().toString());
                editor.commit();
                finish();
            }
        });

        //Progress dialog to show when the bluetooth is disconnected
        myDialog = new ProgressDialog(MessageBox.this);
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    // for receiving from bluetooth
    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // set TAG and Mode for shared preferences
            sharedPreferences = getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            messageBoxReceivedTextView.setText(sharedPreferences.getString("receivedText", ""));
        }
    };

    // for bluetooth
    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();
            TextView connStatusTextView = findViewById(R.id.connStatusTextView);

            if(status.equals("connected")){
                //When the device reconnects, this broadcast will be called again to enter CONNECTED if statement
                //must dismiss the previous dialog that is waiting for connection if not it will block the execution
                try {
                    myDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Log.d(TAG, "mBroadcastReceiver5: Device now connected to "+mDevice.getName());
                Toast.makeText(MessageBox.this, "Device now connected to "+mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected to " + mDevice.getName());
                connStatusTextView.setText("Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Log.d(TAG, "mBroadcastReceiver5: Disconnected from "+mDevice.getName());
                Toast.makeText(MessageBox.this, "Disconnected from "+mDevice.getName(), Toast.LENGTH_LONG).show();
                //start accept thread and wait on the SAME device again
                mBluetoothConnection = new BluetoothConnectionService(MessageBox.this);
                mBluetoothConnection.startAcceptThread();

                // For displaying disconnected for all page
                editor.putString("connStatus", "Disconnected");
                connStatusTextView.setText("Disconnected");

                //show disconnected dialog
                closeKeyboard(MessageBox.this); //NEED TO HIDE KEYBOARD BEFORE SHOWING DIALOG IF NOT THE APP WILL CRASH
                myDialog.show();
            }
            editor.commit();
        }
    };

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);            //unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    // show log message
    private void showLog(String message) {
        Log.d(TAG, message);
    }

    public static void closeKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
