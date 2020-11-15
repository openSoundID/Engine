package org.opensoundid.model.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.opensoundid.configuration.EngineConfiguration;
import org.opensoundid.model.impl.birdslist.Bird;
import org.opensoundid.model.impl.birdslist.BirdsList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import weka.core.Attribute;

public class FeaturesSpecifications {

	

	private List<String> classValues = new ArrayList<>();

	private Map<Integer, String> idToBirdFrenchName = new TreeMap<>();
	private int numberOfAttributes;

	private ArrayList<Attribute> attributes;

	public FeaturesSpecifications(EngineConfiguration engineConfiguration) {

		try {

			numberOfAttributes = ((engineConfiguration.getInt("dsp.filterBankNumFilters")+1)*engineConfiguration.getInt("dsp.numFeatures"))+engineConfiguration.getInt("feature.numberOfMetadataFeatures")+1;
			attributes = new ArrayList<>(numberOfAttributes);
			
			File file = new File(engineConfiguration.getString("featuresSpecifications.yaml_data_file"));

			// Instantiating a new ObjectMapper as a YAMLFactory
			ObjectMapper om = new ObjectMapper(new YAMLFactory());

			BirdsList birdsList;
			birdsList = om.readValue(file, BirdsList.class);

			for (Bird bird : birdsList.getBirdList()) {
				classValues.add(Integer.toString(bird.getBirdRecord().getId()));
				idToBirdFrenchName.put(bird.getBirdRecord().getId(), bird.getBirdRecord().getFrName());
			}

		} catch (Exception e) {

			e.printStackTrace();
		}

		// Access the first element of the list and print it as well

		for (int i = 0; i < numberOfAttributes; i++) {
			String attributeName = "F" + Integer.toString(i);
			attributes.add(new Attribute(attributeName));
		}

		attributes.add(new Attribute("class", classValues));

	}

	public int getNumOfClass() {
		return classValues.size();
	}

	public int getNumOfAttributes() {
		return numberOfAttributes;
	}

	public List<String> getClassValues() {
		return classValues;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public int findClassId(int birdID) {
		String strBirdID = Integer.toString(birdID);

		for (int i = 0; i < classValues.size(); i++) {
			if (strBirdID.compareTo(classValues.get(i)) == 0) {
				return i;
			}
		}

		return -1;

	}

	public String findBirdName(int birdID) {

		return idToBirdFrenchName.get(birdID);

	}

	public int findBirdId(int index) {

		return Integer.parseInt(classValues.get(index));

	}



}
