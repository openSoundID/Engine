package org.opensoundid.ml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.opensoundid.model.impl.FeaturesSpecifications;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.filters.Filter;
import weka.filters.supervised.instance.ClassBalancer;

public class TrainingOptimization {

	private static final Logger logger = LogManager.getLogger(TrainingOptimization.class);
	private static final String ENGINEPROPERTIESFILE = "enginePropertiesFile";
	private String currentFileName = "";
	enum Type {NOTUSED, USED};
	
	public void optimizeDataSet(String enginePropertiesFile)
	{
		EngineConfiguration config = new EngineConfiguration();
		FeaturesSpecifications featureSpec = new FeaturesSpecifications(config);
		
		//DATASET TRAINING
		Instances trainingDataSet = new Instances("Training", (ArrayList<Attribute>) featureSpec.getAttributes(), 0);
		trainingDataSet.setClassIndex(trainingDataSet.numAttributes() - 1);

		//DATASET TEST
		Instances testDataSet = new Instances("Test", (ArrayList<Attribute>) featureSpec.getAttributes(), 0);
		testDataSet.setClassIndex(trainingDataSet.numAttributes() - 1);
		

		
		
		String dataDirectory = config.getString("optimization.dataDirectory");
			

		try {

			RandomForest forest = new RandomForest();
			

			int[] distribution = new int[featureSpec.getNumOfClass()];
			double trainingPercent = 70;
			double[] newScore = new double[1];
			int nombreIteration = 0;

			// Initialization
			Map<Integer, Map<String, Type>> trainingFilesMap = new HashMap<>();
			Map<Integer, Map<String, Type>> testFilesMap = new HashMap<>();

			List<String> listDirectory = Files.walk(Paths.get(dataDirectory)).filter(Files::isDirectory)
					.filter(javaPath -> javaPath.toString().compareTo(dataDirectory) != 0)
					.map(javaPath -> javaPath.getFileName().toString()).collect(Collectors.toList());

			for (String directory : listDirectory) {
				List<String> arffFiles = Files.walk(Paths.get(dataDirectory + "/" + directory))
						.filter(foundPath -> foundPath.toString().endsWith(".arff"))
						.map(javaPath -> javaPath.getFileName().toString()).collect(Collectors.toList());
				Map<String, Type> arffFilesMap = new HashMap<>();

				for (String arffFile : arffFiles) {

					arffFilesMap.put(arffFile, Type.NOTUSED);

				}

				trainingFilesMap.put(Integer.valueOf(directory), arffFilesMap);

			}

			ArffLoader loader = new ArffLoader();
			
			// On calcul la repartition

			trainingFilesMap.forEach((birdID, birdIDDataSet) -> {

				birdIDDataSet.forEach((arffFileName, type) -> {

					try {

						distribution[featureSpec.findClassId(birdID)]++;

					}

					catch (Exception e) {
						
						logger.error(e.getMessage(), e);

					}

				});
			});

			for (int i = 0; i < distribution.length; i++) {
				distribution[i] = (int) Math.round((double) distribution[i] * (100-trainingPercent) / 100.0);
				nombreIteration=nombreIteration+distribution[i];

			}
			
			


			while (nombreIteration-- > 0) {


				
				//Pour chaque type oiseau
				trainingFilesMap.forEach((birdID, birdIDDataSet) -> {

					testFilesMap.putIfAbsent(birdID, new HashMap<String, Type>());
					
					System.out.print(birdID);System.out.print(":");System.out.print(testFilesMap.get(birdID).size());System.out.print(":");System.out.println(distribution[featureSpec.findClassId(birdID)]);

					// on verifie que l'on a pas atteind le max de la distribution
					if (testFilesMap.get(birdID).size() < distribution[featureSpec.findClassId(birdID)]) {

						double[] bestScore = new double[1];
						bestScore[0]=-1000000;
						newScore[0]=0;

						//Pour chaque fichier d'une classe d'oiseu
						birdIDDataSet.forEach((arffFileName, type) -> {

							if (type == Type.NOTUSED) {
								trainingDataSet.delete();
								testDataSet.delete();
								// on ajoute le fichier arrFileName dans le test

								testFilesMap.get(birdID).put(arffFileName, Type.NOTUSED);
								// on suppprime le fichier arrFileName dans le training
								trainingFilesMap.get(birdID).replace(arffFileName, Type.USED);

								// on genere le training dataset
								trainingFilesMap.forEach((birdID2, birdIDDataSet2) -> {

									birdIDDataSet2.forEach((arffFileName2, type2) -> {

										try {

											if (type2 == Type.NOTUSED) {
												File arffFile = new File(dataDirectory + "/" + Integer.toString(birdID2)
														+ "/" + arffFileName2);
												loader.setFile(arffFile);
												Instances fileDataRaw = loader.getDataSet();

												for (int i = 0; i < fileDataRaw.numInstances(); i++) {

													trainingDataSet.add(fileDataRaw.instance(i));
												}
												loader.reset();

											}
										}

										catch (Exception e) {
											
											logger.error(e.getMessage(), e);

										}

									});

								});

								testFilesMap.forEach((birdID2, birdIDDataSet2) -> {

									birdIDDataSet2.forEach((arffFileName2, type2) -> {

										try {

											if (type2 == Type.NOTUSED) {
												File arffFile = new File(dataDirectory + "/" + Integer.toString(birdID2)
														+ "/" + arffFileName2);
												loader.setFile(arffFile);
												Instances fileDataRaw = loader.getDataSet();

												for (int i = 0; i < fileDataRaw.numInstances(); i++) {

													testDataSet.add(fileDataRaw.instance(i));
												}
												loader.reset();

											}
										}

										catch (Exception e) {
											
											logger.error(e.getMessage(), e);

										}

									});

								});

								// on balance afin déquilibrer

								try {

									ClassBalancer classBalancer = new ClassBalancer();
									classBalancer.setInputFormat(trainingDataSet);

									Instances BalancedTrainingDataSet = Filter.useFilter(trainingDataSet,
											classBalancer);

									// Training

									/*forest.setNumExecutionSlots(16);
									forest.setNumIterations(200);
									forest.setBreakTiesRandomly(true); */
									forest.buildClassifier(BalancedTrainingDataSet);

/*									for (int i = 0; i < testDataSet.numInstances(); i++) {

										// Get the prediction probability distribution.
										double[] predictionDistribution = forest
												.distributionForInstance(testDataSet.instance(i));

										for (int j = 0; j < predictionDistribution.length; j++) {
											if (testDataSet.instance(i).value(testDataSet.numAttributes() - 1) == j) {
												new_score[0] = new_score[0] + predictionDistribution[j]
														* testDataSet.instance(i)
																.value(testDataSet.numAttributes() - 2);
											} else {
												new_score[0] = new_score[0] - predictionDistribution[j] * testDataSet.instance(i).value(testDataSet.numAttributes() - 2);
											}
										}

									}
*/
									
									for (int i = 0; i < testDataSet.numInstances(); i++) {

										// Get the prediction probability distribution.
										int prediction = (int)forest.classifyInstance(testDataSet.instance(i));
										double[] predictionDistribution = forest.distributionForInstance(testDataSet.instance(i));

											if (testDataSet.instance(i).value(testDataSet.numAttributes() - 1) == prediction) {
												newScore[0]=newScore[0]+predictionDistribution[prediction];
											} else {
																								
												newScore[0]=newScore[0]-predictionDistribution[prediction];
											}
										

									}
									
									/*
									double weight=0;
									//on calcule le poid du fichier
									for (int i = 0; i < testDataSet.numInstances(); i++) {

									weight=weight+testDataSet.instance(i).value(testDataSet.numAttributes() - 2);
									
									}
									
									*/
									// on compare le score à l'ancien score

									if ((newScore[0]/(double)testDataSet.numInstances()) > bestScore[0]) {
										// on sauvegarde le nom du fichier arffFilename
										bestScore[0] = newScore[0]/(double)testDataSet.numInstances();
										
										currentFileName = arffFileName;

									}

									// on supprime le fichier arrFileName dans le test
									newScore[0] = 0;

									testFilesMap.get(birdID).remove(arffFileName);
									// on suppprime le fichier arrFileName dans le training
									trainingFilesMap.get(birdID).replace(arffFileName, Type.NOTUSED);

								} catch (Exception e) {
									
									logger.error(e.getMessage(), e);
								

								}

							}

						});

						// on ajoute le fichier arrFileName dans le test

						testFilesMap.get(birdID).put(currentFileName, Type.NOTUSED);
						System.out.print(birdID);System.out.print(":");System.out.println(currentFileName);
						// on suppprime le fichier arrFileName dans le training
						trainingFilesMap.get(birdID).replace(currentFileName, Type.USED);
						

					}

				});
	
			}
			
			//resultat 
			testFilesMap.forEach((birdID, birdIDDataSet) -> {

				birdIDDataSet.forEach((arffFileName, type) -> {

					try {

						System.out.print(birdID);System.out.print(":");System.out.println(arffFileName);

					}

					catch (Exception e) {
						
						logger.error(e.getMessage(), e);

					}

				});
			});

		} catch (Exception e) {
			
			logger.error(e.getMessage(), e);

		}
	}
		
	

	public static void main(String[] args) {

		CommandLineParser parser = new DefaultParser();

		Options options = new Options();
		options.addOption(Option.builder(ENGINEPROPERTIESFILE).longOpt(ENGINEPROPERTIESFILE)
				.desc("Engine Properties File").required().hasArg().argName("File Name").build());

		String enginePropertiesFile = "";

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			// validate that arguments has been set
			if (line.hasOption(ENGINEPROPERTIESFILE)) {
				enginePropertiesFile = line.getOptionValue(ENGINEPROPERTIESFILE);

			} else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Signature", options);
				System.exit(-1);

			}
		} catch (ParseException ex) {
			logger.error(ex.getMessage(), ex);
		}
		
		
		TrainingOptimization trainingOptimization = new TrainingOptimization();
		trainingOptimization.optimizeDataSet(enginePropertiesFile);

	}
}
