package org.opensoundid.model.impl;

public class Description {
	  double[][] peakdetect_positions;
	  double[][] peakdetect_amplitudes;

	  
	  
	  public double[][] getPeakdetect_positions() {
		return peakdetect_positions;
	}
	  
	  public double[][] getPeakdetect_amplitudes() {
		return peakdetect_amplitudes;
	}


	public void setPeakdetect_positions(double[][] peakdetect_positions) {
		this.peakdetect_positions = peakdetect_positions;
	}

	public void setPeakdetect_amplitudes(double[][] peakdetect_amplitudes) {
		this.peakdetect_amplitudes = peakdetect_amplitudes;
	}


	}