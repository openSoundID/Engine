package org.opensoundid.model.impl;


public class JsonLowLevelFeatures {
	  Metadata MetadataObject;
	  Description DescriptionObject;
	  Lowlevel LowlevelObject;


	 // Getter Methods 

	  public Metadata getMetadata() {
	    return MetadataObject;
	  }
	  
	  public Description getDescription() {
		    return DescriptionObject;
		  }


	  public Lowlevel getLowlevel() {
	    return LowlevelObject;
	  }

	 // Setter Methods 

	  public void setMetadata( Metadata metadataObject ) {
	    this.MetadataObject = metadataObject;
	  }
	  
	  public void setDescription( Description descriptionObject ) {
		    this.DescriptionObject = descriptionObject;
		  }


	  public void setLowlevel( Lowlevel lowlevelObject ) {
	    this.LowlevelObject = lowlevelObject;
	  }
	}

