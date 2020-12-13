package org.opensoundid.xenocanto;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.jpa.JpaUtil;
import org.opensoundid.jpa.entity.Also;
import org.opensoundid.jpa.entity.Bird;
import org.opensoundid.jpa.entity.Record;
import org.opensoundid.model.impl.xenocanto.XenoCanto;
import org.opensoundid.model.impl.xenocanto.XenoCantoRecording;

public class XenoCantoRecord {

	private static final Logger logger = LogManager.getLogger(XenoCantoRecord.class);

	private String jsonDirectory;

	XenoCantoRecord() {

		try {

			EngineConfiguration engineConfiguration = new EngineConfiguration();
			jsonDirectory = engineConfiguration.getString("XenoCantoRecords.jsonDirectory");
		}

		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}

	}

	void insertAllJsonInDB() {
		List<File> jsonFiles;
		try (Stream<Path> walk = Files.walk(Paths.get(jsonDirectory))) {
			jsonFiles = walk.filter(foundPath -> foundPath.toString().endsWith(".json")).map(Path::toFile)
					.sorted(Comparator.comparing(File::length)).collect(Collectors.toList());

			for (File jsonFile : jsonFiles) {

				insertJsonInDB(jsonFile.getPath());

			}

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

	void insertJsonInDB(String jsonPathName) {

		try (Session session = JpaUtil.getSessionFactory().openSession();) {

			session.getTransaction().begin();
			ObjectMapper objectMapper = new ObjectMapper();
			File jsonFile = new File(jsonPathName);
			XenoCanto xenoCanto = objectMapper.readValue(jsonFile, XenoCanto.class);

			List<XenoCantoRecording> xcRecordings = xenoCanto.getRecordings();

			for (XenoCantoRecording XCRecording : xcRecordings) {

				Query<Bird> queryBird = session.createNamedQuery("Bird.findBySpeciesAndGenre", Bird.class)
						.setParameter("genre", XCRecording.getGen()).setParameter("species", XCRecording.getSp());
				Bird bird = queryBird.getSingleResult();

				Record record = new Record("XC-" + XCRecording.getId(), bird, XCRecording.getCnt(),
						XCRecording.getLoc(), XCRecording.getLat(), XCRecording.getLng(), XCRecording.getAlt(),
						XCRecording.getQ(), XCRecording.getLength(), XCRecording.getTime(), XCRecording.getDate(),
						XCRecording.getRmk(), XCRecording.getType(), "XC");
				session.merge(record);

				for (String birdGenreSpecies : XCRecording.getAlso()) {
					if (!birdGenreSpecies.isBlank()) {

						queryBird = session.createNamedQuery("Bird.findBySpeciesAndGenre", Bird.class)
								.setParameter("genre", birdGenreSpecies.split(" ")[0])
								.setParameter("species", birdGenreSpecies.split(" ")[1]);

						Bird birdAlso = queryBird.uniqueResult();

						int birdID = birdAlso != null ? birdAlso.getId() : Bird.UNKNOW_BIRD_ID;
						session.merge(new Also("XC-" + XCRecording.getId(), birdID));

					}
				}

			}

			session.getTransaction().commit();

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

	public static void main(String[] args) {

		logger.info("start Xeno-Canto Json Metadata to Database");
		Instant start = Instant.now();

		try {

			XenoCantoRecord xenoCantoRecord = new XenoCantoRecord();
			xenoCantoRecord.insertAllJsonInDB();

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

		Instant end = Instant.now();
		logger.info("Xeno-Canto Json Metadata to Database process takes:{}", Duration.between(start, end));
		logger.info("End Xeno-Canto Json Metadata to Database");

	}

}
