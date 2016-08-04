package com.dobong.taskbar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


public class ActivityMain extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if(TaskBarService.instance != null)
		{
			TaskBarService.instance.stopSelf();
			TaskBarService.instance = null;
		}
		
		Intent intent = new Intent(this, TaskBarService.class);
		startService(intent);
		
		finish();
	}
	
}
