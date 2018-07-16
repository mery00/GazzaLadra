package it.unisa.tirocinio.gazzaladra.activity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import it.unisa.tirocinio.gazzaladra.Utils;
import it.unisa.tirocinio.gazzaladra.callbacks.CustomGestureListener;
import it.unisa.tirocinio.gazzaladra.callbacks.CustomScaleDetectorListener;
import it.unisa.tirocinio.gazzaladra.callbacks.WriteDataCallback;
import it.unisa.tirocinio.gazzaladra.database.Session;
import it.unisa.tirocinio.gazzaladra.file_writer.AsyncFileWriter;

public abstract class TemplateActivity extends AppCompatActivity implements SensorEventListener, WriteDataCallback {

	private String sessionFolder = "";

	protected void setSession(Session s) {
		sessionFolder = "GazzaLadra" + "/" + s.getUidUser() + "/" + s.getNumSession();
	}

	public String getSessionFolder() {
		return sessionFolder;
	}

	//Activity related data
	private String activityId;

	public void setActivityId(String id) {
		this.activityId = id;
	}

	private long startActivityTime;

	//Sensors related data
	private SensorManager sm;
	private Sensor accelerometer;
	private Sensor gyroscope;
	private Sensor magnetometer;

	//Gesture Detector
	private GestureDetector gd;
	private ScaleGestureDetector sgd;

	//Data
	private View rootView;

	public void setRootView(View root) {
		rootView = root;
		OnTouchDispatcher dp = new OnTouchDispatcher();
		for (View v : Utils.getAllChildrenBFS(root))
			v.setOnTouchListener(dp);
	}


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			startActivityTime = savedInstanceState.getLong("startActivityTime");
		} else {
			startActivityTime = System.currentTimeMillis();
		}

		gd = new GestureDetector(this, new CustomGestureListener(this));
		sgd = new ScaleGestureDetector(this, new CustomScaleDetectorListener(this));

		sm = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		if (accelerometer == null) {
			Toast.makeText(this, "No accelerometer found :c", Toast.LENGTH_SHORT).show();
		}
		if (gyroscope == null) {
			Toast.makeText(this, "No gyroscope found :c", Toast.LENGTH_SHORT).show();
		}
		if (magnetometer == null) {
			Toast.makeText(this, "No magnetometer found :c", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		sm.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
		sm.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause() {
		super.onPause();
		sm.unregisterListener(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putLong("startActivityTime", startActivityTime);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		String fileName;
		switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				fileName = "Accelerometer.txt";
				break;
			case Sensor.TYPE_GYROSCOPE:
				fileName = "Gyroscope.txt";
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				fileName = "Magnetic.txt";
				break;
			default:
				return;
		}
		String x
				= "" + event.values[0];
		String y = "" + event.values[1];
		String z = "" + event.values[2];

		AsyncFileWriter.write(new String[]{
				"" + Utils.getSystime(),
				"" + Utils.getTimeRelativeTo(startActivityTime),
				activityId,
				x, y, z,
				"" + Utils.getOrientation(this)
		}, sessionFolder, fileName);
	}

	String viewClicked = null;

	//we get info about the event
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		gd.onTouchEvent(event);
		sgd.onTouchEvent(event);

		int actionId = -1;
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:           //first finger on the screen
				actionId = 0;
				break;
			case MotionEvent.ACTION_POINTER_DOWN:   //new finger on the screen and isn't the first
				actionId = 5;
				break;
			case MotionEvent.ACTION_MOVE:           //a finger on the screen moves
				actionId = 2;
				break;
			case MotionEvent.ACTION_POINTER_UP:     // a finger leaves the screen
				actionId = 6;
				break;
			case MotionEvent.ACTION_UP:             //last finger leaves the screen
				actionId = 1;
				break;
		}

		AsyncFileWriter.write(new String[]{
				"" + Utils.getSystime(),
				"" + Utils.getTimeRelativeTo(startActivityTime),
				activityId,
				"" + event.getX(),
				"" + event.getY(),
				"" + event.getPressure(),
				"" + actionId,
				getWidgetIfAny(),
		}, sessionFolder, "rawOnTouchEvent.txt");

		viewClicked = null;
		return super.onTouchEvent(event);
	}

	// we get info about the widget clicked
	public boolean widgetTouchDispatcher(View v, MotionEvent event) {
		Log.w("widgetTouch", "Siamo nel widgetTouchDispatcher " + v.getClass().getName());
		if (!(v instanceof ViewGroup)) {
//			String[] a = v.getClass().getName().split("\\.");
//			viewClicked = a[a.length-1];
			viewClicked = v.getClass().getName();
			Log.w("widgetTouch", "nell'if");
		}
		return onTouchEvent(event);
	}

	@Override
	public void fireSingleEvent(MotionEvent e, int eventType) {
		String event = "";
		switch (eventType) {
			case CustomGestureListener.SINGLE_TAP:
				event = "Single tap";
				break;
			case CustomGestureListener.DOUBLE_TAP:
				event = "Double tap";
				break;
			case CustomGestureListener.LONG_PRESS:
				event = "Long press";
				break;
		}
		AsyncFileWriter.write(new String[]{
				"" + Utils.getSystime(),
				"" + Utils.getTimeRelativeTo(startActivityTime),
				activityId,
				event,
				"" + e.getX(),
				"" + e.getX(),
				"" + e.getPressure(),
				"" + e.getSize(),
				getWidgetIfAny(),
				"" + Utils.getOrientation(this)
		}, sessionFolder, "SingleEvents.txt");
	}

	@Override
	public void fireKeyPress(int keyCode, int position) {
		//TODO: differisce da h-mog, scrivere documentazione
		AsyncFileWriter.write(new String[]{
				"" + Utils.getSystime(),
				"" + Utils.getTimeRelativeTo(startActivityTime),
				activityId,
				"" + keyCode,
				"" + position,
				"" + Utils.getOrientation(this)
		}, sessionFolder, "KeyPressEvent.txt");
	}

	@Override
	public void fireDoubleFingerEvent(ScaleGestureDetector scaleGestureDetector) {
		AsyncFileWriter.write(new String[]{
				"" + Utils.getSystime(),
				"" + Utils.getTimeRelativeTo(startActivityTime),
				activityId,
				"" + scaleGestureDetector.getCurrentSpan(),
				"" + scaleGestureDetector.getScaleFactor(),
				"" + scaleGestureDetector.getTimeDelta(),
				"" + getWidgetIfAny(),
				"" + Utils.getOrientation(this)
		}, sessionFolder, "DoubleFingerEvents.txt");

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		//We have to keep this method even if we're not using it
	}

	private String getWidgetIfAny() {
		return (viewClicked != null) ? viewClicked : "noWidget";
	}

	public class OnTouchDispatcher implements View.OnTouchListener {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			return widgetTouchDispatcher(v, event);
		}
	}
}