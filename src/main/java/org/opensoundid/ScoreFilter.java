package org.opensoundid;

import java.util.Map;
import java.util.stream.Collectors;
import static java.util.Collections.reverseOrder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensoundid.configuration.EngineConfiguration;

public class ScoreFilter {

	private static final Logger logger = LogManager.getLogger(ScoreFilter.class);
	private int maxResult = 1;
	private int maxRatio = 1;

	private EngineConfiguration engineConfiguration = new EngineConfiguration();

	ScoreFilter() {

		maxResult = engineConfiguration.getInt("engine.ScoreFiltrer.maxResult", 1);
		maxRatio = engineConfiguration.getInt("engine.ScoreFiltrer.maxRatio", 100);

	}

	Map<Integer, Long> filtreScore(Map<Integer, Long> scores, EngineConfiguration config) {

		if (scores.size() != 0) {

			if (scores.containsKey(Integer.valueOf(0))) {
				Long noiseScore = scores.get(Integer.valueOf(0));
				if (noiseScore > config.getInt("engine.ScoreAnalyzer.specificScoreThreshold.0")) {
					scores.clear();
					scores.put(Integer.valueOf(0), noiseScore);
				}

			}

			Map<Integer, Long> result = scores.entrySet().stream().sorted(reverseOrder(Map.Entry.comparingByValue()))
					.limit(maxResult).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			Long bestScore = result.values().stream().max(Long::compareTo).get();

			return scores.entrySet().stream().filter(entry -> entry.getValue() >= bestScore / maxRatio)
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		return scores;

	}
}
