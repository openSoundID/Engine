package org.opensoundid.js;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.jpa.JpaUtil;

import org.opensoundid.jpa.entity.Bird;


public class JSUtilities {

	private static final Logger logger = LogManager.getLogger(JSUtilities.class);
	private Path jsonFileName;

	
	JSUtilities(EngineConfiguration config) {

		try {

			jsonFileName = Paths.get(config.getString("JSUtilities.jsonFileName"));

		}

		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);

		}

	}

	public void makeJSClassesDescriptionFile() {

		try (Session session = JpaUtil.getSessionFactory().openSession();) {

			ObjectMapper objectMapper = new ObjectMapper();
	
			
			Query<Bird> queryBird = session.createNamedQuery("Bird.SelectAllBird", Bird.class);

			objectMapper.writeValue(jsonFileName.toFile(), queryBird.getResultList());


		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

	public static void main(String[] args) {


		try {

			EngineConfiguration engineConfiguration = new EngineConfiguration();
			JSUtilities jsUtilities = new JSUtilities(engineConfiguration);
			jsUtilities.makeJSClassesDescriptionFile();

		} catch (Exception ex) {

			logger.error(ex.getMessage(), ex);
		}

	}

}
