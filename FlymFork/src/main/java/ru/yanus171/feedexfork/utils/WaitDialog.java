package ru.yanus171.feedexfork.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

public class WaitDialog extends AsyncTask<Void, Void, Void> {
    private ProgressDialog dialog;
    private String mMessage;
    private Runnable mRun;
    public WaitDialog(Activity activity, int messageID, Runnable run) {
        dialog = new ProgressDialog(activity);
        mMessage = activity.getString( messageID );
        mRun = run;
    }

    @Override
    protected void onPreExecute() {
        dialog.setMessage(mMessage);
        dialog.show();
    }

    protected Void doInBackground(Void... args) {
        mRun.run();
        return null;
    }

    protected void onPostExecute(Void result) {
        // do UI work here
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
