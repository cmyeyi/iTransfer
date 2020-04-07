package cn.edu.sdust.silence.itransfer.reciever;

import android.net.wifi.p2p.WifiP2pDevice;

public interface DirectActionListener{
    void onReconnect();
    void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice);
}
