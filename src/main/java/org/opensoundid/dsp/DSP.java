package org.opensoundid.dsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.image.Spectrogram;

public class DSP {

	private int filterBankNumFilters;
	private int maxAggSize;
	/* calculate delta features based on preceding and following N frames */
	private int deltaOptimizePeakIndice;
	private double peakAmplitudePercentile;
	private int peakMinSequences;
	private double melSpectraNormalisationPercentile;
	

	private static final Logger logger = LogManager.getLogger(DSP.class);

	public DSP(EngineConfiguration config) {

		filterBankNumFilters = config.getInt("dsp.filterBankNumFilters");
		maxAggSize = config.getInt("dsp.maxAggSize");

		deltaOptimizePeakIndice = config.getInt("dsp.deltaOptimizePeakIndice");
		peakAmplitudePercentile = config.getDouble("dsp.peakAmplitudePercentile");
		peakMinSequences = config.getInt("dsp.peakMinSequences");
		melSpectraNormalisationPercentile = config.getDouble("dsp.melSpectraNormalisation.percentile");
		

	}
	
	public int processMelSpectra(String recordId,int[] chunkIDs, double[][] features, double[] energy,
			double[] peakdetectPositions, double[] peakdetectAmplitudes,String spectrogramFilesPath) {

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

		return createSpectograms(recordId,melSpectraNormalisation(chunkedFeatures), peakdetectPositions, peakdetectAmplitudes,spectrogramFilesPath);

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

	public int createSpectograms(String recordId,List<double[][]> featuresList, double[] peakdetectPositions,
			double[] peakdetectAmplitudes,String spectogramImagePath) {

		DescriptiveStatistics[] stats = new DescriptiveStatistics[filterBankNumFilters + 1];
		for (int k = 0; k < stats.length; k++)
			stats[k] = new DescriptiveStatistics();

		int numImage =0;

		DescriptiveStatistics stat = new DescriptiveStatistics();
		for (double peakdetectAmplitude : peakdetectAmplitudes)
			stat.addValue(peakdetectAmplitude);

		double percentile = stat.getPercentile(peakAmplitudePercentile);

		for (int i = 0; i < featuresList.size(); i++) {

			if ((peakdetectAmplitudes[i] >= percentile) || (peakdetectAmplitudes.length < peakMinSequences)) {

				int indice = optimizeIndice((2*deltaOptimizePeakIndice+maxAggSize) / 2 - 1, featuresList.get(i), deltaOptimizePeakIndice);
				if ((indice + maxAggSize / 2) < featuresList.get(i).length) {
					
					int indiceMin = indice - (maxAggSize / 2) + 1;
					int indiceMax = indice + (maxAggSize / 2);
					
					Spectrogram.toImage(featuresList.get(i),indiceMin,indiceMax, recordId+"-"+Integer.toString(numImage++),spectogramImagePath);

	

				}
			}
		}

   return numImage;

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
		
		return correctedIndice;

	}
}
