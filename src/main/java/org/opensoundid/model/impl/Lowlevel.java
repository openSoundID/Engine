package org.opensoundid.model.impl;

public class Lowlevel {
	  double[] energy;
	  double[][] mfcc_bands_log;
	  
	public double[] getEnergy() {
		return energy;
	}
	public double[][] getMfcc_bands_log() {
		return mfcc_bands_log;
	}
	public void setEnergy(double[] energy) {
		this.energy = energy;
	}
	public void setMfcc_bands_log(double[][] mfcc_bands_log) {
		this.mfcc_bands_log = mfcc_bands_log;
	}


	}