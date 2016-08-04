package com.dobong.taskbar;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.dobong.taskbar.R;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint("InflateParams")
public class TaskBarService extends Service implements OnTouchListener, Runnable
{
	public static TaskBarService instance;

	private static final String LAUNCHER = "com.android.launcher";
	private static final String SELF = "com.dobong.taskbar";
	private static final int UPDATE_TICK = 300;
	private static final int NUM_MAX_TASK = 100;
	
	private static final int POSITION = -500;
	private static final int DRAG_Y = 10;

	private float mTouchX;
	private float mTouchY;
	private float mBeforeX;
	private boolean mIsAlive;
	private String mActive;

	private WindowManager.LayoutParams mTaskParams;
	private ArrayList<String> mAppList = new ArrayList<String>();
	private TaskBarHandler mHandler;

	private WindowManager mManager;
	private View mTaskView;
	private View mStartView;
	
	private Object mStartSync = new Object();
	
	native private void getAppList();
	
	static
	{
		System.loadLibrary("getapp");
	}
	
	@Override
	public boolean onTouch(View view, MotionEvent event)
	{
		switch (event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				mTouchX = event.getRawY();
				mTouchY = event.getRawY();
				mBeforeX = event.getRawX();
				break;
				
			case MotionEvent.ACTION_UP:
				if(
					mTouchY + DRAG_Y < event.getRawY() &&
					Math.abs(event.getRawX() - mTouchX) / 2 < event.getRawY() - mTouchY
				)
				{
					expandStatusBar();
				}
				else view.performClick();
				break;
			case MotionEvent.ACTION_MOVE:
				float dX = mBeforeX - event.getRawX();
				mBeforeX = event.getRawX();
				
				HorizontalScrollView scroll = (HorizontalScrollView)mTaskView.findViewById(R.id.taskBarPanel);
				scroll.scrollBy((int)dX, 0);
		}

		return true;
	}


	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		instance = this;

		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mTaskView = inflater.inflate(R.layout.layout_taskbar, null);
		mTaskView.setOnTouchListener(this);
		mTaskView.findViewById(R.id.taskBarPanel).setOnTouchListener(this);
		
		mTaskParams = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);

		mTaskParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

		mTaskParams.x = 0;
		mTaskParams.y = POSITION;

		mManager = (WindowManager)getSystemService(WINDOW_SERVICE);
		mManager.addView(mTaskView, mTaskParams);

		mHandler = new TaskBarHandler(this);

		mIsAlive = true;
		new Thread(this).start();
		
		View startButton = mTaskView.findViewById(R.id.startButton);
		startButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showStartMenu();
			}
		});
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();

		if(mManager != null)
		{
			if(mTaskView != null)
			{
				mManager.removeView(mTaskView);
				mTaskView = null;
			}
			
			hideStartMenu();
		}

		mIsAlive = false;
	}

	private void expandStatusBar()
	{
		try
		{
			Object sbservice = getApplication().getSystemService("statusbar");
			Class<?> statusbarManager;
			statusbarManager = Class.forName("android.app.StatusBarManager");
			Method showsb = statusbarManager.getMethod("expand");
			showsb.invoke(sbservice);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	void drawList()
	{
		if(!mIsAlive) return;
		
		try
		{
			LinearLayout panel = (LinearLayout)mTaskView.findViewById(R.id.taskPanel);
			final PackageManager pm = getPackageManager();
	
			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			panel.removeAllViews();

			synchronized (this)
			{
				for(String packageName : mAppList)
				{
					try
					{
						if(LAUNCHER.equals(packageName)) continue;
	
						ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
	
						String name = pm.getApplicationLabel(appInfo).toString();
						Drawable icon = pm.getApplicationIcon(appInfo);
	
						View item = inflater.inflate(R.layout.layout_taskitem, null);
						panel.addView(item);
						item.setOnTouchListener(this);
	
						ImageView background = (ImageView)item.findViewById(R.id.itemBackground);
	
						final boolean isActive; 
						synchronized (this) { isActive = packageName.equals(mActive); }
	
						int drawable = isActive ? R.drawable.item_selected_overlay : R.drawable.item_overlay; 
						background.setImageResource(drawable);
	
						((TextView)item.findViewById(R.id.taskText)).setText(name);
						((ImageView)item.findViewById(R.id.taskImage)).setImageDrawable(icon);
	
						final String packname = packageName;
						item.setOnClickListener(new OnClickListener()
						{
							@Override
							public void onClick(View view)
							{
								try
								{
									if(!isActive)
									{
										Intent intent = pm.getLaunchIntentForPackage(packname);
										intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
										startActivity(intent);
									}
								}
								catch(Exception e)
								{
									e.printStackTrace();
								}
							}
						});
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		try
		{
			while(mIsAlive)
			{
				getAppList();
				mHandler.sendEmptyMessage(TaskBarHandler.DRAW_LIST);
				Thread.sleep(UPDATE_TICK);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void hideStartMenu()
	{
		if(mStartView != null)
		{
			synchronized(mStartSync)
			{
				if(mStartView != null)
				{
					mManager.removeView(mStartView);
					mStartView = null;
				}
			}
		}
	}
	
    private boolean filterInstalled(ApplicationInfo info)
	{
    	if(SELF.equals(info.packageName)) return false;
    	
        if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) return true;
        else if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) return true;
        
        return false;
    }
	
	private void showStartMenu()
	{
		synchronized (mStartSync)
		{
			if(mStartView != null)
			{
				mManager.removeView(mStartView);
				mStartView = null;
				return;
			}
			
			final PackageManager pm = getPackageManager();
			List<ApplicationInfo> appList = pm.getInstalledApplications(
				PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_DISABLED_COMPONENTS
			);
			
			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mStartView = inflater.inflate(R.layout.layout_startmenu, null);
			
			WindowManager.LayoutParams startParams = new WindowManager.LayoutParams(
					WindowManager.LayoutParams.WRAP_CONTENT,
					WindowManager.LayoutParams.MATCH_PARENT,
					WindowManager.LayoutParams.TYPE_PHONE,
					WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
					PixelFormat.TRANSLUCENT);
			
			startParams.x = POSITION;
			startParams.y = 0;
			
			mManager.addView(mStartView, startParams);
			
			mStartView.setOnTouchListener(new OnTouchListener()
			{	
				@Override
				public boolean onTouch(View view, MotionEvent event)
				{
					if(event.getAction() == MotionEvent.ACTION_UP) view.performClick();
					hideStartMenu();
					
					return false;
				}
			});
			
			int idx = 0;
			ViewGroup panel = (ViewGroup)mStartView.findViewById(R.id.startMenu);
			
			for(ApplicationInfo appInfo : appList)
			{
				if(!filterInstalled(appInfo)) continue;
				
				try
				{
					String name = pm.getApplicationLabel(appInfo).toString();
					Drawable icon = pm.getApplicationIcon(appInfo);
				
					View item = inflater.inflate(R.layout.layout_startmenu_item, null);
					ImageView background = (ImageView)item.findViewById(R.id.startItemBackground);
					
					((ImageView)item.findViewById(R.id.startItemIcon)).setImageDrawable(icon);
					((TextView)item.findViewById(R.id.startItemText)).setText(name);
					
					background.setImageResource(idx % 2 == 0 ? R.drawable.start_item_1 : R.drawable.start_item_2);
					++idx;
					
					panel.addView(item);
					final String packname = appInfo.packageName;
					
					OnClickListener listener = new OnClickListener()
					{
						@Override
						public void onClick(View arg0)
						{
							try
							{
								synchronized (mStartSync)
								{
									Intent intent = pm.getLaunchIntentForPackage(packname);
									
									if(intent != null)
									{
										startActivity(intent);
									}
								}
								
								hideStartMenu();
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}
					};
					
					item.setOnClickListener(listener);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			} /* end for */
		} /* synchronize */
	} /* function */
}
