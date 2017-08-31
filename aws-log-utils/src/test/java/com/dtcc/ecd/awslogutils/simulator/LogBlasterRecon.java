package com.dtcc.ecd.awslogutils.simulator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dtcc.ecd.awslogutils.DefaultConnector;
import com.dtcc.ecd.awslogutils.exception.LogException;
import com.dtcc.ecd.awslogutils.exception.ResiliencyException;
import com.dtcc.ecd.awsutils.auth.AWSAuthUtils;
import com.dtcc.ecd.awsutils.log.AWSAuthSimpleLogger;

public class LogBlasterRecon {

	public static void main(String[] args) {
	
		String testLogGroup = "/DTCC/AD_SOFTWARE/ECD/LOGGINGSIMULATOR".toUpperCase();
		
		AWSAuthUtils awsAuthUtils = new AWSAuthUtils(new AWSAuthSimpleLogger(true));		   	   
		DefaultConnector cwConnector;
		
		Map<String, Integer> resultMap = new HashMap<String, Integer>();
		
		try 
		{           
			if (awsAuthUtils.isUseProxy()) 
			{	            	             
				cwConnector = new DefaultConnector(awsAuthUtils.getCredentials(), awsAuthUtils.getClientConfiguration());	             
			} 
			else 
			{
				cwConnector = new DefaultConnector(awsAuthUtils.getCredentials(), null);
			}
		} 
		catch (Exception e) {
			System.out.println("Could not create CloudWatch client, because: " + e);
			return;
		}
		
		System.out.println("Reconciling results");
		List<String> cwEvents = null;
		try {
			cwEvents = cwConnector.getLogGroupEvents(testLogGroup);
		} catch (LogException | ResiliencyException e) {
			System.out.println("There reading evens from log group, because: " + e);
		}
		
		System.out.println("A Total of " + cwEvents.size() + " were written to CloudWatch");
		
		
		for (String eventText : cwEvents)
		{
			// note that how I extract the publisher Name depends on the actual format of the string
			int beginIndex = "[INFO] ".length();
			int endIndex = beginIndex + "Publisher xxxxx".length();
			
			String publisherName = eventText.substring(beginIndex, endIndex);
			
			if (resultMap.containsKey(publisherName))
			{
				Integer cnt = resultMap.get(publisherName);
				cnt++;
				resultMap.put(publisherName, cnt);				
			}
			else
				resultMap.put(publisherName, 1);
		}
		
		
		for (String pubName : resultMap.keySet())
			System.out.println(pubName + " - " + resultMap.get(pubName));
		
		
	}

}
