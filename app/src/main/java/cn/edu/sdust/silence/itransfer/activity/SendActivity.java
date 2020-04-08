package cn.edu.sdust.silence.itransfer.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.edu.sdust.silence.itransfer.R;
import cn.edu.sdust.silence.itransfer.thread.server.SendActivityHandler;
import cn.edu.sdust.silence.itransfer.qrcode.utils.ZXingUtil;
import cn.edu.sdust.silence.itransfer.reciever.DirectActionListener;
import cn.edu.sdust.silence.itransfer.reciever.WifiP2PBroadcastReceiver;
import cn.edu.sdust.silence.itransfer.thread.server.ServerManager;
import cn.edu.sdust.silence.itransfer.thread.server.ServerManager2;
import cn.edu.sdust.silence.itransfer.ui.loading.RotateLoading;
import cn.edu.sdust.silence.itransfer.ui.progress.NumberProgressBar;

public class SendActivity extends Activity implements DirectActionListener {

    //view
    private NumberProgressBar progress;
    private RotateLoading loading;
    private TextView tv_point;
    private View layout_point_container;

    //WIFI p2p
    public static int PROGRESS = 1;

    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private String filePath;
    private boolean isConnectClient = false;
    private SendActivityHandler sendActivityHandler;
    private ImageView qrcodeView;

    //设备列表
    private List<WifiP2pDevice> peers = new ArrayList();
    private Map<String, Long> map = new HashMap<>();
    private ImageButton back;
    private WifiP2pDevice deviceSelf;
    private WifiP2pInfo mWifiInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        initView();
        getFilePath();
        initIntentFilter();
        initWifiP2p();
        discoverPeers();
    }

    private void getFilePath() {
        Intent intent = getIntent();

        if (Intent.ACTION_SEND == intent.getAction()) {
            Bundle bundle = intent.getExtras();
            Uri uri = (Uri) bundle.get(Intent.EXTRA_STREAM);
            filePath = uri.getPath();
            Log.e("#######", "ACTION_SEND，" + uri.getPath() + "  " + intent.getAction());
        } else if (Intent.ACTION_VIEW == intent.getAction()) {
            Uri uri = intent.getData();
            filePath = uri.getPath();
            Log.e("#######", "ACTION_VIEW，" + uri.getPath() + "  " + intent.getAction());
        } else {
            filePath = intent.getStringExtra("path");
            Log.e("#######", "else filepath：" + filePath);
        }
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
        mWifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this, Looper.myLooper(), new WifiP2pManager.ChannelListener(){

            @Override
            public void onChannelDisconnected() {
                Log.e("#####","发送端，###########onChannelDisconnected");
            }
        });
        WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                Log.e("#####", "onPeersAvailable,peers size:"+peers.size());
                for (int i = 0; i < peers.size(); i++) {
                    WifiP2pDevice device = peers.get(i);
                    Log.e("#####", "#####onPeersAvailable#####"
                            +"\ndeviceName:"+device.deviceName
                            +"\ndeviceAddress:"+device.deviceAddress
                            +"\nisGroupOwner:"+device.isGroupOwner());
                }
            }
        };

        WifiP2pManager.ConnectionInfoListener mInfoListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiInfo) {
                mWifiInfo = wifiInfo;
                showTransferLoading();
                tv_point.setText("连接成功，准备发送数据");
                Log.i("#####", "发送端，#onConnectionInfoAvailable#" + "，isOwner=" + wifiInfo.isGroupOwner + ",isConnectClient=" + isConnectClient);
                Log.i("#####", "发送端，groupFormed：" + wifiInfo.groupFormed);
                Log.i("#####", "发送端 isConnectClient:" + isConnectClient);
                if (wifiInfo.groupFormed && !isConnectClient) {
                    Log.i("#####", "发送端 isGroupOwner:" + wifiInfo.isGroupOwner);
                    if (wifiInfo.isGroupOwner) {
                        ServerManager manager = new ServerManager(sendActivityHandler, filePath);
                        manager.start();
                    } else {
                        ServerManager2 server = new ServerManager2(sendActivityHandler, wifiInfo.groupOwnerAddress.getHostAddress(), filePath);
                        server.start();
                    }
                    isConnectClient = true;
                } else {
                    //TODO
                    Log.w("#####", "发送端，groupFormed false");
                }

            }
        };
        mReceiver = new WifiP2PBroadcastReceiver(mWifiP2pManager, mChannel, this, mPeerListListener, mInfoListener,this);
    }

    private void createConnect(String address) {
        Log.d("#####", "sender createConnect," + address);

        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = address;
        config.groupOwnerIntent = 0;
        config.wps.setup = WpsInfo.PBC;

        mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                if (mWifiInfo != null) {
                    tv_point.setText("连接成功，正在发送数据");
                    Log.d("#####", "createConnect onSuccess" +
                            ",isGroupOwner " + mWifiInfo.isGroupOwner +
                            ",createConnect groupOwner ip " + mWifiInfo.groupOwnerAddress.getHostAddress());
                }
            }

            @Override
            public void onFailure(int reason) {
                Log.d("#####", "createConnect onFailure");
            }
        });
    }

    private void initView() {
        qrcodeView = findViewById(R.id.id_qrcode_view);
        progress = (NumberProgressBar) findViewById(R.id.progress);
        progress.setProgressTextColor(Color.WHITE);
        progress.setReachedBarColor(Color.WHITE);
        progress.setUnreachedBarColor(Color.GRAY);
        progress.setProgressTextSize(40f);
        loading = (RotateLoading) findViewById(R.id.loading);
        tv_point = (TextView) findViewById(R.id.tv_point);
        layout_point_container = findViewById(R.id.layout_point_container);
        back = (ImageButton) findViewById(R.id.back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SendActivity.this);
                builder.setTitle("提示");
                builder.setMessage("确定要取消发送文件吗？");
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
        sendActivityHandler = new SendActivityHandler(SendActivity.this);
        layout_point_container.setVisibility(View.VISIBLE);
        loading.setLoadingColor(Color.WHITE);
        loading.start();
        progress.setMax(100);

    }

    private void showTransferLoading() {
        layout_point_container.setVisibility(View.VISIBLE);
    }

    /**
     * 开启发现节点
     */
    private void discoverPeers() {
        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
            }
        });
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");
            builder.setMessage("确定要取消发送文件吗？");
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

    /**
     * 更新进度
     *
     * @param p
     */
    public void freshProgress(int p) {
        layout_point_container.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(p);
        if (p >= 100) {
            Toast.makeText(this,"文件发送成功",Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(mReceiver, mFilter);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    private void disconnect() {
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e("#####", "removeGroup，onSuccess");
            }

            @Override
            public void onFailure(int i) {
                Log.e("#####", "removeGroup，onFailure,i:"+i);
            }
        });
    }

    @Override
    public void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice) {
        this.deviceSelf = wifiP2pDevice;
        Log.i("#####", "发送端 自己的Name:" + deviceSelf.deviceName);
        Log.i("#####", "发送端 自己的Address:" + deviceSelf.deviceAddress);

        Bitmap bitmap = ZXingUtil.createQRCode(deviceSelf.deviceAddress,150,150);
        qrcodeView.setImageBitmap(bitmap);
    }

    @Override
    public void onReconnect() {
        Log.i("#####", "#####从新开始连接#####");
    }
}
