package hu.imind.android.openspeed;

import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class mainActivity extends Activity {
    /** Tag string for our debug logs */
    private static final String TAG = "DokiNspeed";
    private TextView tv1;
    private TextView tv2;
    private TextView tSpeed;
    private TextView gpsLat;
    private TextView gpsLon;
    private ViewGroup mainLayout;
	private MyLocationListener mLocationListener;
	private LocationManager mLocationManager;
	private int currentSpeedLimit;
	private int currentSpeed;
	private HashMap<Integer, ToggleButton> buttons;
	private WakeLock wl;

    public class MyLocationListener implements android.location.LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			updateGui(location);
		}
		
		@Override
		public void onProviderDisabled(String provider) {
            Log.d(TAG, "locationListener onProviderDisabled: " + provider);
		}

		@Override
		public void onProviderEnabled(String provider) {
            Log.d(TAG, "locationListener onProviderEnabled: " + provider);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "locationListener onStatusChanged: " + provider + "status: " + status);
		}
    	
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mainLayout = (ViewGroup)findViewById(R.id.mainLayout);
        tv1 = (TextView)findViewById(R.id.TextView01);
        tv2 = (TextView)findViewById(R.id.TextView02);
        tSpeed = (TextView)findViewById(R.id.speed);
        gpsLat = (TextView)findViewById(R.id.gpsLat);
        gpsLon = (TextView)findViewById(R.id.gpsLon);
        
        currentSpeedLimit = 50;
        buttons  = new HashMap<Integer, ToggleButton>();
        buttons.put(30,  (ToggleButton)findViewById(R.id.speedButton30));
        buttons.put(50,  (ToggleButton)findViewById(R.id.speedButton50));
        buttons.put(70,  (ToggleButton)findViewById(R.id.speedButton70));
        buttons.put(90,  (ToggleButton)findViewById(R.id.speedButton90));
        buttons.put(130, (ToggleButton)findViewById(R.id.speedButton130));

        OnClickListener speedButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				ToggleButton tg = (ToggleButton)v;
				setCurrentSpeedLimit(Integer.parseInt(tg.getTextOn().toString()));
			}
		};
        for (Iterator i = buttons.values().iterator(); i.hasNext();) {
			ToggleButton b = (ToggleButton) i.next();
			b.setOnClickListener(speedButtonListener);
		}
        ToggleButton activeButton = buttons.get(currentSpeedLimit); 
        if (activeButton != null) {
        	activeButton.setChecked(true);
        }
        
        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Wake Lock");
        wl.acquire();
        
    }
    
    public void setCurrentSpeedLimit(int newSpeedLimit) {
    	ToggleButton tg = buttons.get(currentSpeedLimit); 
    	if (tg != null) {
    		tg.setChecked(false);
    	}
    	currentSpeedLimit = newSpeedLimit;
    	tg = buttons.get(currentSpeedLimit); 
    	if (tg != null) {
    		tg.setChecked(true);
    	}
    	updateGui(null);
    }
    
    public void updateGui(Location location) {
    	if (location != null) {
			tv1.setText("Accuracy: " + location.getAccuracy());
			tv2.setText("Altitude: " + location.getAltitude() + " m");
			gpsLat.setText(Double.toString(location.getLatitude()));
			gpsLon.setText(Double.toString(location.getLongitude()));
			tSpeed.setText(formatSpeed(location.getSpeed()));
			currentSpeed = Math.round(location.getSpeed() * (float)3.6);
    	}
		if (currentSpeed > currentSpeedLimit + 10) {
			mainLayout.setBackgroundColor(getResources().getColor(R.color.speeding_2));
			tSpeed.setTextColor(Color.BLACK);
		} else if (currentSpeed >= currentSpeedLimit) {
			mainLayout.setBackgroundColor(getResources().getColor(R.color.speeding_1));
			tSpeed.setTextColor(Color.BLACK);
		} else {
			mainLayout.setBackgroundColor(getResources().getColor(R.color.speeding_0));
			tSpeed.setTextColor(Color.RED);
		}
        //Log.d(TAG, "locationListener onLocationChanged: " + location);
	}

    private String formatSpeed(float speed) {
		return String.format("%3.0f", speed * (float)3.6);
	}

	@Override
    protected void onResume() {
        super.onResume();
        String provider = LocationManager.GPS_PROVIDER;
        long minTime = 0;
        float minDistance = 0;
        mLocationManager.requestLocationUpdates(provider, minTime, minDistance, mLocationListener);
        wl.acquire();
        Log.d(TAG, "onResume");
    }
    
    @Override
    protected void onStop() {
        //mSensorManager.unregisterListener(mGraphView);
        super.onStop();
        Log.d(TAG, "onStop");
        mLocationManager.removeUpdates(mLocationListener);
        wl.release();
    }
}