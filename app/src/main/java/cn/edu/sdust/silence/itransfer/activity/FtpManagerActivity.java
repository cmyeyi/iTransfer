package cn.edu.sdust.silence.itransfer.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import java.net.InetAddress;

import cn.edu.sdust.silence.itransfer.R;
import cn.edu.sdust.silence.itransfer.ftpserver.ppareit.ppareit.swiftp.FsService;
import cn.edu.sdust.silence.itransfer.ftpserver.ppareit.ppareit.swiftp.FsSettings;

public class FtpManagerActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp_manager);
        initView();
    }

    private void initView() {
        startServer();

        InetAddress address = FsService.getLocalInetAddress();
        if (address == null) {
            Log.w("#####", "Unable to retrieve the local ip address");
            return;
        }
        String ipText = "ftp://" + address.getHostAddress() + ":" + FsSettings.getPortNumber() + "/";
        Log.d("#####", "ipText:" + ipText);

        ((TextView) findViewById(R.id.text)).setText(ipText);
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitFtp();
            }
        });
    }

    /**
     * 发送广播开启FsService
     */
    private void startServer() {
        sendBroadcast(new Intent(FsService.ACTION_START_FTPSERVER));
    }

    private void stopServer() {
        sendBroadcast(new Intent(FsService.ACTION_STOP_FTPSERVER));
    }


    private String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    @Override
    protected void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitFtp();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void exitFtp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage("确定要退出ftp文件管理吗？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
}
