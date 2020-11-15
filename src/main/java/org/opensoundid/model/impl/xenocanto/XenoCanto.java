package org.opensoundid.model.impl.xenocanto;

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
@JsonPropertyOrder({
"numRecordings",
"numSpecies",
"page",
"numPages",
"recordings"
})
public class XenoCanto {

@JsonProperty("numRecordings")
private String numRecordings;
@JsonProperty("numSpecies")
private String numSpecies;
@JsonProperty("page")
private Integer page;
@JsonProperty("numPages")
private Integer numPages;
@JsonProperty("recordings")
private List<XenoCantoRecording> recordings = null;
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

@JsonProperty("numRecordings")
public String getNumRecordings() {
return numRecordings;
}

@JsonProperty("numRecordings")
public void setNumRecordings(String numRecordings) {
this.numRecordings = numRecordings;
}

@JsonProperty("numSpecies")
public String getNumSpecies() {
return numSpecies;
}

@JsonProperty("numSpecies")
public void setNumSpecies(String numSpecies) {
this.numSpecies = numSpecies;
}

@JsonProperty("page")
public Integer getPage() {
return page;
}

@JsonProperty("page")
public void setPage(Integer page) {
this.page = page;
}

@JsonProperty("numPages")
public Integer getNumPages() {
return numPages;
}

@JsonProperty("numPages")
public void setNumPages(Integer numPages) {
this.numPages = numPages;
}

@JsonProperty("recordings")
public List<XenoCantoRecording> getRecordings() {
return recordings;
}

@JsonProperty("recordings")
public void setRecordings(List<XenoCantoRecording> recordings) {
this.recordings = recordings;
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
