package net.toload.main.hd.ui;

import android.os.Handler;
import android.os.Message;
import java.lang.ref.WeakReference;
import net.toload.main.hd.MainActivity;

public class SetupImHandler extends Handler {

    private final WeakReference<MainActivity> activityRef;

    public SetupImHandler(MainActivity activity) {
        super(android.os.Looper.getMainLooper());
        this.activityRef = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        MainActivity activity = activityRef.get();
        if (activity == null) return; // Activity has been destroyed, drop message

        String action = msg.getData().getString("action");
        String type = msg.getData().getString("type");

        if (action != null && action.equalsIgnoreCase("progress")) {
            if (type != null) {
                if (type.equalsIgnoreCase("showSpinner")) {
                    String message = msg.getData().getString("message");
                    activity.showProgress(true, message);
                } else if (type.equalsIgnoreCase("showHorizontal")) {
                    String message = msg.getData().getString("message");
                    activity.showProgress(false, message);
                } else if (type.equalsIgnoreCase("cancel")) {
                    activity.cancelProgress();
                } else if (type.equalsIgnoreCase("update")) {
                    int value = msg.getData().getInt("value");
                    activity.updateProgress(value);
                } else if (type.equalsIgnoreCase("message")) {
                    String message = msg.getData().getString("message");
                    activity.updateProgress(message);
                } else if (type.equalsIgnoreCase("indeterminate")) {
                    Boolean flag = msg.getData().getBoolean("flag");
                    activity.setProgressIndeterminate(flag);
                }
            }
        } else if (action != null && action.equalsIgnoreCase("toast")) {
            String message = msg.getData().getString("message");
            int length = msg.getData().getInt("length");

            if (message != null) {
                activity.showToastMessage(message, length);
            } else {
                activity.showToastMessage("Error", length);
            }

        } else if (action != null && action.equalsIgnoreCase("initialbutton")) {
            activity.refreshImportStatus();
        } else if (action != null && action.equalsIgnoreCase("updatecustombutton")) {
            activity.refreshImportStatus();
        } else if (action != null && action.equalsIgnoreCase("reset")) {
            String imtype = msg.getData().getString("im");
            boolean backuplearning = msg.getData().getBoolean("backup");
            activity.resetImTable(imtype, backuplearning);
        } else if (action != null && action.equalsIgnoreCase("finish")) {
            String imtype = msg.getData().getString("im");
            activity.finishProgress(imtype);
        }

    }

    public void cancelProgress() {
        Message m = new Message();
        m.getData().putString("action", "progress");
        m.getData().putString("type", "cancel");
        this.sendMessageDelayed(m, 1);
    }


    public void showProgress(boolean spinnerStyle, String message) {

        Message m = new Message();
        m.getData().putString("action", "progress");
        if (message != null && !message.isEmpty()) {
            m.getData().putString("message", message);
        }

        if (spinnerStyle)
            m.getData().putString("type", "showSpinner");
        else
            m.getData().putString("type", "showHorizontal");
        this.sendMessageDelayed(m, 1);
    }

    public void setProgressIndeterminate(boolean flag) {
        Message m = new Message();
        m.getData().putString("action", "progress");
        m.getData().putString("type", "indeterminate");
        m.getData().putBoolean("flag", flag);
        this.sendMessageDelayed(m, 1);
    }

    public void updateProgress(int value) {
        Message m = new Message();
        m.getData().putString("action", "progress");
        m.getData().putString("type", "update");
        m.getData().putInt("value", value);
        this.sendMessageDelayed(m, 1);
    }

    public void updateProgress(String message) {
        Message m = new Message();
        m.getData().putString("action", "progress");
        m.getData().putString("type", "message");
        m.getData().putString("message", message);
        this.sendMessageDelayed(m, 1);
    }

    public void showToastMessage(String message, int length) {
        Message m = new Message();
        m.getData().putString("action", "toast");
        m.getData().putString("message", message);
        m.getData().putInt("length", length);
        this.sendMessageDelayed(m, 1);
    }

    public void initialImButtons() {
        Message m = new Message();
        m.getData().putString("action", "initialbutton");
        this.sendMessageDelayed(m, 1);
    }

    public void updateCustomButton() {
        Message m = new Message();
        m.getData().putString("action", "updatecustombutton");
        this.sendMessageDelayed(m, 1);
    }

    public void resetImTable(String imtype, Boolean backuplearning) {
        Message m = new Message();
        m.getData().putString("action", "reset");
        m.getData().putString("im", imtype);
        m.getData().putBoolean("backup", backuplearning);
        this.sendMessageDelayed(m, 1);
    }


    public void finishLoading(String imtype) {
        Message m = new Message();
        m.getData().putString("action", "finish");
        m.getData().putString("im", imtype);
        this.sendMessageDelayed(m, 1);
    }
}
