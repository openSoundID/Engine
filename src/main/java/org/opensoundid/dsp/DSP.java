package org.opensoundid.dsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.opensoundid.configuration.EngineConfiguration;

public class DSP {

	private int filterBankNumFilters;
	private int blankSize;
	private int maxAggSize;
	private int deltaAggSize;
	private int aggSize;
	/* calculate delta features based on preceding and following N frames */
	private int deltaN;

	public DSP(EngineConfiguration config) {
		
		filterBankNumFilters = config.getInt("dsp.filterBankNumFilters");
		blankSize = config.getInt("dsp.blankSize");
		maxAggSize = config.getInt("dsp.maxAggSize");
		deltaAggSize = config.getInt("dsp.deltaAggSize");
		aggSize = config.getInt("dsp.aggSize");
		deltaN = config.getInt("dsp.deltaN");
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

	public List<double[]> pooling(double[][] features) {

		DescriptiveStatistics[] stats = new DescriptiveStatistics[filterBankNumFilters + 1];
		for (int k = 0; k < stats.length; k++)
			stats[k] = new DescriptiveStatistics();
		List<double[]> pooledFeatures = new ArrayList<>();
		int currentAggSize = 0;
		List<int[]> featureStructure = new ArrayList<>();

		int j = 0;
		int i = 0;
		// remove blank
		while (i < features.length) {
			if (features[i][0] != 0) {
				j = 0;

				while (((i + j) < features.length) && (features[i + j][0] != 0) && (j < aggSize)) {
					j++;

				}

				int[] interval = new int[3];
				interval[0] = 1;
				interval[1] = i;
				interval[2] = j;
				featureStructure.add(interval);

			} else {

				j = 0;

				while (((i + j) < features.length) && (features[i + j][0] == 0)) {
					j++;
				}

				int[] interval = new int[3];
				interval[0] = 0;
				interval[1] = i;
				interval[2] = j;
				featureStructure.add(interval);

			}

			i = i + j;

		}

		// compute agg
		i = 0;
		while (i < featureStructure.size()) {
			while ((i < featureStructure.size()) && (currentAggSize < maxAggSize)
					&& !((featureStructure.get(i)[0] == 0) && (featureStructure.get(i)[2] >= blankSize))) {
				if ((featureStructure.get(i)[0] != 0)) {
					currentAggSize = currentAggSize + featureStructure.get(i)[2];
					for (int k = featureStructure.get(i)[1]; k < featureStructure.get(i)[1]
							+ featureStructure.get(i)[2]; k++) {
						for (int l = 0; l < stats.length; l++) {
							stats[l].addValue(features[k][l]);
						}
					}
				}
				i++;

			}

			if (currentAggSize >= (maxAggSize - deltaAggSize)) {

				double[] pooledFeature = new double[8 * (filterBankNumFilters + 1) + 1 + 1 + 1];

				for (int k = 0; k < filterBankNumFilters + 1; k++) {
					pooledFeature[k] = stats[k].getMean();
					pooledFeature[k + filterBankNumFilters + 1] = stats[k].getStandardDeviation();
					pooledFeature[k + 3 * (filterBankNumFilters + 1)] = stats[k].getPercentile(15);
					pooledFeature[k + 2 * (filterBankNumFilters + 1)] = stats[k].getPercentile(90);
					pooledFeature[k + 4 * (filterBankNumFilters + 1)] = stats[k].getKurtosis();
					pooledFeature[k + 5 * (filterBankNumFilters + 1)] = stats[k].getSkewness();
					pooledFeature[k + 6 * (filterBankNumFilters + 1)] = Arrays.stream(delta(stats[k].getValues()))
							.sum();
					pooledFeature[k + 7 * (filterBankNumFilters + 1)] = Arrays
							.stream(delta(delta(stats[k].getValues()))).sum();

				}

				pooledFeature[pooledFeature.length - 3] = currentAggSize;

				pooledFeatures.add(pooledFeature);

			}

			currentAggSize = 0;
			for (int k = 0; k < stats.length; k++)
				stats[k].clear();

			i++;

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

	public double[][] melSpectraMeanNormalisation(double[][] features, double percentile) {

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

			double median = stats.getPercentile(percentile);

			for (int j = 0; j < normalizedFeat.length; j++) {
				
				if ((normalizedFeat[j][i] - median) < 0) {

						normalizedFeat[j][i] = 0;
	

				}

			}

		}

		return normalizedFeat;

	}

	public List<double[]> processMelSpectra(double[][] features, double[] energy, double percentile) {

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

		return pooling(melSpectraMeanNormalisation(concatFeat, percentile));

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

}
