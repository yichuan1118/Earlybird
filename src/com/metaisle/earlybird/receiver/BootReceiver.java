package com.metaisle.earlybird.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.metaisle.profiler.CollectorService;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		context.startService(new Intent(context, CollectorService.class));
		
	}

}
