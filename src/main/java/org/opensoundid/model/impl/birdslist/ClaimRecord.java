package org.opensoundid.model.impl.birdslist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "q", "cnt" })
public class ClaimRecord {

	@JsonProperty("q")
	private String q;
	@JsonProperty("cnt")
	private List<String> cnt = null;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	@JsonProperty("q")
	public String getQ() {
		return q;
	}

	@JsonProperty("q")
	public void setQ(String q) {
		this.q = q;
	}

	@JsonProperty("cnt")
	public List<String> getCnt() {
		return cnt;
	}

	@JsonProperty("cnt")
	public void setCnt(List<String> cnt) {
		this.cnt = cnt;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}

}
