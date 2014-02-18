package com.ifree.test;

import com.flurry.android.FlurryAgent;//update if needed
import com.ifree.breakpad.AssetUtil;
import com.ifree.breakpad.BreakpadAgent;
import com.ifree.test.R;
import com.ifree.test.R.layout;
import com.ifree.test.R.menu;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TestActivity extends Activity {

	private final String TAG="BreakpadTest::log";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		
		Button btnDump=(Button)this.findViewById(R.id.btnDump);
		btnDump.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				String root=Environment.getExternalStorageDirectory().getAbsolutePath()+"/tmp/";
				PackageManager pm=getPackageManager();
				String exec_path=getFileStreamPath("").getAbsolutePath();
			    
				BreakpadAgent ba=new BreakpadAgent(getAssets(),/*root+*/"symbols",root+"dump");
				ba.setBacktraceCallback(new AssetUtil.ActionCallback<Throwable>() {

					@Override
					public void handle(Throwable result) {
						FlurryAgent.onError(TAG, "Crash", result);
						Log.e(TAG,"crash handled",result);
					}
					
				});
				//ba.getBacktrace("minidump_stackwalk",exec_path, root);
				ba.getBacktrace(ba.findFirstDump(),ba.getSymbolDir());
				Log.d(TAG,"start test");
			}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_test, menu);
		return true;
	}

}
