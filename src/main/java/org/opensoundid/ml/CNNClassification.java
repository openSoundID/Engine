package org.opensoundid.ml;

import java.io.File;
import java.io.IOException;
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
import org.datavec.api.split.CollectionInputSplit;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.opensoundid.configuration.EngineConfiguration;

import weka.core.Instances;

import weka.core.converters.ArffLoader;

public class CNNClassification {

	private static final Logger logger = LogManager.getLogger(CNNClassification.class);

	private ComputationGraph computationGraph;

	private String modelFileName;
	private String spectrogramsDirectory;
	private int batchSize;

	public CNNClassification() {

		try {

			EngineConfiguration engineConfiguration = new EngineConfiguration();
			modelFileName = engineConfiguration.getString("CNNClassification.modelFileName");
			batchSize = engineConfiguration.getInt("CNNClassification.batchSize");
			spectrogramsDirectory = engineConfiguration.getString("CNNClassification.spectrogramsDirectory");

			File modelLocation = new File(modelFileName);
			logger.info("Loading computationGraph model: {}", modelFileName);
			computationGraph = ComputationGraph.load(modelLocation, false);

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}

	}

	protected ImageRecordReader getImageRecordReader(Instances data) throws IOException {

		ArffMetaDataLabelGenerator labelGenerator = new ArffMetaDataLabelGenerator(data,
				spectrogramsDirectory);
		ImageRecordReader reader = new ImageRecordReader(299, 299, 3, labelGenerator);
		CollectionInputSplit cis = new CollectionInputSplit(labelGenerator.getPathURIs());
		reader.initialize(cis);

		return reader;
	}

	public double[][] distributionsForInstances(Instances instances) throws Exception {

		// Get predictions

		ImageRecordReader imageRecordReader = getImageRecordReader(instances);

		DataSetIterator dataSetIterator = new RecordReaderDataSetIterator(imageRecordReader, batchSize, 1,
				instances.numClasses());

		DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
		scaler.fit(dataSetIterator);

		dataSetIterator.setPreProcessor(scaler);

		double[][] predictions = new double[instances.numInstances()][instances.numClasses()];

		int offset = 0;
		boolean next = dataSetIterator.hasNext();

		// Get predictions batch-wise
		while (next) {
			INDArray predBatch;
			predBatch = computationGraph.outputSingle(dataSetIterator.next().getFeatures());

			int currentBatchSize = (int) predBatch.shape()[0];

			// Build weka distribution output
			for (int i = 0; i < currentBatchSize; i++) {
				for (int j = 0; j < instances.numClasses(); j++) {
					predictions[i + offset][j] = predBatch.getDouble(i, j);
				}
			}
			offset += currentBatchSize; // add batchsize as offset
			boolean hasInstancesLeft = offset < instances.numInstances();
			next = dataSetIterator.hasNext() || hasInstancesLeft;
		}

		// Normalize prediction
		for (int i = 0; i < predictions.length; i++) {
			// only normalise if we're dealing with classification
			weka.core.Utils.normalize(predictions[i]);
		}
		return predictions;
	}

	public double[][] evaluate(Instances instances) {

		double[][] resultat = null;

		try {

			int numTestInstances = instances.numInstances();
			logger.info("There are {} instances to evaluate", numTestInstances);

			resultat = distributionsForInstances(instances);

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

		return resultat;

	}

	public double[] evaluateScore(Instances instances) {

		double[] resultat = new double[instances.numClasses()];
		double[] score = new double[instances.numClasses()];
		double[] returnScore = new double[2];

		try {

			int numInstances = instances.numInstances();
			logger.info("There are {} test instances", numInstances);

			double[][] predictionDistribution = distributionsForInstances(instances);

			// Loop over each test instance.
			for (int i = 0; i < numInstances; i++) {

				// Get the prediction probability distribution.

				for (int j = 0; j < instances.numClasses(); j++) {

					resultat[j] = resultat[j] + predictionDistribution[i][j];

				}

			}

			double total = Arrays.stream(resultat).sum();
			for (int i = 0; i < score.length; i++) {
				score[i] = resultat[i] / total;
			}
			returnScore[0] = score[(int) instances.instance(0).classValue()];
			returnScore[1] = Arrays.stream(score).max().orElse(-1);

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

		return returnScore;

	}

	public Evaluation predict(Instances instances) {

		Evaluation eval = null;

		try {

			ImageRecordReader reader = getImageRecordReader(instances);
			DataSetIterator iterator = new RecordReaderDataSetIterator(reader, batchSize, 1, instances.numClasses());

			DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
			scaler.fit(iterator);
			iterator.setPreProcessor(scaler);

			eval = computationGraph.evaluate(iterator);

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

		return eval;

	}

	public static void main(String[] args) {

		logger.info("start CNN classification");
		Instant start = Instant.now();
		CommandLineParser parser = new DefaultParser();

		Options options = new Options();
		options.addOption(Option.builder("arffTestDirectory").longOpt("arffTestDirectory").desc("Arff Test Directory")
				.required().hasArg().argName("File Name").build());

		String arffTestDirectory = "arffTestDirectory";

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			// validate that arguments has been set
			if (line.hasOption("arffTestDirectory")) {

				arffTestDirectory = line.getOptionValue("arffTestDirectory");

			} else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("CNN classification", options);
				System.exit(-1);

			}

		} catch (ParseException exp) {
			logger.error("Unexpected exception:", exp);
		}

		List<File> arffFiles;
		try (Stream<Path> walk = Files.walk(Paths.get(arffTestDirectory))) {
			arffFiles = walk.filter(foundPath -> foundPath.toString().endsWith(".arff")).map(Path::toFile)
					.sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());

			CNNClassification classification = new CNNClassification();

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
					logger.info(resultat.stats());

				} else {
					logger.info("number of instance ==0: file {}", arffFile.getName());

				}

			}

		} catch (Exception e) {

			logger.error("Unexpected exception:", e);
		}

		Instant end = Instant.now();
		logger.info("classification process takes:{}", Duration.between(start, end));
		logger.info("End CNN classification");

	}

}
