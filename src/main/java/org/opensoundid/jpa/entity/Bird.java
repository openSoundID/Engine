package org.opensoundid.jpa.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@NamedQuery(name ="Bird.findBySpeciesAndGenre",query="select b from Bird b where b.species = :species and b.genre = :genre")
@NamedQuery(name ="Bird.findById",query="select b from Bird b where b.id = :id")
@NamedQuery(name ="Bird.SelectAllBird",query="from Bird")
@Table(name = "Bird")
public class Bird {
	
	public static final int UNKNOW_BIRD_ID=99999;

	@Id
	@Column(name = "id")
	private int id;

	@Column(name = "gen")
	private String genre;

	@Column(name = "sp")
	private String species;

	@Column(name = "ssp")
	private String subspecies;

	@Column(name = "en")
	private String enName;
	
	@Column(name = "fr")
	private String frName;


	public Bird() {

	}

	public Bird(int id, String genre, String species, String subspecies,String enName,String frName) {
		this.id = id;
		this.genre = genre;
		this.species = species;
		this.subspecies = subspecies;
		this.enName = enName;
		this.frName = frName;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public String getSpecies() {
		return species;
	}

	public void setSpecies(String species) {
		this.species = species;
	}

	public String getSubspecies() {
		return subspecies;
	}

	public void setSubspecies(String subspecies) {
		this.subspecies = subspecies;
	}

	public String getEnName() {
		return enName;
	}

	public void setEnName(String enName) {
		this.enName = enName;
	}

	public String getFrName() {
		return frName;
	}

	public void setFrName(String frName) {
		this.frName = frName;
	}

	@Override
	public String toString() {
		return "Bird [id=" + id + ", genre=" + genre + ", species=" + species + ", subspecies=" + subspecies
				+ ", enName=" + enName + ", frName=" + frName + "]";
	}



}
