package org.opensoundid.noise;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.jpa.JpaUtil;
import org.opensoundid.jpa.entity.Bird;
import org.opensoundid.jpa.entity.Record;

public class NoiseRecords {

	private static final Logger logger = LogManager.getLogger(NoiseRecords.class);

	private String directory;

	NoiseRecords() {

		try {

			EngineConfiguration engineConfiguration = new EngineConfiguration();
			this.directory = engineConfiguration.getString("noiseRecords.recordsDirectory");
		}

		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}

	}

	void insertAllNoiseRecordsInDB() {
		List<File> noiseRecords;
		try (Stream<Path> walk = Files.walk(Paths.get(directory))) {
			noiseRecords = walk.filter(foundPath -> foundPath.toString().toLowerCase().endsWith(".mp3"))
					.map(Path::toFile).sorted(Comparator.comparing(File::length)).collect(Collectors.toList());

			for (File noiseRecord : noiseRecords) {

				String recordId = FilenameUtils.getBaseName(noiseRecord.getPath());
				String classId = FilenameUtils
						.getBaseName(FilenameUtils.getFullPathNoEndSeparator(noiseRecord.getPath()));
				insertNoiseRecordInDB(recordId, classId);

			}

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

	void insertNoiseRecordInDB(String recordID, String classId) {

		try (Session session = JpaUtil.getSessionFactory().openSession();) {

			session.getTransaction().begin();

			Query<Bird> queryBird = session.createNamedQuery("Bird.findById", Bird.class).setParameter("id", classId);
			Bird bird = queryBird.getSingleResult();

			Record record = new Record("NO-"+recordID, bird, "", "", "", "", "", "", "", "", "", "", "", "NO");
			session.merge(record);

			session.getTransaction().commit();

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

	public static void main(String[] args) {

		logger.info("start Insert noise records to Database");
		Instant start = Instant.now();
		
		try {
			


			NoiseRecords noiseRecords = new NoiseRecords();
			noiseRecords.insertAllNoiseRecordsInDB();

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

		Instant end = Instant.now();
		logger.info("Insert noise records to Database process takes:{}", Duration.between(start, end));
		logger.info("End Insert noise records to Database");

	}

}
