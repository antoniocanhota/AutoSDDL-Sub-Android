package br.pucrio.inf.acanhota.autosddl.pub;

import java.util.Date;
import java.util.UUID;

import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import br.pucrio.acanhota.autosddl.commons.VehicleMessage;
import br.pucrio.acanhota.autosddl.commons.VehicleMessageType;
import lac.contextnet.sddl_pingservicetest.CommunicationService;
import lac.contextnet.sddl_pingservicetest.IPPort;

public abstract class MainActivityTask extends Activity {
	public static final String APP_NAME = "AutoSDDL-Pub";
	//public static final String IP_PORT = "10.1.1.31:5500";
	public static final String IP_PORT = "40.84.150.221:5500";
	
	/* Shared Preferences */
	private static String uniqueID = null;
	private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

	private int DELAY_MILLIS = 1000;
	
	private Handler handler;
	private Runnable runnable;
	
	private BluetoothSocket btSocket;
	private boolean obd2Valid = false;
	
	BroadcastReceiver locationMessageReceiver;
	private double lat;
	private double lng;
	private AccelerationCalculator accelarationCalculator; 
	
	public void startMainActivityTask() {		
		if (!isRunning()) {
			onMainActivityStart();
			
			startLocationService();
			
			sendStartMainActivityTaskMessage();
			
			handler = new Handler();
			runnable = new Runnable(){
				@Override
				public void run() {
					mainActivitTask();
					handler.postDelayed(this, DELAY_MILLIS);
				}	
			};
			handler.postDelayed(runnable, DELAY_MILLIS);
		}
	}

	private void sendStartMainActivityTaskMessage() {
		VehicleMessage vehicleMessage = new VehicleMessage(getLicensePlate(), VehicleMessageType.START, isObd2Valid());			
		Intent i = new Intent(MainActivityTask.this, CommunicationService.class);
		i.setAction(CommunicationService.ACTION_SEND_VEHICLE_STATUS);
		i.putExtra(CommunicationService.VEHICLE_STATUS, vehicleMessage);
		LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);		
	}

	protected abstract String getLicensePlate();

	protected abstract void onMainActivityStart();

	public void stopMainActivityTask() {
		if (isRunning()) {
			stopLocationService();
			
			accelarationCalculator = null;
			
			handler.removeCallbacks(runnable); 
			runnable = null;
			handler = null;
			
			sendStopMainActivityTaskMessage();
			
			onMainActivityStop();
		}
	}

	private void sendStopMainActivityTaskMessage() {
		VehicleMessage vehicleMessage = new VehicleMessage(getLicensePlate(), VehicleMessageType.END, isObd2Valid());			
		Intent i = new Intent(MainActivityTask.this, CommunicationService.class);
		i.setAction(CommunicationService.ACTION_SEND_VEHICLE_STATUS);
		i.putExtra(CommunicationService.VEHICLE_STATUS, vehicleMessage);
		LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
	}

	protected abstract void onMainActivityStop();
	
	public void mainActivitTask() {
		VehicleMessage vehicleMessage = new VehicleMessage(
				getLicensePlate(),
				VehicleMessageType.STATUS, 
				isObd2Valid()
		);
		
		int currVehicleSpeed = Obd2Gateway.getSpeedInKmh(btSocket);
		Date currDate = vehicleMessage.getCreatedAt();
		if (accelarationCalculator == null) {
			accelarationCalculator = new AccelerationCalculator(currVehicleSpeed, currDate);
		}
		vehicleMessage.setSpeed(currVehicleSpeed);
		vehicleMessage.setVehicleAcceleration(accelarationCalculator.getAcceleration(currVehicleSpeed, currDate));
		vehicleMessage.setRpm(Obd2Gateway.getRpm(btSocket));		
		vehicleMessage.setLat(lat);
		vehicleMessage.setLng(lng);
		
		/* Calling the SendPingMsg action to the PingBroadcastReceiver */
		Intent i = new Intent(MainActivityTask.this, CommunicationService.class);
		i.setAction(CommunicationService.ACTION_SEND_VEHICLE_STATUS);
		i.putExtra(CommunicationService.VEHICLE_STATUS, vehicleMessage);
		LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
	}
	
	public void startCommunicationService() {
		IPPort ipPortObj = new IPPort(IP_PORT);
		
		/* Starting the communication service */
		Intent intent = new Intent(MainActivityTask.this, CommunicationService.class);
		intent.putExtra("ip", ipPortObj.getIP());
		intent.putExtra("port", Integer.valueOf(ipPortObj.getPort()));
		intent.putExtra("uuid", GetUUID(getBaseContext()));
		startService(intent);
	}

	public void stopCommunicationService() {
		/* Stops the service and finalizes the connection */
		stopService(new Intent(getBaseContext(), CommunicationService.class));
	}
	
    //See http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-in-android
    protected boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
	protected void startObd2(String deviceAddress) {
		try {
			BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
			BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
			UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
			
			btSocket = device.createRfcommSocketToServiceRecord(uuid);
			
			btSocket.connect();
			
			new ObdResetCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());				
			new EchoOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());				
			new LineFeedOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());				
			new TimeoutCommand(62).run(btSocket.getInputStream(), btSocket.getOutputStream());				
			new SelectProtocolCommand(ObdProtocols.AUTO).run(btSocket.getInputStream(), btSocket.getOutputStream());
			
			obd2Valid = true;
			onObd2Connected(device);
		} catch (Exception e) {
			obd2Valid = false;
			onObd2NotConnected();
			Log.d("OBD-2", "Não conectou");
			e.printStackTrace();
		}
	}
    
    protected abstract void onObd2Connected(BluetoothDevice device);

	protected abstract void onObd2NotConnected();
    
    protected boolean isRunning() {
    	return (handler != null);
    }
	
	protected boolean isObd2Valid() {
		return obd2Valid;
	}
	
	protected void startLocationService() {
		Intent locationIntent = new Intent(this, LocationService.class);
		startService(locationIntent);
		
		locationMessageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {				
				lat = intent.getDoubleExtra("lat", 0.0);
				lng = intent.getDoubleExtra("lng", 0.0);			
			}
		};
		
		LocalBroadcastManager.getInstance(this).registerReceiver(
				locationMessageReceiver, new IntentFilter("locationChanged"));
	}
	
	protected void stopLocationService() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				locationMessageReceiver);
	}
    
    //See http://androidsnippets.com/generate-random-uuid-and-store-it
    public synchronized static String GetUUID(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();
            }
        }
        return uniqueID;
    }

}
