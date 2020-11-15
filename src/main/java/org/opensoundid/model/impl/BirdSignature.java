package org.opensoundid.model.impl;

public class BirdSignature {

	private int birdId;
	double[] timeSeries;
	private int numFeature;
	private double Energie;
	private double Variance;
	private int size;
	private double MaxDistance;
	private int Weight;
	private int falsePositiveNumber;
	private double score;

	public double[] getTimeSeries() {
		return timeSeries;
	}

	public void setTimeSeries(double[] timeSeries) {
		this.timeSeries = timeSeries;
	}

	public int getNumFeature() {
		return numFeature;
	}

	public void setNumFeature(int numFeature) {
		this.numFeature = numFeature;
	}

	public double getEnergie() {
		return Energie;
	}

	public void setEnergie(double Energie) {
		this.Energie = Energie;
	}

	public double getVariance() {
		return Variance;
	}

	public void setVariance(double Variance) {
		this.Variance = Variance;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public double getMaxDistance() {
		return MaxDistance;
	}

	public void setMaxDistance(double maxDistance) {
		MaxDistance = maxDistance;
	}

	public int getWeight() {
		return Weight;
	}

	public void setWeight(int weight) {
		Weight = weight;
	}

	public int getBirdId() {
		return birdId;
	}

	public void setBirdId(int birdId) {
		this.birdId = birdId;
	}

	public BirdSignature(double[] timeSeries, int numFeature, double Energie, double Variance, int size,
			double maxDistance, int weight, int birdId,int falsePositiveNumer,int score) {

		this.timeSeries = timeSeries;
		this.numFeature = numFeature;
		this.Energie = Energie;
		this.Variance = Variance;
		this.size = size;
		this.MaxDistance = maxDistance;
		this.Weight = weight;
		this.birdId = birdId;
		this.falsePositiveNumber = falsePositiveNumer;
		this.score = score;
	}

	public BirdSignature() {
	}

	public int getFalsePositiveNumber() {
		return falsePositiveNumber;
	}

	public void setFalsePositiveNumber(int falsePositiveNumber) {
		this.falsePositiveNumber = falsePositiveNumber;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

}