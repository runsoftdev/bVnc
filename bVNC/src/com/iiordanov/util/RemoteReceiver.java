package com.iiordanov.util;

import com.iiordanov.bVNC.RemoteCanvasActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class RemoteReceiver extends BroadcastReceiver{

	public static final String CUSTOMER_RESTART_INTENT = "com.remote.customer_restart";
	public static final String CUSTOMER_END_INTENT = "com.remote.customerend";
	public static final String CUSTOMER_CHAT_INTENT = "com.remote.custome_caht";

	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.e("bVnc", "Receiver broadcat action = " + intent.getAction());

	    String action = intent.getAction();
	    
	    if(RemoteCanvasActivity.mThis == null) {
	    	return;
	    }
	    
	    if (action.equals(CUSTOMER_RESTART_INTENT)) {
	    	RemoteCanvasActivity.mThis.finish();

	    }
	    
	    if (action.equals(CUSTOMER_END_INTENT)) {
	    	RemoteCanvasActivity.mThis.finish();

	    }
	    
	    
	    if (action.equals(CUSTOMER_CHAT_INTENT)) {
	    	String msg = intent.getStringExtra("msg");
	    	RemoteCanvasActivity.mThis.messageArrvied(msg);
	    	
	    }
	    
	    
	}

}
