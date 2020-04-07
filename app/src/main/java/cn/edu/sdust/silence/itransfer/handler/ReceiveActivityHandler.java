package cn.edu.sdust.silence.itransfer.handler;

import android.os.Handler;
import android.os.Message;
import cn.edu.sdust.silence.itransfer.activity.ReceiveActivity;

public class ReceiveActivityHandler extends Handler {

    public static int TYPE_PROCESS;
    private ReceiveActivity activity;

    public ReceiveActivityHandler(ReceiveActivity activity) {
        this.activity = activity;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == TYPE_PROCESS) {
            activity.refreshProcess(msg.arg1);
        }

        super.handleMessage(msg);
    }
}
