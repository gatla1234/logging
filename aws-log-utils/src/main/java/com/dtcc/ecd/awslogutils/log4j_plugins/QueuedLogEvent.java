package com.dtcc.ecd.awslogutils.log4j_plugins;

public class QueuedLogEvent {
	
	private long eventTimeStamp;
	private String formattedMessage;
	
	public QueuedLogEvent(long eventTimeStamp, String formattedMessage)
	{
		this.eventTimeStamp = eventTimeStamp;
		this.formattedMessage = formattedMessage;
	}

	public long getEventTimeStamp() {
		return eventTimeStamp;
	}

	public String getFormattedMessage() {
		return formattedMessage;
	}
	
	
	

}
