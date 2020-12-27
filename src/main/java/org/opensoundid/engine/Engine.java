package org.opensoundid.engine;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.model.impl.FeaturesSpecifications;

public class Engine {

	private static final Logger logger = LogManager.getLogger(Engine.class);
	EngineConfiguration engineConfiguration;
	FeaturesSpecifications featuresSpecifications;
	double percentile;

	public Engine(EngineConfiguration engineConfiguration) {
		this.engineConfiguration = engineConfiguration;
		featuresSpecifications = new FeaturesSpecifications(engineConfiguration);
		percentile = engineConfiguration.getDouble("engine.percentile");

	}

	public Map<Integer, Long> computeScore(double[][] resultat) {

		Map<Integer, Long> score = new HashMap<>();
		DescriptiveStatistics stats = new DescriptiveStatistics();

		double[] aggregation = new double[resultat[0].length];

		for (int i = 0; i < resultat.length; i++) {
			for (int j = 0; j < resultat[i].length; j++) {
				aggregation[j] = aggregation[j] + resultat[i][j];				
			}
		}

		for (int i = 0; i < aggregation.length; i++) {
			stats.addValue(aggregation[i]);
		}

		for (int i = 0; i < aggregation.length; i++) {
			if (aggregation[i] > stats.getPercentile(percentile)) {
				score.put(featuresSpecifications.findBirdId(i), Math.round(aggregation[i]));
			}
		}

		return score;

	}

}
