package com.rnfingerprint;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import com.facebook.react.bridge.ReadableMap;

public class FingerprintDialog extends DialogFragment implements FingerprintHandler.Callback {

    private FingerprintManager.CryptoObject mCryptoObject;
    private DialogResultListener dialogCallback;
    private FingerprintHandler mFingerprintHandler;
    private boolean isAuthInProgress;

    private String authReason;
    private int dialogColor = 0;
    private String dialogTitle = "";

    private String feedbackAwaiting = "Touch sensor";
    private String feedbackNotRecognised = "Not recognised";
    private String feedbackRecognised = "Fingerprint recognised";
    private String feedbackTooManyAttempts = "Too many attempts. Try again later.";

    private int countInvalid = 0;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.mFingerprintHandler = new FingerprintHandler(context, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fingerprint_dialog, container, false);

        final TextView mFingerprintDescription = (TextView) v.findViewById(R.id.fingerprint_description);
        mFingerprintDescription.setText(this.authReason);

        final ImageView mFingerprintImage = (ImageView) v.findViewById(R.id.fingerprint_icon);
        if (this.dialogColor != 0) {
            mFingerprintImage.setColorFilter(this.dialogColor);
        }

        final TextView mfeedbackText = (TextView) v.findViewById(R.id.feedback_text);
        mfeedbackText.setText(this.feedbackAwaiting);

        final Button mCancelButton = (Button) v.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelled();
            }
        });
        if (this.dialogColor != 0) {
            mCancelButton.setTextColor(this.dialogColor);
        }

        getDialog().setTitle(this.dialogTitle);
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode != KeyEvent.KEYCODE_BACK || mFingerprintHandler == null) {
                    return false; // pass on to be processed as normal
                }

                onCancelled();
                return true; // pretend we've processed it
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (this.isAuthInProgress) {
            return;
        }

        this.isAuthInProgress = true;
        this.mFingerprintHandler.startAuth(mCryptoObject);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isAuthInProgress) {
            this.mFingerprintHandler.endAuth();
            this.isAuthInProgress = false;
        }
    }


    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        this.mCryptoObject = cryptoObject;
    }

    public void setDialogCallback(DialogResultListener newDialogCallback) {
        this.dialogCallback = newDialogCallback;
    }

    public void setReasonForAuthentication(String reason) {
        this.authReason = reason;
    }

    public void setAuthConfig(final ReadableMap config) {
        if (config == null) {
            return;
        }

        if (config.hasKey("feedbackAwaiting")) {
            this.feedbackAwaiting = config.getString("feedbackAwaiting");
        }
        if (config.hasKey("feedbackNotRecognised")) {
            this.feedbackNotRecognised = config.getString("feedbackNotRecognised");
        }
        if (config.hasKey("feedbackRecognised")) {
            this.feedbackRecognised = config.getString("feedbackRecognised");
        }
        if (config.hasKey("feedbackTooManyAttempts")) {
            this.feedbackTooManyAttempts = config.getString("feedbackTooManyAttempts");
        }

        if (config.hasKey("title")) {
            this.dialogTitle = config.getString("title");
        }

        if (config.hasKey("color")) {
            this.dialogColor = config.getInt("color");
        }
    }

    public interface DialogResultListener {
        void onAuthenticated();

        void onError(String errorString);

        void onCancelled();
    }

    @Override
    public void onAuthenticated() {

        final TextView feedbackText = (TextView) getView().findViewById(R.id.feedback_text);
        feedbackText.setText(this.feedbackRecognised);
        this.isAuthInProgress = false;

        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        close();
                    }
                },
                1000
        );
    }

    public void close() {
        this.dialogCallback.onAuthenticated();
        dismiss();
    }

    @Override
    public void onError(String errorString) {

        final TextView feedbackText = (TextView) getView().findViewById(R.id.feedback_text);
        this.countInvalid++;
        if (this.countInvalid > 4) {
            feedbackText.setText(this.feedbackTooManyAttempts);
            this.isAuthInProgress = false;
            this.mFingerprintHandler.endAuth();
        }
        else {
            feedbackText.setText(this.feedbackNotRecognised);
            this.mFingerprintHandler.startAuth(mCryptoObject);
        }
    }

    @Override
    public void onCancelled() {
        this.isAuthInProgress = false;
        this.mFingerprintHandler.endAuth();
        this.dialogCallback.onCancelled();
        dismiss();
    }
}
