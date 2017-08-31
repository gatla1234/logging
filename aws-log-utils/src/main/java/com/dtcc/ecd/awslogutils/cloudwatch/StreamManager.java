package com.dtcc.ecd.awslogutils.cloudwatch;

import java.util.HashMap;
import java.util.Map;

import com.dtcc.ecd.awslogutils.exception.LogException;
import com.dtcc.ecd.awslogutils.exception.ResiliencyException;

public class StreamManager {
	
	private Map<String, Stream> streamMap;
	private final String KEY_DELIMITER = "%%@!!@%%";
	
	public StreamManager()
	{
		streamMap = new HashMap<String, Stream>();		
	}

	public StreamManager(Map<String, Stream> testMap)
	{
		streamMap = testMap;	
	}
	
	public void removeStreams()
	{
		streamMap.clear();
	}
	
	private String getKeyDelimiter(String groupName, String streamName)
	{
		return groupName + KEY_DELIMITER + streamName;
	}
		
	synchronized public Stream getStream(CloudWatchConnector cwClient, String groupName, String streamName)
		throws LogException, ResiliencyException
	{	
		String key = getKeyDelimiter(groupName, streamName);
		
		// look for the stream in the map. if it's not there, then create it.
		Stream s = streamMap.get(key);
		
		if (s == null)
		{
			String nextToken = cwClient.createLogStream(groupName, streamName);
			
			s = new Stream(groupName, streamName, nextToken);			
			streamMap.put(key, s);
		}
		
		return s;
	}
	
	
	
}
