package com.dtcc.ecd.awslogutils.cloudwatch;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.dtcc.ecd.awslogutils.exception.LogException;
import com.dtcc.ecd.awslogutils.exception.ResiliencyException;
import com.google.common.util.concurrent.RateLimiter;

public class Stream
{
	private String groupName;
	private String streamName;
	private String sequenceToken;
	
	// Used to automatically throttle calls to cloudWatch
	private RateLimiter cwAPIThrottle;
	
	public Stream(String groupName, String streamName, String nextToken)
	{
		this.groupName = groupName;
		this.streamName = streamName;
		this.sequenceToken = nextToken;
		
		cwAPIThrottle = RateLimiter.create(5.0);
	}
	
	public void updateSequenceToken(String nextToken)
	{
		this.sequenceToken = nextToken;
	}
	
	
	
	public void publishEventBatch(CloudWatchConnector cwClient, Collection<InputLogEvent> eventBatch)
			throws LogException, ResiliencyException
	{
		cwAPIThrottle.acquire(1);
		
		String nextToken = cwClient.publishLogEvents(groupName, streamName, eventBatch, sequenceToken);
		updateSequenceToken(nextToken);			
	}

		
	public synchronized void publishEvent(CloudWatchConnector cwClient, String eventText)
		throws LogException, ResiliencyException
	{
		Collection<InputLogEvent> eventLogs = new ArrayList<InputLogEvent>();
		InputLogEvent logEvent = new InputLogEvent();
		logEvent.setMessage(eventText);
		logEvent.setTimestamp(new Date().getTime());
		
		cwAPIThrottle.acquire(1);
				
		eventLogs.add(logEvent);
		
		String nextToken = cwClient.publishLogEvents(groupName, streamName, eventLogs, sequenceToken);
		updateSequenceToken(nextToken);			
	
	}
	
}
