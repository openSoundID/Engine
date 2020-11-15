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
"id",
"gen",
"sp",
"ssp",
"en",
"rec",
"cnt",
"loc",
"lat",
"lng",
"alt",
"type",
"url",
"file",
"file-name",
"sono",
"lic",
"q",
"length",
"time",
"date",
"uploaded",
"also",
"rmk",
"bird-seen",
"playback-used"
})
public class XenoCantoRecording {

@JsonProperty("id")
private String id;
@JsonProperty("gen")
private String gen;
@JsonProperty("sp")
private String sp;
@JsonProperty("ssp")
private String ssp;
@JsonProperty("en")
private String en;
@JsonProperty("rec")
private String rec;
@JsonProperty("cnt")
private String cnt;
@JsonProperty("loc")
private String loc;
@JsonProperty("lat")
private String lat;
@JsonProperty("lng")
private String lng;
@JsonProperty("alt")
private String alt;
@JsonProperty("type")
private String type;
@JsonProperty("url")
private String url;
@JsonProperty("file")
private String file;
@JsonProperty("file-name")
private String fileName;
@JsonProperty("sono")
private XenoCantoSono xenoCantoSono;
@JsonProperty("lic")
private String lic;
@JsonProperty("q")
private String q;
@JsonProperty("length")
private String length;
@JsonProperty("time")
private String time;
@JsonProperty("date")
private String date;
@JsonProperty("uploaded")
private String uploaded;
@JsonProperty("also")
private List<String> also = null;
@JsonProperty("rmk")
private String rmk;
@JsonProperty("bird-seen")
private String birdSeen;
@JsonProperty("playback-used")
private String playbackUsed;
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

@JsonProperty("id")
public String getId() {
return id;
}

@JsonProperty("id")
public void setId(String id) {
this.id = id;
}

@JsonProperty("gen")
public String getGen() {
return gen;
}

@JsonProperty("gen")
public void setGen(String gen) {
this.gen = gen;
}

@JsonProperty("sp")
public String getSp() {
return sp;
}

@JsonProperty("sp")
public void setSp(String sp) {
this.sp = sp;
}

@JsonProperty("ssp")
public String getSsp() {
return ssp;
}

@JsonProperty("ssp")
public void setSsp(String ssp) {
this.ssp = ssp;
}

@JsonProperty("en")
public String getEn() {
return en;
}

@JsonProperty("en")
public void setEn(String en) {
this.en = en;
}

@JsonProperty("rec")
public String getRec() {
return rec;
}

@JsonProperty("rec")
public void setRec(String rec) {
this.rec = rec;
}

@JsonProperty("cnt")
public String getCnt() {
return cnt;
}

@JsonProperty("cnt")
public void setCnt(String cnt) {
this.cnt = cnt;
}

@JsonProperty("loc")
public String getLoc() {
return loc;
}

@JsonProperty("loc")
public void setLoc(String loc) {
this.loc = loc;
}

@JsonProperty("lat")
public String getLat() {
return lat;
}

@JsonProperty("lat")
public void setLat(String lat) {
this.lat = lat;
}

@JsonProperty("lng")
public String getLng() {
return lng;
}

@JsonProperty("lng")
public void setLng(String lng) {
this.lng = lng;
}

@JsonProperty("alt")
public String getAlt() {
return alt;
}

@JsonProperty("alt")
public void setAlt(String alt) {
this.alt = alt;
}

@JsonProperty("type")
public String getType() {
return type;
}

@JsonProperty("type")
public void setType(String type) {
this.type = type;
}

@JsonProperty("url")
public String getUrl() {
return url;
}

@JsonProperty("url")
public void setUrl(String url) {
this.url = url;
}

@JsonProperty("file")
public String getFile() {
return file;
}

@JsonProperty("file")
public void setFile(String file) {
this.file = file;
}

@JsonProperty("file-name")
public String getFileName() {
return fileName;
}

@JsonProperty("file-name")
public void setFileName(String fileName) {
this.fileName = fileName;
}

@JsonProperty("sono")
public XenoCantoSono getSono() {
return xenoCantoSono;
}

@JsonProperty("sono")
public void setSono(XenoCantoSono xenoCantoSono) {
this.xenoCantoSono = xenoCantoSono;
}

@JsonProperty("lic")
public String getLic() {
return lic;
}

@JsonProperty("lic")
public void setLic(String lic) {
this.lic = lic;
}

@JsonProperty("q")
public String getQ() {
return q;
}

@JsonProperty("q")
public void setQ(String q) {
this.q = q;
}

@JsonProperty("length")
public String getLength() {
return length;
}

@JsonProperty("length")
public void setLength(String length) {
this.length = length;
}

@JsonProperty("time")
public String getTime() {
return time;
}

@JsonProperty("time")
public void setTime(String time) {
this.time = time;
}

@JsonProperty("date")
public String getDate() {
return date;
}

@JsonProperty("date")
public void setDate(String date) {
this.date = date;
}

@JsonProperty("uploaded")
public String getUploaded() {
return uploaded;
}

@JsonProperty("uploaded")
public void setUploaded(String uploaded) {
this.uploaded = uploaded;
}

@JsonProperty("also")
public List<String> getAlso() {
return also;
}

@JsonProperty("also")
public void setAlso(List<String> also) {
this.also = also;
}

@JsonProperty("rmk")
public String getRmk() {
return rmk;
}

@JsonProperty("rmk")
public void setRmk(String rmk) {
this.rmk = rmk;
}

@JsonProperty("bird-seen")
public String getBirdSeen() {
return birdSeen;
}

@JsonProperty("bird-seen")
public void setBirdSeen(String birdSeen) {
this.birdSeen = birdSeen;
}

@JsonProperty("playback-used")
public String getPlaybackUsed() {
return playbackUsed;
}

@JsonProperty("playback-used")
public void setPlaybackUsed(String playbackUsed) {
this.playbackUsed = playbackUsed;
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
