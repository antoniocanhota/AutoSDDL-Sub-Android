package br.pucrio.inf.acanhota.autosddl.pub;

import com.github.pires.obd.commands.*;
import com.github.pires.obd.commands.engine.*;

import android.bluetooth.BluetoothSocket;

public class Obd2Gateway {
	public static int DEFAULT_ERROR = -9999;
	
	public static int getSpeedInKmh(BluetoothSocket socket) {
		SpeedCommand command = new SpeedCommand();
		try {
			command.run(socket.getInputStream(), socket.getOutputStream());
			return command.getMetricSpeed();
		} catch (Exception e) {
			return DEFAULT_ERROR;		
		}
	}
	
	public static int getRpm(BluetoothSocket socket) {
		RPMCommand command = new RPMCommand();
		try {
			command.run(socket.getInputStream(), socket.getOutputStream());
			return command.getRPM();
		} catch (Exception e) {
			return DEFAULT_ERROR;		
		}
	}
	
}
