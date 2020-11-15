package org.opensoundid.model.impl;

import java.util.ArrayList;
import java.util.List;

public class InventoryYamlFile {
	
	public InventoryYamlFile()
	{
		birdRecList= new ArrayList<>();
	}
	
	private List<InventoryYamlBirdRecList> birdRecList;

	@Override
	public String toString() {
		return "InventoryYamlFile [birdRecList=" + birdRecList + "]";
	}

	public List<InventoryYamlBirdRecList> getBirdRecList() {
		return birdRecList;
	}

	public void setBirdRecList(List<InventoryYamlBirdRecList> birdRecList) {
		this.birdRecList = birdRecList;
	}
    

}
