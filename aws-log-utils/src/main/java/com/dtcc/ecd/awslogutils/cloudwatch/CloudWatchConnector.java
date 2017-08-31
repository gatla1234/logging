package com.dtcc.ecd.awslogutils.cloudwatch;

import java.util.Collection;
import java.util.List;

import com.amazonaws.services.logs.model.InputLogEvent;
import com.dtcc.ecd.awslogutils.exception.LogException;
import com.dtcc.ecd.awslogutils.exception.ResiliencyException;

public interface CloudWatchConnector {
	
	public String publishLogEvents(String groupName, 
								   String streamName, 
								   Collection<InputLogEvent> eventLogs,
								   String sequenceToken)
			throws LogException, ResiliencyException;
	
	
	public void createLogGroup(String logGroup)
			throws LogException, ResiliencyException;
	
	public void deleteLogGroup(String groupName)
			throws LogException, ResiliencyException;
	
	public boolean logGroupExists(String logGroup)
			throws LogException, ResiliencyException;
	
	public String createLogStream(String logGroup, String logStream)
			throws LogException, ResiliencyException;
	
	public void deleteLogStream(String logGroup, String logStream)
			throws LogException, ResiliencyException;
	
	public boolean logStreamExists(String groupName, String streamName)
			throws LogException, ResiliencyException;
	
	public String getStreamNextToken(String groupName, String streamName)
			throws LogException, ResiliencyException;
	
	public List<String> getLogGroupEvents(String groupName)
			throws LogException, ResiliencyException;
	
}
