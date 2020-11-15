package org.opensoundid.jpa.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@IdClass(Also.class)
@NamedQuery(name = "Also.findByBirdId", query = "select a from Also a where a.birdId = :birdId")
@NamedQuery(name = "Also.findByRecordId", query = "select a from Also a where a.recordId = :recordId")
@Table(name = "Also")
public class Also implements Serializable {
	public Also(String recordId, int birdId) {
		super();
		this.recordId = recordId;
		this.birdId = birdId;
	}

	public Also() {
	}

	static final long serialVersionUID = 20200720;

	@Id
	@Column(name = "record_id")
	private String recordId;

	@Id
	@Column(name = "bird_id")
	private int birdId;

	public String getRecordId() {
		return recordId;
	}

	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	public int getBirdId() {
		return birdId;
	}

	public void setBirdId(int birdId) {
		this.birdId = birdId;
	}

	@Override
	public String toString() {
		return "Also [recordId=" + recordId + ", birdId=" + birdId + "]";
	}
}