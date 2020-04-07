package cn.edu.sdust.silence.itransfer.reciever;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.Collection;

public interface DirectActionListener{
    void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice);
}
