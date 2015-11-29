package br.pucrio.inf.acanhota.autosddl.pub;

import java.util.Date;

public class AccelerationCalculator {	
	private int lastSpeedInKmh; 
	private Date lastSpeedAt;
	
	public AccelerationCalculator(int lastSpeedInKmh, Date lastSpeedAt) {
		this.lastSpeedInKmh = lastSpeedInKmh;
		this.lastSpeedAt = lastSpeedAt;
	}
	
	public double getAcceleration(int currSpeedInKmh, Date currSpeedAt){
		if (currSpeedInKmh == Obd2Gateway.DEFAULT_ERROR) {
			return 0.0;
		}
		
		double deltaVInMs = convertSpeedFromKmhToMs(currSpeedInKmh - lastSpeedInKmh);
		double deltaTInSeconds = (currSpeedAt.getTime() - lastSpeedAt.getTime()) / 1000;
		
		lastSpeedInKmh = currSpeedInKmh;
		lastSpeedAt = currSpeedAt;
		
		return (deltaVInMs / deltaTInSeconds);
	}
	
	private static double convertSpeedFromKmhToMs(int speedInKmh) {
		return (speedInKmh / 3.6);
	}
}
