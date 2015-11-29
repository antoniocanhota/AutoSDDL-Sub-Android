package lac.contextnet.sddl_pingservicetest;

import java.util.ArrayList;
import java.util.Set;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import br.pucrio.inf.acanhota.autosddl.pub.MainActivityTask;

/**
 * MainActivity: This is our application's MainActivity. It consists in 
 * 				 a UUID randomly generated and shown in txt_uuid, a text 
 * 				 field for the IP:PORT in et_ip, a "Ping!" button 
 * 				 (btn_ping) to send a Ping object message, a "Start 
 * 				 Service!" button (btn_startservice) to start the 
 * 				 communication service and a "Stop Service!" button 
 * 				 (btn_stopservice) to stop it.
 * 
 * @author andremd
 * 
 */
public class MainActivity extends MainActivityTask {
	/* Static Elements */
	private TextView txt_uuid;		
	private EditText et_plate;
	private TextView txt_conn;
	private TextView txt_obd2;
	private Button btn_startservice;
	private Button btn_stopservice;

	BroadcastReceiver receiver; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		startCommunicationService();
		openObd2SelectionMenu();

		/* GUI Elements */
		txt_uuid = (TextView) findViewById(R.id.txt_uuid);				
		et_plate = (EditText) findViewById(R.id.et_plate);
		txt_conn = (TextView) findViewById(R.id.txt_conn);
		txt_obd2 = (TextView) findViewById(R.id.txt_obd2);
		btn_startservice = (Button) findViewById(R.id.btn_startservice);
		btn_stopservice = (Button) findViewById(R.id.btn_stopservice);
		btn_stopservice.setEnabled(false);
		txt_uuid.setText(GetUUID(getBaseContext()));
		txt_conn.setText("Connecting...");
		txt_obd2.setText("Não conectado");

		/* Start Service Button Listener*/
		btn_startservice.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {			
				startMainActivityTask();
			}
		});

		/* Stop Service Button Listener*/
		btn_stopservice.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stopMainActivityTask();
			}
		});
		
		/* Receive messages from services */
		receiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	            if (intent.getAction() == MessageHandler.ACTION_UPDATE_CONNECTION_STATUS) {
	            	String msg = intent.getStringExtra(MessageHandler.CONNECTION_STATUS);	            	
	            	
	            	txt_conn.setText(msg);
	            }
	        }
	    };
	}
	
	protected void openObd2SelectionMenu() {
		ArrayList<String> deviceStrs = new ArrayList<String>();
		final ArrayList<String> devices = new ArrayList<String>();
		
		/* List of paired devices */
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				deviceStrs.add(device.getName() + "\n" + device.getAddress());
				devices.add(device.getAddress());
			}
		}
		
		/* Show list */
		final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle("Escolha o dispositivo OBD-2");
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice, deviceStrs.toArray(new String[deviceStrs.size()]));
		alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
				String deviceAddress = (String) devices.get(position);
				startObd2(deviceAddress);
			}
		});
		alertDialog.show();
	}

	@Override
	protected void onStart() {
	    super.onStart();
	    LocalBroadcastManager.getInstance(this).registerReceiver((receiver), 
	        new IntentFilter(MessageHandler.ACTION_UPDATE_CONNECTION_STATUS)
	    );
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopMainActivityTask();
		stopCommunicationService();
	}
	
	@Override
	protected void onMainActivityStart() {
		btn_startservice.setEnabled(false);
		btn_stopservice.setEnabled(true);
	}
	
	@Override
	protected void onMainActivityStop() {
		btn_startservice.setEnabled(true);
		btn_stopservice.setEnabled(false);
	}

	@Override
	protected void onObd2Connected(BluetoothDevice device) {
		txt_obd2.setText(device.getName());
	}

	@Override
	protected void onObd2NotConnected() {
		txt_obd2.setText("Fora do alcance, desligado ou dispositivo inválido");
	}

	@Override
	protected String getLicensePlate() {
		return et_plate.getText().toString();
	}	
}
