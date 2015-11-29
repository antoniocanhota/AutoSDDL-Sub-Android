package br.pucrio.inf.acanhota.autosddl.sub;

import java.util.UUID;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import lac.contextnet.sddl_pingservicetest.CommunicationService;
import lac.contextnet.sddl_pingservicetest.IPPort;

public abstract class MainActivityTask extends Activity {
	public static final String APP_NAME = "AutoSDDL-Sub";
	public static final String IP_PORT = "10.1.1.31:5500";
	//public static final String IP_PORT = "40.84.150.221:5500";
	
	/* Shared Preferences */
	private static String uniqueID = null;
	private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

	private int DELAY_MILLIS = 1000;
	
	private Handler handler;
	private Runnable runnable; 
	
	public void startMainActivityTask() {		
		if (!isRunning()) {
			onMainActivityStart();
			
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

	protected abstract String getLicensePlate();

	protected abstract void onMainActivityStart();

	public void stopMainActivityTask() {
		if (isRunning()) {						
			handler.removeCallbacks(runnable); 
			runnable = null;
			handler = null;

			onMainActivityStop();
		}
	}

	protected abstract void onMainActivityStop();
	
	public void mainActivitTask() {
		Intent i = new Intent(MainActivityTask.this, CommunicationService.class);
		i.setAction(CommunicationService.ACTION_GET_VEHICLE_ALERT);
		i.putExtra(CommunicationService.VEHICLE_LICENSE_PLATE, getLicensePlate());
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
    
    protected boolean isRunning() {
    	return (handler != null);
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
