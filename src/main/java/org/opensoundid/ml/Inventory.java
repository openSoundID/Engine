package org.opensoundid.ml;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.jpa.JpaUtil;
import org.opensoundid.jpa.entity.Also;
import org.opensoundid.jpa.entity.Bird;
import org.opensoundid.jpa.entity.Record;
import org.opensoundid.model.impl.InventoryYamlBirdRecList;
import org.opensoundid.model.impl.InventoryYamlFile;

public class Inventory {

	private static final Logger logger = LogManager.getLogger(Inventory.class);
	private Path inventoryFile;
	private Path testInventoryFile;
	private String[] excludedKeywords;

	Inventory(EngineConfiguration config) {

		try {

			inventoryFile = Paths.get(config.getString("inventory.trainingInventoryFile"));
			testInventoryFile = Paths.get(config.getString("inventory.testInventoryFile"));
			excludedKeywords = config.getStringArray("inventory.excludedKeyword");

		}

		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}

	}

	public void makeInventoryFile() {

		try (Session session = JpaUtil.getSessionFactory().openSession();) {

			ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER));
			File yamlFile = inventoryFile.toFile();
			File alsoYamlFile = testInventoryFile.toFile();

			Query<Bird> queryBird = session.createNamedQuery("Bird.SelectAllBird", Bird.class);

			List<Bird> birdList = queryBird.getResultList();
			InventoryYamlFile inventoryYamlFile = new InventoryYamlFile();
			InventoryYamlFile testInventoryYamlFile = new InventoryYamlFile();

			for (Bird bird : birdList) {
				Query<Record> queryRecords = session.createNamedQuery("Record.findByBirdId", Record.class)
						.setParameter("birdId", bird.getId());
				List<Record> recordList = queryRecords.getResultList();

				InventoryYamlBirdRecList birdRecList = new InventoryYamlBirdRecList();
				InventoryYamlBirdRecList birdAlsoRecList = new InventoryYamlBirdRecList();
				birdRecList.setBirdID(bird.getId());
				birdAlsoRecList.setBirdID(bird.getId());

				for (Record record : recordList) {

					Query<Also> queryAlso = session.createNamedQuery("Also.findByRecordId", Also.class)
							.setParameter("recordId", record.getId());

					if (queryAlso.getResultList().isEmpty() && !isRecordExcluded(record)) {
						birdRecList.getRecID().add(record.getId());
					}
					if (!queryAlso.getResultList().isEmpty() && !isRecordExcluded(record)) {
						birdAlsoRecList.getRecID().add(record.getId());
					}

				}

				inventoryYamlFile.getBirdRecList().add(birdRecList);
				testInventoryYamlFile.getBirdRecList().add(birdAlsoRecList);
			}

			objectMapper.writeValue(yamlFile, inventoryYamlFile);
			objectMapper.writeValue(alsoYamlFile, testInventoryYamlFile);

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

	private boolean isRecordExcluded(Record record) {

		for (String excludedKeyword : excludedKeywords) {
			if (record.getType().toLowerCase().contains(excludedKeyword.toLowerCase()))
				return true;
		}

		return false;
	}

	public static void main(String[] args) {


		try {

			EngineConfiguration engineConfiguration = new EngineConfiguration();
			Inventory inventory = new Inventory(engineConfiguration);
			inventory.makeInventoryFile();

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

}
