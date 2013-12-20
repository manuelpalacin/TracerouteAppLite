package edu.upf.nets.mercury.pojo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TracerouteSession {
	
	private String sessionId;
	private List<String> tracerouteGroupIds;
	private String description;
	private String author;
	private Date dateStart;
	private Date dateEnd;
	
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	public List<String> getTracerouteGroupIds() {
		if(tracerouteGroupIds==null){
			tracerouteGroupIds = new ArrayList<String>();
		}
		return tracerouteGroupIds;
	}
	public void addTracerouteGroupId(String tracerouteGroupId) {
		getTracerouteGroupIds().add(tracerouteGroupId);
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public Date getDateStart() {
		return dateStart;
	}
	public void setDateStart(Date dateStart) {
		this.dateStart = dateStart;
	}
	public Date getDateEnd() {
		return dateEnd;
	}
	public void setDateEnd(Date dateEnd) {
		this.dateEnd = dateEnd;
	}
	
	public String toJsonString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("{");
		buffer.append("\"sessionId\":\""+sessionId+"\",");
		buffer.append("\"author\":\""+author+"\",");
		buffer.append("\"description\":\""+description+"\",");
		if(dateStart != null){
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			String myDate = formatter.format(dateStart);
			buffer.append("\"dateStart\":\""+myDate+"\"");
		}
	    buffer.append("}");
	    return buffer.toString();
	}

}
