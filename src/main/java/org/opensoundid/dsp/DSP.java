package org.opensoundid.dsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensoundid.configuration.EngineConfiguration;

public class DSP {

	private int filterBankNumFilters;
	private int maxAggSize;
	/* calculate delta features based on preceding and following N frames */
	private int deltaN;
	private int deltaOptimizePeakIndice;
	private double peakAmplitudePercentile;
	private int peakMinSequences;
	private double percentileHigh;
	private double percentileLow;
	private double melSpectraNormalisationPercentile;
	private static final Logger logger = LogManager.getLogger(DSP.class);

	public DSP(EngineConfiguration config) {

		filterBankNumFilters = config.getInt("dsp.filterBankNumFilters");
		maxAggSize = config.getInt("dsp.maxAggSize");
		deltaN = config.getInt("dsp.deltaN");
		percentileHigh = config.getDouble("dsp.pooling.percentileHigh");
		percentileLow = config.getDouble("dsp.pooling.percentileLow");
		deltaOptimizePeakIndice = config.getInt("dsp.deltaOptimizePeakIndice");
		peakAmplitudePercentile = config.getDouble("dsp.peakAmplitudePercentile");
		peakMinSequences = config.getInt("dsp.peakMinSequences");
		melSpectraNormalisationPercentile = config.getDouble("dsp.melSpectraNormalisation.percentile");

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

	public List<double[]> pooling(List<double[][]> featuresList, double[] peakdetectPositions,
			double[] peakdetectAmplitudes) {

		DescriptiveStatistics[] stats = new DescriptiveStatistics[filterBankNumFilters + 1];
		for (int k = 0; k < stats.length; k++)
			stats[k] = new DescriptiveStatistics();
		List<double[]> pooledFeatures = new ArrayList<>();
		int currentAggSize = 0;

		DescriptiveStatistics stat = new DescriptiveStatistics();
		for (double peakdetectAmplitude : peakdetectAmplitudes)
			stat.addValue(peakdetectAmplitude);

		double percentile = stat.getPercentile(peakAmplitudePercentile);

		for (int i = 0; i < featuresList.size(); i++) {

			if ((peakdetectAmplitudes[i] >= percentile) || (peakdetectAmplitudes.length < peakMinSequences)) {

				int indice = optimizeIndice(44 / 2 - 1, featuresList.get(i), deltaOptimizePeakIndice);
				if ((indice + maxAggSize / 2) < featuresList.get(i).length) {
					currentAggSize = 0;

					int indiceMin = indice - (maxAggSize / 2) + 1;
					int indiceMax = indice + (maxAggSize / 2);

					while ((featuresList.get(i)[indiceMin][0] == 0) && (indiceMin <= indice))
						indiceMin++;

					while ((featuresList.get(i)[indiceMax][0] == 0) && (indiceMax < (indice + 1)))
						indiceMax--;

					for (int j = indiceMin; j <= indiceMax; j++) {
						currentAggSize++;
						for (int k = 0; k < filterBankNumFilters + 1; k++) {
							stats[k].addValue(featuresList.get(i)[j][k]);

						}
					}

					double[] pooledFeature = new double[8 * (filterBankNumFilters + 1) + 3];

					for (int k = 0; k < filterBankNumFilters + 1; k++) {
						pooledFeature[k] = stats[k].getMean();
						pooledFeature[k + filterBankNumFilters + 1] = stats[k].getStandardDeviation();
						pooledFeature[k + 2 * (filterBankNumFilters + 1)] = stats[k].getPercentile(percentileLow);
						pooledFeature[k + 3 * (filterBankNumFilters + 1)] = stats[k].getPercentile(percentileHigh);
						pooledFeature[k + 4 * (filterBankNumFilters + 1)] = stats[k].getKurtosis();
						pooledFeature[k + 5 * (filterBankNumFilters + 1)] = stats[k].getSkewness();
						pooledFeature[k + 6 * (filterBankNumFilters + 1)] = Arrays.stream(delta(stats[k].getValues()))
								.sum();
						pooledFeature[k + 7 * (filterBankNumFilters + 1)] = Arrays
								.stream(delta(delta(stats[k].getValues()))).sum();

					}

					pooledFeature[pooledFeature.length - 3] = currentAggSize;
					logger.info("Corrected Aggregation size {}",currentAggSize);
					pooledFeatures.add(pooledFeature);

					for (int k = 0; k < stats.length; k++)
						stats[k].clear();

				}
			}
		}

		if (!pooledFeatures.isEmpty()) {

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
					pooledFeature[k] = pooledFeature[k] - statsPooling[k].getMean();

			}

		}

		return pooledFeatures;

	}

	public List<double[][]> melSpectraNormalisation(List<double[][]> chunkedFeatures) {

		DescriptiveStatistics stats = new DescriptiveStatistics();

		for (double[][] normalizedFeat : chunkedFeatures) {

			// Compute median

			stats.clear();
			// Add the data from the array
			for (int j = 0; j < normalizedFeat.length; j++) {
				stats.addValue(normalizedFeat[j][0]);
			}

			double median = stats.getPercentile(melSpectraNormalisationPercentile);

			for (int j = 0; j < normalizedFeat.length; j++) {

				if ((normalizedFeat[j][0]) < median) {

					normalizedFeat[j][0] = 0;

				}

			}

		}

		return chunkedFeatures;

	}

	public List<double[]> processMelSpectra(int chunkIDs[], double[][] features, double[] energy,
			double[] peakdetectPositions, double[] peakdetectAmplitudes) {

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

		ArrayList<double[][]> chunkedFeatures = new ArrayList<>();

		int[] chunkLength = new int[chunkIDs[chunkIDs.length - 1] + 1];

		for (int i = 0; i < chunkIDs.length; i++) {

			chunkLength[chunkIDs[i]]++;
		}

		int j = 0;
		for (int i = 0; i < chunkLength.length; i++) {

			double[][] chunk = new double[chunkLength[i]][filterBankNumFilters + 1];

			for (int k = 0; k < chunkLength[i]; k++) {
				// Add the data from the array
				System.arraycopy(concatFeat[j + k], 0, chunk[k], 0, filterBankNumFilters + 1);
			}

			j += chunkLength[i];
			chunkedFeatures.add(chunk);
		}

		return pooling(melSpectraNormalisation(chunkedFeatures), peakdetectPositions, peakdetectAmplitudes);

	}

	public double[] delta(double[] sequence) {

		double[] deltaSequence = new double[sequence.length];
		double denominator = 0;

		int correctedDeltaN = deltaN;
		// correct deltaN size
		if ((sequence.length) < (2 * deltaN + 1))
			correctedDeltaN = (sequence.length - 1) / 2;

		for (int i = 1; i < correctedDeltaN + 1; i++) {
			denominator = denominator + Math.pow(i, 2);
		}

		denominator = 2 * denominator;

		for (int i = 0; i < sequence.length; i++) {

			double currentDelta = 0;
			for (int j = 1; j < correctedDeltaN + 1; j++) {
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

	int optimizeIndice(int peakdetectPositions, double[][] features, int delta) {

		int correctedIndice = peakdetectPositions;
		double[] energySum = new double[2 * delta + 1];

		for (int i = -delta; i <= delta; i++) {
			for (int j = peakdetectPositions - maxAggSize / 2 + 1; j <= peakdetectPositions + maxAggSize / 2; j++) {
				if (((j + i) >= 0) && ((j + i) < features.length))
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
					correctedIndice = peakdetectPositions - delta + i;
				}
			}
		}
		logger.info("corrected indice = {}", correctedIndice);
		return correctedIndice;

	}
}
