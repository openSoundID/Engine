package org.opensoundid.model.impl;

public class Lowlevel {
	double[] energy;
	double[][] mfccBandLogs;
	int[] chunckID;
	public double[] getEnergy() {
		return energy;
	}
	public double[][] getMfccBandLogs() {
		return mfccBandLogs;
	}
	public void setMfccBandLogs(double[][] mfccBandLogs) {
		this.mfccBandLogs = mfccBandLogs;
	}
	public int[] getChunckID() {
		return chunckID;
	}
	public void setChunckID(int[] chunckID) {
		this.chunckID = chunckID;
	}
	public void setEnergy(double[] energy) {
		this.energy = energy;
	}


}