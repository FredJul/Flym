package ru.yanus171.feedexfork.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.preference.ListPreference;
import android.util.AttributeSet;


import ru.yanus171.feedexfork.R;

public class LanguageSelectPreference extends ListPreference {

    // ------------------------------------------------------------------
    public LanguageSelectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // --------------------------------------------------------------
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.restart_app_apply_changes);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            });
            builder.create().show();
        }
    }
}

