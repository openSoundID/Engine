package org.opensoundid.configuration;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EngineConfiguration {

	private Configuration config;
	private static final Logger logger = LogManager.getLogger(EngineConfiguration.class);

	public EngineConfiguration() {

		String propertiesFileName="engine.properties";
		Parameters params = new Parameters();
		FileBasedConfigurationBuilder<FileBasedConfiguration> builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(
				PropertiesConfiguration.class).configure(params.properties().setFileName(propertiesFileName));

		try {

			config = builder.getConfiguration();

		} catch (ConfigurationException e) {

			logger.error(e.getMessage(), e);
			System.exit(1000);
		}

	}

	public String getString(String parameterName) {
		return config.getString(parameterName);

	}

	public int getInt(String parameterName) {
		return config.getInt(parameterName);

	}

	public int getInt(String parameterName, int defaultValue) {
		return config.getInt(parameterName, defaultValue);

	}

	public double getDouble(String parameterName, double defaultValue) {
		return config.getDouble(parameterName, defaultValue);

	}

	public double getDouble(String parameterName) {
		return config.getDouble(parameterName);

	}

	public String[] getStringArray(String parameterName) {
		return config.getStringArray(parameterName);

	}

}
