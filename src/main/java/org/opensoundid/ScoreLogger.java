package org.opensoundid;

import java.io.FileWriter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensoundid.configuration.EngineConfiguration;

public class ScoreLogger {

	private static final Logger logger = LogManager.getLogger(ScoreLogger.class);
	private String savePath;
	private CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader("filename","timestamp", "BirdId", "BirdName", "Score")
			.withFirstRecordAsHeader();
	private EngineConfiguration engineConfiguration = new EngineConfiguration();

	ScoreLogger() {

		savePath = engineConfiguration.getString("scoreLogger.savePath");

	}

	public void logScore(String fileName,String timestamp, Integer birdID, String birdName, Long score) {

		try (FileWriter fileWriter = new FileWriter(savePath, true);
				CSVPrinter csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);) {

			csvFilePrinter.printRecord(fileName,timestamp, birdID, birdName, score);

			fileWriter.flush();

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);

		}

	}

}
