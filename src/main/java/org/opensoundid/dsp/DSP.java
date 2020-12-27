package org.opensoundid.dsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensoundid.SoundAnalyzer;
import org.opensoundid.configuration.EngineConfiguration;

public class DSP {

	private int filterBankNumFilters;
	private int maxAggSize;
	/* calculate delta features based on preceding and following N frames */
	private int deltaN;
	private int deltaOptimizePeakIndice;
	private double peakAmplitudePercentile;
	private int peakMinArrayLength;
	private int hopSize;
	private double percentileHigh;
	private double percentileLow;
	private static final Logger logger = LogManager.getLogger(DSP.class);

	public DSP(EngineConfiguration config) {

		filterBankNumFilters = config.getInt("dsp.filterBankNumFilters");
		maxAggSize = config.getInt("dsp.maxAggSize");
		deltaN = config.getInt("dsp.deltaN");
		percentileHigh = config.getDouble("dsp.percentileHigh");
		percentileLow = config.getDouble("dsp.percentileLow");
		hopSize = config.getInt("dsp.hopsize");
		deltaOptimizePeakIndice = config.getInt("dsp.deltaOptimizePeakIndice");
		peakAmplitudePercentile = config.getDouble("dsp.peakAmplitudePercentile");
		peakMinArrayLength = config.getInt("dsp.peakMinArrayLength");

	}

	/**
	 * Compute dynamic pooling from a feature vector sequence.
	 * 
	 * @param features: A array of size (NUMFRAMES by number of features) containing
	 *                  features. Each row holds 1 feature vector.
	 * 
	 * @return A array of size (NUMFRAMES by 2*number of features) containing delta
	 *         features. Each row holds 1 delta feature vector.
	 */

	public List<double[]> pooling(double[][] features, double[] peakdetectPositions, double[] peakdetectAmplitudes) {

		DescriptiveStatistics[] stats = new DescriptiveStatistics[filterBankNumFilters + 1];
		for (int k = 0; k < stats.length; k++)
			stats[k] = new DescriptiveStatistics();
		List<double[]> pooledFeatures = new ArrayList<>();
		int currentAggSize = 0;

		DescriptiveStatistics stat = new DescriptiveStatistics();
		for (double peakdetectAmplitude : peakdetectAmplitudes)
			stat.addValue(peakdetectAmplitude);

		double percentile = stat.getPercentile(peakAmplitudePercentile);

		for (int i = 0; i < peakdetectPositions.length; i++) {

			if ((peakdetectAmplitudes[i] >= percentile) || (peakdetectAmplitudes.length<peakMinArrayLength)) {

				int indice = optimizeIndice(peakdetectPositions[i], features, deltaOptimizePeakIndice);
				if ((indice + maxAggSize / 2) < features.length) {

					for (int j = indice - maxAggSize / 2 + 1; j <= indice + maxAggSize / 2; j++) {
						for (int k = 0; k < filterBankNumFilters + 1; k++) {
							stats[k].addValue(features[j][k]);
						}

					}

					double[] pooledFeature = new double[8 * (filterBankNumFilters + 1) + 3];

					for (int k = 0; k < filterBankNumFilters + 1; k++) {
						pooledFeature[k] = stats[k].getMean();
						pooledFeature[k + filterBankNumFilters + 1] = stats[k].getStandardDeviation();
						pooledFeature[k + 3 * (filterBankNumFilters + 1)] = stats[k].getPercentile(percentileLow);
						pooledFeature[k + 2 * (filterBankNumFilters + 1)] = stats[k].getPercentile(percentileHigh);
						pooledFeature[k + 4 * (filterBankNumFilters + 1)] = stats[k].getKurtosis();
						pooledFeature[k + 5 * (filterBankNumFilters + 1)] = stats[k].getSkewness();
						pooledFeature[k + 6 * (filterBankNumFilters + 1)] = Arrays.stream(delta(stats[k].getValues()))
								.sum();
						pooledFeature[k + 7 * (filterBankNumFilters + 1)] = Arrays
								.stream(delta(delta(stats[k].getValues()))).sum();

					}

					pooledFeature[pooledFeature.length - 3] = currentAggSize;

					pooledFeatures.add(pooledFeature);

					for (int k = 0; k < stats.length; k++)
						stats[k].clear();

				}
			}
		}

		if (pooledFeatures.size() > 1) {

			/* POOLED FEATURE STANDARDISATION */
			DescriptiveStatistics[] statsPooling = new DescriptiveStatistics[2 * (filterBankNumFilters + 1)];
			for (int k = 0; k < statsPooling.length; k++)
				statsPooling[k] = new DescriptiveStatistics();

			for (double[] pooledFeature : pooledFeatures) {
				for (int k = 0; k < statsPooling.length; k++)
					statsPooling[k].addValue(pooledFeature[k]);

			}

			for (double[] pooledFeature : pooledFeatures) {
				for (int k = 0; k < statsPooling.length; k++)
					pooledFeature[k] = (pooledFeature[k] - statsPooling[k].getMean());

			}

		}

		return pooledFeatures;

	}

	/**
	 * Compute cepstral Mean Normalisation from a feature vector sequence.
	 * 
	 * @param delta_feat: A array of size (NUMFRAMES by number of features)
	 *                    containing features. Each row holds 1 feature vector.
	 * @return A array of size (NUMFRAMES by number of features) containing features
	 *         delta features and delta delta features. Each row holds 1 delta
	 *         feature vector.
	 */

	public double[][] melSpectraMeanNormalisation(double[][] features) {

		double[][] normalizedFeat = new double[features.length][filterBankNumFilters + 1];
		DescriptiveStatistics stats = new DescriptiveStatistics();

		// Compute the mean for each dimension
		for (int i = 0; i < filterBankNumFilters + 1; i++) {

			// Get a DescriptiveStatistics instance

			stats.clear();
			// Add the data from the array
			for (int j = 0; j < features.length; j++) {
				stats.addValue(features[j][i]);
			}

			double mean = stats.getMean();

			for (int j = 0; j < features.length; j++) {

				normalizedFeat[j][i] = features[j][i] - mean;

			}
		}

		// Compute median for each feature
		for (int i = 0; i < filterBankNumFilters + 1; i++) {

			// Get a DescriptiveStatistics instance

			stats.clear();
			// Add the data from the array
			for (int j = 0; j < normalizedFeat.length; j++) {
				stats.addValue(normalizedFeat[j][i]);
			}

			// double median = stats.getPercentile(50);

			for (int j = 0; j < normalizedFeat.length; j++) {

				if ((normalizedFeat[j][i]) < 0) {

					normalizedFeat[j][i] = 0;

				}

			}

		}

		return normalizedFeat;

	}

	public List<double[]> processMelSpectra(double[][] features, double[] energy, double[] peakdetectPositions,
			double[] peakdetectAmplitudes) {

		double[][] concatFeat = new double[features.length][filterBankNumFilters + 1];

		for (int i = 0; i < features.length; i++) {
			concatFeat[i][0] = energy[i];
		}

		for (int i = 1; i < filterBankNumFilters + 1; i++) {

			// Add the data from the array
			for (int j = 0; j < features.length; j++) {
				concatFeat[j][i] = features[j][i - 1];
			}

		}

		return pooling(melSpectraMeanNormalisation(concatFeat), peakdetectPositions, peakdetectAmplitudes);

	}

	/**
	 * Compute delta features from vector sequence.
	 * 
	 * @param features: A array of size (NUMFRAMES by number of features) containing
	 *                  features. Each row holds 1 feature vector.
	 * 
	 * @return A array of size (NUMFRAMES by number of features) containing delta
	 *         features. Each row holds 1 delta feature vector.
	 */

	public double[] delta(double[] sequence) {

		double[] deltaSequence = new double[sequence.length];
		double denominator = 0;

		for (int i = 1; i < deltaN + 1; i++) {
			denominator = denominator + Math.pow(i, 2);
		}

		denominator = 2 * denominator;

		for (int i = 0; i < sequence.length; i++) {

			double currentDelta = 0;
			for (int j = 1; j < deltaN + 1; j++) {
				if (((i - j) >= 0) && ((i + j) < sequence.length)) {

					currentDelta = currentDelta + j * (sequence[i + j] - sequence[i - j]);
				}
				if (((i - j) < 0)) {

					currentDelta = currentDelta + j * (sequence[i + j] - sequence[0]);
				}
				if ((i + j) >= deltaSequence.length) {

					currentDelta = currentDelta + j * (sequence[sequence.length - 1] - sequence[i - j]);
				}

				deltaSequence[i] = currentDelta / denominator;
			}

		}

		return deltaSequence;

	}

	int optimizeIndice(double peakdetectPositions, double[][] features, int delta) {
		int featuresIndice = (int) (Math.floor(peakdetectPositions / hopSize) - 1);
		int correctedIndice = featuresIndice;
		double[] energySum = new double[2 * delta + 1];

		for (int i = -delta; i <= delta; i++) {
			for (int j = featuresIndice - maxAggSize / 2 + 1; j <= featuresIndice + maxAggSize / 2; j++) {
				if (((j + i) >= 0) && ((j + i + maxAggSize / 2) < features.length))
					energySum[i + delta] = energySum[i + delta] + features[j + i][0];
				else
					energySum[i + delta] = Double.NEGATIVE_INFINITY;
			}
		}

		OptionalDouble var = Arrays.stream(energySum).max();

		if (var.isPresent()) {
			double max = Arrays.stream(energySum).max().getAsDouble();

			for (int i = 0; i < energySum.length; i++) {
				if (energySum[i] == max) {
					correctedIndice = featuresIndice - delta + i;
				}
			}
		}

		return correctedIndice;

	}
}
