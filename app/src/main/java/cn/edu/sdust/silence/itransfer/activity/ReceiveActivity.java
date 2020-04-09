package cn.edu.sdust.silence.itransfer.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.edu.sdust.silence.itransfer.R;
import cn.edu.sdust.silence.itransfer.thread.receiver.OnTransferListener;
import cn.edu.sdust.silence.itransfer.reciever.DirectActionListener;
import cn.edu.sdust.silence.itransfer.reciever.WifiP2PBroadcastReceiver;
import cn.edu.sdust.silence.itransfer.thread.receiver.ReceiveService;
import cn.edu.sdust.silence.itransfer.ui.loading.RotateLoading;
import cn.edu.sdust.silence.itransfer.ui.progress.NumberProgressBar;

public class ReceiveActivity extends AppCompatActivity implements DirectActionListener {

    //broadcastReceive
    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;

    //wifi p2p
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    //this is a flag about connection
    private boolean isConnect = false;
    private boolean isConnectServer = false;

    //设备列表
    private List<WifiP2pDevice> peers = new ArrayList();

    //view
    private NumberProgressBar progress;
    private RotateLoading loading;
    private TextView tv_point;
    private View layout_point_container;
    private WifiP2pDevice deviceSelf;
    private String connectAddress;
    private ReceiveService mReceiveService;
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ReceiveService.MyBinder binder = (ReceiveService.MyBinder) service;
            mReceiveService = binder.getService();
            mReceiveService.setOnTransferListener(mOnProgressChangListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mReceiveService = null;
            bindService();
        }
    };

    OnTransferListener mOnProgressChangListener = new OnTransferListener() {
        @Override
        public void onError() {
            Toast.makeText(ReceiveActivity.this, "数据接收失败", Toast.LENGTH_SHORT).show();
            finish();
        }

        @Override
        public void onTransferFinished(File file) {
            finish();
        }

        @Override
        public void onProgressChanged(int progressValue) {
            refreshProcess(progressValue);
        }
    };

    /**
     * 设置进度
     * @param progressValue
     */
    public void refreshProcess(int progressValue) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layout_point_container.setVisibility(View.GONE);
                if (progress.getVisibility() != View.VISIBLE) {
                    progress.setVisibility(View.VISIBLE);
                }
                progress.setProgress(progressValue);
            }
        });
    }

    private void bindService() {
        Intent intent = new Intent(ReceiveActivity.this, ReceiveService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void unBindService() {
        if (mReceiveService != null) {
            mReceiveService.setOnTransferListener(null);
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getConnectAddress();
        setContentView(R.layout.activity_scan);
        initView();
        initIntentFilter();
        initWifiP2p();
        discoverPeers();
        createConnect(connectAddress);
        bindService();
    }

    private void getConnectAddress() {
        connectAddress = getIntent().getStringExtra("address");
        Log.d("#####", "接收端，扫码获取connectAddress：" + connectAddress);
    }

    private void initIntentFilter() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void initWifiP2p() {
        mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, Looper.myLooper(), new WifiP2pManager.ChannelListener() {

            @Override
            public void onChannelDisconnected() {
                Log.e("#####", "接收端，#onChannelDisconnected#");
            }
        });

        WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                for (int i = 0; i < peers.size(); i++) {
                    WifiP2pDevice d = peers.get(i);
                    Log.e("#####", "onPeersAvailable deviceAddress:" + d.deviceAddress);
//                    createConnect(connectAddress);
                }
            }
        };

        WifiP2pManager.ConnectionInfoListener cInfo = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiInfo) {
                showTransferLoading();
                tv_point.setText("连接成功，准备接收数据");
                Log.i("#####", "接收端，#onConnectionInfoAvailable#");
                Log.i("#####", "接收端，isOwner=" + wifiInfo.isGroupOwner);
                Log.i("#####", "接收端，groupFormed:" + wifiInfo.groupFormed);
                Log.i("#####", "接收端，isConnectServer:" + isConnectServer);
                startReceiveService(wifiInfo);
            }
        };

        mReceiver = new WifiP2PBroadcastReceiver(mManager, mChannel, this, mPeerListListener, cInfo, this);
    }

    private void startReceiveService(WifiP2pInfo wifiInfo) {
        if (wifiInfo.groupFormed && !isConnectServer) {
            if (wifiInfo.isGroupOwner) {
                startReceiveService(true, null);
            } else {
                startReceiveService(false, wifiInfo.groupOwnerAddress.getHostAddress());
            }
            isConnectServer = true;
        }
    }

    private void startReceiveService(boolean isGroupOwner, String p2pMac) {
        if (mReceiveService != null) {
            Intent intent = new Intent(ReceiveActivity.this, ReceiveService.class);
            intent.putExtra(ReceiveService.IS_GROUPOWNER, isGroupOwner);
            intent.putExtra(ReceiveService.P2P_MAC, p2pMac);
            startService(intent);
        }
    }

    private void disconnect() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e("#####", "removeGroup，onSuccess");
            }

            @Override
            public void onFailure(int i) {
                Log.e("#####", "removeGroup，onFailure");
            }
        });
    }

    private void createConnect(String address) {
        Log.e("#####", "#####createConnect#####");
        if (isConnect) {
            Log.e("#####", "已经创建过连接，本次连接无效");
            return;
        }
        Log.e("#####", "接收端,开始创建连接，address=" + address);
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = address;
        config.groupOwnerIntent = 15;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                isConnect = true;
                tv_point.setText("连接成功，正在接收数据");
                Log.d("#####", "接收端,连接成功");
            }

            @Override
            public void onFailure(int reason) {
                isConnect = false;
                Log.d("#####", "接收端, 连接失败");
                reconnect();
            }
        });
    }

    private void reconnect() {
        Log.d("#####", "reconnect");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                createConnect(connectAddress);
            }
        },1000);
    }

    private void initView() {
        findViewById(R.id.id_qrcode_view).setVisibility(View.GONE);
        progress = findViewById(R.id.progress);
        progress.setProgressTextColor(Color.WHITE);
        progress.setReachedBarColor(Color.WHITE);
        progress.setUnreachedBarColor(Color.GRAY);
        progress.setProgressTextSize(40f);

        loading = findViewById(R.id.loading);
        tv_point = findViewById(R.id.tv_point);
        layout_point_container = findViewById(R.id.layout_point_container);

        loading.setLoadingColor(Color.WHITE);
        loading.start();
        progress.setMax(100);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ReceiveActivity.this);
                builder.setTitle("提示");
                builder.setMessage("确定要取消接收文件吗？");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });
    }

    private void showTransferLoading() {
        layout_point_container.setVisibility(View.VISIBLE);
    }

    /**
     * 开启发现节点
     */
    private void discoverPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mFilter);
        showTransferLoading();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        unBindService();
        disconnect();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");
            builder.setMessage("确定要取消接受文件吗？");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice) {
        this.deviceSelf = wifiP2pDevice;
        Log.i("#####", "接收端 自己的Name:" + deviceSelf.deviceName);
        Log.i("#####", "接收端 自己的Address:" + deviceSelf.deviceAddress);
    }

    @Override
    public void onReconnect() {
        Log.i("#####", "接收端，请求重新连接");
        reconnect();
    }
}
