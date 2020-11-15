package org.opensoundid;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.dsp.DSP;
import org.opensoundid.engine.Engine;
import org.opensoundid.ml.Classification;
import org.opensoundid.model.impl.BirdObservation;
import org.opensoundid.model.impl.FeaturesSpecifications;
import org.opensoundid.model.impl.JsonLowLevelFeatures;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class SoundAnalyzer {

	private static final Logger logger = LogManager.getLogger(SoundAnalyzer.class);

	public static boolean isCompletelyWritten(File file) {
		RandomAccessFile stream = null;
		try {
			stream = new RandomAccessFile(file, "rw");
			return true;
		} catch (Exception e) {
			logger.info("Skipping file " + file.getName() + " for this iteration due it's not completely written");
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					logger.error("Exception during closing file " + file.getName());
				}
			}
		}
		return false;
	}

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

		Classification classification = new Classification(config);

		String recordDirectory = config.getString("engine.SoundAnalyzer.recordDirectory");
		Engine engine = new Engine(config);
		ScoreAnalyzer scoreAnalyzer = new ScoreAnalyzer(config);
		ScoreFilter scoreFilter = new ScoreFilter(config);
		ScoreLogger scoreLogger = new ScoreLogger(config);
		DSP dsp = new DSP(config);
		ResultSender resultSender = new ResultSender(config);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(config.getString("engine.SoundAnalyzer.dateFormat"));
		String endFile = config.getString("engine.SoundAnalyzer.endFile");

		try {

			while (!Files.exists(Paths.get(endFile))) {
				List<String> jsonFilePaths = Files.walk(Paths.get(recordDirectory))
						.filter(foundPath -> foundPath.toString().endsWith(".json"))
						.sorted((f1, f2) -> (int) -1
								* Long.compare(f1.toFile().lastModified(), f2.toFile().lastModified()))
						.map(javaPath -> javaPath.toString()).collect(Collectors.toList());

				if (!jsonFilePaths.isEmpty()) {

					if (Files.exists(Paths.get("/tmp/engine.lock"))) {
						Thread.sleep(5000);

					}

					// control if the wav file has not been already processed
					String jsonFileName = jsonFilePaths.get(0);
					String reportName = jsonFileName.substring(0, jsonFileName.lastIndexOf('.')) + ".txt";
					if (!Files.exists(Paths.get(reportName))) {

						while (!isCompletelyWritten(new File(jsonFileName))) {
							Thread.sleep(5);
						}

						ObjectMapper objectMapper = new ObjectMapper();
						File jsonFile = new File(jsonFileName);
						JsonLowLevelFeatures jsonLowLevelFeatures = objectMapper.readValue(jsonFile,
								JsonLowLevelFeatures.class);

						double[][] features = jsonLowLevelFeatures.getLowlevel().getMfcc_bands_log();
						double[] energy = jsonLowLevelFeatures.getLowlevel().getEnergy();

						List<double[]> normalizedFeatures = dsp.processMelSpectra(features, energy, 66.0);

						if (!normalizedFeatures.isEmpty()) {
							Instances dataRaw = new Instances("Sound Analyse",
									(ArrayList<Attribute>) featureSpec.getAttributes(), 0);

							for (int i = 0; i < normalizedFeatures.size(); i++) {

								double[] instanceValue = new double[dataRaw.numAttributes()];

								for (int j = 0; j < normalizedFeatures.get(i).length; j++) {
									instanceValue[j] = normalizedFeatures.get(i)[j];

								}

								dataRaw.add(new DenseInstance(1.0, instanceValue));

							}

							dataRaw.setClassIndex(dataRaw.numAttributes() - 1);
							double[][] resultat = classification.evaluate(dataRaw);

							Map<Integer, Long> scores = engine.computeScore(resultat);
							Map<Integer, Long> analyzedScores = scoreAnalyzer.analyzeScore(scores);

							analyzedScores.forEach((k, v) -> {
								try {

									BirdObservation birdObservation = new BirdObservation(simpleDateFormat
											.format(Files.getLastModifiedTime(Paths.get(jsonFileName)).toMillis()), k);
									Files.write(Paths.get(reportName),
											String.format("signature %d:%d\n", k, v).getBytes(),
											StandardOpenOption.CREATE, StandardOpenOption.APPEND);

								} catch (IOException ex) {

									logger.error(ex.getMessage(), ex);
								}
							});

							try {
								Files.write(Paths.get(reportName), String.format("Filtred Score\n").getBytes(),
										StandardOpenOption.CREATE, StandardOpenOption.APPEND);
							} catch (IOException ex) {

								logger.error(ex.getMessage(), ex);

							}

							Map<Integer, Long> filtredScores = scoreFilter.filtreScore(analyzedScores, config);

							filtredScores.forEach((birdId, score) -> {
								try {
									String date = simpleDateFormat
											.format(Files.getLastModifiedTime(Paths.get(jsonFileName)).toMillis());
									BirdObservation birdObservation = new BirdObservation(date, birdId);
									Files.write(Paths.get(reportName),
											String.format("signature %d:%d\n", birdId, score).getBytes(),
											StandardOpenOption.CREATE, StandardOpenOption.APPEND);
									scoreLogger.logScore(date, birdId, featureSpec.findBirdName(birdId), score);
									if (birdObservation.getBirdCallID() != 0)
										resultSender.sendResult(birdObservation);

								} catch (IOException ex) {

									logger.error(ex.getMessage(), ex);
								}
							});
						} else {
							try {
								Files.write(Paths.get(reportName), String.format("Empty instance\n").getBytes(),
										StandardOpenOption.CREATE, StandardOpenOption.APPEND);
							} catch (IOException ex) {

								logger.error(ex.getMessage(), ex);

							}
						}

					} else {
						Thread.sleep(10);
					}
				}
			}

		} catch (IOException ex) {

			logger.error(ex.getMessage(), ex);
		}

		catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

}
