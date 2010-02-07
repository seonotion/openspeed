package hu.imind.android.openspeed;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
	private WakeLock wl;
	private ViewGroup linLayout;
	private MyDrawView myDrawView;
	private Button bMinus;
	private Button bPlus;
	private boolean speedLimitDirty;
	private Location currentLocation;

	
    private static final String DATABASE_NAME = "openspeed.db";
    private static final int DATABASE_VERSION = 2;
	
	/**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE poi ("
                    + "_id   INTEGER PRIMARY KEY,"
                    + "value TEXT,"
                    + "tag TEXT,"
                    + "lat DOUBLE,"
                    + "long DOUBLE,"
                    + "bearing FLOAT,"
                    + "speed FLOAT,"
                    + "time DATETIME"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS poi");
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;
	
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
    
    public class MyDrawView extends View {
    	public MyDrawView(Context context) {
    		super(context);
    	}
    	@Override 
    	protected void onDraw(Canvas canvas) { 
    		Paint paint = new Paint();
    		paint.setStrokeWidth(3);
    		paint.setColor(getResources().getColor(R.color.circle_margin));
    		canvas.drawCircle(100, 70, 65, paint);
    		
    		if (speedLimitDirty) {
    			paint.setColor(getResources().getColor(R.color.circle_dirty));
    		} else {
    			paint.setColor(getResources().getColor(R.color.circle_inner));
    		}
    		canvas.drawCircle(100, 70, 50, paint);
    		
    		if (speedLimitDirty) {
    			paint.setColor(getResources().getColor(R.color.circle_dirty_text));
    		} else {
        		paint.setColor(getResources().getColor(R.color.circle_text));
    		}
    		paint.setTextAlign(Paint.Align.CENTER);
    		paint.setTextSize(50);
    		canvas.drawText("" + currentSpeedLimit, 100, 87, paint);

    		// refresh the canvas 
    		//invalidate(); 
    	}     	 
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mOpenHelper = new DatabaseHelper(getBaseContext());
        
        speedLimitDirty = false;
        
        mainLayout = (ViewGroup)findViewById(R.id.mainLayout);
        linLayout = (ViewGroup)findViewById(R.id.linear);
        myDrawView = new MyDrawView(getBaseContext());
		linLayout.addView(myDrawView);
		myDrawView.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG, "Touch event, save the speed limit sign!");
		        speedLimitDirty = false;
				updateGui(null);
				// save the current speed limit
		        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		        if (db == null || currentLocation == null) {
		        	return false;
		        }
		        
		        ContentValues values;
	            values = new ContentValues();
	            values.put("lat", currentLocation.getLatitude());
	            values.put("long", currentLocation.getLongitude());
	            values.put("tag", "maxspeed");
	            values.put("value", Integer.toString(currentSpeedLimit));
	            values.put("bearing", currentLocation.getBearing());
	            values.put("speed", currentLocation.getSpeed() * (float)3.6);
	            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // set the format to sql date time
	            Date date = new Date();
	            values.put("time", dateFormat.format(date));
		        
		        try{
		        	db.insertOrThrow("poi", null, values);
		        }catch (Exception e) {
					// TODO: handle exception
		        	Log.d(TAG, "Sql insert error");
				}
				
				return false;
			}
		});
        
        tv1 = (TextView)findViewById(R.id.TextView01);
        tv2 = (TextView)findViewById(R.id.TextView02);
        tSpeed = (TextView)findViewById(R.id.speed);
        gpsLat = (TextView)findViewById(R.id.gpsLat);
        gpsLon = (TextView)findViewById(R.id.gpsLon);
        
        currentSpeedLimit = 50;
        bMinus = (Button)findViewById(R.id.buttonMinus);
        bPlus = (Button)findViewById(R.id.buttonplus);

        OnClickListener speedButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Button tg = (Button)v;
				if (tg.getId() == R.id.buttonMinus) {
					speedLimitDirty = true;
					setCurrentSpeedLimit(currentSpeedLimit - 10);
				} else {
					speedLimitDirty = true;
					setCurrentSpeedLimit(currentSpeedLimit + 10);
				}
			}
		};
		bMinus.setOnClickListener(speedButtonListener);
		bPlus.setOnClickListener(speedButtonListener);
        
        mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Wake Lock");
        wl.acquire();
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	MenuInflater menuInflater = getMenuInflater();
    	menuInflater.inflate(R.menu.main, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Log.d(TAG, "onOptionsItemSelected: " + item.getItemId() + " save: " + R.id.menuSaveKml);
    	switch (item.getItemId()) {
		case R.id.menuSaveKml:
	        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
	        Cursor c = db.rawQuery("select * from poi", null);
	        File dir = new File("/sdcard/openspeed/");
	        if (! dir.exists()) {
	        	dir.mkdirs();
	        }
	        File file = new File("/sdcard/openspeed/openspeed.kml");
	        FileOutputStream out;
	        DataOutputStream dout = null;
			try {
				out = new FileOutputStream(file);
				dout = new DataOutputStream(out);
				dout.writeBytes("<?xml version='1.0' encoding='UTF-8'?>");
				dout.writeBytes("<kml xmlns='http://earth.google.com/kml/2.0' xmlns:atom='http://www.w3.org/2005/Atom'>");
				dout.writeBytes("<Document>");
				dout.writeBytes("<atom:author><atom:name>Openspeed running on Android</atom:name></atom:author>");
				dout.writeBytes("<name><![CDATA[Export]]></name>");
				dout.writeBytes("<description><![CDATA[]]></description>");
				dout.writeBytes("<Style id='openspeed_icon'><IconStyle><scale>1.3</scale><Icon><href>http://openspeed.googlecode.com/files/openspeed_icon.png</href></Icon><hotSpot x='32' y='1' xunits='pixels' yunits='pixels'/></IconStyle></Style>");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				Log.d(TAG, "Exeption: " + e.getMessage());
				Toast.makeText(mainActivity.this, "Failed to save file", Toast.LENGTH_LONG).show();
				return true;
			} catch (IOException e) {
				Log.d(TAG, "Count not write KML header.");
			}
	        if (c != null && c.getCount() > 0) {
	        	StringBuffer s = new StringBuffer();
	        	for (int i = 0; i < c.getCount(); i++) {
	        		c.moveToNext();
	        		s.append("<Placemark>\n");
	        		s.append("  <name><![CDATA[(" + c.getString(1) + ")]]></name>\n");
	        		s.append("  <description><![CDATA[");
	        		s.append("Speed limit: " + c.getString(1) + "<br>");
	        		s.append("Time: " + c.getString(7) + "<br>");
	        		s.append("Actual speed: " + c.getString(6) + "<br>");
	        		s.append(")]]></description>\n");
	        		s.append("  <styleUrl>#openspeed_icon</styleUrl>\n");
    				s.append("  <Point>");
    				s.append("<coordinates>" + c.getString(4) + "," + c.getString(3) + "</coordinates>");
    				s.append("</Point>\n");
	        		s.append("</Placemark>\n");

	        		Log.d(TAG, s.toString());
	        		try {
						dout.writeBytes(s.toString());
					} catch (IOException e) {
						//e.printStackTrace();
						Log.d(TAG, "Exeption: " + e.getMessage());
						Toast.makeText(mainActivity.this, "Failed to write in the file", Toast.LENGTH_LONG).show();
						return true;
					}
	        		s.setLength(0);
	        	}
	        	try {
					dout.writeBytes("</Document>");
					dout.writeBytes("</kml>");
				} catch (IOException e) {
					Log.d(TAG, "Exeption: " + e.getMessage());
				}
	        }

			Toast.makeText(mainActivity.this, "KML saved to /sdcard/openspeed", Toast.LENGTH_LONG).show();
			break;
		case R.id.menuLoadKml:
			Toast.makeText(getBaseContext(), "Not implemented, yet!", Toast.LENGTH_SHORT).show();
			break;

		default:
			break;
		}
    	return super.onOptionsItemSelected(item);
    }
    
    public void setCurrentSpeedLimit(int newSpeedLimit) {
    	currentSpeedLimit = newSpeedLimit;
    	updateGui(null);
    }
    
    public void updateGui(Location location) {
    	if (location != null) {
    		currentLocation = location;
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
		myDrawView.invalidate();
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