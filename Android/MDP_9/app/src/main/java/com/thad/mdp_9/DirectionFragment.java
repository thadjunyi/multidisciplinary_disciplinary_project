package com.thad.mdp_9;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Thad on 25/1/2019.
 */

public class DirectionFragment extends DialogFragment {

    private static final String TAG = "DirectionFragment";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    // declaration of variables
    Button saveBtn, cancelDirectionBtn;
    EditText directionValueEditView;
    String direction = "";
    View rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        showLog("Entering onCreateView");
        // hide keyboard
        // getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        // create dialog fragment view
        rootView = inflater.inflate(R.layout.activity_direction, container, false);
        super.onCreate(savedInstanceState);

        // set title
        getDialog().setTitle("Change Direction");
        // set TAG and Mode for shared preferences
        sharedPreferences = getActivity().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // find all view by id
        saveBtn = rootView.findViewById(R.id.saveBtn);
        cancelDirectionBtn = rootView.findViewById(R.id.cancelDirectionBtn);
        directionValueEditView = rootView.findViewById(R.id.directionValueEditText);
        //final TextView directionAxisTextView = ((Activity)getActivity()).findViewById(R.id.directionAxisTextView);

        directionValueEditView.setSelectAllOnFocus(true);
        direction = sharedPreferences.getString("direction","");
        directionValueEditView.setText(direction);

        // not used, restore state for states when tilting devices
        if (savedInstanceState != null)
            direction = savedInstanceState.getString("direction");

        // when save button clicked
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked saveBtn");
                direction = String.valueOf(directionValueEditView.getText());
                editor.putString("direction",direction);
                directionValueEditView.setText(direction);
                ((MainActivity)getActivity()).refreshDirection(direction);
                Toast.makeText(getActivity(), "Saving direction...", Toast.LENGTH_SHORT).show();
                showLog("Exiting saveBtn");
                editor.commit();
                // close dialog fragment
                getDialog().dismiss();
            }
        });

        // when cancel button clicked
        cancelDirectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked cancelDirectionBtn");
                directionValueEditView.setText(direction);
                showLog( "Exiting cancelDirectionBtn");
                // close dialog fragment
                getDialog().dismiss();
            }
        });
        showLog("Exiting onCreateView");
        return rootView;
    }

    // not used, for saving states when tilting devices
    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);
        saveBtn = rootView.findViewById(R.id.saveBtn);
        showLog("Exiting onSaveInstanceState");
        outState.putString(TAG, direction);
    }

    // when exiting dialogfragment
    @Override
    public void onDismiss(DialogInterface dialog) {
        showLog("Entering onDismiss");
        super.onDismiss(dialog);
        directionValueEditView.clearFocus();
        showLog("Exiting onDismiss");
    }

    /*
    // try catch method
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            onInputListener = (OnInputListener) getActivity();
        }catch (ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException: " + e.getMessage() );
        }
    }*/

    // show log message
    private void showLog(String message) {
        Log.d(TAG, message);
    }
}

