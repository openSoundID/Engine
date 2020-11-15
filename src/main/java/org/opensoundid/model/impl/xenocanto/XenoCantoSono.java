package org.opensoundid.model.impl.xenocanto;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"small",
"med",
"large",
"full"
})
public class XenoCantoSono {

@JsonProperty("small")
private String small;
@JsonProperty("med")
private String med;
@JsonProperty("large")
private String large;
@JsonProperty("full")
private String full;
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

@JsonProperty("small")
public String getSmall() {
return small;
}

@JsonProperty("small")
public void setSmall(String small) {
this.small = small;
}

@JsonProperty("med")
public String getMed() {
return med;
}

@JsonProperty("med")
public void setMed(String med) {
this.med = med;
}

@JsonProperty("large")
public String getLarge() {
return large;
}

@JsonProperty("large")
public void setLarge(String large) {
this.large = large;
}

@JsonProperty("full")
public String getFull() {
return full;
}

@JsonProperty("full")
public void setFull(String full) {
this.full = full;
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