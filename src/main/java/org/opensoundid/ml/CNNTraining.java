package org.opensoundid.ml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.datavec.api.split.CollectionInputSplit;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.saver.LocalFileGraphSaver;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxTimeIterationTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingGraphTrainer;
import org.deeplearning4j.earlystopping.trainer.IEarlyStoppingTrainer;
import org.deeplearning4j.nn.conf.CacheMode;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.layers.GlobalPoolingLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.deeplearning4j.zoo.PretrainedType;
import org.deeplearning4j.zoo.model.Xception;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.opensoundid.configuration.EngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Instances;

public class CNNTraining {
	private static final Logger logger = LoggerFactory.getLogger(CNNTraining.class);
	private String spectrogramsDirectory;
	private String arffTrainingFiles;
	private String arffTestFiles;
	private int randomSeed;
	private String locationToSaveModelWithUpdaterFileName;
	private String locationToSaveModelWithoutUpdaterFileName;
	private String modelDirectoryName;
	private int maxEpochsTermination;
	private int maxHour;
	private int evaluateEveryNEpochs;
	private int trainingBatchSize;
	private int testBatchSize;

	public CNNTraining() {

		try {

			EngineConfiguration engineConfiguration = new EngineConfiguration();
			spectrogramsDirectory = engineConfiguration.getString("CNNTraining.spectrogramsDirectory");
			arffTrainingFiles = engineConfiguration.getString("CNNTraining.arffTrainingFiles");
			arffTestFiles = engineConfiguration.getString("CNNTraining.arffTestFiles");
			randomSeed = engineConfiguration.getInt("CNNTraining.randomSeed");
			locationToSaveModelWithUpdaterFileName = engineConfiguration
					.getString("CNNTraining.locationToSaveModelWithUpdaterFileName");
			locationToSaveModelWithoutUpdaterFileName = engineConfiguration
					.getString("CNNTraining.locationToSaveModelWithoutUpdaterFileName");
			maxEpochsTermination = engineConfiguration
					.getInt("CNNTraining.EarlyStoppingConfiguration.maxEpochsTermination");
			evaluateEveryNEpochs = engineConfiguration
					.getInt("CNNTraining.EarlyStoppingConfiguration.evaluateEveryNEpochs");
			maxHour = engineConfiguration.getInt("CNNTraining.EarlyStoppingConfiguration.maxHour");
			trainingBatchSize = engineConfiguration.getInt("CNNTraining.trainingBatchSize");
			testBatchSize = engineConfiguration.getInt("CNNTraining.testBatchSize");
			modelDirectoryName = engineConfiguration.getString("CNNTraining.modelDirectoryName");

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}

	}

	public void training() {

		try {

			Instances trainingData = new weka.core.Instances(new FileReader(arffTrainingFiles));
			trainingData.setClassIndex(trainingData.numAttributes() - 1);

			Instances testData = new weka.core.Instances(new FileReader(arffTestFiles));
			testData.setClassIndex(testData.numAttributes() - 1);

			File locationToSaveModelWithUpdater = new File(locationToSaveModelWithUpdaterFileName);
			File locationToSaveModelWithoutUpdater = new File(locationToSaveModelWithoutUpdaterFileName);
			ComputationGraph computationGraph = locationToSaveModelWithUpdater.canRead()
					? ComputationGraph.load(locationToSaveModelWithUpdater, true)
					: getComputationGraph(trainingData.numClasses(), randomSeed);
			


			UIServer uiServer = UIServer.getInstance();

			// Configure where the network information (gradients, score vs. time etc) is to
			// be stored. Here: store in memory.
			StatsStorage statsStorage = new InMemoryStatsStorage();

			// Stratify and split the data
			Random rand = new Random(0);
			trainingData.randomize(rand);

			ImageRecordReader trainingReader = getImageRecordReader(trainingData);
			ImageRecordReader testReader = getImageRecordReader(testData);
			DataSetIterator trainingIter = new RecordReaderDataSetIterator(trainingReader, trainingBatchSize,
					1, trainingData.numClasses());
			DataSetIterator testIter = new RecordReaderDataSetIterator(testReader, testBatchSize,
					1, testData.numClasses());

			trainingIter.setPreProcessor(new ImagePreProcessingScaler(0, 1));
			testIter.setPreProcessor(new ImagePreProcessingScaler(0, 1));

			computationGraph.setListeners(new ScoreIterationListener(1), new StatsListener(statsStorage, 1));
			// Attach the StatsStorage instance to the UI: this allows the contents of the
			// StatsStorage to be visualized
			uiServer.attach(statsStorage);

			EarlyStoppingConfiguration<ComputationGraph> esConf = new EarlyStoppingConfiguration.Builder<ComputationGraph>()
					.epochTerminationConditions(new MaxEpochsTerminationCondition(maxEpochsTermination))
					.scoreCalculator(new DataSetLossCalculator(testIter, true))
					.evaluateEveryNEpochs(evaluateEveryNEpochs)
					.iterationTerminationConditions(new MaxTimeIterationTerminationCondition(maxHour, TimeUnit.HOURS))
					.modelSaver(new LocalFileGraphSaver(modelDirectoryName)).build();

			IEarlyStoppingTrainer<ComputationGraph> trainer = new EarlyStoppingGraphTrainer(esConf, computationGraph, trainingIter);

			Nd4j.getMemoryManager().togglePeriodicGc(false);
			EarlyStoppingResult<ComputationGraph> result = trainer.fit();
			Nd4j.getMemoryManager().togglePeriodicGc(true);

			// Print out the results:
			logger.info("Termination reason: {}", result.getTerminationReason());
			logger.info("Termination details: {}", result.getTerminationDetails());
			logger.info("Total epochs: {}", result.getTotalEpochs());
			logger.info("Best epoch number: {}", result.getBestModelEpoch());
			logger.info("Score at best epoch: {}", result.getBestModelScore());
			// Get the best model:
			ComputationGraph bestModel = result.getBestModel();
			bestModel.save(locationToSaveModelWithUpdater, true); // Save with updater
			bestModel.save(locationToSaveModelWithoutUpdater, false); // Save without updater

			logger.info("Best model evaluation {}", bestModel.evaluate(testIter));

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}

	}

	protected ImageRecordReader getImageRecordReader(Instances data) throws IOException {

		ArffMetaDataLabelGenerator labelGenerator = new ArffMetaDataLabelGenerator(data, spectrogramsDirectory);
		ImageRecordReader reader = new ImageRecordReader(299, 299, 3, labelGenerator);
		CollectionInputSplit cis = new CollectionInputSplit(labelGenerator.getPathURIs());
		reader.initialize(cis);

		return reader;
	}

	protected OutputLayer createOutputLayer(int numClasses) {
		return new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).nIn(2048).nOut(numClasses)
				.activation(Activation.SOFTMAX).build();
	}

	protected ComputationGraph getComputationGraph(int numClasses, int randomSeed) throws IOException {

		org.deeplearning4j.zoo.model.Xception net = Xception.builder().cacheMode(CacheMode.NONE)
				.numClasses(numClasses).build();

		FineTuneConfiguration fineTuneConf = new FineTuneConfiguration.Builder().seed(randomSeed)
				.updater(new Nesterovs(0.01, 0.9)).l2(1e-4).build();

		ComputationGraph computationGraph = (ComputationGraph) net.initPretrained(PretrainedType.IMAGENET);

		TransferLearning.GraphBuilder graphBuilder = new TransferLearning.GraphBuilder(computationGraph)
				.fineTuneConfiguration(fineTuneConf).setFeatureExtractor("predictions")
				.removeVertexKeepConnections("predictions")
				.addLayer("intermediate_pooling", new GlobalPoolingLayer.Builder().build(), "avg_pool")
				.addLayer("opensoundid_prediction", createOutputLayer(numClasses), "intermediate_pooling")
				.setOutputs("opensoundid_prediction");

		return graphBuilder.build();

	}

	public static void main(String[] args) {

		try {

			logger.info("start CNN Training");
			Instant start = Instant.now();

			new CNNTraining().training();

			Instant end = Instant.now();
			logger.info("CNN Training process takes:{}", Duration.between(start, end));
			logger.info("End CNN Training");
			System.exit(0);

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			System.exit(0);

		}
	}

}