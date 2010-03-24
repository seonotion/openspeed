package hu.imind.android.openspeed;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

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
    private TextView tvAccuracy;
    private TextView tvAltitude;
    private TextView tvSpeed;
    private TextView tvGpsLat;
    private TextView tvGpsLon;
	private TextView tvDebug;
    private ViewGroup mainLayout;
	private MyLocationListener mLocationListener;
	private LocationManager mLocationManager;
	private int currentSpeedLimit;
	private int currentSpeed;
	private WakeLock wl;
	private ViewGroup linLayout;
	private MyDrawView myDrawView;
	private Button btMinus;
	private Button btPlus;
	private boolean speedLimitDirty;
	private Location currentLocation;

    private DatabaseHelper mOpenHelper;
	
	
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

    public class MyLocationListener implements android.location.LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			updateGui(location);
		}
		
		@Override
		public void onProviderDisabled(String provider) {
            debug("locationListener onProviderDisabled: " + provider);
		}

		@Override
		public void onProviderEnabled(String provider) {
            debug("locationListener onProviderEnabled: " + provider);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
            debug("locationListener onStatusChanged: " + provider + "status: " + status);
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
        setContentView(R.layout.drive);
        
        mOpenHelper = new DatabaseHelper(getBaseContext());
        
        speedLimitDirty = false;
        
        mainLayout = (ViewGroup)findViewById(R.id.layoutDrive);
        linLayout = (ViewGroup)findViewById(R.id.layoutSign);
        tvAccuracy = (TextView)findViewById(R.id.tvAccuracy);
        tvAltitude = (TextView)findViewById(R.id.tvAltitude);
        tvSpeed = (TextView)findViewById(R.id.tvSpeed);
        tvGpsLat = (TextView)findViewById(R.id.tvGpsLat);
        tvGpsLon = (TextView)findViewById(R.id.tvGpsLon);
        
        currentSpeedLimit = 50;
        btMinus = (Button)findViewById(R.id.btMinus);
        btPlus = (Button)findViewById(R.id.btPlus);
        tvDebug = (TextView)findViewById(R.id.tvDebug);

        myDrawView = new MyDrawView(getBaseContext());
		linLayout.addView(myDrawView);
		myDrawView.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				debug("Touch event, save the speed limit sign!");
		        speedLimitDirty = false;
				updateGui(null);
				// save the current speed limit
		        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		        if (db == null || currentLocation == null) {
		        	debug("db is null: " + (db == null) + " currentLocation is null: " + (currentLocation == null));
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
		        	debug("Sql insert error");
				}
				
				return false;
			}
		});
        
        OnClickListener speedButtonListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Button tg = (Button)v;
				if (tg.getId() == R.id.btMinus) {
					speedLimitDirty = true;
					setCurrentSpeedLimit(currentSpeedLimit - 10);
					debug("Minus pushed, speed: " + currentSpeedLimit);
				} else {
					speedLimitDirty = true;
					setCurrentSpeedLimit(currentSpeedLimit + 10);
					debug("Plus pushed, speed: " + currentSpeedLimit);
				}
			}
		};
		btMinus.setOnClickListener(speedButtonListener);
		btPlus.setOnClickListener(speedButtonListener);
        
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
    	debug("onOptionsItemSelected: " + item.getItemId() + " save: " + R.id.menuSaveKml);
    	switch (item.getItemId()) {
		case R.id.menuSaveKml:
	        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
	        Cursor c = db.rawQuery("select * from poi", null);
	        File dir = new File("/sdcard/openspeed/");
	        if (! dir.exists()) {
	        	debug("Creating directory: /sdcard/openspeed");
	        	dir.mkdirs();
	        }
	        File file = new File("/sdcard/openspeed/openspeed.kml");
	        FileOutputStream out;
	        DataOutputStream dout = null;
			try {
				out = new FileOutputStream(file);
				dout = new DataOutputStream(out);
				debug("Data output stream created.");
				dout.writeBytes("<?xml version='1.0' encoding='UTF-8'?>");
				dout.writeBytes("<kml xmlns='http://earth.google.com/kml/2.0' xmlns:atom='http://www.w3.org/2005/Atom'>");
				dout.writeBytes("<Document>");
				dout.writeBytes("<atom:author><atom:name>Openspeed running on Android</atom:name></atom:author>");
				dout.writeBytes("<name><![CDATA[Export]]></name>");
				dout.writeBytes("<description><![CDATA[]]></description>");
				dout.writeBytes("<Style id='openspeed_icon'><IconStyle><scale>1.3</scale><Icon><href>http://openspeed.googlecode.com/files/openspeed_icon.png</href></Icon><hotSpot x='32' y='1' xunits='pixels' yunits='pixels'/></IconStyle></Style>");
			} catch (FileNotFoundException e) {
				debug("Exeption: " + e.getMessage());
				Toast.makeText(mainActivity.this, "Failed to save file", Toast.LENGTH_LONG).show();
				return true;
			} catch (IOException e) {
				debug("Count not write KML header.");
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
    				s.append("  <sqlite>");
    				for(int j = 0; j <= 7; j++) {
    					if (j > 0) {
    						s.append(";");
    					}
    					s.append(c.getString(j));
    				}
    				s.append("</sqlite>\n");
	        		s.append("</Placemark>\n");

	        		//debug(s.toString());
	        		try {
						dout.writeBytes(s.toString());
					} catch (IOException e) {
						//e.printStackTrace();
						debug("Exeption: " + e.getMessage());
						Toast.makeText(mainActivity.this, "Failed to write in the file", Toast.LENGTH_LONG).show();
						return true;
					}
	        		s.setLength(0);
	        	}
	        	try {
					dout.writeBytes("</Document>");
					dout.writeBytes("</kml>");
				} catch (IOException e) {
					debug("Exeption: " + e.getMessage());
				}
	        }

			Toast.makeText(mainActivity.this, "KML saved to /sdcard/openspeed", Toast.LENGTH_LONG).show();
			break;
		case R.id.menuLoadKml:
			InputStream in;
			DocumentBuilder builder;
			try {
				String filename = "/sdcard/openspeed/openspeed.kml";
				in = new FileInputStream(filename);
				debug("File opened: " + filename);
				builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				debug("Document builder ok.");
				Document doc=builder.parse(in, null);
				debug("XML parse ok.");
				NodeList placemarks = doc.getElementsByTagName("sqlite");
				debug("Sign list loaded: " + placemarks.getLength());
				
				String csv;
				String[] csvs;
		        ContentValues values;
	            values = new ContentValues();
		        SQLiteDatabase db2 = mOpenHelper.getWritableDatabase();
		        if (db2 == null) {
		        	debug("db is null: " + (db2 == null));
		        	return false;
		        }
				
		        db2.execSQL("delete from poi");
		        debug("poi data deleted.");

				for (int i = 0; i < placemarks.getLength(); i++) {
					csv = placemarks.item(i).getFirstChild().getNodeValue();
					csvs = csv.split(";", 8);

					values.put("_id", csvs[0]);
		            values.put("value", csvs[1]);
		            values.put("tag", csvs[2]);
		            values.put("lat", csvs[3]);
		            values.put("long", csvs[4]);
		            values.put("bearing", csvs[5]);
		            values.put("speed", csvs[6]);
		            values.put("time", csvs[7]);

			        try{
			        	db2.insertOrThrow("poi", null, values);
			        }catch (Exception e) {
			        	debug("Sql insert error: " + e.getMessage());
			        	debug("sqlite: " + csv);
					}
				}
			} catch (Exception e) {
				debug(e.getMessage());
			}
			Toast.makeText(getBaseContext(), "Kml imported!", Toast.LENGTH_SHORT).show();
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
			tvAccuracy.setText("Accuracy: " + location.getAccuracy());
			tvAltitude.setText("Altitude: " + location.getAltitude() + " m");
			tvGpsLat.setText(String.format("%3.8f", location.getLatitude()));
			tvGpsLon.setText(String.format("%3.8f", location.getLongitude()));
			tvSpeed.setText(formatSpeed(location.getSpeed()));
			currentSpeed = Math.round(location.getSpeed() * (float)3.6);
    	}
		if (currentSpeed > currentSpeedLimit + 10) {
			mainLayout.setBackgroundColor(getResources().getColor(R.color.speeding_2));
			tvSpeed.setTextColor(Color.BLACK);
		} else if (currentSpeed >= currentSpeedLimit) {
			mainLayout.setBackgroundColor(getResources().getColor(R.color.speeding_1));
			tvSpeed.setTextColor(Color.BLACK);
		} else {
			mainLayout.setBackgroundColor(getResources().getColor(R.color.speeding_0));
			tvSpeed.setTextColor(Color.RED);
		}
		myDrawView.invalidate();
        //debug("locationListener onLocationChanged: " + location);
	}
    
    public void debug(String s) {
    	debug(s, false);
    }
    public void debug(String s, boolean nonewline) {
    	Log.d(TAG, s);
    	if (nonewline) {
    		tvDebug.setText(s + tvDebug.getText());
    	} else {
    		tvDebug.setText(s + "\n" + tvDebug.getText());
    	}
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
        debug("onResume");
    }
    
    @Override
    protected void onStop() {
        //mSensorManager.unregisterListener(mGraphView);
        super.onStop();
        debug("onStop");
        mLocationManager.removeUpdates(mLocationListener);
        wl.release();
    }
}