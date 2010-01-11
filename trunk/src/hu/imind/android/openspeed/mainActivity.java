package hu.imind.android.openspeed;

import java.util.Formatter;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class mainActivity extends Activity {
    /** Tag string for our debug logs */
    private static final String TAG = "DokiNspeed";
    private int count = 0;
    private TextView tv1;
    private TextView tv2;
    private TextView tSpeed;
    private TextView gpsLat;
    private TextView gpsLon;
    private Button button;
    private ViewGroup mainLayout;
    private Formatter formatter;
	private SensorManager mSensorManager;
	private MyLocationListener mLocationListener;
	private LocationManager mLocationManager;

    public class MyLocationListener implements android.location.LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			updateGui(location);
		}
		
		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
            Log.d(TAG, "locationListener onProviderDisabled: " + provider);
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
            Log.d(TAG, "locationListener onProviderEnabled: " + provider);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
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
        button = (Button)this.findViewById(R.id.Button01);
        
        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();
        
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                count ++;
                tv1.setText("Szia Ajna és helló világ!");
                tv2.setText("Esemény: " + count);
                Location l = new Location("gps");
                l.setSpeed((float)34.16);
                updateGui(l);
            }
        });
    }
    public void updateGui(Location location) {
		tv1.setText("Accuracy: " + location.getAccuracy());
		tv2.setText("Altitude: " + location.getAltitude());
		gpsLat.setText(Double.toString(location.getLatitude()));
		gpsLon.setText(Double.toString(location.getLongitude()));
		tSpeed.setText(formatSpeed(location.getSpeed()));
		int speed = Math.round(location.getSpeed() * (float)3.6);
		//tSpeed.setBackgroundColor(Color.RED);
		if (speed > 60) {
			mainLayout.setBackgroundColor(Color.RED);
			tSpeed.setTextColor(Color.BLACK);
		} else if (speed > 50) {
			//mainLayout.setBackgroundColor(R.color.speeding_1);
			mainLayout.setBackgroundColor(Color.GREEN);
			tSpeed.setTextColor(Color.BLACK);
		} else {
			//mainLayout.setBackgroundColor(R.color.speeding_0);
			mainLayout.setBackgroundColor(Color.BLACK);
			tSpeed.setTextColor(Color.RED);
		}
        Log.d(TAG, "locationListener onLocationChanged: " + location);
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
        Log.d(TAG, "onResume");
    }
    
    @Override
    protected void onStop() {
        //mSensorManager.unregisterListener(mGraphView);
        super.onStop();
        Log.d(TAG, "onStop");
        mLocationManager.removeUpdates(mLocationListener);
    }
}