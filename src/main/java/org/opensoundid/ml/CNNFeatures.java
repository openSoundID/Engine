package org.opensoundid.ml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.dsp.DSP;
import org.opensoundid.jpa.JpaUtil;
import org.opensoundid.jpa.entity.Record;
import org.opensoundid.model.impl.FeaturesSpecifications;
import org.opensoundid.model.impl.JsonLowLevelFeatures;

import com.fasterxml.jackson.databind.ObjectMapper;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.supervised.instance.SpreadSubsample;

public class CNNFeatures {

	private static final Logger logger = LogManager.getLogger(CNNFeatures.class);

	private EngineConfiguration engineConfiguration = new EngineConfiguration();
	private int featuresNumber;
	private double trainingSizePct;
	private String featuresDirectory;
	private String arffTrainingFiles;
	private String arffTestAlsoFiles;
	private String subSampleArffTestAlsoFiles;
	private String arffTestFiles;
	private String subSampleArffTestFiles;
	private int subSampleArffTestAlsoMaxCount;
	private int subSampleArffTestMaxCount;
	private SpreadSubsample spreadSubsample;

	private DSP dsp;
	private FeaturesSpecifications featureSpec;

	public CNNFeatures() {

		try {
			dsp = new DSP(engineConfiguration);
			featureSpec = new FeaturesSpecifications(engineConfiguration);
			spreadSubsample = new SpreadSubsample();
			featuresNumber = engineConfiguration.getInt("CNNFeatures.featuresNumber");
			trainingSizePct = engineConfiguration.getDouble("CNNFeatures.trainingSizePct");
			featuresDirectory = engineConfiguration.getString("CNNFeatures.featuresDirectory");
			arffTrainingFiles = engineConfiguration.getString("CNNFeatures.arffTrainingFiles");
			arffTestAlsoFiles = engineConfiguration.getString("CNNFeatures.arffTestAlsoFiles");
			subSampleArffTestAlsoFiles = engineConfiguration.getString("CNNFeatures.subSampleArffTestAlsoFiles");
			arffTestFiles = engineConfiguration.getString("CNNFeatures.arffTestFiles");
			subSampleArffTestMaxCount = engineConfiguration.getInt("CNNFeatures.subSampleArffTestMaxCount");
			subSampleArffTestAlsoMaxCount = engineConfiguration.getInt("CNNFeatures.subSampleArffTestAlsoMaxCount");
			subSampleArffTestFiles = engineConfiguration.getString("CNNFeatures.subSampleArffTestFiles");

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}

	}

	public void saveCNNMLStandardizedFeature(String fileName, String recordId, int numSpectograms, int birdID,
			String date, String time) throws IOException {

		int classID = featureSpec.findClassId(birdID);

		Instances dataRaw = new Instances("Training", (ArrayList<Attribute>) featureSpec.getCNNAttributes(), 0);
		dataRaw.setClassIndex(dataRaw.numAttributes() - 1);

		for (int i = 0; i < numSpectograms; i++) {

			double[] instanceValue = new double[4];
			instanceValue[0] = dataRaw.attribute(0).addStringValue(recordId + "-" + Integer.toString(i) + ".png");
			double[] dateTimeValue = convertDateTime(date, time);
			instanceValue[1] = dateTimeValue[0];
			instanceValue[2] = dateTimeValue[1];
			instanceValue[3] = classID;

			dataRaw.add(new DenseInstance(1.0, instanceValue));

		}

		if (!dataRaw.isEmpty()) {

			ArffSaver saver = new ArffSaver();
			saver.setInstances(dataRaw);
			saver.setFile(new File(fileName));

			saver.writeBatch();
			

		}

	}

	void computeAllFeatures(String jsonDirectory, String featuresDirectory) {
		List<File> jsonFiles;
		try (Session session = JpaUtil.getSessionFactory().openSession();
				Stream<Path> walk = Files.walk(Paths.get(jsonDirectory))) {
			jsonFiles = walk.filter(foundPath -> foundPath.toString().endsWith(".json")).map(Path::toFile)
					.collect(Collectors.toList());

			for (File jsonFile : jsonFiles) {
				String recordId = jsonFile.getName().split("\\.json")[0];
				logger.info("Record id {}", recordId);
				File featuresFile = new File(featuresDirectory + "/" + recordId + ".arff");
				if (!featuresFile.exists() && (jsonFile.length() != 0)) {
					Query<Record> queryRecord = session.createNamedQuery("Record.findByRecordId", Record.class)
							.setParameter("recordId", recordId);
					Record record = queryRecord.uniqueResult();
					if (record != null) {
						if (record.isEnabled()) {
							computeMLNormalizedFeature(jsonDirectory, featuresDirectory, recordId,
									record.getBird().getId(), record.getDate(), record.getTime());
						} else {
							logger.info("Record {} is disabled", recordId);
						}

					} else {
						logger.error("Record not find in database :  {} ", recordId);
					}

				}

			}
		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

	void concateAllCNNArffFilesForTraining() {

		Instances dataRaw = new Instances("CNN Training", (ArrayList<Attribute>) featureSpec.getCNNAttributes(), 0);
		dataRaw.setClassIndex(dataRaw.numAttributes() - 1);

		List<File> arffFiles;

		try (Stream<Path> walk = Files.walk(Paths.get(featuresDirectory))) {
			arffFiles = walk.filter(foundPath -> foundPath.toString().endsWith(".arff")).map(Path::toFile)
					.collect(Collectors.toList());

			ArffLoader loader = new ArffLoader();
			for (File arffFile : arffFiles) {
				logger.info(arffFile.getName());
				loader.setFile(arffFile);
				Instances fileDataRaw = loader.getDataSet();
				
				for (int i = 0; i < fileDataRaw.numInstances(); i++) {
					double[] instanceValue = new double[4];

					instanceValue[0] = dataRaw.attribute(0).addStringValue(fileDataRaw.instance(i).stringValue(0));

					instanceValue[1] = fileDataRaw.instance(i).value(1);
					instanceValue[2] = fileDataRaw.instance(i).value(2);
					instanceValue[3] = fileDataRaw.instance(i).value(3);

					dataRaw.add(new DenseInstance(1.0, instanceValue));
				}
				loader.reset();

			}

			ArffSaver saver = new ArffSaver();

			dataRaw.randomize(new java.util.Random(0));

			int trainSize = (int) Math.round(dataRaw.numInstances() * (100.0 - trainingSizePct) / 100.0);
			int testSize = dataRaw.numInstances() - trainSize;

			Instances train = new Instances(dataRaw, 0, trainSize);
			Instances test = new Instances(dataRaw, trainSize, testSize);

			spreadSubsample.setMaxCount(featuresNumber);
			spreadSubsample.setInputFormat(train);
			spreadSubsample.setRandomSeed(1);
			train = Filter.useFilter(train, spreadSubsample);

			saver.setInstances(train);
			saver.setFile(new File(arffTrainingFiles));
			saver.writeBatch();

			saver.setInstances(test);
			saver.setFile(new File(arffTestFiles));
			saver.writeBatch();

			spreadSubsample.setMaxCount(subSampleArffTestMaxCount);
			spreadSubsample.setInputFormat(test);
			spreadSubsample.setRandomSeed(1);
			test = Filter.useFilter(test, spreadSubsample);

			saver.setInstances(test);
			saver.setFile(new File(subSampleArffTestFiles));
			saver.writeBatch();

			saver.setInstances(dataRaw);
			saver.setFile(new File(arffTrainingFiles));
			saver.writeBatch();

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
		}

	}

	void concateAllCNNArffFilesForTestDataset(String featuresDirectory) {

		Instances dataRaw = new Instances("CNN Test", (ArrayList<Attribute>) featureSpec.getCNNAttributes(), 0);
		dataRaw.setClassIndex(dataRaw.numAttributes() - 1);

		List<File> arffFiles;

		try (Stream<Path> walk = Files.walk(Paths.get(featuresDirectory))) {
			arffFiles = walk.filter(foundPath -> foundPath.toString().endsWith(".arff")).map(Path::toFile)
					.collect(Collectors.toList());

			ArffLoader loader = new ArffLoader();
			for (File arffFile : arffFiles) {
				logger.info(arffFile.getName());
				loader.setFile(arffFile);
				Instances fileDataRaw = loader.getDataSet();
				logger.info("nombre instances lues {}", fileDataRaw.numInstances());
				for (int i = 0; i < fileDataRaw.numInstances(); i++) {
					double[] instanceValue = new double[4];

					instanceValue[0] = dataRaw.attribute(0).addStringValue(fileDataRaw.instance(i).stringValue(0));

					instanceValue[1] = fileDataRaw.instance(i).value(1);
					instanceValue[2] = fileDataRaw.instance(i).value(2);
					instanceValue[3] = fileDataRaw.instance(i).value(3);

					dataRaw.add(new DenseInstance(1.0, instanceValue));
				}
				logger.info("nombre instances dans dataraw: {}", dataRaw.numInstances());
				loader.reset();

			}

			ArffSaver saver = new ArffSaver();

			dataRaw.randomize(new java.util.Random(0));

			saver.setInstances(dataRaw);
			saver.setFile(new File(arffTestAlsoFiles));
			saver.writeBatch();

			spreadSubsample.setMaxCount(subSampleArffTestAlsoMaxCount);
			spreadSubsample.setInputFormat(dataRaw);
			spreadSubsample.setRandomSeed(1);
			dataRaw = Filter.useFilter(dataRaw, spreadSubsample);

			saver.setInstances(dataRaw);
			saver.setFile(new File(subSampleArffTestAlsoFiles));
			saver.writeBatch();

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
		}

	}

	/*
	 * convert date and time to day of year and number of minute of day
	 */
	public double[] convertDateTime(String date, String time) {

		double[] returnValue = new double[2];
		DateFormat format;

		Calendar calendar = new GregorianCalendar();
		boolean parseError = false;

		if (time.length() == 5)
			format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
		else
			format = new SimpleDateFormat("yyyy-MM-dd");

		Date parseDate;

		try {

			time = time.replace(".", ":");
			time = time.replace("~", " ");
			if (time.length() == 5)
				parseDate = format.parse(date + " " + time);
			else
				parseDate = format.parse(date);

			calendar.setTime(parseDate);
		} catch (java.text.ParseException e) {

			logger.error(e.getMessage(), e);
			parseError = true;
		}

		int randday = 0;
		int randminute = 0;

		randday = ThreadLocalRandom.current().nextInt(-5, 5);
		randminute = ThreadLocalRandom.current().nextInt(-5, 5);

		returnValue[0] = !parseError ? randday + calendar.get(Calendar.DAY_OF_YEAR) : Double.NaN;
		returnValue[1] = !parseError && time.length() == 5
				? calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) + randminute
				: Double.NaN;
		if ((returnValue[0] != Double.NaN) && (returnValue[0] < 1))
			returnValue[0] = 1;

		if ((returnValue[1] != Double.NaN) && (returnValue[1] < 1))
			returnValue[1] = 1;

		return returnValue;

	}

	public int computeMLFeatures(String recordId, double[] peakdetectPositions, double[] peakdetectAmplitudes,
			int[] chunkIDs, double[][] features, double[] energy) {

		return dsp.processMelSpectra(recordId, chunkIDs, features, energy, peakdetectPositions, peakdetectAmplitudes);

	}

	void computeMLNormalizedFeature(String jsonDirectory, String featuresDirectory, String recordId, int birdID,
			String date, String time) {

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			File jsonFile = new File(jsonDirectory + "/" + recordId + ".json");
			JsonLowLevelFeatures jsonLowLevelFeatures = objectMapper.readValue(jsonFile, JsonLowLevelFeatures.class);

			double[][] features = jsonLowLevelFeatures.getLowlevel() != null
					? jsonLowLevelFeatures.getLowlevel().getMfccBandLogs()
					: null;
			double[] energy = jsonLowLevelFeatures.getLowlevel() != null
					? jsonLowLevelFeatures.getLowlevel().getEnergy()
					: null;
			int[] chunkIDs = jsonLowLevelFeatures.getLowlevel() != null
					? jsonLowLevelFeatures.getLowlevel().getChunckID()
					: null;
			double[] peakdetectPositions = jsonLowLevelFeatures.getDescription() != null
					? jsonLowLevelFeatures.getDescription().getPeakdetect_positions()[0]
					: null;
			double[] peakdetectAmplitudes = jsonLowLevelFeatures.getDescription() != null
					? jsonLowLevelFeatures.getDescription().getPeakdetect_amplitudes()[0]
					: null;

			if ((features != null) && (energy != null) && (chunkIDs != null) && (peakdetectPositions != null)
					&& (peakdetectAmplitudes != null)) {
				int numSpectograms = computeMLFeatures(recordId, peakdetectPositions, peakdetectAmplitudes, chunkIDs,
						features, energy);

				// Directory creation
				Path path = Paths.get(featuresDirectory);
				if (!Files.exists(path)) {

					Files.createDirectories(path);

				}

				saveCNNMLStandardizedFeature(path + "/" + recordId + ".arff", recordId, numSpectograms, birdID, date,
						time);
			}

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

	public static void main(String[] args) {

		logger.info("start CNN Features Extraction");
		Instant start = Instant.now();

		try {

			EngineConfiguration engineConfiguration = new EngineConfiguration();
			CNNFeatures features = new CNNFeatures();

			features.computeAllFeatures(engineConfiguration.getString("CNNFeatures.alsoJsonDirectory"),
					engineConfiguration.getString("CNNFeatures.alsoFeaturesDirectory"));
			features.computeAllFeatures(engineConfiguration.getString("CNNFeatures.jsonDirectory"),
					engineConfiguration.getString("CNNFeatures.featuresDirectory"));
			features.concateAllCNNArffFilesForTraining();

			features.concateAllCNNArffFilesForTestDataset(
					engineConfiguration.getString("CNNFeatures.alsoFeaturesDirectory"));

		} catch (Exception e) {

			e.printStackTrace();
		}

		Instant end = Instant.now();
		logger.info("Features extraction process takes:{}", Duration.between(start, end));
		logger.info("End CNN Features extraction");

	}

}
