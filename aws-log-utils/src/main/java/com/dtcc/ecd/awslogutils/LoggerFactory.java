package com.dtcc.ecd.awslogutils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.dtcc.ecd.awslogutils.cloudwatch.CloudWatchConnector;
import com.dtcc.ecd.awslogutils.cloudwatch.LogConstants;
import com.dtcc.ecd.awslogutils.cloudwatch.LoggingUtils;
import com.dtcc.ecd.awslogutils.cloudwatch.Validator;
import com.dtcc.ecd.awslogutils.exception.LogException;
import com.dtcc.ecd.awslogutils.log4j_plugins.CloudWatchBulkAppender;
import com.dtcc.ecd.awslogutils.log4j_plugins.CloudWatchIncidentAppender;


public class LoggerFactory {
	
	// default configuration that is shipped with this jar	
	private static String SYSID;
	private static String applicationName;
	private static String instanceIdenitifier;
	private static String incidentOrigin;
	
	private static String vmUUID;
		
	private static boolean cwConfigured = false;

	public static void configureLog4j(String sysID, 
									   String appName, 
									   CloudWatchConnector cwClientInterface,
									   boolean enableRequestLogging,
									   boolean enableWireLogging)
		throws LogException
	{
	   if (configured())
	   {
		   return;
	   }
	   
	   //ThreadContext Configuration.
	   // leave off for now as it does not work with Executor Thread pools
	   //System.setProperty("isThreadContextMapInheritable" , "true");
	   
	   vmUUID = LoggingUtils.getUUID();
	   
	   SYSID = sysID;
	   applicationName = appName;
	   
	   Validator.validateSYSID(SYSID);
	   Validator.validateCloudWatchComponentString(applicationName);
	   	   
	   LoggerContext context = null;
	   
	   // unless an external log4j configuration file has been specified, use the internal one
	   
	   String log4jConfiguration = System.getProperty("log4j.configurationFile");
	   boolean useDefaultConfig = (log4jConfiguration == null); 
	   
	   if (useDefaultConfig)
		   context = Configurator.initialize(null, LogConstants.LOG4J_INTERNAL_CONFIG_XML);
	   else
		   context = LoggerContext.getContext(false);
	   
	   Logger consoleLogger = LogManager.getLogger(BuiltinLoggers.CONSOLE_LOGGER);
	   
	   if (useDefaultConfig)
		   consoleLogger.debug("Initialized log4j using internal configuration");
	   else
		   consoleLogger.debug("Initialized log4j using externally supplied configuration: " + log4jConfiguration);
	   
	   instanceIdenitifier = LoggingUtils.getHostName(); 
	   
	   // get the EC2 instance name. ideally this is the instance ID
	   // but if that fails settle for the host name
	   consoleLogger.debug("Determining instance name");
	   try
	   {
		   instanceIdenitifier = LoggingUtils.getEC2InstanceName();
	   }
	   catch(LogException le)
	   {
		   instanceIdenitifier = LoggingUtils.getHostName(); 
	   }
	   
	   incidentOrigin = instanceIdenitifier + ":" + applicationName;
	   
		
	   Configuration config = context.getConfiguration();
	   
	   consoleLogger.debug("Configuring custom appenders");
	   
	   CloudWatchBulkAppender cwAppender = (CloudWatchBulkAppender) config.getAppender(LogConstants.LOG4J_CLOUDWATCH_APPENDER_NAME);
	   CloudWatchBulkAppender cwSplunkAppender = (CloudWatchBulkAppender) config.getAppender(LogConstants.LOG4J_CLOUDWATCH_SPLUNK_APPENDER_NAME);	   	   
	   CloudWatchIncidentAppender incidentAppender = (CloudWatchIncidentAppender) config.getAppender(LogConstants.LOG4J_CLOUDWATCH_INCIDENT_APPENDER_NAME);	   
	   
	   if (enableRequestLogging)
	   {
		   LoggerConfig logConfig = config.getLoggerConfig("com.amazonaws.request");
		   logConfig.setLevel(Level.DEBUG);
	   }
	   
	   if (enableWireLogging)
	   {
		   LoggerConfig logConfig = config.getLoggerConfig("org.apache.http.wire");
		   logConfig.setLevel(Level.DEBUG);
	   }
	   
	   if (enableRequestLogging || enableWireLogging)
		   context.updateLoggers();
	   
	   configureBulkAppender(cwAppender, cwClientInterface);
	   configureBulkAppender(cwSplunkAppender, cwClientInterface);
	   
	   configureIncidentAppender(incidentAppender, cwClientInterface);
	 	   
	   consoleLogger.debug("Log4j custom configuration is completed");
	   
	   cwConfigured = true;
	}
	
	private static void configureBulkAppender(CloudWatchBulkAppender appender, CloudWatchConnector cwClientInterface)
			throws LogException
	{
	   if (appender == null)
			throw new LogException(String.format("Could not reconfigure CloudWatchAppender [%s] located in [%s]", 
				LogConstants.LOG4J_CLOUDWATCH_APPENDER_NAME, LogConstants.LOG4J_INTERNAL_CONFIG_XML));
		  	   
	   	   
	   appender.customInit(cwClientInterface,
			   vmUUID,
			   SYSID, 
			   applicationName);
	}
	
	
	private static void configureIncidentAppender(CloudWatchIncidentAppender appender, CloudWatchConnector cwConnector)
		throws LogException
	{
		if (appender == null)
			throw new LogException(String.format("Could not reconfigure CloudWatchAppender [%s] located in [%s]", 
				LogConstants.LOG4J_CLOUDWATCH_INCIDENT_APPENDER_NAME, LogConstants.LOG4J_INTERNAL_CONFIG_XML));		  
	   	   
		appender.customInit(cwConnector,
			   SYSID, 
			   applicationName);
	}
	
	
	
	private static boolean configured()
	{
		return cwConfigured;
	}
	

	public static Logger getEC2Logger(String logName)
		throws LogException
	{
	   if (!configured())
		   throw new LogException("Logger Factory not initialized by calling configureLog4j method.");
	   
	   // Stick some default values in the Thread Context
	   
	   ThreadContext.put("_APPLICATION_NAME_", applicationName);
	   ThreadContext.put("_SYSID_", SYSID);
	   ThreadContext.put("_INSTANCE_IDENTIFIER_", instanceIdenitifier);
	   ThreadContext.put("_VM_UUID_", vmUUID);
	   
	   if (logName == null || logName.isEmpty())
		   logName = BuiltinLoggers.SIMPLE_LOGGER;
	   
	   
	   return LogManager.getLogger(logName);
	   
	}
	
	
	public static Logger getIncidentLogger()
			throws LogException
	{
	   if (!configured())
		   throw new LogException("Logger Factory not initialized by calling configureLog4j method.");
	   
	   //TODO: this is somewhat unreliable. It's better to enhance the pattern layout
	   ThreadContext.put(LogConstants.LOG4J_THREADCONTEXT_SYSID, SYSID);
	   ThreadContext.put(LogConstants.LOG4J_THREADCONTEXT_APPLICATION, applicationName);
	   ThreadContext.put(LogConstants.LOG4J_THREADCONTEXT_INCIDENT_ORIGIN, incidentOrigin);
	   	   
	   return LogManager.getLogger(BuiltinLoggers.INCIDENT_LOGGER);
	   
	}
	

	public static String getSYSID() {
		return SYSID;
	}


	public static String getApplicationName() {
		return applicationName;
	}


	public static String getInstanceIdenitifier() {
		return instanceIdenitifier;
	}

}
