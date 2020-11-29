package org.opensoundid.model.impl;

public class Description {
	  double[] zero_crossing_rate;
	  double[] flatness;
	  double[][] envelope;

		public double[] getFlatness() {
			return flatness;
		}

		public void setFlatness(double[] flatness) {
			this.flatness = flatness;
		}
	  
	  
	  public double[] getZero_crossing_rate() {
		return zero_crossing_rate;
	}

	public void setZero_crossing_rate(double[] zero_crossing_rate) {
		this.zero_crossing_rate = zero_crossing_rate;
	}

	public double[][] getEnvelope() {
		return envelope;
	}

	public void setEnvelope(double[][] envelope) {
		this.envelope = envelope;
	}


	}