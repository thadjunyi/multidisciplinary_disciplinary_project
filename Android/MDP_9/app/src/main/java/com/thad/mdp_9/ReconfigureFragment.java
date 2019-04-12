package com.thad.mdp_9;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ReconfigureFragment extends DialogFragment {
    private static final String TAG = "ReconfigureFragment";
    // for saving values
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    // declaration of variables
    Button saveBtn, cancelReconfigureBtn;
    EditText f1ValueEditText, f2ValueEditText;
    String f1Value, f2Value;
    View rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        showLog("Entering onCreateView");
        // create dialog fragment view
        rootView = inflater.inflate(R.layout.activity_reconfigure, container, false);
        super.onCreate(savedInstanceState);

        // set title
        getDialog().setTitle("Reconfiguration");

        // find all view by id
        saveBtn = rootView.findViewById(R.id.saveBtn);
        cancelReconfigureBtn = rootView.findViewById(R.id.cancelReconfigureBtn);
        f1ValueEditText = rootView.findViewById(R.id.f1ValueEditText);
        f2ValueEditText = rootView.findViewById(R.id.f2ValueEditText);

        // set TAG and Mode for shared preferences
        sharedPreferences = getActivity().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        if (sharedPreferences.contains("F1")) {
            f1ValueEditText.setText(sharedPreferences.getString("F1", ""));
            f1Value = sharedPreferences.getString("F1", "");
            // hide keyboard
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        }
        if (sharedPreferences.contains("F2")) {
            f2ValueEditText.setText(sharedPreferences.getString("F2", ""));
            f2Value = sharedPreferences.getString("F2", "");
        }

        // not used, restore state for states when tilting devices
        if (savedInstanceState != null) {
            f1Value = savedInstanceState.getStringArray("F1F2 value")[0];
            f2Value = savedInstanceState.getStringArray("F1F2 value")[1];
        }

        // when save button clicked
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked saveBtn");
                // allows editing to shared preferences
                editor = sharedPreferences.edit();
                // insert key and string value
                editor.putString("F1", f1ValueEditText.getText().toString());
                editor.putString("F2", f2ValueEditText.getText().toString());
                // saving values to shared preferences
                editor.commit();
                if (!sharedPreferences.getString("F1", "").equals(""))
                    f1Value = f1ValueEditText.getText().toString();
                if (!sharedPreferences.getString("F2", "").equals(""))
                    f2Value = f2ValueEditText.getText().toString();
                Toast.makeText(getActivity(), "Saving values...", Toast.LENGTH_SHORT).show();
                showLog("Exiting saveBtn");
                // close dialog fragment
                getDialog().dismiss();
            }
        });

        // when cancel button clicked
        cancelReconfigureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked cancelReconfigureationBtn");
                // restore values when new values not saved
                if (sharedPreferences.contains("F1"))
                    f1ValueEditText.setText(sharedPreferences.getString("F1", ""));
                if (sharedPreferences.contains("F2"))
                    f2ValueEditText.setText(sharedPreferences.getString("F2", ""));
                showLog("Exiting cancelReconfigureationBtn");
                // close dialog fragment
                getDialog().dismiss();
            }
        });
        showLog("Exiting onCreateView");
        return rootView;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        showLog("Entering onDismiss");
        super.onDismiss(dialog);
        // if value exist and does not equal to empty string
        if (f1Value != null && !f1Value.equals(""))
            ((MainActivity)getActivity()).f1Btn.setContentDescription(f1Value);
        if (f2Value != null && !f2Value.equals(""))
            ((MainActivity)getActivity()).f2Btn.setContentDescription(f2Value);
        f1ValueEditText.clearFocus();

        showLog("Exiting onDismiss");
    }

    // not used, for saving states when tilting devices
    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);

        String[] value = new String[]{f1Value, f2Value};
        showLog("Exiting onSaveInstanceState");
        outState.putStringArray(TAG, value);
    }

    // show log message
    private void showLog(String message) {
        Log.d(TAG, message);
    }
}