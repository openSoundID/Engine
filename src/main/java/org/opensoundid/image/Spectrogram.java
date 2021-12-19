package org.opensoundid.image;

import java.io.File;
import java.io.IOException;

import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;


public class Spectrogram {
	
public static void toImage(double[][] features,int indiceMin,int indiceMax,String name,String spectrogramsImagePath) {

			// Load the image
			MBFImage image = new MBFImage( 299,299,3);

			for (int y = 1; y < image.getHeight(); y++) {
				for (int x = indiceMin; x <= indiceMax; x++) {
					
					image.getBand(0).setPixel(x, image.getHeight() - y, (float)(features[x][y]*-1.0/100.0));
					image.getBand(1).setPixel(x, image.getHeight() - y, (float)(features[x][y]*-1.0/100.0));
					image.getBand(2).setPixel(x, image.getHeight() - y, (float)(features[x][y]*-1.0/100.0));
				}
			}
			
			
			try {
				ImageUtilities.write(image,"png", new File(spectrogramsImagePath+"/"+name+".png"));
			} catch (IOException e) {

				e.printStackTrace();
			}

		}
	}


