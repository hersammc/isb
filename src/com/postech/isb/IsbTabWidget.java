package com.postech.isb;


import com.postech.isb.boardList.BoardList;
import com.postech.isb.compose.NotesList;
import com.postech.isb.login.Login;

import com.postech.isb.readThread.ReadThread;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;

public class IsbTabWidget extends TabActivity {
	
	private static final String logName = "TabWidget";
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);

	    Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    /*
	    intent = new Intent().setClass(this, BoardList.class);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("read_boards").setIndicator("ReadBoards",
	                      res.getDrawable(R.drawable.ic_tab_read_boards))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    */
	    // Do the same for the other tabs
	    /*
	    intent = new Intent().setClass(this, NewThreads.class);
	    spec = tabHost.newTabSpec("new_threads").setIndicator("New Threads",
	                      res.getDrawable(R.drawable.ic_tab_new_threads))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    */

	    intent = new Intent().setClass(this, NotesList.class);
	    spec = tabHost.newTabSpec("compose").setIndicator("Compose",
	                      res.getDrawable(R.drawable.ic_tab_compose))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, Login.class);
	    spec = tabHost.newTabSpec("login").setIndicator("Login",
	                      res.getDrawable(R.drawable.ic_tab_login))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    
	    tabHost.setCurrentTab(1);
	}
	
	@Override
	protected void onPause () {
		super.onPause();
		Log.i(logName, "Tabwidget paused.");
	}
	
	@Override
	protected void onResume () {
		super.onResume();
		Log.i(logName, "Tabwidget resumed.");
	}
	
	@Override
	protected void onDestroy () {
		super.onDestroy();
		((PostechIsb)getApplicationContext()).isb.disconnect();
		Log.i(logName, "Tabwidget Destroyed.");
	}
}
