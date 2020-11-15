package org.opensoundid.model.impl;

public class BirdObservation {

	public BirdObservation(String date, int birdCallID) {
		super();
		this.date = date;
		this.birdCallID = birdCallID;
	}

	public BirdObservation() {
		super();
	}

	String date;
	int birdCallID;

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public int getBirdCallID() {
		return birdCallID;
	}

	public void setBirdCallID(int birdCallID) {
		this.birdCallID = birdCallID;
	}
}
