package com.smartagent.Common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.smartagent.Service.SimpleJobIntentService;

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_USER_UNLOCKED)){
            Intent i = new Intent(context, SimpleJobIntentService.class);
            i.putExtra("startwork", 1);
            SimpleJobIntentService.enqueueWork(context, i);
        }
    }
}
