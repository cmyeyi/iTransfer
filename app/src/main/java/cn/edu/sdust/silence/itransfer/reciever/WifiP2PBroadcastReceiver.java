package cn.edu.sdust.silence.itransfer.reciever;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

public class WifiP2PBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private Activity mActivity;
    private WifiP2pManager.PeerListListener mPeerListListener;
    private WifiP2pManager.ConnectionInfoListener mInfoListener;
    private DirectActionListener mDirectActionListener;

    public WifiP2PBroadcastReceiver(WifiP2pManager manager,
                                    WifiP2pManager.Channel channel,
                                    Activity activity,
                                    WifiP2pManager.PeerListListener peerListListener,
                                    WifiP2pManager.ConnectionInfoListener infoListener,
                                    DirectActionListener directActionListener
    ) {
        this.mManager = manager;
        this.mChannel = channel;
        this.mPeerListListener = peerListListener;
        this.mActivity = activity;
        this.mInfoListener = infoListener;
        this.mDirectActionListener = directActionListener;
    }


    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        /*check if the wifi is enable*/
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            Log.i("######", "接收端，WIFI_P2P_STATE_CHANGED_ACTION，state=" + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.i("######", "接收端，WIFI_P2P_PEERS_CHANGED_ACTION");
            mManager.requestPeers(mChannel, mPeerListListener);
        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
            Log.i("######", "接收端，WIFI_P2P_DISCOVERY_CHANGED_ACTION ，state=" + state);
            if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                Toast.makeText(mActivity, "搜索开启", Toast.LENGTH_SHORT).show();
            } else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                Toast.makeText(mActivity, "搜索已关闭", Toast.LENGTH_SHORT).show();
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.i("######", "接收端，WIFI_P2P_CONNECTION_CHANGED_ACTION");
            if (mManager == null) {
                return;
            }
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                Log.i("######", "已连接，请求连接数据");
                mManager.requestConnectionInfo(mChannel, mInfoListener);
            } else {
                Log.i("######", "连接断开");
                mDirectActionListener.connectError();
                return;
            }
        }else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            //本设备的设备信息发生了变化
            Log.i("######", "接收端，WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            WifiP2pDevice wifiP2pDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            mDirectActionListener.onSelfDeviceAvailable(wifiP2pDevice);
        }
    }
}
