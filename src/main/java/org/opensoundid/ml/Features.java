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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.hibernate.Session;
import org.hibernate.query.Query;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.opensoundid.configuration.EngineConfiguration;
import java.util.Calendar;
import org.opensoundid.dsp.DSP;
import java.util.GregorianCalendar;
import org.opensoundid.jpa.JpaUtil;
import org.opensoundid.jpa.entity.Record;
import org.opensoundid.model.impl.FeaturesSpecifications;
import org.opensoundid.model.impl.JsonLowLevelFeatures;

import weka.classifiers.meta.RotationForest;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.supervised.instance.ClassBalancer;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.unsupervised.attribute.PrincipalComponents;
import weka.filters.unsupervised.instance.RemoveMisclassified;

public class Features {

	private static final Logger logger = LogManager.getLogger(Features.class);

	private int featuresNumber;
	private double trainingSizePct;
	private String featuresDirectory;
	private String arffTrainingFiles;
	private String arffTestFiles;
	private String subSampleArffTestFiles;
	private int subSampleArffTestMaxCount;
	private String arffTestAlsoFiles;
	private String subSampleArffTestAlsoFiles;
	private int subSampleArffTestAlsoMaxCount;
	private SpreadSubsample spreadSubsample;
	private double spreadSubsampleFactor;
	private int rotationForestNumExecutionSlots;
	private int rotationForestMaxGroup;
	private int rotationForestMinGroup;
	private int rotationForestNumIterations;
	private double principalComponentsVarianceCovered;

	private DSP dsp;
	private FeaturesSpecifications featureSpec;

	public Features(EngineConfiguration config) {

		try {
			dsp = new DSP(config);
			featureSpec = new FeaturesSpecifications(config);
			spreadSubsample = new SpreadSubsample();
			featuresNumber = config.getInt("features.featuresNumber");
			trainingSizePct = config.getDouble("features.trainingSizePct");
			featuresDirectory = config.getString("features.featuresDirectory");
			arffTrainingFiles = config.getString("features.arffTrainingFiles");
			arffTestFiles = config.getString("features.arffTestFiles");
			subSampleArffTestFiles = config.getString("features.subSampleArffTestFiles");
			subSampleArffTestMaxCount = config.getInt("features.subSampleArffTestMaxCount");
			arffTestAlsoFiles = config.getString("features.arffTestAlsoFiles");
			subSampleArffTestAlsoFiles = config.getString("features.subSampleArffTestAlsoFiles");
			subSampleArffTestAlsoMaxCount = config.getInt("features.subSampleArffTestAlsoMaxCount");
			spreadSubsampleFactor = config.getDouble("features.spreadSubsampleFactor");
			rotationForestNumExecutionSlots = config
					.getInt("features.removeMisclassified.rotationForest.NumExecutionSlots");
			rotationForestMaxGroup = config.getInt("features.removeMisclassified.rotationForest.MaxGroup");
			rotationForestMinGroup = config.getInt("features.removeMisclassified.rotationForest.MinGroup");
			rotationForestNumIterations = config.getInt("features.removeMisclassified.rotationForest.NumIterations");
			principalComponentsVarianceCovered = config
					.getDouble("features.removeMisclassified.principalComponents.varianceCovered");

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}

	}

	public void saveMLStandardizedFeature(String fileName, List<double[]> normalizedFeatures, int birdID)
			throws IOException {

		Path path = Paths.get(fileName);
		int classID = featureSpec.findClassId(birdID);
		if (Files.exists(path)) {

			logger.error("File {} already created. Delete it before", path);
			System.exit(-1);

		}

		Instances dataRaw = new Instances("Training", (ArrayList<Attribute>) featureSpec.getAttributes(), 0);
		dataRaw.setClassIndex(featureSpec.getNumOfAttributes());

		for (double[] normalizedFeature : normalizedFeatures) {

			double[] instanceValue = Arrays.copyOf(normalizedFeature, dataRaw.numAttributes());
			instanceValue[featureSpec.getNumOfAttributes()] = classID;
			dataRaw.add(new DenseInstance(1.0, instanceValue));

		}

		ArffSaver saver = new ArffSaver();
		saver.setInstances(dataRaw);
		saver.setFile(new File(fileName));

		saver.writeBatch();

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
						computeMLNormalizedFeature(jsonDirectory, featuresDirectory, recordId, record.getBird().getId(),
								record.getDate(), record.getTime());
					} else {
						logger.error("Record not find in database :  {} ", recordId);
					}

				}

			}
		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

	void concateAllArffFilesForTraining() {

		Instances dataRaw = new Instances("Training", (ArrayList<Attribute>) featureSpec.getAttributes(), 0);
		dataRaw.setClassIndex(featureSpec.getNumOfAttributes());

		List<File> arffFiles;

		try (Stream<Path> walk = Files.walk(Paths.get(featuresDirectory))) {
			arffFiles = walk.filter(foundPath -> foundPath.toString().endsWith(".arff")).map(Path::toFile)
					.collect(Collectors.toList());
			ArffLoader loader = new ArffLoader();

			for (File arffFile : arffFiles) {

				loader.setFile(arffFile);
				Instances fileDataRaw = loader.getDataSet();

				for (int i = 0; i < fileDataRaw.numInstances(); i++) {
					dataRaw.add(fileDataRaw.instance(i));
				}
				loader.reset();
			}

			ArffSaver saver = new ArffSaver();

			dataRaw.randomize(new java.util.Random(0));

			int trainSize = (int) Math.round(dataRaw.numInstances() * (100.0 - trainingSizePct) / 100.0);
			int testSize = dataRaw.numInstances() - trainSize;

			Instances train = new Instances(dataRaw, 0, trainSize);
			Instances test = new Instances(dataRaw, trainSize, testSize);

			spreadSubsample.setMaxCount(featuresNumber * spreadSubsampleFactor);
			spreadSubsample.setInputFormat(train);
			spreadSubsample.setRandomSeed(1);
			train = Filter.useFilter(train, spreadSubsample);
			
			RotationForest rotationForest = new RotationForest();
			rotationForest.setNumExecutionSlots(rotationForestNumExecutionSlots);
			rotationForest.setMaxGroup(rotationForestMaxGroup);
			rotationForest.setMinGroup(rotationForestMinGroup);
			rotationForest.setNumIterations(rotationForestNumIterations);
			J48 j48 = new J48();
			rotationForest.setClassifier(j48);
			PrincipalComponents principalComponents = new PrincipalComponents();
			principalComponents.setVarianceCovered(principalComponentsVarianceCovered);
			principalComponents.setMaximumAttributeNames(-1);
			rotationForest.setProjectionFilter(principalComponents);

			RemoveMisclassified removeMisclassified = new RemoveMisclassified();
			removeMisclassified.setClassifier(rotationForest);
			removeMisclassified.setMaxIterations(1);
			removeMisclassified.setThreshold(0.1);
			removeMisclassified.setClassIndex(-1);
			removeMisclassified.setInputFormat(train);

			train = Filter.useFilter(train, removeMisclassified);

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

		} catch (Exception e) {

			logger.error(e.getMessage(), e);
		}

	}

	void concateAllArffFilesForTestDataset(String featuresDirectory) {

		Instances dataRaw = new Instances("Test", (ArrayList<Attribute>) featureSpec.getAttributes(), 0);
		dataRaw.setClassIndex(featureSpec.getNumOfAttributes());

		List<File> arffFiles;

		try (Stream<Path> walk = Files.walk(Paths.get(featuresDirectory))) {
			arffFiles = walk.filter(foundPath -> foundPath.toString().endsWith(".arff")).map(Path::toFile)
					.collect(Collectors.toList());

			ArffLoader loader = new ArffLoader();

			for (File arffFile : arffFiles) {

				loader.setFile(arffFile);
				Instances fileDataRaw = loader.getDataSet();

				for (int i = 0; i < fileDataRaw.numInstances(); i++) {
					dataRaw.add(fileDataRaw.instance(i));
				}
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

	void addMetaData(List<double[]> normalizedFeatures, String date, String time) {

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

		for (double[] normalizedFeature : normalizedFeatures) {
			int randday = 0;
			int randminute = 0;

			randday = ThreadLocalRandom.current().nextInt(-5, 5);
			randminute = ThreadLocalRandom.current().nextInt(-5, 5);

			normalizedFeature[normalizedFeature.length - 1] = !parseError ? randday + calendar.get(Calendar.DAY_OF_YEAR)
					: Double.NaN;
			normalizedFeature[normalizedFeature.length - 2] = !parseError && time.length() == 5
					? calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) + randminute
					: Double.NaN;
			if ((normalizedFeature[normalizedFeature.length - 1] != Double.NaN)
					&& (normalizedFeature[normalizedFeature.length - 1] < 1))
				normalizedFeature[normalizedFeature.length - 1] = 1;

			if ((normalizedFeature[normalizedFeature.length - 2] != Double.NaN)
					&& (normalizedFeature[normalizedFeature.length - 2] < 1))
				normalizedFeature[normalizedFeature.length - 2] = 1;

		}

	}

	public List<double[]> computeMLFeatures(double[] peakdetectPositions, double[] peakdetectAmplitudes, int chunkIDs[],
			double[][] features, double[] energy, String date, String time) {

		List<double[]> normalizedFeatures = dsp.processMelSpectra(chunkIDs, features, energy, peakdetectPositions,
				peakdetectAmplitudes);
		addMetaData(normalizedFeatures, date, time);

		return normalizedFeatures;

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
				List<double[]> normalizedFeatures = computeMLFeatures(peakdetectPositions, peakdetectAmplitudes,
						chunkIDs, features, energy, date, time);

				// Directory creation
				Path path = Paths.get(featuresDirectory);
				if (!Files.exists(path)) {

					Files.createDirectories(path);

				}

				saveMLStandardizedFeature(path + "/" + recordId + ".arff", normalizedFeatures, birdID);
			}

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

	public static void main(String[] args) {

		logger.info("start Features Extraction");
		Instant start = Instant.now();

		try {

			EngineConfiguration engineConfiguration = new EngineConfiguration();
			Features features = new Features(engineConfiguration);

			features.computeAllFeatures(engineConfiguration.getString("features.alsoJsonDirectory"),
					engineConfiguration.getString("features.alsoFeaturesDirectory"));
			features.computeAllFeatures(engineConfiguration.getString("features.jsonDirectory"),
					engineConfiguration.getString("features.featuresDirectory"));
			features.concateAllArffFilesForTraining();
			features.concateAllArffFilesForTestDataset(engineConfiguration.getString("features.alsoFeaturesDirectory"));

		} catch (Exception e) {

			e.printStackTrace();
		}

		Instant end = Instant.now();
		logger.info("Features extraction process takes:{}", Duration.between(start, end));
		logger.info("End Features extraction");

	}

}
