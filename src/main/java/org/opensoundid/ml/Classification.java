package org.opensoundid.ml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;

import weka.classifiers.meta.RotationForest;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;

import weka.filters.unsupervised.attribute.PrincipalComponents;

public class Classification {

	private static final Logger logger = LogManager.getLogger(Classification.class);

	private Classifier classifier;
	private RotationForest rotationForest = new RotationForest();
	private String trainingDirectory;
	private String modelFileName;
	private int rotationForestNumExecutionSlots;
	private int rotationForestMaxGroup;
	private int rotationForestMinGroup;
	private int rotationForestNumIterations;
	private double principalComponentsVarianceCovered;

	public Classification(EngineConfiguration engineConfiguration) {

		try {

			trainingDirectory = engineConfiguration.getString("classification.trainingDirectory");
			modelFileName = engineConfiguration.getString("classification.modelFileName");

			rotationForestNumExecutionSlots = engineConfiguration
					.getInt("classification.rotationForest.NumExecutionSlots");
			rotationForestMaxGroup = engineConfiguration.getInt("classification.rotationForest.MaxGroup");
			rotationForestMinGroup = engineConfiguration.getInt("classification.rotationForest.MinGroup");
			rotationForestNumIterations = engineConfiguration.getInt("classification.rotationForest.NumIterations");
			principalComponentsVarianceCovered = engineConfiguration
					.getDouble("classification.principalComponents.varianceCovered");

			if (Files.exists(Paths.get(modelFileName))) {
				classifier = (Classifier) weka.core.SerializationHelper.read(modelFileName);
			} else {

				ArffLoader loader = new ArffLoader();
				loader.setSource(new File(trainingDirectory + "/training.arff"));

				Instances trainingDataSet = loader.getDataSet();
				trainingDataSet.setClassIndex(trainingDataSet.numAttributes() - 1);

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

				rotationForest.buildClassifier(trainingDataSet);

				classifier = rotationForest;

				SerializationHelper.write(modelFileName, classifier);
			}

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}

	}

	public double[][] evaluate(Instances testInstances) {

		Evaluation eval = null;
		double[][] resultat = new double[testInstances.numInstances()][testInstances.numClasses()];

		try {

			eval = new Evaluation(testInstances);
			eval.evaluateModel(classifier, testInstances);

			int numTestInstances = testInstances.numInstances();
			logger.info("There are {} test instances", numTestInstances);

			// Loop over each test instance.
			for (int i = 0; i < numTestInstances; i++) {

				// Get the prediction probability distribution.
				double[] predictionDistribution = classifier.distributionForInstance(testInstances.instance(i));

				for (int j = 0; j < predictionDistribution.length; j++) {
					resultat[i][j] = predictionDistribution[j]
							* testInstances.instance(i).value(testInstances.numAttributes() - 2);
				}

			}

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

		return resultat;

	}

	public double[] evaluateScore(Instances testInstances) {

		Evaluation eval = null;
		double[] resultat = new double[testInstances.numClasses()];
		double[] score = new double[testInstances.numClasses()];
		double[] returnScore = new double[2];

		try {

			eval = new Evaluation(testInstances);
			eval.evaluateModel(classifier, testInstances);

			int numTestInstances = testInstances.numInstances();
			logger.info("There are {} test instances", numTestInstances);

			// Loop over each test instance.
			for (int i = 0; i < numTestInstances; i++) {

				// Get the prediction probability distribution.
				double[] predictionDistribution = classifier.distributionForInstance(testInstances.instance(i));

				for (int j = 0; j < predictionDistribution.length; j++) {

					resultat[j] = resultat[j] + predictionDistribution[j];

				}

			}

			double total = Arrays.stream(resultat).sum();
			for (int i = 0; i < score.length; i++) {
				score[i] = resultat[i] / total;
			}
			returnScore[0] = score[(int) testInstances.instance(0).classValue()];
			returnScore[1] = Arrays.stream(score).max().orElse(-1);

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

		return returnScore;

	}

	public Evaluation predict(Instances testInstances) {

		Evaluation eval = null;

		try {

			eval = new Evaluation(testInstances);
			eval.evaluateModel(classifier, testInstances);

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

		return eval;

	}

	public static void main(String[] args) {

		logger.info("start classification");
		Instant start = Instant.now();
		CommandLineParser parser = new DefaultParser();

		Options options = new Options();
		options.addOption(Option.builder("featuresPropertiesFile").longOpt("featuresPropertiesFile")
				.desc("Signature properties file").required().hasArg().argName("File Name").build());
		options.addOption(Option.builder("arffTestDirectory").longOpt("arffTestDirectory").desc("Arff Test Directory")
				.required().hasArg().argName("File Name").build());

		String arffTestDirectory = "arffTestDirectory";

		String signaturePropertiesFile = "";

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			// validate that arguments has been set
			if (line.hasOption("featuresPropertiesFile") && line.hasOption("arffTestDirectory")) {

				signaturePropertiesFile = line.getOptionValue("featuresPropertiesFile");
				arffTestDirectory = line.getOptionValue("arffTestDirectory");

			} else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Signature", options);
				System.exit(-1);

			}

		} catch (ParseException exp) {
			logger.error("Unexpected exception:", exp);
		}

		List<File> arffFiles;
		try (Stream<Path> walk = Files.walk(Paths.get(arffTestDirectory))) {
			arffFiles = walk.filter(foundPath -> foundPath.toString().endsWith(".arff")).map(Path::toFile)
					.sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());

			EngineConfiguration config = new EngineConfiguration(signaturePropertiesFile);
			Classification classification = new Classification(config);

			Map<String, Evaluation> resultats = new HashMap<>();

			for (File arffFile : arffFiles) {

				ArffLoader loader = new ArffLoader();
				loader.setSource(arffFile);
				Instances evaluation = loader.getDataSet();
				evaluation.setClassIndex(evaluation.numAttributes() - 1);

				if (evaluation.numInstances() > 0) {

					Evaluation resultat = classification.predict(evaluation);

					resultats.put(arffFile.getName(), resultat);
					logger.info(arffFile.getName());
					logger.info(resultat.toSummaryString("\nResults\n======\n", false));
					logger.info(resultat.toClassDetailsString("\n=== Detailed Accuracy By Class ===\n"));
					logger.info(resultat.toMatrixString("\nMatrice\n======\n"));
					double[] evaluateScore = classification.evaluateScore(evaluation);
					logger.info("Score: {},max score {}", evaluateScore[0], evaluateScore[1]);

				} else {
					logger.info("number of instance ==0: file {}", arffFile.getName());

				}

			}

		} catch (Exception e) {

			logger.error("Unexpected exception:", e);
		}
		
		Instant end = Instant.now();
		logger.info("classification process takes:{}",Duration.between(start, end));
		logger.info("End classification");

	}

}
