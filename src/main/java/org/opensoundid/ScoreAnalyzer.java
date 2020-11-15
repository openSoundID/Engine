package org.opensoundid;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensoundid.configuration.EngineConfiguration;

public class ScoreAnalyzer {

	private static final Logger logger = LogManager.getLogger(ScoreAnalyzer.class);
	private int defaultScoreThreshold;

	private EngineConfiguration config;

	ScoreAnalyzer(EngineConfiguration config) {

		this.config = config;
		defaultScoreThreshold = config.getInt("engine.ScoreAnalyzer.defaultScoreThreshold");

	}

	Map<Integer, Long> analyzeScore(Map<Integer, Long> scores) {

		Map<Integer, Long> analyzedScore = new HashMap<>();

		try {

			for (Entry<Integer, Long> entry : scores.entrySet()) {
				Integer birdID = entry.getKey();
				Long score = entry.getValue();

				int numSignatureThreshold = config.getInt(
						"engine.ScoreAnalyzer.specificScoreThreshold." + Integer.toString(birdID),
						defaultScoreThreshold);

				if (score > numSignatureThreshold) {

					analyzedScore.put(birdID, score);

				}

			}

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

		return analyzedScore;
	}

}
