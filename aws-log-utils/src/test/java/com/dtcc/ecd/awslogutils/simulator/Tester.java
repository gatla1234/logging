package com.dtcc.ecd.awslogutils.simulator;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.dtcc.ecd.awsutils.auth.AWSAuthUtils;
import com.dtcc.ecd.awsutils.log.AWSAuthSimpleLogger;
import com.dtcc.ecd.awslogutils.BuiltinLoggers;
import com.dtcc.ecd.awslogutils.DefaultConnector;
import com.dtcc.ecd.awslogutils.LoggerFactory;
import com.dtcc.ecd.awslogutils.cloudwatch.LoggingUtils;
import com.dtcc.ecd.awslogutils.exception.LogException;

public class Tester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
	   System.out.println(LoggingUtils.getTimeStamp());
	   		   		   
	   AWSAuthUtils awsAuthUtils = new AWSAuthUtils(new AWSAuthSimpleLogger(true));		   
	   
	   DefaultConnector cwConnector = null;
	   		   
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

          Logger logger = null;
                    
          //
          // Configure the logger. this only needs to be done once.
          //          
          LoggerFactory.configureLog4j("ECS", 				//SYSID
        		  					   "CARS-Application",  //App Name 
        		  					   cwConnector, 		// Cloudwatch Connector	
        		  					   false,               // Disable AWS request debug info
        		  					   false);              // Disable wire level tracing info
          
          
          //
          // Add some items to the threadContext. They will show up in the default log
          // The log4j ThreadContext is very useful and baked right into
          //
          ThreadContext.put("TID", "1234");
          ThreadContext.put("UserID", "mark");
          ThreadContext.put("request-id", "some id");
          	
          //
          // Get the default logger and log some random stuff
          //
          logger = LoggerFactory.getEC2Logger(BuiltinLoggers.SIMPLE_LOGGER);
      
          for (int i = 0; i < 10; i++)
        	  logger.info("Ground application event: " + i);
          
          
          final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
        		  .withCredentials(new AWSStaticCredentialsProvider(awsAuthUtils.getCredentials()))
        		  .withClientConfiguration(awsAuthUtils.getClientConfiguration()).build();
          
          logger.info(s3.listBuckets().toString());
          
          //
          // Log and Exception
          //
          
          LogException le = new LogException("This exception will be logged");
          logger.error(le);
          
          //
          // Log an outage
          //
          Logger incidentLogger = LoggerFactory.getIncidentLogger();
          
          incidentLogger.info("this is an incident");
          
	      
	   } catch (LogException e) {
	      e.printStackTrace();
	   }
	   
	   System.out.println("test end");

	}

}
