package org.opensoundid.ml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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

	private static final Logger logger = LogManager.getLogger(Features.class);

	private double principalComponentsVarianceCovered = 1.0;
	private Classifier classifier;
	private RotationForest rotationForest = new RotationForest();
	private String trainingDirectory;
	private String modelFileName;

	public Classification(EngineConfiguration engineConfiguration) {

		try {

			trainingDirectory = engineConfiguration.getString("classification.trainingDirectory");
			modelFileName = engineConfiguration.getString("classification.modelFileName");
			principalComponentsVarianceCovered = engineConfiguration
					.getDouble("classification.principalComponentsVarianceCovered", principalComponentsVarianceCovered);
			
			if (Files.exists(Paths.get(modelFileName))) {
				classifier = (Classifier) weka.core.SerializationHelper.read(modelFileName);
			} else {

				ArffLoader loader = new ArffLoader();
				loader.setSource(new File(trainingDirectory + "/training.arff"));

				Instances trainingDataSet = loader.getDataSet();
				trainingDataSet.setClassIndex(trainingDataSet.numAttributes() - 1);

				
				rotationForest.setNumExecutionSlots(16);
				rotationForest.setMaxGroup(3);
				rotationForest.setMinGroup(3);
				rotationForest.setNumIterations(100);
				J48 j48 = new J48();
				rotationForest.setClassifier(j48);
				PrincipalComponents principalComponents = new PrincipalComponents();
				principalComponents.setVarianceCovered(1.0);
				principalComponents.setMaximumAttributeNames(-1);
				rotationForest.setProjectionFilter(principalComponents);

				rotationForest.buildClassifier(trainingDataSet);
				
				classifier= rotationForest;

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
			System.out.printf("There are %d test instances\n", numTestInstances);

			// Loop over each test instance.
			for (int i = 0; i < numTestInstances; i++) {

				// Get the prediction probability distribution.
				double[] predictionDistribution = classifier.distributionForInstance(testInstances.instance(i));

				for (int j = 0; j < predictionDistribution.length; j++) {
					resultat[i][j] = predictionDistribution[j]
							* testInstances.instance(i).value(testInstances.numAttributes() - 2);
				}

			}

			System.out.printf("\n");

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
			System.out.printf("There are %d test instances\n", numTestInstances);

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

			System.out.printf("\n");

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
			System.out.println("Unexpected exception:" + exp.getMessage());
		}

		try {

			EngineConfiguration config = new EngineConfiguration(signaturePropertiesFile);
			Classification classification = new Classification(config);
			int correct = 0;
			int incorrect = 0;
			Map<String, Evaluation> resultats = new HashMap<>();

			List<File> arffFiles = Files.list(Paths.get(arffTestDirectory))
					.filter(foundPath -> foundPath.toString().endsWith(".arff")).map(javaPath -> javaPath.toFile())
					.sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());

			for (File arffFile : arffFiles) {

				ArffLoader loader = new ArffLoader();
				loader.setSource(arffFile);
				Instances evaluation = loader.getDataSet();
				evaluation.setClassIndex(evaluation.numAttributes() - 1);

				if (evaluation.numInstances() > 0) {

					Evaluation resultat = classification.predict(evaluation);

					resultats.put(arffFile.getName(), resultat);
					System.out.println(arffFile.getName());
					System.out.println(resultat.toSummaryString("\nResults\n======\n", false));
					System.out.println(resultat.toClassDetailsString("\n=== Detailed Accuracy By Class ===\n"));
					System.out.println(resultat.toMatrixString("\nMatrice\n======\n"));
					double[] evaluateScore = classification.evaluateScore(evaluation);
					System.out.printf("Score: %f,max score %f\n", evaluateScore[0], evaluateScore[1]);
					if (evaluateScore[0] == evaluateScore[1]) {
						correct += 1;
					} else {
						incorrect += 1;
					}
				} else {
					System.out.print("number of instance ==0");
					System.out.println(arffFile.getName());
				}

			}

			resultats.entrySet().stream()
					.forEach(e -> System.out.printf("%s:%f:%f\n", e.getKey(),
							100.0 * e.getValue().correct() / (e.getValue().correct() + e.getValue().incorrect()),
							e.getValue().correct() + e.getValue().incorrect()));

			System.out.printf("Correctly classified:%d\n", correct);
			System.out.printf("Incorrectly classified:%d\n", incorrect);
			System.out.printf("total classified:%d\n", correct + incorrect);
			System.out.printf("Pct Correctly classified:%f\n", (correct * 1.0 / (correct + incorrect)) * 100.0);

		} catch (Exception e) {

			e.printStackTrace();
		}

	}

}
