package org.opensoundid;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.engine.Engine;
import org.opensoundid.ml.CNNFeatures;
import org.opensoundid.ml.CNNClassification;
import org.opensoundid.model.impl.BirdObservation;
import org.opensoundid.model.impl.FeaturesSpecifications;
import org.opensoundid.model.impl.JsonLowLevelFeatures;

import com.fasterxml.jackson.databind.ObjectMapper;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class SoundAnalyzer {

	private static final Logger logger = LogManager.getLogger(SoundAnalyzer.class);
	EngineConfiguration config = new EngineConfiguration();
	FeaturesSpecifications featureSpec = new FeaturesSpecifications(config);

	Instances jsonFileToFeatures(String jsonFilePath,String spectrogramFilesPath) throws Exception {

		CNNFeatures mlFeatures = new CNNFeatures();
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm");
		ObjectMapper objectMapper = new ObjectMapper();
		File jsonFile = new File(jsonFilePath);

		Instances dataRaw = new Instances("Sound Analyze", (ArrayList<Attribute>) featureSpec.getCNNAttributes(), 0);

		JsonLowLevelFeatures jsonLowLevelFeatures = objectMapper.readValue(jsonFile, JsonLowLevelFeatures.class);

		String date = dateFormatter.format(Files.getLastModifiedTime(Paths.get(jsonFilePath)).toMillis());
		String time = timeFormatter.format(Files.getLastModifiedTime(Paths.get(jsonFilePath)).toMillis());

		double[][] features = jsonLowLevelFeatures.getLowlevel() != null
				? jsonLowLevelFeatures.getLowlevel().getMfccBandLogs()
				: null;
		double[] energy = jsonLowLevelFeatures.getLowlevel() != null ? jsonLowLevelFeatures.getLowlevel().getEnergy()
				: null;
		int[] chunkIDs = jsonLowLevelFeatures.getLowlevel() != null ? jsonLowLevelFeatures.getLowlevel().getChunckID()
				: null;
		double[] peakdetectPositions = jsonLowLevelFeatures.getDescription() != null
				? jsonLowLevelFeatures.getDescription().getPeakdetect_positions()[0]
				: null;
		double[] peakdetectAmplitudes = jsonLowLevelFeatures.getDescription() != null
				? jsonLowLevelFeatures.getDescription().getPeakdetect_amplitudes()[0]
				: null;

		int numFeatures = ((features != null) && (energy != null) && (chunkIDs != null)
				&& (peakdetectPositions != null) && (peakdetectAmplitudes != null))
						? mlFeatures.computeMLFeatures(FilenameUtils.getBaseName(jsonFilePath),peakdetectPositions, peakdetectAmplitudes, chunkIDs, features,
								energy,spectrogramFilesPath)
						: 0;

		if (numFeatures!=0) {

			dataRaw.setClassIndex(featureSpec.getNumOfAttributes());

			for (int i=0;i< numFeatures;i++) {

				double[] instanceValue = new double[4];
				instanceValue[0] = dataRaw.attribute(0).addStringValue(FilenameUtils.getBaseName(jsonFilePath) + "-" + Integer.toString(i) + ".png");
				double[] dateTimeValue=mlFeatures.convertDateTime(date,time);
				instanceValue[1] = dateTimeValue[0];
				instanceValue[2] = dateTimeValue[1];
				
				
				dataRaw.add(new DenseInstance(1.0, instanceValue));

			}

		}
		return dataRaw;
	}

	void analyze() {

		CNNClassification classification = new CNNClassification();
		String recordDirectory = config.getString("soundAnalyzer.recordDirectory");
		Engine engine = new Engine(config);
		ScoreAnalyzer scoreAnalyzer = new ScoreAnalyzer();
		ScoreFilter scoreFilter = new ScoreFilter();
		ScoreLogger scoreLogger = new ScoreLogger();
		ResultSender resultSender = new ResultSender();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(config.getString("SoundAnalyzer.dateFormat"));
		String endFile = config.getString("SoundAnalyzer.endFile");
		boolean analyzeLastFileOnly = config.getBoolean("SoundAnalyzer.analyzeLastFileOnly");

		while (!Files.exists(Paths.get(endFile))) {
			try (Stream<Path> walk = Files.walk(Paths.get(recordDirectory))) {
				List<String> jsonFilePaths = walk.filter(foundPath -> foundPath.toString().endsWith(".json"))
						.sorted((f1, f2) -> Long.compare(f2.toFile().lastModified(), f1.toFile().lastModified()))
						.map(Path::toString).collect(Collectors.toList());

				if (!jsonFilePaths.isEmpty()) {

					if (analyzeLastFileOnly) {
						String jsonFilePath = jsonFilePaths.get(0);
						jsonFilePaths = new ArrayList<>();
						jsonFilePaths.add(jsonFilePath);
					}
					for (String jsonFilePath : jsonFilePaths) {
						
						// control if the wav file has not been already processed

						String reportName = jsonFilePath.substring(0, jsonFilePath.lastIndexOf('.')) + ".txt";
						if (!Files.exists(Paths.get(reportName))) {
							
							Instances instances = jsonFileToFeatures(jsonFilePath, recordDirectory);
							if (instances.isEmpty()) {

								Files.write(Paths.get(reportName), "Empty instance\n".getBytes(),
										StandardOpenOption.CREATE, StandardOpenOption.APPEND);

							} else {
								
								double[][] resultat = classification.predict(instances,recordDirectory);
      							Map<Integer, Long> scores = engine.computeScore(resultat);
								Map<Integer, Long> analyzedScores = scoreAnalyzer.analyzeScore(scores);
								analyzedScores.forEach((k, v) -> {
									try {

										Files.write(Paths.get(reportName),
												String.format("class %d:%d%n", k, v).getBytes(),
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
												.format(Files.getLastModifiedTime(Paths.get(jsonFilePath)).toMillis());
										BirdObservation birdObservation = new BirdObservation(fileDate, birdId);
										Files.write(Paths.get(reportName),
												String.format("signature %d:%d%n", birdId, score).getBytes(),
												StandardOpenOption.CREATE, StandardOpenOption.APPEND);
										scoreLogger.logScore(Paths.get(jsonFilePath).getFileName().toString(), fileDate,
												birdId, featureSpec.findBirdName(birdId), score);
										if (birdObservation.getBirdCallID() != 0)
											resultSender.sendResult(birdObservation);

									} catch (IOException ex) {

										logger.error(ex.getMessage(), ex);
									}
								});
							}

						}
					}
				} else {
					Thread.sleep(10);
				}

			}

			catch (Exception ex) {

				logger.error(ex.getMessage(), ex);
			}
		}

	}

	SoundAnalyzer() {

	}

	public static void main(String[] args) {

		SoundAnalyzer soundAnalyzer = new SoundAnalyzer();
		soundAnalyzer.analyze();

	}

}
