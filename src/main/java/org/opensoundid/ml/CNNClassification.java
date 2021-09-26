package org.opensoundid.ml;

import java.io.FileReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

public class CNNClassification {
	private static final Logger logger = LoggerFactory.getLogger(CNNClassification.class);

	public static void main(String[] args) {

		try {
			
			logger.info("start classification");
			Instant start = Instant.now();

//Load all packages so that Dl4jMlpClassifier class can be found using forName("weka.filters.unsupervised.attribute.Dl4jMlpClassifier")
			weka.core.WekaPackageManager.loadPackages(true);

//Load the dataset
			weka.core.Instances data = new weka.core.Instances(new FileReader("/home/opensoundid/dataset/results/training.arff"));
			data.setClassIndex(data.numAttributes() - 1);
			String[] classifierOptions = weka.core.Utils.splitOptions("-S 1 -cache-mode FILESYSTEM -early-stopping \"weka.dl4j.earlystopping.EarlyStopping -maxEpochsNoImprovement 0 -valPercentage 0.0\" -normalization \"Standardize training data\" -iterator \"weka.dl4j.iterators.instance.ImageInstanceIterator -channelsLast false -height 299 -imagesLocation /home/opensoundid/Spectograms -numChannels 3 -width 299 -bs 16\" -iteration-listener \"weka.dl4j.listener.EpochListener -eval true -n 1\" -layer \"weka.dl4j.layers.OutputLayer -lossFn \\\"weka.dl4j.lossfunctions.LossMCXENT \\\" -nOut 2 -activation \\\"weka.dl4j.activations.ActivationSoftmax \\\" -name \\\"Output layer\\\"\" -logConfig \"weka.core.LogConfiguration -append true -dl4jLogLevel WARN -logFile /home/opensoundid/wekafiles/wekaDeeplearning4j.log -nd4jLogLevel INFO -wekaDl4jLogLevel INFO\" -config \"weka.dl4j.NeuralNetConfiguration -biasInit 0.0 -biasUpdater \\\"weka.dl4j.updater.Sgd -lr 0.001 -lrSchedule \\\\\\\"weka.dl4j.schedules.ConstantSchedule -scheduleType EPOCH\\\\\\\"\\\" -dist \\\"weka.dl4j.distribution.Disabled \\\" -dropout \\\"weka.dl4j.dropout.Disabled \\\" -gradientNormalization None -gradNormThreshold 1.0 -l1 NaN -l2 NaN -minimize -algorithm STOCHASTIC_GRADIENT_DESCENT -updater \\\"weka.dl4j.updater.Adam -beta1MeanDecay 0.9 -beta2VarDecay 0.999 -epsilon 1.0E-8 -lr 0.001 -lrSchedule \\\\\\\"weka.dl4j.schedules.ConstantSchedule -scheduleType EPOCH\\\\\\\"\\\" -weightInit XAVIER -weightNoise \\\"weka.dl4j.weightnoise.Disabled \\\"\" -numEpochs 10 -numGPUs 1 -averagingFrequency 10 -prefetchSize 24 -queueSize 0 -zooModel \"weka.dl4j.zoo.Dl4jXception -channelsLast false -pretrained IMAGENET\"");
			weka.classifiers.AbstractClassifier myClassifier = (AbstractClassifier) weka.core.Utils.forName(
					weka.classifiers.AbstractClassifier.class, "weka.classifiers.functions.Dl4jMlpClassifier",
					classifierOptions);

			// Stratify and split the data
			Random rand = new Random(0);
			data.randomize(rand);
						
			int trainSize = (int) Math.round(data.numInstances() * (100.0 - 5.0) / 100.0);
			int testSize = data.numInstances() - trainSize;

			Instances train = new Instances(data, 0, trainSize);
			Instances test = new Instances(data, trainSize, testSize);

//Build the classifier on the training data
			myClassifier.buildClassifier(train);

//Evaluate the model on test data
			Evaluation eval = new Evaluation(test);
			eval.evaluateModel(myClassifier, test);

//Output some summary statistics
			System.out.println(eval.toSummaryString());
			System.out.println(eval.toMatrixString());
			
			Instant end = Instant.now();
			logger.info("classification process takes:{}",Duration.between(start, end));
			logger.info("End classification");
			
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}
	}
}