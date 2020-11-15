package org.opensoundid.model.impl;

import java.util.ArrayList;
import java.util.List;

public class InventoryYamlBirdRecList {
	
	public InventoryYamlBirdRecList()
	{
		recID = new ArrayList<>();
	}
	
	private int birdID;
	private List<String> recID;
 
	@Override
	public String toString() {
		return "InventoryYamlBirdRecList [birdID=" + birdID + ", recID=" + recID + "]";
	}
	public List<String> getRecID() {
		return recID;
	}
	public void setRecID(List<String> recID) {
		this.recID = recID;
	}
	public int getBirdID() {
		return birdID;
	}
	public void setBirdID(int birdID) {
		this.birdID = birdID;
	}
	


}
