package com.colorcloud.wifichat;

import static com.colorcloud.wifichat.Constant.MSG_STARTCLIENT;
import static com.colorcloud.wifichat.Constant.MSG_STARTSERVER;
import android.app.Application;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Message;
import android.util.Log;

public class WiFiChatApp extends Application {

	private static final String TAG = "PTP_APP";
	
	Channel mP2pChannel;
	WiFiDirectActivity mHomeActivity = null;
	boolean mP2pConnected = false;
	String mMyAddr = null;
	
	@Override
    public void onCreate() {
        super.onCreate();
    }
	
	/**
     * upon p2p connection available, group owner start server socket channel
     * start socket server and select monitor the socket
     */
    public void startSocketServer() {
    	Message msg = ConnectionService.getInstance().getHandler().obtainMessage();
    	msg.what = MSG_STARTSERVER;
    	ConnectionService.getInstance().getHandler().sendMessage(msg);
    }
    
    /**
     * upon p2p connection available, non group owner start socket channel connect to group owner.
     */
    public void startSocketClient(String hostname) {
    	Log.d(TAG, "startSocketClient : client connect to group owner : " + hostname);
    	Message msg = ConnectionService.getInstance().getHandler().obtainMessage();
    	msg.what = MSG_STARTCLIENT;
    	msg.obj = hostname;
    	ConnectionService.getInstance().getHandler().sendMessage(msg);
    }
    
    
    public void setMyAddr(String addr){
    	mMyAddr = addr;
    }
    
	public static class PTPLog {
		public static void i(String tag, String msg) {
            Log.i(tag, msg);
        }
		public static void d(String tag, String msg) {
            Log.d(tag, msg);
        }
	}

}
