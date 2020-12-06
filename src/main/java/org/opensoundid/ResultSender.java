package org.opensoundid;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;

import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.model.impl.BirdObservation;

public class ResultSender {

	private Client client;
	private EngineConfiguration engineConfiguration = new EngineConfiguration();

	private static final Logger logger = LogManager.getLogger(ResultSender.class);

	ResultSender() {

		try {

			client = ClientBuilder.newBuilder().register(JacksonFeature.class).build();
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

	}

	void sendResult(BirdObservation birdObservation) {

		try {

			client.target(engineConfiguration.getString("engine.ResultSender.RestUrl"))
					.request(MediaType.APPLICATION_JSON)
					.post(Entity.entity(birdObservation, MediaType.APPLICATION_JSON));
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

	}

}
