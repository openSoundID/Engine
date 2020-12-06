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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.opensoundid.jpa.JpaUtil;
import org.opensoundid.jpa.entity.Also;
import org.opensoundid.jpa.entity.Bird;
import org.opensoundid.jpa.entity.Record;
import org.opensoundid.model.impl.xenocanto.XenoCanto;
import org.opensoundid.model.impl.xenocanto.XenoCantoRecording;

public class JsonToDB {

	private static final Logger logger = LogManager.getLogger(JsonToDB.class);

	private String jsonDirectory;


	JsonToDB(String jsonDirectory) {

		try {

			this.jsonDirectory = jsonDirectory;
		}

		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}

	}

	void insertAllJsonInDB() {
		List<File> jsonFiles;
		try(Stream<Path> walk =Files.walk(Paths.get(jsonDirectory))) {
			jsonFiles = walk.filter(foundPath -> foundPath.toString().endsWith(".json"))
					.map(Path::toFile).sorted(Comparator.comparing(File::length))
					.collect(Collectors.toList());

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

				Record record = new Record(XCRecording.getId(), bird, XCRecording.getCnt(), XCRecording.getLoc(),
						XCRecording.getLat(), XCRecording.getLng(), XCRecording.getAlt(), XCRecording.getQ(),
						XCRecording.getLength(), XCRecording.getTime(), XCRecording.getDate(), XCRecording.getRmk(),XCRecording.getType(),"XC");
				session.merge(record);
				
				for (String birdGenreSpecies : XCRecording.getAlso()) {
					if (!birdGenreSpecies.isBlank()) {

						queryBird = session.createNamedQuery("Bird.findBySpeciesAndGenre", Bird.class)
								.setParameter("genre", birdGenreSpecies.split(" ")[0])
								.setParameter("species", birdGenreSpecies.split(" ")[1]);

						Bird birdAlso = queryBird.uniqueResult();

						int birdID = birdAlso != null ? birdAlso.getId() : Bird.UNKNOW_BIRD_ID;
						session.merge(new Also(XCRecording.getId(), birdID));

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

		CommandLineParser parser = new DefaultParser();
		
		Options options = new Options();
		options.addOption(Option.builder("jsonRepository").longOpt("jsonRepository").desc("Json repository").required()
				.hasArg().argName("Directory Name").build());

		String jsonRepository = "";

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			// validate that arguments has been set
			if (line.hasOption("jsonRepository")) {

				jsonRepository = line.getOptionValue("jsonRepository");

			} else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("JsonToDb", options);
				System.exit(-1);

			}

		} catch (ParseException exp) {
			logger.error(exp.getMessage(), exp);
		}

		try {

			JsonToDB jsonToDB = new JsonToDB(jsonRepository);
			jsonToDB.insertAllJsonInDB();

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}
		
		Instant end = Instant.now();
		logger.info("Xeno-Canto Json Metadata to Database process takes:{}",Duration.between(start, end));
		logger.info("End Xeno-Canto Json Metadata to Database");

	}

}
