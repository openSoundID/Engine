package org.opensoundid.jpa.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@NamedQuery(name = "Record.findByBirdId", query = "select r from Record r where r.bird.id = :birdId")
@NamedQuery(name = "Record.findByRecordId", query = "select r from Record r where r.id = :recordId")
@Table(name = "Record")
public class Record {

	public Record(String id, Bird bird, String countries, String locality, String latitude, String longitude,
			String altitude, String quality, String length, String time, String date, String remark, String type, String origin) {
		super();
		this.id = id;
		this.bird = bird;
		this.countries = countries;
		this.locality = locality;
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.quality = quality;
		this.length = length;
		this.time = time;
		this.date = date;
		this.remark = remark;
		this.type = type;
		this.origin = origin;
	}

	@Id
	@Column(name = "id")
	private String id;

	@ManyToOne
	@JoinColumn(name = "bird_id")
	private Bird bird;

	@Column(name = "cnt")
	private String countries;

	@Column(name = "loc")
	private String locality;

	@Column(name = "lat")
	private String latitude;

	@Column(name = "lng")
	private String longitude;

	@Column(name = "alt")
	private String altitude;

	@Column(name = "q")
	private String quality;

	@Column(name = "length")
	private String length;

	@Column(name = "time")
	private String time;

	@Column(name = "date")
	private String date;

	@Column(name = "rmk")
	private String remark;
	
	@Column(name = "type")
	private String type;
	
	@Column(name = "origin")
	private String origin;


	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Record() {

	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCountries() {
		return countries;
	}

	public void setCountries(String countries) {
		this.countries = countries;
	}

	public String getLocality() {
		return locality;
	}

	public void setLocality(String locality) {
		this.locality = locality;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getAltitude() {
		return altitude;
	}

	public void setAltitude(String altitude) {
		this.altitude = altitude;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public String getLength() {
		return length;
	}

	public void setLength(String length) {
		this.length = length;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public Bird getBird() {
		return bird;
	}

	public void setBird(Bird bird) {
		this.bird = bird;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

}
