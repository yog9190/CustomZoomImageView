package com.customc.customzoomview;



import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {
	RelativeLayout rootLayout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);
		rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);		
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub	
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		//unbindDrawables(rootLayout);		
		super.onDestroy();
	}

	/**
	 * Utility method to unbind drawables when an activity is destroyed. This
	 * ensures the drawables can be garbage collected.
	 */
	public static void unbindDrawables(View view) {
		if (view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}

		if (view instanceof ImageView) {
			ImageView imageView = (ImageView) view;
			imageView.setImageBitmap(null);
		} else if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}

			try {
				// AdapterView objects do not support the removeAllViews method
				if (!(view instanceof AdapterView)) {
					((ViewGroup) view).removeAllViews();
				}
			} catch (Exception e) {
				Log.w("Ignore Exception in unbindDrawables", e);
			}
		}
	}
}
