package com.dtcc.ecd.awslogutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.CreateLogGroupRequest;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.DeleteLogGroupRequest;
import com.amazonaws.services.logs.model.DeleteLogStreamRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsResult;
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsResult;
import com.amazonaws.services.logs.model.FilterLogEventsRequest;
import com.amazonaws.services.logs.model.FilterLogEventsResult;
import com.amazonaws.services.logs.model.FilteredLogEvent;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.LogGroup;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.amazonaws.services.logs.model.ResourceAlreadyExistsException;
import com.amazonaws.services.logs.model.ServiceUnavailableException;
import com.amazonaws.services.logs.model.ResourceNotFoundException;
import com.dtcc.ecd.awslogutils.cloudwatch.CloudWatchConnector;
import com.dtcc.ecd.awslogutils.exception.LogException;
import com.dtcc.ecd.awslogutils.exception.ResiliencyException;

public class DefaultConnector implements CloudWatchConnector
{	
	private AWSLogs awsLogs;
		
	public DefaultConnector(ClientConfiguration clientConfig)
	{	
		awsLogs = AWSLogsClientBuilder.defaultClient();		
	}
	
	public DefaultConnector(AWSCredentialsProvider credProvider, ClientConfiguration clientConfig)
	{		
		this(credProvider.getCredentials(), clientConfig);		
	}
	
	public DefaultConnector(AWSCredentials creds, ClientConfiguration clientConfig)
	{		
		awsLogs = null;		
		
		if (clientConfig != null)
		{
			awsLogs = AWSLogsClientBuilder.standard().withCredentials(
					new AWSStaticCredentialsProvider(creds))
						.withClientConfiguration(clientConfig).build();
		}
		else
			awsLogs = AWSLogsClientBuilder.standard().withCredentials(
					new AWSStaticCredentialsProvider(creds)).build();
	}
	
	
	@Override
	public String publishLogEvents(String groupName,
								   String streamName,
								   Collection<InputLogEvent> eventLogs,
								   String sequenceToken)
			throws LogException, ResiliencyException
	{

		PutLogEventsRequest logEvtRequest = new PutLogEventsRequest();
		
		logEvtRequest.setLogGroupName(groupName);
		logEvtRequest.setLogStreamName(streamName);
		logEvtRequest.setSequenceToken(sequenceToken);
		
		logEvtRequest.setLogEvents(eventLogs);
		
		try
		{
			PutLogEventsResult results = awsLogs.putLogEvents(logEvtRequest);		
			
			return results.getNextSequenceToken();
		}
		catch(ServiceUnavailableException sue)
		{
			throw new ResiliencyException(sue);
		}
		catch(ResourceNotFoundException rnfe)
		{
			// if the stream or group was deleted externally, then attempt to fix it
			// rather than fail right away. Only do this once. if subsequent exceptions
			// are throws then fail
			
			createLogGroup(groupName);
			createLogStream(groupName, streamName);
			
			logEvtRequest.setSequenceToken(null);
			
			try
			{
				PutLogEventsResult results = awsLogs.putLogEvents(logEvtRequest);			
				return results.getNextSequenceToken();
			}
			catch(Throwable t)
			{
				throw new LogException(t);
			}
			
		}
		catch(Throwable t)
		{
			throw new LogException(t);
		}
	}

	@Override
	public void createLogGroup(String logGroup)
			throws LogException, ResiliencyException
	{
		CreateLogGroupRequest logGroupRequest = new CreateLogGroupRequest();
		
		logGroupRequest.setLogGroupName(logGroup);
		
		try
		{
			awsLogs.createLogGroup(logGroupRequest);
		}		
		catch(ResourceAlreadyExistsException exc)
		{
			return;
		}
		catch(ServiceUnavailableException sue)
		{
			throw new ResiliencyException(sue);
		}
		catch(Throwable t)
		{
			throw new LogException(t);
		}
	}
	
	@Override
	public void deleteLogGroup(String groupName) throws LogException,
			ResiliencyException {
		
		DeleteLogGroupRequest req = new DeleteLogGroupRequest();
		
		req.setLogGroupName(groupName);
		
		try
		{
			awsLogs.deleteLogGroup(req);			
		}
		catch(ServiceUnavailableException sue)
		{
			throw new ResiliencyException(sue);
		}
		catch(ResourceNotFoundException rnfe)
		{
			return;
		}
		catch(Throwable t)
		{
			throw new LogException(t);
		}			
	}
	
	@Override
	public boolean logGroupExists(String logGroup) throws LogException,
			ResiliencyException {
		
		DescribeLogGroupsResult result = null;
		
		try
		{
			result = awsLogs.describeLogGroups();
		}
		catch(ServiceUnavailableException sue)
		{
			throw new ResiliencyException(sue);
		}
		catch(Throwable t)
		{
			throw new LogException(t);
		}
		 
		for (LogGroup nextGroup : result.getLogGroups())
			if (nextGroup.getLogGroupName().equals(logGroup))
				return true;
		
		return false;
	}

	@Override
	public String createLogStream(String logGroup, String logStream)
			throws LogException, ResiliencyException
	{
		CreateLogStreamRequest logStreamRequest = new CreateLogStreamRequest();
		
		logStreamRequest.setLogGroupName(logGroup);
		logStreamRequest.setLogStreamName(logStream);
		
		try
		{
			awsLogs.createLogStream(logStreamRequest);
			return null;
		}
		catch(ResourceNotFoundException rnfe)
		{
			// this would indicate that the log group does not yet exist, so create it
			createLogGroup(logGroup);
			
			awsLogs.createLogStream(logStreamRequest);
			return null;
		}
		catch(ResourceAlreadyExistsException exc)
		{
			return getStreamNextToken(logGroup, logStream);
		}
		catch(ServiceUnavailableException sue)
		{
			throw new ResiliencyException(sue);
		}
		catch(Throwable t)
		{
			throw new LogException(t);
		}
	}
	
	@Override
	public void deleteLogStream(String logGroup, String logStream)
			throws LogException, ResiliencyException {
		
		DeleteLogStreamRequest req = new DeleteLogStreamRequest();
				
		req.setLogGroupName(logGroup);
		req.setLogStreamName(logStream);
		
		try
		{
			awsLogs.deleteLogStream(req);			
		}
		catch(ServiceUnavailableException sue)
		{
			throw new ResiliencyException(sue);
		}
		catch(ResourceNotFoundException rnfe)
		{
			return;
		}
		catch(Throwable t)
		{
			throw new LogException(t);
		}		
	}
	
	@Override
	public boolean logStreamExists(String groupName, String streamName)
			throws LogException, ResiliencyException {
		DescribeLogStreamsRequest req = new DescribeLogStreamsRequest();
		req.setLogGroupName(groupName);
		req.setLogStreamNamePrefix(streamName);
		
		try
		{
			DescribeLogStreamsResult results = awsLogs.describeLogStreams(req);

			if (results.getLogStreams().size() == 1)
				return true;
			else
				return false;
		}
		catch(ServiceUnavailableException sue)
		{
			throw new ResiliencyException(sue);
		}
		catch(Throwable t)
		{
			throw new LogException(t);
		}
	}

	@Override
	public String getStreamNextToken(String groupName, String streamName) 
			throws LogException, ResiliencyException
	{
		DescribeLogStreamsRequest req = new DescribeLogStreamsRequest();
		req.setLogGroupName(groupName);
		req.setLogStreamNamePrefix(streamName);
		
		try
		{
			DescribeLogStreamsResult results = awsLogs.describeLogStreams(req);

			return results.getLogStreams().get(0).getUploadSequenceToken();
		}
		catch(ServiceUnavailableException sue)
		{
			throw new ResiliencyException(sue);
		}
		catch(Throwable t)
		{
			throw new LogException(t);
		}	
	}
	
	@Override
	public List<String> getLogGroupEvents(String groupName)
			throws LogException, ResiliencyException
	{		
		List<String> eventList = new ArrayList<String>();
		
		String nextToken = "";
		boolean done = false;
		
		FilterLogEventsRequest req = null;
		FilterLogEventsResult result = null;
		
		// to get all events this has to be done multiple time and the events need to be chained
		while (!done)
		{	
			
			if (nextToken.isEmpty())				
				req = new FilterLogEventsRequest();
			else
				req = new FilterLogEventsRequest().withNextToken(nextToken);
			
			
			req.setLogGroupName(groupName);			
			
			try{
				result = awsLogs.filterLogEvents(req);
			}
			catch(Throwable t){
				throw new LogException(t);
			}
			
			List<FilteredLogEvent> events = result.getEvents();
			
			
			for (FilteredLogEvent event : events)
					eventList.add(event.getMessage());
			
			nextToken = result.getNextToken();
			
			if (nextToken == null)
				done = true;
		}
		
		return eventList;
	}	
	

}
