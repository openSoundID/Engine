package org.opensoundid;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.engine.Engine;
import org.opensoundid.ml.Classification;
import org.opensoundid.ml.Features;
import org.opensoundid.model.impl.BirdObservation;
import org.opensoundid.model.impl.FeaturesSpecifications;
import org.opensoundid.model.impl.JsonLowLevelFeatures;

import com.fasterxml.jackson.databind.ObjectMapper;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class SoundAnalyzer {

	private static final Logger logger = LogManager.getLogger(SoundAnalyzer.class);

	public static void main(String[] args) {

		CommandLineParser parser = new DefaultParser();

		Options options = new Options();
		options.addOption(Option.builder("enginePropertiesFile").longOpt("enginePropertiesFile")
				.desc("Engine Properties File").required().hasArg().argName("File Name").build());

		String enginePropertiesFile = "";

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			// validate that arguments has been set
			if (line.hasOption("enginePropertiesFile")) {
				enginePropertiesFile = line.getOptionValue("enginePropertiesFile");

			} else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Signature", options);
				System.exit(-1);

			}
		} catch (ParseException ex) {
			logger.error(ex.getMessage(), ex);
		}

		EngineConfiguration config = new EngineConfiguration(enginePropertiesFile);

		FeaturesSpecifications featureSpec = new FeaturesSpecifications(config);
		Features mlFeatures = new Features(config);
		Classification classification = new Classification(config);

		String recordDirectory = config.getString("soundAnalyzer.recordDirectory");
		Engine engine = new Engine(config);
		ScoreAnalyzer scoreAnalyzer = new ScoreAnalyzer(config);
		ScoreFilter scoreFilter = new ScoreFilter(config);
		ScoreLogger scoreLogger = new ScoreLogger(config);

		ResultSender resultSender = new ResultSender(config);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(config.getString("SoundAnalyzer.dateFormat"));
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm");
		String endFile = config.getString("SoundAnalyzer.endFile");

		while (!Files.exists(Paths.get(endFile))) {
			try (Stream<Path> walk = Files.walk(Paths.get(recordDirectory))) {
				List<String> jsonFilePaths = walk.filter(foundPath -> foundPath.toString().endsWith(".json"))
						.sorted((f1, f2) -> Long.compare(f2.toFile().lastModified(), f1.toFile().lastModified()))
						.map(Path::toString).collect(Collectors.toList());

				if (!jsonFilePaths.isEmpty()) {

					// control if the wav file has not been already processed
					String jsonFileName = jsonFilePaths.get(0);
					String reportName = jsonFileName.substring(0, jsonFileName.lastIndexOf('.')) + ".txt";
					if (!Files.exists(Paths.get(reportName))) {

						ObjectMapper objectMapper = new ObjectMapper();
						File jsonFile = new File(jsonFileName);
						JsonLowLevelFeatures jsonLowLevelFeatures = objectMapper.readValue(jsonFile,
								JsonLowLevelFeatures.class);

						double[][] features = jsonLowLevelFeatures.getLowlevel().getMfcc_bands_log();
						double[] energy = jsonLowLevelFeatures.getLowlevel().getEnergy();
						double zeroCrossingRate = jsonLowLevelFeatures.getDescription().getZero_crossing_rate()[0];
						double[] envelope = jsonLowLevelFeatures.getDescription().getEnvelope()[0];

						String date = dateFormatter
								.format(Files.getLastModifiedTime(Paths.get(jsonFileName)).toMillis());
						String time = timeFormatter
								.format(Files.getLastModifiedTime(Paths.get(jsonFileName)).toMillis());

						List<double[]> normalizedFeatures = mlFeatures.computeMLFeatures(zeroCrossingRate, envelope,
								features, energy, date, time);

						if (!normalizedFeatures.isEmpty()) {
							Instances dataRaw = new Instances("Sound Analyse",
									(ArrayList<Attribute>) featureSpec.getAttributes(), 0);

							for (int i = 0; i < normalizedFeatures.size(); i++) {

								double[] instanceValue = new double[dataRaw.numAttributes()];

								instanceValue = Arrays.copyOf(normalizedFeatures.get(i),
										normalizedFeatures.get(i).length);

								dataRaw.add(new DenseInstance(1.0, instanceValue));

							}

							dataRaw.setClassIndex(dataRaw.numAttributes() - 1);
							double[][] resultat = classification.evaluate(dataRaw);

							Map<Integer, Long> scores = engine.computeScore(resultat);
							Map<Integer, Long> analyzedScores = scoreAnalyzer.analyzeScore(scores);

							analyzedScores.forEach((k, v) -> {
								try {

									Files.write(Paths.get(reportName),
											String.format("signature %d:%d%n", k, v).getBytes(),
											StandardOpenOption.CREATE, StandardOpenOption.APPEND);

								} catch (IOException ex) {

									logger.error(ex.getMessage(), ex);
								}
							});

							Files.write(Paths.get(reportName), String.format("Filtred Score%n").getBytes(),
									StandardOpenOption.CREATE, StandardOpenOption.APPEND);

							Map<Integer, Long> filtredScores = scoreFilter.filtreScore(analyzedScores, config);

							filtredScores.forEach((birdId, score) -> {
								try {
									String fileDate = simpleDateFormat
											.format(Files.getLastModifiedTime(Paths.get(jsonFileName)).toMillis());
									BirdObservation birdObservation = new BirdObservation(fileDate, birdId);
									Files.write(Paths.get(reportName),
											String.format("signature %d:%d%n", birdId, score).getBytes(),
											StandardOpenOption.CREATE, StandardOpenOption.APPEND);
									scoreLogger.logScore(fileDate, birdId, featureSpec.findBirdName(birdId), score);
									if (birdObservation.getBirdCallID() != 0)
										resultSender.sendResult(birdObservation);

								} catch (IOException ex) {

									logger.error(ex.getMessage(), ex);
								}
							});
						} else {
							try {
								Files.write(Paths.get(reportName), "Empty instance\n".getBytes(),
										StandardOpenOption.CREATE, StandardOpenOption.APPEND);
							} catch (IOException ex) {

								logger.error(ex.getMessage(), ex);

							}
						}

					} else {
						Thread.sleep(10);
					}
				}
			} catch (Exception ex) {

				logger.error(ex.getMessage(), ex);
			}

		}

	}

}
