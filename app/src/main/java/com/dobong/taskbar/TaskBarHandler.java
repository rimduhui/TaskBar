package com.dobong.taskbar;

import android.os.Handler;
import android.os.Message;

public class TaskBarHandler extends Handler
{
	public static final int DRAW_LIST = 1;
	
	private TaskBarService mService;
	
	
	public TaskBarHandler(TaskBarService service)
	{
		mService = service;
	}
	
	@Override
	public void handleMessage(Message msg)
	{
		super.handleMessage(msg);
		
		switch(msg.what)
		{
			case DRAW_LIST:
				mService.drawList();
				break;
		}
	}
	
}
