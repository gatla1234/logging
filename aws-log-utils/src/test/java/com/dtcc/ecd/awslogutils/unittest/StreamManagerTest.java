package com.dtcc.ecd.awslogutils.unittest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.dtcc.ecd.awsutils.auth.AWSAuthUtils;
import com.dtcc.ecd.awsutils.log.AWSAuthSimpleLogger;
import com.dtcc.ecd.awslogutils.DefaultConnector;
import com.dtcc.ecd.awslogutils.cloudwatch.Stream;
import com.dtcc.ecd.awslogutils.cloudwatch.StreamManager;
import com.dtcc.ecd.awslogutils.exception.LogException;
import com.dtcc.ecd.awslogutils.exception.ResiliencyException;

public class StreamManagerTest  {
	
	DefaultConnector cwClient;
	final String SYSID = "ECD";
	final String AppName = "StreamManagerTest";
	
	final String testLogGroup = String.format("/DTCC/AD-SOFTWARE/%s/%s/", SYSID, AppName);
	
	@Before
	//force JUnit to use the dev instance of datapower
	public void setupTestCases()
		throws Exception
	{
		AWSAuthUtils awsAuthUtils = new AWSAuthUtils(new AWSAuthSimpleLogger(true));		   		 
		
		if (awsAuthUtils.isUseProxy()) 
        {	            	             
           cwClient = new DefaultConnector(awsAuthUtils.getCredentials(), awsAuthUtils.getClientConfiguration());	             
        } 
        else 
        {
      	   cwClient = new DefaultConnector(awsAuthUtils.getCredentials(), null);
        }
		
		cwClient.deleteLogGroup(testLogGroup);
	}
	
	//
	// positive test cases
	//
	
	@Test
	// Tests that getting a new stream and publishing to it, will generate the appropriate
	// artifacts in CloudWatch
	public void test_get_new_stream_and_publish_event() 
	{		
		String testStreamName = "test-stream-name";
		Map<String, Stream> testMap =  new HashMap<String, Stream>();
		StreamManager sm = new StreamManager(testMap);
		
		try {
			
			cwClient.deleteLogGroup(testLogGroup);
			
			Stream s = sm.getStream(cwClient, testLogGroup, testStreamName);
			
			s.publishEvent(cwClient, "test event");
			
			if (!cwClient.logStreamExists(testLogGroup, testStreamName) )
				fail("publishing to new stream did not generate appropriate log groups or streams");
			
			//check that there is only 1 item in the map
			if (testMap.size() != 1)
				fail("Stream Map does not contain the expected number of arguments");
									
		} catch (LogException | ResiliencyException e) {
			fail(e.getMessage());
		}		
	}
	
	@Test
	// Tests that getting a new stream and publishing to it, will generate the appropriate
	// artifacts in CloudWatch
	public void test_get_new_stream() 
	{		
		String testStreamName = "test-stream-name";
		Map<String, Stream> testMap =  new HashMap<String, Stream>();
		StreamManager sm = new StreamManager(testMap);
		
		try {
			
			cwClient.deleteLogGroup(testLogGroup);
			
			sm.getStream(cwClient, testLogGroup, testStreamName);			
			
			if (!cwClient.logStreamExists(testLogGroup, testStreamName) )
				fail("publishing to new stream did not generate appropriate log groups or streams");
			
			//check that there is only 1 item in the map
			if (testMap.size() != 1)
				fail("Stream Map does not contain the expected number of arguments");

								
		} catch (LogException | ResiliencyException e) {
			fail(e.getMessage());
		}		
	}
	
	
	//
	// negative test cases
	//
	
	@Test
	// Tests that if a stream is deleted by an outsite entity, the logger can recreate it
	// on the fly.
	public void test_write_to_deleted_group()
	{				
		Map<String, Stream> testMap =  new HashMap<String, Stream>();
				
		String deletedStream = "deleted-stream";			
		StreamManager sm = new StreamManager(testMap);		
				
		//
		// stage the test create a stream, and then physically delete it.
		//
		try {
			
			cwClient.deleteLogGroup(testLogGroup);
			
			Stream s = sm.getStream(cwClient, testLogGroup, deletedStream);			
			s.publishEvent(cwClient, "this event, containing stream and grouo will be deleted");
			
			cwClient.deleteLogGroup(testLogGroup);
										
		} catch (LogException | ResiliencyException e) {
			fail(e.getMessage());
		}		
		
		//
		// Write to a stream that does not exist
		//
		try {			
			Stream s = sm.getStream(cwClient, testLogGroup, deletedStream);
			
			s.publishEvent(cwClient, "Client will recover from this error");			
			
		} catch (LogException | ResiliencyException e) {
			fail("was not able to recover from a deleted stream");
		}		
	}
	
	@Test
	// Tests that if a stream is deleted by an outside entity, the logger can recreate it
	// on the fly.
	public void test_write_to_deleted_stream()
	{				
		Map<String, Stream> testMap =  new HashMap<String, Stream>();
		
		String deletedStream = "deleted-stream";			
		StreamManager sm = new StreamManager(testMap);		
				
		//
		// stage the test create a stream, and then physically delete it.
		//
		try {
			
			cwClient.deleteLogGroup(testLogGroup);
			
			Stream s = sm.getStream(cwClient, testLogGroup, deletedStream);			
			s.publishEvent(cwClient, "this event will be deleted by the JUNIT test");
			
			cwClient.deleteLogStream(testLogGroup, deletedStream);
										
		} catch (LogException | ResiliencyException e) {
			fail(e.getMessage());
		}		
		
		//
		// Write to a stream that does not exist
		//
		try {			
			Stream s = sm.getStream(cwClient, testLogGroup, deletedStream);
			
			s.publishEvent(cwClient, "client will recover from this error");			
			
		} catch (LogException | ResiliencyException e) {
			fail(e.getMessage());
		}		
	}
	
}
