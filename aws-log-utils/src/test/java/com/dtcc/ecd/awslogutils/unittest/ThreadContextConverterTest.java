package com.dtcc.ecd.awslogutils.unittest;

import org.apache.logging.log4j.ThreadContext;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.dtcc.ecd.awslogutils.cloudwatch.LoggingUtils;
import com.dtcc.ecd.awslogutils.log4j_plugins.ThreadContextConverter;

public class ThreadContextConverterTest  {
	
	final String SYSID = "ECD";
	final String AppName = "ThreadContextConverterTest";	
	
	@Before
	//force JUnit to use the dev instance of datapower
	public void setupTestCases()
		throws Exception
	{
		
	}
	
	
	@Test
	public void test_format_ThreadContext_None()
	{	
		LoggingUtils.cleanLog4jTheadContext();
		
        ThreadContextConverter tcc = ThreadContextConverter.newInstance(null);
        
        StringBuilder convertedString = new StringBuilder("");
        
        tcc.format(null, convertedString);
                
        
        if(convertedString.length() > 0)
        	fail ("expected an empty string, received: " + convertedString);
	}
	
	
	@Test
	public void test_format_ThreadContext_OneItem()
	{		
		LoggingUtils.cleanLog4jTheadContext();
		
		ThreadContext.put("TID", "1234");
        
        ThreadContextConverter tcc = ThreadContextConverter.newInstance(null);
        
        StringBuilder convertedString = new StringBuilder("");
        
        tcc.format(null, convertedString);
                
        
        if(!convertedString.toString().equals("TID=1234"))
        	fail ("Unexpected conversion. expecting 'TID=1234', received '" + convertedString + "'");
	}
	
	@Test
	public void test_format_ThreadContext_Two()
	{	
		LoggingUtils.cleanLog4jTheadContext();
		
		ThreadContext.put("TID", "1234");
        ThreadContext.put("request-id", "some id");
        
        ThreadContextConverter tcc = ThreadContextConverter.newInstance(null);
        
        StringBuilder convertedString = new StringBuilder("");
        
        tcc.format(null, convertedString);
                
        
        if(!convertedString.toString().equals("request-id='some id' TID=1234"))
        	fail ("Unexpected conversion. expecting 'request-id='some id' TID=1234', received '" + convertedString + "'");
	}
	
}
