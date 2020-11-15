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
@JsonPropertyOrder({
"id",
"enName",
"frName",
"Genre",
"Espece",
"claims"
})
public class BirdRecord {

@JsonProperty("id")
private Integer id;
@JsonProperty("enName")
private String enName;
@JsonProperty("frName")
private String frName;
@JsonProperty("Genre")
private String genre;
@JsonProperty("Espece")
private String espece;
@JsonProperty("claims")
private List<Claim> claims = null;
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

@JsonProperty("id")
public Integer getId() {
return id;
}

@JsonProperty("id")
public void setId(Integer id) {
this.id = id;
}

@JsonProperty("enName")
public String getEnName() {
return enName;
}

@JsonProperty("enName")
public void setEnName(String enName) {
this.enName = enName;
}

@JsonProperty("frName")
public String getFrName() {
return frName;
}

@JsonProperty("frName")
public void setFrName(String frName) {
this.frName = frName;
}

@JsonProperty("Genre")
public String getGenre() {
return genre;
}

@JsonProperty("Genre")
public void setGenre(String genre) {
this.genre = genre;
}

@JsonProperty("Espece")
public String getEspece() {
return espece;
}

@JsonProperty("Espece")
public void setEspece(String espece) {
this.espece = espece;
}

@JsonProperty("claims")
public List<Claim> getClaims() {
return claims;
}

@JsonProperty("claims")
public void setClaims(List<Claim> claims) {
this.claims = claims;
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
